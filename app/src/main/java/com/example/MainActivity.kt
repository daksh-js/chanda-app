package com.example

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppSettings
import com.example.data.ChandaRepository
import com.example.data.Donor
import com.example.data.ReceiptRecord
import com.example.data.UiState
import com.example.ui.screens.AddDonationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DonorsScreen
import com.example.ui.screens.ReceiptPreviewDialog
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChandaViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        MyApplicationTheme(darkTheme = true) {
            AdminDeviceAuthorizationGate(
                onAuthSuccess = { /* authStateFlow updates automatically */ }
            )
        }
    } else {
        AuthenticatedChandaApp(user = user)
    }
}

@Composable
fun AdminDeviceAuthorizationGate(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val installationId = remember(context) { ChandaViewModel.getOrCreateInstallationId(context) }
    val deviceModel = remember { ChandaViewModel.getDeviceModelDescription() }
    var authError by remember { mutableStateOf<String?>(null) }

    // Attempt silent auto-authorization on launch so previously authorized devices open directly
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on Device Enrollment Gate */ },
            scope = scope
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Akhada Vault Security",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "PRIVATE AKHADA ADMIN-ONLY LEDGER • MAX 6 DEVICES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "CHANDA APP",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Strict Private Device Authorization\nNo public registration • Cash-only Akhada ledger.\nVerify your administrator Google identity to enroll this device ($deviceModel).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "DEVICE INSTALLATION IDENTITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = installationId,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!authError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    GoogleSignInButton(
                        onAuthSuccess = onAuthSuccess,
                        onAuthError = { msg -> authError = msg }
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false }
            )
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthenticatedChandaApp(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val installationId = remember(context) { ChandaViewModel.getOrCreateInstallationId(context) }
    val deviceModelName = remember { ChandaViewModel.getDeviceModelDescription() }

    val viewModel: ChandaViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChandaViewModel(
                    repository = ChandaRepository(db),
                    currentUserId = user.uid,
                    currentUserEmail = user.email,
                    currentUserDisplayName = user.displayName,
                    installationId = installationId,
                    deviceModelName = deviceModelName
                )
            }
        }
    )

    val donorsState by viewModel.donorsState.collectAsStateWithLifecycle()
    val donationsState by viewModel.donationsState.collectAsStateWithLifecycle()
    val receiptsState by viewModel.receiptsState.collectAsStateWithLifecycle()
    val devicesState by viewModel.devicesState.collectAsStateWithLifecycle()
    val activityLogsState by viewModel.activityLogsState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val latestGeneratedReceipt by viewModel.latestGeneratedReceipt.collectAsStateWithLifecycle()

    val donors = (donorsState as? UiState.Success)?.data ?: emptyList()
    val donations = (donationsState as? UiState.Success)?.data ?: emptyList()
    val receipts = (receiptsState as? UiState.Success)?.data ?: emptyList()
    val devices = (devicesState as? UiState.Success)?.data ?: emptyList()
    val activityLogs = (activityLogsState as? UiState.Success)?.data ?: emptyList()
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings(userId = user.uid)

    val currentDeviceRecord = viewModel.currentDeviceRecord(devices)
    val currentAdminName = viewModel.currentAdminName(devices, settings)
    val isOnline = rememberConnectivityState(context)

    var currentTab by remember { mutableStateOf("HOME") } // HOME, DONORS, ADD_DONATION, REPORTS, SETTINGS
    var openAddDonorDialogOnLaunch by remember { mutableStateOf(false) }
    var preselectedDonorForDonation by remember { mutableStateOf<Donor?>(null) }
    var viewingReceiptModal by remember { mutableStateOf<ReceiptRecord?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    // Back button returns to Home Dashboard when on secondary tabs
    BackHandler(enabled = currentTab != "HOME") {
        currentTab = "HOME"
    }

    MyApplicationTheme(darkTheme = settings.darkMode) {
        // Security Check: If this device was explicitly REVOKED by Owner Admin, block access
        if (currentDeviceRecord?.status == "REVOKED") {
            RevokedDeviceBlockScreen(
                installationId = installationId,
                onSignOut = {
                    signOut(context, credentialManager, {}, scope)
                }
            )
            return@MyApplicationTheme
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ChandaBottomNavigationBar(
                    currentTab = currentTab,
                    onSelectTab = { tab -> currentTab = tab }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Offline Read & Pending Sync Banner
                if (!isOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline Mode",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Offline Mode: Reading cached records. Pending entries will sync automatically when online.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        "HOME" -> DashboardScreen(
                            donors = donors,
                            donations = donations,
                            receipts = receipts,
                            devices = devices,
                            activityLogs = activityLogs,
                            settings = settings,
                            currentAdminName = currentAdminName,
                            isOnline = isOnline,
                            onNavigateToAddDonation = {
                                preselectedDonorForDonation = donors.firstOrNull()
                                currentTab = "ADD_DONATION"
                            },
                            onNavigateToDonors = { openAddSheet ->
                                openAddDonorDialogOnLaunch = openAddSheet
                                currentTab = "DONORS"
                            },
                            onNavigateToReports = { currentTab = "REPORTS" },
                            onNavigateToSettings = { currentTab = "SETTINGS" },
                            onSelectReceipt = { viewingReceiptModal = it },
                            onToggleLanguage = {
                                val nextLang = when (settings.language) {
                                    "BILINGUAL" -> "HI"
                                    "HI" -> "EN"
                                    else -> "BILINGUAL"
                                }
                                viewModel.saveSettings(settings.copy(language = nextLang), currentAdminName)
                            }
                        )

                        "DONORS" -> DonorsScreen(
                            donors = donors,
                            donations = donations,
                            settings = settings,
                            currentAdminName = currentAdminName,
                            canAddDonor = currentDeviceRecord?.canAddDonor ?: true,
                            canEditDonor = currentDeviceRecord?.canEditDonor ?: true,
                            canDeleteDonor = currentDeviceRecord?.canDeleteDonor ?: true,
                            openAddDialogInitially = openAddDonorDialogOnLaunch,
                            onConsumedInitialAddDialog = { openAddDonorDialogOnLaunch = false },
                            onAddDonor = { fullName, father, mobile, whatsapp, altMobile, address, village, city, district, state, pin, occupation, birthday, notes, photoUri, isFav, consent ->
                                viewModel.addDonor(
                                    existingDonors = donors,
                                    fullName = fullName,
                                    fatherName = father,
                                    mobileNumber = mobile,
                                    whatsappNumber = whatsapp,
                                    alternateMobile = altMobile,
                                    address = address,
                                    village = village,
                                    city = city,
                                    district = district,
                                    state = state,
                                    pinCode = pin,
                                    occupation = occupation,
                                    birthday = birthday,
                                    notes = notes,
                                    photoUri = photoUri,
                                    addedByAdmin = currentAdminName,
                                    isFavorite = isFav,
                                    consentGiven = consent
                                )
                            },
                            onUpdateDonor = { updated ->
                                viewModel.updateDonor(updated, currentAdminName)
                            },
                            onDeleteDonor = { target ->
                                viewModel.deleteDonor(target, currentAdminName)
                            },
                            onToggleFavorite = { target ->
                                viewModel.toggleDonorFavorite(target, currentAdminName)
                            },
                            onDonateForDonor = { target ->
                                preselectedDonorForDonation = target
                                currentTab = "ADD_DONATION"
                            }
                        )

                        "ADD_DONATION" -> AddDonationScreen(
                            donors = donors,
                            preselectedDonor = preselectedDonorForDonation,
                            settings = settings,
                            currentAdminName = currentAdminName,
                            isOnline = isOnline,
                            latestGeneratedReceipt = latestGeneratedReceipt,
                            isDuplicateCheck = { donorId, amount ->
                                viewModel.isPotentialDuplicateSubmission(donorId, amount)
                            },
                            onRecordCashDonation = { donor, amount, date, purpose, admin, notes ->
                                viewModel.recordCashDonation(
                                    donor = donor,
                                    amount = amount,
                                    donationDate = date,
                                    purpose = purpose,
                                    receivedByAdmin = admin,
                                    notes = notes,
                                    settings = settings
                                )
                            },
                            onDismissReceiptModal = { viewModel.clearLatestReceipt() },
                            onNavigateToAddDonor = {
                                openAddDonorDialogOnLaunch = true
                                currentTab = "DONORS"
                            }
                        )

                        "REPORTS" -> ReportsScreen(
                            donors = donors,
                            donations = donations,
                            receipts = receipts,
                            settings = settings,
                            canExportData = currentDeviceRecord?.canExportData ?: true,
                            onSelectReceipt = { viewingReceiptModal = it },
                            onCorrectOrReverseDonation = { donation, newAmount, newPurpose, newStatus, correctionNote ->
                                val linkedDonor = donors.firstOrNull { it.id == donation.donorId }
                                viewModel.correctOrReverseDonation(
                                    donation = donation,
                                    donor = linkedDonor,
                                    newAmount = newAmount,
                                    newPurpose = newPurpose,
                                    newStatus = newStatus,
                                    correctionNote = correctionNote,
                                    adminName = currentAdminName
                                )
                            }
                        )

                        "SETTINGS" -> SettingsScreen(
                            settings = settings,
                            devices = devices,
                            activityLogs = activityLogs,
                            donors = donors,
                            donations = donations,
                            currentInstallationId = installationId,
                            currentDeviceModel = deviceModelName,
                            onSaveSettings = { updated ->
                                viewModel.saveSettings(updated, currentAdminName)
                            },
                            onAuthorizeOrUpdateDevice = { target, instId, devName, admName, role, status, canAdd, canEdit, canDel, canDonate, canRep, canExp ->
                                viewModel.authorizeOrUpdateDevice(
                                    existingDevices = devices,
                                    targetDevice = target,
                                    installationIdInput = instId,
                                    deviceNameInput = devName,
                                    adminNameInput = admName,
                                    roleInput = role,
                                    statusInput = status,
                                    canAddDonor = canAdd,
                                    canEditDonor = canEdit,
                                    canDeleteDonor = canDel,
                                    canRecordDonation = canDonate,
                                    canViewReports = canRep,
                                    canExportData = canExp
                                )
                            },
                            onImportCsvDonors = { csv ->
                                viewModel.importCsvDonors(csv, donors, currentAdminName)
                            },
                            onSignOutDevice = {
                                signOut(context, credentialManager, {}, scope)
                            }
                        )
                    }
                }
            }
        }

        viewingReceiptModal?.let { receipt ->
            ReceiptPreviewDialog(
                receipt = receipt,
                onDismiss = { viewingReceiptModal = null }
            )
        }
    }
}

@Composable
private fun ChandaBottomNavigationBar(
    currentTab: String,
    onSelectTab: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.GridView,
                    label = "Home",
                    selected = currentTab == "HOME",
                    tag = "nav_home",
                    onClick = { onSelectTab("HOME") }
                )
                BottomNavItem(
                    icon = Icons.Default.Group,
                    label = "Donors",
                    selected = currentTab == "DONORS",
                    tag = "nav_donors",
                    onClick = { onSelectTab("DONORS") }
                )

                // Prominent Center Add Donation Button (Matched to Reference HTML)
                Box(
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onSelectTab("ADD_DONATION") }
                        .testTag("nav_add_donation"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Cash Donation",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                BottomNavItem(
                    icon = Icons.Default.ReceiptLong,
                    label = "Reports",
                    selected = currentTab == "REPORTS",
                    tag = "nav_reports",
                    onClick = { onSelectTab("REPORTS") }
                )
                BottomNavItem(
                    icon = Icons.Default.Tune,
                    label = "Settings",
                    selected = currentTab == "SETTINGS",
                    tag = "nav_settings",
                    onClick = { onSelectTab("SETTINGS") }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun RevokedDeviceBlockScreen(
    installationId: String,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Revoked",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Device Access Revoked",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Installation ID ($installationId) has been revoked by the Akhada Owner Admin. Access to private donor and cash donation records is blocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onSignOut) {
                    Text("Disconnect Session")
                }
            }
        }
    }
}

@Composable
private fun rememberConnectivityState(context: Context): Boolean {
    var isConnected by remember { mutableStateOf(true) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected = true
            }

            override fun onLost(network: Network) {
                isConnected = false
            }
        }
        try {
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != false
            cm?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            isConnected = true
        }
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
    return isConnected
}
