package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.Donation
import com.example.data.Donor
import com.example.data.IndianCurrencyFormatter
import com.example.data.ReceiptRecord
import com.example.util.PdfAndExportHelper

@Composable
fun ReportsScreen(
    donors: List<Donor>,
    donations: List<Donation>,
    receipts: List<ReceiptRecord>,
    settings: AppSettings,
    canExportData: Boolean,
    onSelectReceipt: (ReceiptRecord) -> Unit,
    onCorrectOrReverseDonation: (
        donation: Donation,
        newAmount: Long,
        newPurpose: String,
        newStatus: String,
        correctionNote: String
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedReportTab by remember { mutableStateOf("DAILY") }
    var startDateFilter by remember { mutableStateOf("2026-01-01") }
    var endDateFilter by remember { mutableStateOf(IndianCurrencyFormatter.todayIsoDate()) }
    var editingAuditDonation by remember { mutableStateOf<Donation?>(null) }
    val isHindi = settings.language == "HI"

    val reportTabs = listOf(
        "DAILY" to "1. Daily Cash",
        "MONTHLY" to "2. Monthly Cash",
        "YEARLY" to "3. Yearly Cash",
        "DONOR_WISE" to "4. Donor-wise",
        "ADMIN_WISE" to "5. Admin-wise",
        "DATE_RANGE" to "6. Date Range",
        "HISTORY" to "7. Full History",
        "TOP_DONORS" to "8. Top Donors",
        "ALL_DONORS" to "9. Registered Donors"
    )

    val activeDonations = remember(donations) {
        donations.filter { it.status != "REVERSED" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isHindi) "वित्तीय रिपोर्ट एवं विश्लेषण" else "Financial Reports & Cash Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "9 Real-time Firebase Reports • Export to PDF, Excel & CSV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Export Bar (PDF, Excel, CSV)
        if (canExportData) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "रिपोर्ट एक्सपोर्ट करें (Export Data)" else "Export Current Report & Ledger",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    PdfAndExportHelper.exportFinancialReport(
                                        context = context,
                                        format = "PDF",
                                        reportTitle = selectedReportTab,
                                        donors = donors,
                                        donations = donations,
                                        akhadaName = settings.akhadaName
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_pdf_button"),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    PdfAndExportHelper.exportFinancialReport(
                                        context = context,
                                        format = "EXCEL",
                                        reportTitle = selectedReportTab,
                                        donors = donors,
                                        donations = donations,
                                        akhadaName = settings.akhadaName
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_excel_button"),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    PdfAndExportHelper.exportFinancialReport(
                                        context = context,
                                        format = "CSV",
                                        reportTitle = selectedReportTab,
                                        donors = donors,
                                        donations = donations,
                                        akhadaName = settings.akhadaName
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_csv_button"),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 9 Report Selector Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reportTabs) { (key, title) ->
                    FilterChip(
                        selected = selectedReportTab == key,
                        onClick = { selectedReportTab = key },
                        label = { Text(title, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Dynamic Report Body
        when (selectedReportTab) {
            "DAILY" -> {
                val groupedByDate = activeDonations.groupBy { it.donationDate.take(10) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } to entry.value.size }
                    .entries
                    .sortedByDescending { it.key }
                item {
                    ReportSectionHeader(
                        title = "Daily Cash Collection Report",
                        subtitle = "${groupedByDate.size} Active Collection Days"
                    )
                }
                items(groupedByDate) { (date, pair) ->
                    ReportMetricRow(
                        primaryText = date,
                        secondaryText = "${pair.second} Cash Donations • Mode: CASH",
                        amountText = IndianCurrencyFormatter.formatInr(pair.first)
                    )
                }
            }

            "MONTHLY" -> {
                val groupedByMonth = activeDonations.groupBy { it.donationDate.take(7) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } to entry.value.size }
                    .entries
                    .sortedByDescending { it.key }
                item {
                    ReportSectionHeader(
                        title = "Monthly Cash Collection Report",
                        subtitle = "${groupedByMonth.size} Active Months"
                    )
                }
                items(groupedByMonth) { (month, pair) ->
                    ReportMetricRow(
                        primaryText = "Month: $month",
                        secondaryText = "${pair.second} Cash Receipts",
                        amountText = IndianCurrencyFormatter.formatInr(pair.first)
                    )
                }
            }

            "YEARLY" -> {
                val groupedByYear = activeDonations.groupBy { it.donationDate.take(4) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } to entry.value.size }
                    .entries
                    .sortedByDescending { it.key }
                item {
                    ReportSectionHeader(
                        title = "Yearly Cash Collection Report",
                        subtitle = "Annual Akhada Financial Summary"
                    )
                }
                items(groupedByYear) { (year, pair) ->
                    ReportMetricRow(
                        primaryText = "Financial Year $year",
                        secondaryText = "${pair.second} Cash Receipts",
                        amountText = IndianCurrencyFormatter.formatInr(pair.first)
                    )
                }
            }

            "DONOR_WISE" -> {
                item {
                    ReportSectionHeader(
                        title = "Donor-wise Cash Collection",
                        subtitle = "${donors.size} Registered Patrons"
                    )
                }
                items(donors, key = { it.id }) { donor ->
                    ReportMetricRow(
                        primaryText = "${donor.fullName} (${donor.donorCode})",
                        secondaryText = "${donor.village}, ${donor.district} • ${donor.donationCount} Cash Entries",
                        amountText = IndianCurrencyFormatter.formatInr(donor.lifetimeDonation)
                    )
                }
            }

            "ADMIN_WISE" -> {
                val byAdmin = activeDonations.groupBy { it.receivedByAdmin.ifBlank { "Owner Admin" } }
                    .mapValues { entry -> entry.value.sumOf { it.amount } to entry.value.size }
                    .entries
                    .sortedByDescending { it.value.first }
                item {
                    ReportSectionHeader(
                        title = "Admin-wise Cash Collection Report",
                        subtitle = "Responsible Administrator Accountability"
                    )
                }
                items(byAdmin) { (admin, pair) ->
                    ReportMetricRow(
                        primaryText = admin,
                        secondaryText = "${pair.second} Cash Receipts Collected",
                        amountText = IndianCurrencyFormatter.formatInr(pair.first)
                    )
                }
            }

            "DATE_RANGE" -> {
                val inRange = activeDonations.filter {
                    val d = it.donationDate.take(10)
                    d >= startDateFilter.trim() && d <= endDateFilter.trim()
                }
                val rangeTotal = inRange.sumOf { it.amount }
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Custom Date Range Filter", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = startDateFilter,
                                    onValueChange = { startDateFilter = it },
                                    label = { Text("From (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endDateFilter,
                                    onValueChange = { endDateFilter = it },
                                    label = { Text("To (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Range Total (${inRange.size} entries): ${IndianCurrencyFormatter.formatInr(rangeTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                items(inRange, key = { it.id }) { d ->
                    ReportMetricRow(
                        primaryText = "${d.donorName} (${d.receiptNumber})",
                        secondaryText = "${d.donationDate} • ${d.purpose} • By ${d.receivedByAdmin}",
                        amountText = IndianCurrencyFormatter.formatInr(d.amount)
                    )
                }
            }

            "HISTORY" -> {
                item {
                    ReportSectionHeader(
                        title = "Complete Donation History & Receipt Audit",
                        subtitle = "Tap receipt icon to view PDF or edit icon for audited correction"
                    )
                }
                items(donations, key = { it.id }) { donation ->
                    val matchingReceipt = receipts.firstOrNull {
                        it.donationId == donation.id || it.receiptNumber == donation.receiptNumber
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = donation.receiptNumber,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = when (donation.status) {
                                            "REVERSED" -> MaterialTheme.colorScheme.errorContainer
                                            "CORRECTED" -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        }
                                    ) {
                                        Text(
                                            text = donation.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${donation.donorName} (${donation.donorCode})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${donation.donationDate} • ${donation.purpose} • Admin: ${donation.receivedByAdmin}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!donation.correctionNote.isNullOrBlank()) {
                                    Text(
                                        text = "Audit Note: ${donation.correctionNote}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = IndianCurrencyFormatter.formatInr(donation.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (donation.status == "REVERSED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    if (matchingReceipt != null) {
                                        IconButton(onClick = { onSelectReceipt(matchingReceipt) }) {
                                            Icon(
                                                imageVector = Icons.Default.Receipt,
                                                contentDescription = "View PDF Receipt",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { editingAuditDonation = donation }) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = "Authorized Correction",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "TOP_DONORS" -> {
                val ranked = donors.sortedByDescending { it.lifetimeDonation }
                item {
                    ReportSectionHeader(
                        title = "Top Donors Leaderboard Report",
                        subtitle = "Ranked by Lifetime Cash Contribution"
                    )
                }
                items(ranked, key = { it.id }) { donor ->
                    ReportMetricRow(
                        primaryText = "${donor.fullName} (${donor.donorCode})",
                        secondaryText = "${donor.village}, ${donor.district} • Mobile: ${donor.mobileNumber}",
                        amountText = IndianCurrencyFormatter.formatInr(donor.lifetimeDonation)
                    )
                }
            }

            "ALL_DONORS" -> {
                item {
                    ReportSectionHeader(
                        title = "Total Registered Donors Master Directory",
                        subtitle = "${donors.size} Registered Patrons"
                    )
                }
                items(donors, key = { it.id }) { donor ->
                    ReportMetricRow(
                        primaryText = "${donor.donorCode} • ${donor.fullName} S/o ${donor.fatherName}",
                        secondaryText = "${donor.address}, ${donor.village}, ${donor.district} (${donor.pinCode}) • Added by ${donor.addedByAdmin}",
                        amountText = IndianCurrencyFormatter.formatInr(donor.lifetimeDonation)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }

    // Authorized Correction / Reversal Dialog (Financial records are never silently deleted)
    editingAuditDonation?.let { donation ->
        var updatedAmount by remember(donation) { mutableStateOf(donation.amount.toString()) }
        var updatedPurpose by remember(donation) { mutableStateOf(donation.purpose) }
        var selectedStatus by remember(donation) { mutableStateOf("CORRECTED") }
        var correctionReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { editingAuditDonation = null },
            title = { Text("Authorized Audit Correction (${donation.receiptNumber})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Financial records are never silently deleted. Any correction or reversal is permanently recorded in the Akhada audit history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedStatus == "CORRECTED",
                            onClick = { selectedStatus = "CORRECTED" },
                            label = { Text("Correct Entry") }
                        )
                        FilterChip(
                            selected = selectedStatus == "REVERSED",
                            onClick = { selectedStatus = "REVERSED" },
                            label = { Text("Reverse Entry") }
                        )
                    }
                    if (selectedStatus == "CORRECTED") {
                        OutlinedTextField(
                            value = updatedAmount,
                            onValueChange = { updatedAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("Corrected Cash Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = updatedPurpose,
                            onValueChange = { updatedPurpose = it },
                            label = { Text("Corrected Purpose") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = correctionReason,
                        onValueChange = { correctionReason = it },
                        label = { Text("Mandatory Audit Reason *") },
                        placeholder = { Text("Explain why this cash record is being corrected/reversed...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = updatedAmount.toLongOrNull() ?: donation.amount
                        if (amt > 0L && correctionReason.isNotBlank()) {
                            onCorrectOrReverseDonation(
                                donation,
                                amt,
                                updatedPurpose,
                                selectedStatus,
                                correctionReason
                            )
                            editingAuditDonation = null
                        }
                    },
                    enabled = correctionReason.isNotBlank()
                ) {
                    Text("Save Audited Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAuditDonation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReportSectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportMetricRow(
    primaryText: String,
    secondaryText: String,
    amountText: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
