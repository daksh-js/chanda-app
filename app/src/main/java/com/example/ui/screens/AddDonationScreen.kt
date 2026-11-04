package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.Donor
import com.example.data.IndianCurrencyFormatter
import com.example.data.ReceiptRecord
import com.example.util.PdfAndExportHelper
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDonationScreen(
    donors: List<Donor>,
    preselectedDonor: Donor?,
    settings: AppSettings,
    currentAdminName: String,
    isOnline: Boolean,
    latestGeneratedReceipt: ReceiptRecord?,
    isDuplicateCheck: (donorId: String, amount: Long) -> Boolean,
    onRecordCashDonation: (
        donor: Donor,
        amount: Long,
        donationDate: String,
        purpose: String,
        receivedByAdmin: String,
        notes: String?
    ) -> Unit,
    onDismissReceiptModal: () -> Unit,
    onNavigateToAddDonor: () -> Unit
) {
    val context = LocalContext.current
    var selectedDonor by remember(donors, preselectedDonor) {
        mutableStateOf(preselectedDonor ?: donors.firstOrNull())
    }
    LaunchedEffect(preselectedDonor) {
        if (preselectedDonor != null) {
            selectedDonor = preselectedDonor
        }
    }

    var amountInput by remember { mutableStateOf("5100") }
    var donationDate by remember { mutableStateOf(IndianCurrencyFormatter.todayIsoDate()) }
    var selectedPurpose by remember { mutableStateOf("Kushti Peeth Diet Grant") }
    var receivedByAdmin by remember(currentAdminName) { mutableStateOf(currentAdminName) }
    var additionalNotes by remember { mutableStateOf("") }
    var showDonorPickerDialog by remember { mutableStateOf(false) }
    var showManualConfirmationDialog by remember { mutableStateOf(false) }

    val parsedAmount = amountInput.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    val amountInWords = remember(parsedAmount) { IndianCurrencyFormatter.amountToWords(parsedAmount) }
    val isHindi = settings.language == "HI"

    val presetAmounts = listOf(501L, 1100L, 2100L, 5100L, 11000L, 21000L)
    val purposeOptions = listOf(
        "Kushti Peeth Diet Grant",
        "General Akhada Chanda",
        "Arena Sand (Mitti) Refill",
        "Annual Dangal Sponsorship",
        "Gym & Wrestling Mat Fund",
        "Gaushala & Athlete Seva"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("add_donation_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sub-header Status Bar & Receipt Preview Tag (Matched to Reference HTML)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "CASH RECEIPT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Auto-ID • #AKD-${donationDate.take(4)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.tertiary
                                )
                        )
                        Text(
                            text = if (isOnline) "Live Sync" else "Offline Queue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Donor Selection Card (Matched to Reference HTML)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = if (isHindi) "दानदाता का विवरण (DONOR DETAILS)" else "DONOR DETAILS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clickable { showDonorPickerDialog = true }
                                .testTag("change_donor_button")
                        ) {
                            Text(
                                text = if (donors.isEmpty()) "Add Donor" else "Change",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Change Donor",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val donor = selectedDonor
                    if (donor == null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No donor selected yet. Register or pick an Akhada donor first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onNavigateToAddDonor) {
                                Text("Register New Donor")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = donor.fullName.take(2).uppercase(Locale.US),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = donor.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Patron",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${donor.donorCode} • ${donor.village.ifBlank { donor.city }}, ${donor.district} (${donor.state})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "+91 ${donor.mobileNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text("•", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "Lifetime: ${IndianCurrencyFormatter.formatInr(donor.lifetimeDonation)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Numeric Input Display Card (Matched to Reference HTML)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isHindi) "नकद दान राशि (Cash Chanda Amount)" else "Donation Amount (Cash Chanda)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = IndianCurrencyFormatter.formatInr(parsedAmount).removePrefix("₹"),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = ".00",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = amountInWords,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { newVal ->
                            amountInput = newVal.filter { it.isDigit() }.take(9)
                        },
                        label = { Text("Enter Cash Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donation_amount_input")
                    )

                    // Preset Amount Quick Pills
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetAmounts) { preset ->
                            val isSelected = parsedAmount == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { amountInput = preset.toString() },
                                label = {
                                    Text(
                                        text = IndianCurrencyFormatter.formatInr(preset),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Strict CASH-ONLY Payment Method Lock Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Cash Only",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Payment Method: CASH ONLY (केवल नकद)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Strict Akhada Policy: Online, UPI, Cheque & Bank modes disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "CASH ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 5. Fund Purpose Allocation Tags & Entry Metadata
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isHindi) "दान का उद्देश्य (Donation Purpose)" else "Fund Purpose Allocation",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        purposeOptions.forEach { option ->
                            val selected = selectedPurpose == option
                            FilterChip(
                                selected = selected,
                                onClick = { selectedPurpose = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = selectedPurpose,
                        onValueChange = { selectedPurpose = it },
                        label = { Text("Donation Purpose") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = donationDate,
                            onValueChange = { donationDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = receivedByAdmin,
                            onValueChange = { receivedByAdmin = it },
                            label = { Text("Received By Admin") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Additional Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 6. CTA Confirm & Save Donation Button (Matched to Reference HTML)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showManualConfirmationDialog = true },
                    enabled = selectedDonor != null && parsedAmount > 0L,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("confirm_save_donation_button")
                ) {
                    Text(
                        text = if (isHindi) "नकद चंदा पुष्टि करें और रसीद बनाएं" else "Confirm & Save Cash Donation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Encrypted Ledger Sync • Digital Akhada OS v2.4",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }

    // Donor Picker Modal Dialog
    if (showDonorPickerDialog) {
        var filterText by remember { mutableStateOf("") }
        val filtered = donors.filter {
            it.fullName.contains(filterText, ignoreCase = true) ||
                it.donorCode.contains(filterText, ignoreCase = true) ||
                it.mobileNumber.contains(filterText)
        }
        AlertDialog(
            onDismissRequest = { showDonorPickerDialog = false },
            title = { Text("Select Akhada Donor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search name, DONOR-ID, mobile...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No matching donor found.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(260.dp)) {
                            items(filtered, key = { it.id }) { itemDonor ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDonor = itemDonor
                                            showDonorPickerDialog = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${itemDonor.fullName} (${itemDonor.donorCode})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${itemDonor.village} • +91 ${itemDonor.mobileNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = IndianCurrencyFormatter.formatInr(itemDonor.lifetimeDonation),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDonorPickerDialog = false
                        onNavigateToAddDonor()
                    }
                ) {
                    Text("+ Register New Donor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonorPickerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Manual Admin Confirmation Dialog before recording Cash Donation
    if (showManualConfirmationDialog && selectedDonor != null) {
        val donor = selectedDonor!!
        val isDuplicate = isDuplicateCheck(donor.id, parsedAmount)
        AlertDialog(
            onDismissRequest = { showManualConfirmationDialog = false },
            title = { Text("Confirm Cash Chanda Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDuplicate) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "⚠️ Duplicate Alert: A cash donation of ${IndianCurrencyFormatter.formatInr(parsedAmount)} for ${donor.fullName} was just submitted moments ago. Please verify before confirming again.",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Text("Donor: ${donor.fullName} (${donor.donorCode})", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Cash Amount: ${IndianCurrencyFormatter.formatInr(parsedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("In Words: $amountInWords", style = MaterialTheme.typography.bodySmall)
                    Text("Payment Method: CASH ONLY", style = MaterialTheme.typography.bodySmall)
                    Text("Purpose: $selectedPurpose", style = MaterialTheme.typography.bodySmall)
                    Text("Received By Admin: $receivedByAdmin", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualConfirmationDialog = false
                        onRecordCashDonation(
                            donor,
                            parsedAmount,
                            donationDate,
                            selectedPurpose,
                            receivedByAdmin,
                            additionalNotes.takeIf { it.isNotBlank() }
                        )
                    },
                    modifier = Modifier.testTag("final_confirm_donation_button")
                ) {
                    Text("Confirm Cash Received")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualConfirmationDialog = false }) {
                    Text("Edit")
                }
            }
        )
    }

    // Instant PDF Receipt Preview & Share Dialog
    latestGeneratedReceipt?.let { receipt ->
        ReceiptPreviewDialog(
            receipt = receipt,
            onDismiss = onDismissReceiptModal
        )
    }
}

@Composable
fun ReceiptPreviewDialog(
    receipt: ReceiptRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(receipt.qrPayload, receipt.receiptNumber) {
        PdfAndExportHelper.generateQrCodeBitmap(
            receipt.qrPayload.ifBlank { receipt.receiptNumber },
            260
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Receipt Ready",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text("Official Cash Receipt", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = receipt.receiptNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = receipt.akhadaName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${IndianCurrencyFormatter.formatInr(receipt.amount)} (CASH ONLY)",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(receipt.amountInWords, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Donor: ${receipt.donorName} (${receipt.donorCode})", style = MaterialTheme.typography.bodySmall)
                        Text("Mobile: ${receipt.donorMobile}", style = MaterialTheme.typography.bodySmall)
                        Text("Purpose: ${receipt.purpose ?: "General Chanda"}", style = MaterialTheme.typography.bodySmall)
                        Text("Date: ${receipt.donationDate} • Admin: ${receipt.receivedByAdmin}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Receipt QR Verification",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text(
                    text = "Unique Receipt Verification QR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { PdfAndExportHelper.shareReceiptPdf(context, receipt, preferWhatsApp = true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp PDF", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { PdfAndExportHelper.printReceiptPdf(context, receipt) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / Save PDF", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
