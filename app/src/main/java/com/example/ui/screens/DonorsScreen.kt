package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AppSettings
import com.example.data.Donation
import com.example.data.Donor
import com.example.data.IndianCurrencyFormatter
import com.example.util.PdfAndExportHelper
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun DonorsScreen(
    donors: List<Donor>,
    donations: List<Donation>,
    settings: AppSettings,
    currentAdminName: String,
    canAddDonor: Boolean,
    canEditDonor: Boolean,
    canDeleteDonor: Boolean,
    openAddDialogInitially: Boolean = false,
    onConsumedInitialAddDialog: () -> Unit = {},
    onAddDonor: (
        fullName: String,
        fatherName: String,
        mobile: String,
        whatsapp: String,
        altMobile: String?,
        address: String,
        village: String,
        city: String,
        district: String,
        state: String,
        pinCode: String,
        occupation: String?,
        birthday: String?,
        notes: String?,
        photoUri: String?,
        isFavorite: Boolean,
        consentGiven: Boolean
    ) -> Unit,
    onUpdateDonor: (Donor) -> Unit,
    onDeleteDonor: (Donor) -> Unit,
    onToggleFavorite: (Donor) -> Unit,
    onDonateForDonor: (Donor) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, HIGHEST, LOWEST, RECENT, FAVORITES
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingDonor by remember { mutableStateOf<Donor?>(null) }
    var viewingDonorProfile by remember { mutableStateOf<Donor?>(null) }
    var whatsappConfirmTarget by remember { mutableStateOf<Pair<Donor, String>?>(null) }
    val isHindi = settings.language == "HI"

    LaunchedEffect(openAddDialogInitially) {
        if (openAddDialogInitially && canAddDonor) {
            editingDonor = null
            showAddOrEditDialog = true
            onConsumedInitialAddDialog()
        }
    }

    val filteredDonors = remember(donors, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase(Locale.US)
        val matched = if (q.isEmpty()) {
            donors
        } else {
            donors.filter { d ->
                d.fullName.lowercase(Locale.US).contains(q) ||
                    d.mobileNumber.contains(q) ||
                    d.whatsappNumber.contains(q) ||
                    d.donorCode.lowercase(Locale.US).contains(q) ||
                    d.village.lowercase(Locale.US).contains(q) ||
                    d.city.lowercase(Locale.US).contains(q) ||
                    d.district.lowercase(Locale.US).contains(q)
            }
        }
        when (selectedFilter) {
            "HIGHEST" -> matched.sortedByDescending { it.lifetimeDonation }
            "LOWEST" -> matched.sortedBy { it.lifetimeDonation }
            "RECENT" -> matched.sortedByDescending { it.createdAt?.seconds ?: 0L }
            "OLD" -> matched.sortedBy { it.createdAt?.seconds ?: 0L }
            "FAVORITES" -> matched.filter { it.isFavorite }
            else -> matched
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (canAddDonor) {
                FloatingActionButton(
                    onClick = {
                        editingDonor = null
                        showAddOrEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(bottom = 72.dp)
                        .testTag("add_donor_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Donor")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("donors_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isHindi) "दानदाता निर्देशिका (Donors)" else "Akhada Donors Directory",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${donors.size} Registered Patrons • Unique IDs (DONOR-XXXX)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canAddDonor) {
                    Button(
                        onClick = {
                            editingDonor = null
                            showAddOrEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("new_donor_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "नया दाता" else "Add Donor")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = {
                    Text("Search Name, Mobile, WhatsApp, DONOR-ID, Village, District...")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donor_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chips = listOf(
                    "ALL" to "All (${donors.size})",
                    "HIGHEST" to "Highest Donors",
                    "LOWEST" to "Lowest Donors",
                    "RECENT" to "Recent",
                    "OLD" to "Oldest",
                    "FAVORITES" to "Favorites ★"
                )
                items(chips) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredDonors.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "कोई दानदाता नहीं मिला" else "No Donors Found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap 'Add Donor' to register a patron with automatic unique ID (e.g. DONOR-0001), photo, and address.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredDonors, key = { it.id }) { donor ->
                        DonorProfileCard(
                            donor = donor,
                            onViewProfile = { viewingDonorProfile = donor },
                            onToggleFavorite = { onToggleFavorite(donor) },
                            onCall = { PdfAndExportHelper.dialPhoneNumber(context, donor.mobileNumber) },
                            onWhatsApp = {
                                whatsappConfirmTarget = donor to settings.thankYouTemplateHi
                            },
                            onDonate = { onDonateForDonor(donor) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Donor Modal Dialog
    if (showAddOrEditDialog) {
        val existing = editingDonor
        AddOrEditDonorDialog(
            existingDonor = existing,
            allDonors = donors,
            nextCodePreview = existing?.donorCode ?: String.format(Locale.US, "DONOR-%04d", donors.size + 1),
            onDismiss = { showAddOrEditDialog = false },
            onSaveNew = { fullName, father, mobile, whatsapp, altMobile, address, village, city, district, state, pin, occupation, birthday, notes, photoUri, isFav, consent ->
                onAddDonor(
                    fullName, father, mobile, whatsapp, altMobile,
                    address, village, city, district, state, pin,
                    occupation, birthday, notes, photoUri, isFav, consent
                )
                showAddOrEditDialog = false
            },
            onSaveEdited = { updated ->
                onUpdateDonor(updated)
                showAddOrEditDialog = false
            }
        )
    }

    // Full Donor Profile & History Dialog
    viewingDonorProfile?.let { donor ->
        val donorHistory = donations.filter { it.donorId == donor.id || it.donorCode == donor.donorCode }
        DonorDetailDialog(
            donor = donor,
            donorDonations = donorHistory,
            canEdit = canEditDonor,
            canDelete = canDeleteDonor,
            onDismiss = { viewingDonorProfile = null },
            onEdit = {
                viewingDonorProfile = null
                editingDonor = donor
                showAddOrEditDialog = true
            },
            onDelete = {
                viewingDonorProfile = null
                onDeleteDonor(donor)
            },
            onSendReminder = {
                viewingDonorProfile = null
                whatsappConfirmTarget = donor to settings.reminderTemplateHi
            },
            onDonateNow = {
                viewingDonorProfile = null
                onDonateForDonor(donor)
            }
        )
    }

    // Manual Approval Dialog before sending WhatsApp message
    whatsappConfirmTarget?.let { (donor, initialTemplate) ->
        var customMessage by remember(donor, initialTemplate) {
            mutableStateOf("नमस्कार ${donor.fullName} जी (${donor.donorCode}),\n$initialTemplate\n- ${settings.akhadaName}")
        }
        AlertDialog(
            onDismissRequest = { whatsappConfirmTarget = null },
            title = { Text("Confirm WhatsApp Message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Recipient: ${donor.fullName} (+91 ${donor.whatsappNumber.ifBlank { donor.mobileNumber }})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Approve / Customize Message") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phone = donor.whatsappNumber.ifBlank { donor.mobileNumber }
                        whatsappConfirmTarget = null
                        PdfAndExportHelper.openWhatsAppMessage(context, phone, customMessage)
                    }
                ) {
                    Text("Approve & Open WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { whatsappConfirmTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DonorProfileCard(
    donor: Donor,
    onViewProfile: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDonate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() }
            .testTag("donor_card_${donor.donorCode}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!donor.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = donor.photoUri,
                                contentDescription = donor.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = donor.fullName.take(2).uppercase(Locale.US),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = donor.donorCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (donor.fatherName.isNotBlank()) {
                                Text(
                                    text = "S/o ${donor.fatherName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = donor.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${donor.village.ifBlank { "Village" }}, ${donor.district.ifBlank { donor.city }} (${donor.state})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (donor.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite Donor",
                        tint = if (donor.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lifetime Cash Donated (${donor.donationCount} entries)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyFormatter.formatInr(donor.lifetimeDonation),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Donor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "WhatsApp Donor",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Button(
                        onClick = onDonate,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("+ Cash", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditDonorDialog(
    existingDonor: Donor?,
    allDonors: List<Donor>,
    nextCodePreview: String,
    onDismiss: () -> Unit,
    onSaveNew: (
        fullName: String,
        fatherName: String,
        mobile: String,
        whatsapp: String,
        altMobile: String?,
        address: String,
        village: String,
        city: String,
        district: String,
        state: String,
        pinCode: String,
        occupation: String?,
        birthday: String?,
        notes: String?,
        photoUri: String?,
        isFavorite: Boolean,
        consentGiven: Boolean
    ) -> Unit,
    onSaveEdited: (Donor) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(existingDonor?.fullName ?: "") }
    var fatherName by remember { mutableStateOf(existingDonor?.fatherName ?: "") }
    var mobileNumber by remember { mutableStateOf(existingDonor?.mobileNumber ?: "") }
    var whatsappNumber by remember { mutableStateOf(existingDonor?.whatsappNumber ?: "") }
    var alternateMobile by remember { mutableStateOf(existingDonor?.alternateMobile ?: "") }
    var address by remember { mutableStateOf(existingDonor?.address ?: "") }
    var village by remember { mutableStateOf(existingDonor?.village ?: "") }
    var city by remember { mutableStateOf(existingDonor?.city ?: "") }
    var district by remember { mutableStateOf(existingDonor?.district ?: "") }
    var state by remember { mutableStateOf(existingDonor?.state ?: "Haryana") }
    var pinCode by remember { mutableStateOf(existingDonor?.pinCode ?: "") }
    var occupation by remember { mutableStateOf(existingDonor?.occupation ?: "") }
    var birthday by remember { mutableStateOf(existingDonor?.birthday ?: "") }
    var notes by remember { mutableStateOf(existingDonor?.notes ?: "") }
    var photoUri by remember { mutableStateOf(existingDonor?.photoUri) }
    var isFavorite by remember { mutableStateOf(existingDonor?.isFavorite ?: false) }
    var consentGiven by remember { mutableStateOf(existingDonor?.consentGiven ?: true) }

    val duplicateDonor = remember(mobileNumber, allDonors, existingDonor) {
        val clean = mobileNumber.trim()
        if (clean.length < 6) null
        else allDonors.firstOrNull { it.mobileNumber == clean && it.id != existingDonor?.id }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    // Camera Thumbnail Capture
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val dir = File(context.cacheDir, "photos").apply { mkdirs() }
            val file = File(dir, "donor_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            photoUri = file.toURI().toString()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraCaptureLauncher.launch(null)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingDonor == null) "Register Donor ($nextCodePreview)" else "Edit ${existingDonor.donorCode}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Upload / Capture Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (!photoUri.isNullOrBlank()) {
                    Text(
                        text = "✓ Profile photograph attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (duplicateDonor != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Duplicate Mobile",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Duplicate mobile number detected: already registered to ${duplicateDonor.fullName} (${duplicateDonor.donorCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name (पूरा नाम) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donor_fullname_input")
                )
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name (पिता का नाम)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = {
                        mobileNumber = it
                        if (whatsappNumber.isBlank()) whatsappNumber = it
                    },
                    label = { Text("Mobile Number (मोबाइल नंबर) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donor_mobile_input")
                )
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("WhatsApp Number (व्हाट्सएप नंबर)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = alternateMobile,
                    onValueChange = { alternateMobile = it },
                    label = { Text("Alternate Mobile (वैकल्पिक नंबर)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village (गाँव)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("PIN Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Residential Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = { Text("Occupation") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = birthday,
                        onValueChange = { birthday = it },
                        label = { Text("Birthday (DD/MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = consentGiven,
                        onCheckedChange = { consentGiven = it }
                    )
                    Text(
                        text = "Donor consent obtained to store photograph & contact details in Akhada vault",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && mobileNumber.length >= 5) {
                        if (existingDonor == null) {
                            onSaveNew(
                                fullName,
                                fatherName,
                                mobileNumber,
                                whatsappNumber.ifBlank { mobileNumber },
                                alternateMobile.takeIf { it.isNotBlank() },
                                address,
                                village,
                                city,
                                district,
                                state,
                                pinCode,
                                occupation.takeIf { it.isNotBlank() },
                                birthday.takeIf { it.isNotBlank() },
                                notes.takeIf { it.isNotBlank() },
                                photoUri,
                                isFavorite,
                                consentGiven
                            )
                        } else {
                            onSaveEdited(
                                existingDonor.copy(
                                    fullName = fullName,
                                    fatherName = fatherName,
                                    mobileNumber = mobileNumber,
                                    whatsappNumber = whatsappNumber.ifBlank { mobileNumber },
                                    alternateMobile = alternateMobile.takeIf { it.isNotBlank() },
                                    address = address,
                                    village = village,
                                    city = city,
                                    district = district,
                                    state = state,
                                    pinCode = pinCode,
                                    occupation = occupation.takeIf { it.isNotBlank() },
                                    birthday = birthday.takeIf { it.isNotBlank() },
                                    notes = notes.takeIf { it.isNotBlank() },
                                    photoUri = photoUri,
                                    isFavorite = isFavorite,
                                    consentGiven = consentGiven
                                )
                            )
                        }
                    }
                },
                enabled = fullName.isNotBlank() && mobileNumber.length >= 5 && consentGiven,
                modifier = Modifier.testTag("save_donor_confirm_button")
            ) {
                Text(if (existingDonor == null) "Save Donor" else "Update Donor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DonorDetailDialog(
    donor: Donor,
    donorDonations: List<Donation>,
    canEdit: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendReminder: () -> Unit,
    onDonateNow: () -> Unit
) {
    val qrBitmap = remember(donor.donorCode, donor.fullName) {
        PdfAndExportHelper.generateQrCodeBitmap(
            "CHANDA_DONOR|ID:${donor.donorCode}|NAME:${donor.fullName}|MOB:${donor.mobileNumber}",
            220
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(donor.donorCode, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(donor.fullName, style = MaterialTheme.typography.titleLarge)
                }
                Row {
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Donor")
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Donor", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Lifetime Cash Total: ${IndianCurrencyFormatter.formatInr(donor.lifetimeDonation)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Father's Name: ${donor.fatherName.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Mobile: ${donor.mobileNumber} | WhatsApp: ${donor.whatsappNumber}", style = MaterialTheme.typography.bodySmall)
                        Text("Address: ${donor.address}, ${donor.village}, ${donor.district}, ${donor.state} - ${donor.pinCode}", style = MaterialTheme.typography.bodySmall)
                        if (!donor.occupation.isNullOrBlank()) {
                            Text("Occupation: ${donor.occupation}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!donor.birthday.isNullOrBlank()) {
                            Text("Birthday: ${donor.birthday}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Added By Admin: ${donor.addedByAdmin}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                // Donor QR Code Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Donor QR Identity", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = "Unique verification code for ${donor.donorCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = onSendReminder) {
                            Text("Send WhatsApp Reminder", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Donor QR Code",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Text(
                    text = "Individual Cash Donation History (${donorDonations.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (donorDonations.isEmpty()) {
                    Text("No donations recorded for this patron yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    donorDonations.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${d.receiptNumber} • ${d.donationDate}", style = MaterialTheme.typography.labelMedium)
                                Text("${d.purpose} (By ${d.receivedByAdmin})", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = IndianCurrencyFormatter.formatInr(d.amount),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (d.status == "REVERSED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDonateNow) {
                Text("Record Cash Chanda")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
