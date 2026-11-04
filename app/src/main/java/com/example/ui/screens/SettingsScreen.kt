package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ActivityLog
import com.example.data.AppSettings
import com.example.data.AuthorizedDevice
import com.example.data.Donation
import com.example.data.Donor
import com.example.util.PdfAndExportHelper

@Composable
fun SettingsScreen(
    settings: AppSettings,
    devices: List<AuthorizedDevice>,
    activityLogs: List<ActivityLog>,
    donors: List<Donor>,
    donations: List<Donation>,
    currentInstallationId: String,
    currentDeviceModel: String,
    onSaveSettings: (AppSettings) -> Unit,
    onAuthorizeOrUpdateDevice: (
        targetDevice: AuthorizedDevice?,
        installationId: String,
        deviceName: String,
        adminName: String,
        role: String,
        status: String,
        canAddDonor: Boolean,
        canEditDonor: Boolean,
        canDeleteDonor: Boolean,
        canRecordDonation: Boolean,
        canViewReports: Boolean,
        canExportData: Boolean
    ) -> Unit,
    onImportCsvDonors: (String) -> Unit,
    onSignOutDevice: () -> Unit
) {
    val context = LocalContext.current
    var akhadaName by remember(settings) { mutableStateOf(settings.akhadaName) }
    var akhadaRegNumber by remember(settings) { mutableStateOf(settings.akhadaRegNumber) }
    var akhadaAddress by remember(settings) { mutableStateOf(settings.akhadaAddress) }
    var ownerAdminName by remember(settings) { mutableStateOf(settings.ownerAdminName) }
    var ownerAdminPhone by remember(settings) { mutableStateOf(settings.ownerAdminPhone) }
    var vaultAllocationId by remember(settings) { mutableStateOf(settings.vaultAllocationId) }
    var activeSeason by remember(settings) { mutableStateOf(settings.activeSeason) }
    var language by remember(settings) { mutableStateOf(settings.language) }
    var darkMode by remember(settings) { mutableStateOf(settings.darkMode) }
    var thankYouHi by remember(settings) { mutableStateOf(settings.thankYouTemplateHi) }
    var thankYouEn by remember(settings) { mutableStateOf(settings.thankYouTemplateEn) }
    var reminderHi by remember(settings) { mutableStateOf(settings.reminderTemplateHi) }
    var reminderEn by remember(settings) { mutableStateOf(settings.reminderTemplateEn) }

    var showEnrollDeviceDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<AuthorizedDevice?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val approvedCount = devices.count { it.status == "APPROVED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Owner Admin & Security Control",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Device Authorization (Max 6 Devices) • Akhada Vault Settings • Backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Authorized Device Management (Up to 6 Devices)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "Authorized Devices",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Authorized Admin Devices ($approvedCount / 6)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This Device ID: $currentInstallationId ($currentDeviceModel)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (approvedCount < 6) {
                            Button(
                                onClick = {
                                    editingDevice = null
                                    showEnrollDeviceDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("authorize_device_button")
                            ) {
                                Icon(Icons.Default.AddModerator, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Device", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    devices.forEach { dev ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${dev.adminName} • ${dev.role}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${dev.deviceName} • ID: ${dev.installationId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Last Active: ${dev.lastActiveAt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = when (dev.status) {
                                            "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
                                            "REVOKED" -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.tertiaryContainer
                                        }
                                    ) {
                                        Text(
                                            text = dev.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (dev.status) {
                                                "APPROVED" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                "REVOKED" -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                                            },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editingDevice = dev
                                            showEnrollDeviceDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Permissions / Edit", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (dev.status != "APPROVED" && approvedCount < 6) {
                                        Button(
                                            onClick = {
                                                onAuthorizeOrUpdateDevice(
                                                    dev,
                                                    dev.installationId,
                                                    dev.deviceName,
                                                    dev.adminName,
                                                    dev.role,
                                                    "APPROVED",
                                                    dev.canAddDonor,
                                                    dev.canEditDonor,
                                                    dev.canDeleteDonor,
                                                    dev.canRecordDonation,
                                                    dev.canViewReports,
                                                    dev.canExportData
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else if (dev.status == "APPROVED" && dev.installationId != currentInstallationId) {
                                        OutlinedButton(
                                            onClick = {
                                                onAuthorizeOrUpdateDevice(
                                                    dev,
                                                    dev.installationId,
                                                    dev.deviceName,
                                                    dev.adminName,
                                                    dev.role,
                                                    "REVOKED",
                                                    false,
                                                    false,
                                                    false,
                                                    false,
                                                    false,
                                                    false
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Block,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revoke", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Language & Theme Settings
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
                    Text("Language & Display Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BILINGUAL" to "Hindi + English", "HI" to "हिन्दी (Hindi)", "EN" to "English").forEach { (code, label) ->
                            FilterChip(
                                selected = language == code,
                                onClick = {
                                    language = code
                                    onSaveSettings(settings.copy(language = code, darkMode = darkMode))
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Vault Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Toggle between Dark Emerald-Navy and Royal Light mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                darkMode = it
                                onSaveSettings(settings.copy(darkMode = it, language = language))
                            }
                        )
                    }
                }
            }
        }

        // 3. Akhada Organization & Owner Admin Details
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Akhada Details & WhatsApp Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = akhadaName,
                        onValueChange = { akhadaName = it },
                        label = { Text("Akhada Name (अखाड़े का नाम)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = akhadaRegNumber,
                        onValueChange = { akhadaRegNumber = it },
                        label = { Text("Registration / Corpus Subtitle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = akhadaAddress,
                        onValueChange = { akhadaAddress = it },
                        label = { Text("Akhada Official Address (Printed on Receipts)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ownerAdminName,
                            onValueChange = { ownerAdminName = it },
                            label = { Text("Owner Admin Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ownerAdminPhone,
                            onValueChange = { ownerAdminPhone = it },
                            label = { Text("Owner Phone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vaultAllocationId,
                            onValueChange = { vaultAllocationId = it },
                            label = { Text("Vault Allocation ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = activeSeason,
                            onValueChange = { activeSeason = it },
                            label = { Text("Active Season") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = thankYouHi,
                        onValueChange = { thankYouHi = it },
                        label = { Text("Hindi Thank-You Message Template") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderHi,
                        onValueChange = { reminderHi = it },
                        label = { Text("Hindi Donation Reminder Template") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    akhadaName = akhadaName,
                                    akhadaRegNumber = akhadaRegNumber,
                                    akhadaAddress = akhadaAddress,
                                    ownerAdminName = ownerAdminName,
                                    ownerAdminPhone = ownerAdminPhone,
                                    vaultAllocationId = vaultAllocationId,
                                    activeSeason = activeSeason,
                                    language = language,
                                    darkMode = darkMode,
                                    thankYouTemplateHi = thankYouHi,
                                    thankYouTemplateEn = thankYouEn,
                                    reminderTemplateHi = reminderHi,
                                    reminderTemplateEn = reminderEn
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Akhada Configuration")
                    }
                }
            }
        }

        // 4. Cloud Backup, Excel Import & Data Export
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Backup, Excel Import & Complete Data Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel/CSV Import", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = {
                                PdfAndExportHelper.exportFinancialReport(
                                    context = context,
                                    format = "EXCEL",
                                    reportTitle = "Complete_Akhada_Backup",
                                    donors = donors,
                                    donations = donations,
                                    akhadaName = settings.akhadaName
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Full Backup", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 5. Complete Activity Audit Logs & App Privacy Info
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Admin & Device Activity History (${activityLogs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (activityLogs.isEmpty()) {
                        Text("No activity logs recorded yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        activityLogs.take(8).forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "[${log.actionType}] ${log.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "By ${log.adminName} • Device ${log.deviceId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Privacy & App Version Card + Sign Out
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("CHANDA APP v2.4.0 • Private Akhada Edition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Strict Privacy & Zero Public Access: Donor photographs and contact records are protected by zero-trust Firestore Security Rules. Only up to 6 authorized Akhada devices can access this ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onSignOutDevice,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect Google Admin Session on This Device")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }

    // Add / Edit Authorized Device Dialog
    if (showEnrollDeviceDialog) {
        val target = editingDevice
        var instId by remember(target) { mutableStateOf(target?.installationId ?: "AKH-STAFF-${devices.size + 1}") }
        var devName by remember(target) { mutableStateOf(target?.deviceName ?: "Akhada Staff Android") }
        var admName by remember(target) { mutableStateOf(target?.adminName ?: "") }
        var role by remember(target) { mutableStateOf(target?.role ?: "STAFF_ADMIN") }
        var status by remember(target) { mutableStateOf(target?.status ?: "APPROVED") }
        var canAddDonor by remember(target) { mutableStateOf(target?.canAddDonor ?: true) }
        var canEditDonor by remember(target) { mutableStateOf(target?.canEditDonor ?: true) }
        var canDeleteDonor by remember(target) { mutableStateOf(target?.canDeleteDonor ?: false) }
        var canRecordDonation by remember(target) { mutableStateOf(target?.canRecordDonation ?: true) }
        var canViewReports by remember(target) { mutableStateOf(target?.canViewReports ?: true) }
        var canExportData by remember(target) { mutableStateOf(target?.canExportData ?: false) }

        AlertDialog(
            onDismissRequest = { showEnrollDeviceDialog = false },
            title = { Text(if (target == null) "Authorize New Admin Device" else "Manage Device Permissions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = admName,
                        onValueChange = { admName = it },
                        label = { Text("Assigned Admin Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        label = { Text("Device Model / Label *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instId,
                        onValueChange = { instId = it },
                        label = { Text("Device Installation ID *") },
                        singleLine = true,
                        enabled = target == null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == "OWNER_ADMIN",
                            onClick = { role = "OWNER_ADMIN" },
                            label = { Text("Owner Admin") }
                        )
                        FilterChip(
                            selected = role == "STAFF_ADMIN",
                            onClick = { role = "STAFF_ADMIN" },
                            label = { Text("Staff Admin") }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canAddDonor, onCheckedChange = { canAddDonor = it })
                        Text("Can Add Donors", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canEditDonor, onCheckedChange = { canEditDonor = it })
                        Text("Can Edit Donor Profiles", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canRecordDonation, onCheckedChange = { canRecordDonation = it })
                        Text("Can Record Cash Donations", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canViewReports, onCheckedChange = { canViewReports = it })
                        Text("Can View Financial Reports", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canExportData, onCheckedChange = { canExportData = it })
                        Text("Can Export Complete Data", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (admName.isNotBlank() && instId.length >= 4) {
                            onAuthorizeOrUpdateDevice(
                                target,
                                instId,
                                devName,
                                admName,
                                role,
                                status,
                                canAddDonor,
                                canEditDonor,
                                canDeleteDonor,
                                canRecordDonation,
                                canViewReports,
                                canExportData
                            )
                            showEnrollDeviceDialog = false
                        }
                    },
                    enabled = admName.isNotBlank() && instId.length >= 4
                ) {
                    Text("Save Device")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollDeviceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Excel / CSV Bulk Import Dialog
    if (showImportDialog) {
        var csvInput by remember {
            mutableStateOf(
                "Choudhary Surender Sangwan,9812345678,Chhara,Jhajjar,Ch. Ramphal Sangwan\n" +
                    "Raghunath Sharma,9876543210,Beri,Jhajjar,Pt. Devdutt Sharma\n" +
                    "Subedar Mahavir Phogat,9811122334,Balali,Charkhi Dadri,Ch. Manphool Singh"
            )
        }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Excel / CSV Donor Data Import") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste comma-separated or tab-separated rows from Excel: FullName, MobileNumber, Village, District, FatherName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = csvInput,
                        onValueChange = { csvInput = it },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCsvDonors(csvInput)
                        showImportDialog = false
                    }
                ) {
                    Text("Import Donors to Cloud")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
