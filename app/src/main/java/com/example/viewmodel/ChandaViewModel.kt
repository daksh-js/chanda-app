package com.example.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLog
import com.example.data.AppSettings
import com.example.data.AuthorizedDevice
import com.example.data.ChandaRepository
import com.example.data.Donation
import com.example.data.Donor
import com.example.data.IndianCurrencyFormatter
import com.example.data.ReceiptRecord
import com.example.data.UiState
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChandaViewModel(
    private val repository: ChandaRepository,
    private val currentUserId: String,
    private val currentUserEmail: String?,
    private val currentUserDisplayName: String?,
    private val installationId: String,
    private val deviceModelName: String
) : ViewModel() {

    val donorsState: StateFlow<UiState<List<Donor>>> = repository.observeDonors(currentUserId)
        .map<List<Donor>, UiState<List<Donor>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing donors", error)
            emit(UiState.Error(error.message ?: "Failed to load donors"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val donationsState: StateFlow<UiState<List<Donation>>> = repository.observeDonations(currentUserId)
        .map<List<Donation>, UiState<List<Donation>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing donations", error)
            emit(UiState.Error(error.message ?: "Failed to load donations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val receiptsState: StateFlow<UiState<List<ReceiptRecord>>> = repository.observeReceipts(currentUserId)
        .map<List<ReceiptRecord>, UiState<List<ReceiptRecord>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing receipts", error)
            emit(UiState.Error(error.message ?: "Failed to load receipts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val devicesState: StateFlow<UiState<List<AuthorizedDevice>>> = repository.observeAuthorizedDevices(currentUserId)
        .map<List<AuthorizedDevice>, UiState<List<AuthorizedDevice>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing devices", error)
            emit(UiState.Error(error.message ?: "Failed to load authorized devices"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activityLogsState: StateFlow<UiState<List<ActivityLog>>> = repository.observeActivityLogs(currentUserId)
        .map<List<ActivityLog>, UiState<List<ActivityLog>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing activity logs", error)
            emit(UiState.Error(error.message ?: "Failed to load activity logs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val settingsState: StateFlow<UiState<AppSettings>> = repository.observeAppSettings(currentUserId)
        .map<AppSettings?, UiState<AppSettings>> { settings ->
            UiState.Success(
                settings ?: AppSettings(
                    id = "settings_$currentUserId",
                    userId = currentUserId,
                    ownerAdminName = currentUserDisplayName?.takeIf { it.isNotBlank() } ?: "Vikramaditya Sengupta"
                )
            )
        }
        .catch { error ->
            Log.w(TAG, "Error observing settings", error)
            emit(UiState.Error(error.message ?: "Failed to load settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _latestGeneratedReceipt = MutableStateFlow<ReceiptRecord?>(null)
    val latestGeneratedReceipt: StateFlow<ReceiptRecord?> = _latestGeneratedReceipt.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var lastSubmissionSignature: String = ""
    private var lastSubmissionTimeMs: Long = 0L
    private var bootstrappedSession = false

    val currentInstallationId: String get() = installationId
    val currentDeviceModel: String get() = deviceModelName

    init {
        bootstrapDeviceAndSettingsIfNeeded()
    }

    private fun bootstrapDeviceAndSettingsIfNeeded() {
        if (bootstrappedSession) return
        bootstrappedSession = true
        viewModelScope.launch {
            // Ensure current device is registered in authorized_devices
            val adminDefaultName = currentUserDisplayName?.takeIf { it.isNotBlank() }
                ?: currentUserEmail?.substringBefore("@")
                ?: "Vikramaditya Sengupta"

            val deviceDocId = "dev_${installationId.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").take(32)}"
            repository.enrollOrUpdateAuthorizedDevice(
                deviceId = deviceDocId,
                installationId = installationId,
                deviceName = deviceModelName,
                adminName = adminDefaultName,
                role = "OWNER_ADMIN",
                status = "APPROVED",
                canAddDonor = true,
                canEditDonor = true,
                canDeleteDonor = true,
                canRecordDonation = true,
                canViewReports = true,
                canExportData = true
            )
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearLatestReceipt() {
        _latestGeneratedReceipt.value = null
    }

    fun showLatestReceipt(receipt: ReceiptRecord) {
        _latestGeneratedReceipt.value = receipt
    }

    fun nextDonorCode(existingDonors: List<Donor>): String {
        val maxNum = existingDonors.mapNotNull { donor ->
            donor.donorCode.removePrefix("DONOR-").toIntOrNull()
        }.maxOrNull() ?: 0
        return String.format(Locale.US, "DONOR-%04d", maxNum + 1)
    }

    fun currentAdminName(devices: List<AuthorizedDevice>, settings: AppSettings): String {
        val thisDevice = devices.firstOrNull { it.installationId == installationId }
        return thisDevice?.adminName?.takeIf { it.isNotBlank() }
            ?: settings.ownerAdminName.takeIf { it.isNotBlank() }
            ?: currentUserDisplayName?.takeIf { it.isNotBlank() }
            ?: "Owner Admin"
    }

    fun currentDeviceRecord(devices: List<AuthorizedDevice>): AuthorizedDevice? {
        return devices.firstOrNull { it.installationId == installationId }
            ?: devices.firstOrNull()
    }

    fun addDonor(
        existingDonors: List<Donor>,
        fullName: String,
        fatherName: String,
        mobileNumber: String,
        whatsappNumber: String,
        alternateMobile: String?,
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
        addedByAdmin: String,
        isFavorite: Boolean,
        consentGiven: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        if (!consentGiven) {
            _statusBanner.value = "Consent is required before storing donor photograph and contact details."
            return
        }
        val code = nextDonorCode(existingDonors)
        _isBusy.value = true
        viewModelScope.launch {
            val result = repository.createDonor(
                donorCode = code,
                fullName = fullName,
                fatherName = fatherName,
                mobileNumber = mobileNumber,
                whatsappNumber = whatsappNumber,
                alternateMobile = alternateMobile,
                address = address,
                village = village,
                city = city,
                district = district,
                state = state,
                pinCode = pinCode,
                occupation = occupation,
                birthday = birthday,
                notes = notes,
                photoUri = photoUri,
                addedByAdmin = addedByAdmin,
                isFavorite = isFavorite,
                consentGiven = consentGiven,
                deviceId = installationId
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Donor $fullName ($code) saved to Cloud Firestore."
                onSuccess()
            }.onFailure { err ->
                _statusBanner.value = "Failed to save donor: ${err.localizedMessage}"
            }
        }
    }

    fun updateDonor(donor: Donor, adminName: String, onSuccess: () -> Unit = {}) {
        _isBusy.value = true
        viewModelScope.launch {
            val result = repository.updateDonor(donor, adminName, installationId)
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Updated donor ${donor.fullName} (${donor.donorCode})."
                onSuccess()
            }.onFailure { err ->
                _statusBanner.value = "Failed to update donor: ${err.localizedMessage}"
            }
        }
    }

    fun toggleDonorFavorite(donor: Donor, adminName: String) {
        viewModelScope.launch {
            repository.updateDonor(
                donor = donor.copy(isFavorite = !donor.isFavorite),
                adminName = adminName,
                deviceId = installationId
            )
        }
    }

    fun deleteDonor(donor: Donor, adminName: String, onSuccess: () -> Unit = {}) {
        _isBusy.value = true
        viewModelScope.launch {
            val result = repository.deleteDonor(donor, adminName, installationId)
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Donor ${donor.fullName} removed."
                onSuccess()
            }.onFailure { err ->
                _statusBanner.value = "Delete failed: ${err.localizedMessage}"
            }
        }
    }

    fun isPotentialDuplicateSubmission(donorId: String, amount: Long): Boolean {
        val sig = "$donorId:$amount"
        val now = System.currentTimeMillis()
        return sig == lastSubmissionSignature && (now - lastSubmissionTimeMs) < 20_000L
    }

    fun recordCashDonation(
        donor: Donor,
        amount: Long,
        donationDate: String,
        purpose: String,
        receivedByAdmin: String,
        notes: String?,
        settings: AppSettings,
        onSuccess: (ReceiptRecord) -> Unit = {}
    ) {
        _isBusy.value = true
        lastSubmissionSignature = "${donor.id}:$amount"
        lastSubmissionTimeMs = System.currentTimeMillis()

        viewModelScope.launch {
            val result = repository.recordCashDonation(
                donor = donor,
                amount = amount,
                donationDate = donationDate,
                purpose = purpose,
                receivedByAdmin = receivedByAdmin,
                notes = notes,
                akhadaName = settings.akhadaName,
                akhadaAddress = settings.akhadaAddress,
                thankYouMessage = settings.thankYouTemplateHi,
                deviceId = installationId
            )
            _isBusy.value = false
            result.onSuccess { receipt ->
                _latestGeneratedReceipt.value = receipt
                _statusBanner.value = "CASH Chanda ${IndianCurrencyFormatter.formatInr(amount)} saved • Receipt ${receipt.receiptNumber}"
                onSuccess(receipt)
            }.onFailure { err ->
                _statusBanner.value = "Failed to record cash donation: ${err.localizedMessage}"
            }
        }
    }

    fun correctOrReverseDonation(
        donation: Donation,
        donor: Donor?,
        newAmount: Long,
        newPurpose: String,
        newStatus: String,
        correctionNote: String,
        adminName: String
    ) {
        if (correctionNote.isBlank()) {
            _statusBanner.value = "Audit reason is mandatory for correcting or reversing a cash entry."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            val result = repository.correctOrReverseDonation(
                donation = donation,
                donor = donor,
                newAmount = newAmount,
                newPurpose = newPurpose,
                newStatus = newStatus,
                correctionNote = correctionNote,
                adminName = adminName,
                deviceId = installationId
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Receipt ${donation.receiptNumber} marked as $newStatus in audit trail."
            }.onFailure { err ->
                _statusBanner.value = "Audit update failed: ${err.localizedMessage}"
            }
        }
    }

    fun authorizeOrUpdateDevice(
        existingDevices: List<AuthorizedDevice>,
        targetDevice: AuthorizedDevice?,
        installationIdInput: String,
        deviceNameInput: String,
        adminNameInput: String,
        roleInput: String,
        statusInput: String,
        canAddDonor: Boolean,
        canEditDonor: Boolean,
        canDeleteDonor: Boolean,
        canRecordDonation: Boolean,
        canViewReports: Boolean,
        canExportData: Boolean
    ) {
        val activeApprovedCount = existingDevices.count {
            it.status == "APPROVED" && it.id != targetDevice?.id
        }
        if (statusInput == "APPROVED" && activeApprovedCount >= 6) {
            _statusBanner.value = "Maximum 6 authorized administrator devices allowed! Revoke a device first."
            return
        }

        val docId = targetDevice?.id?.takeIf { it.isNotBlank() }
            ?: "dev_${installationIdInput.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").take(32)}"

        _isBusy.value = true
        viewModelScope.launch {
            val result = repository.enrollOrUpdateAuthorizedDevice(
                deviceId = docId,
                installationId = installationIdInput,
                deviceName = deviceNameInput,
                adminName = adminNameInput,
                role = roleInput,
                status = statusInput,
                canAddDonor = canAddDonor,
                canEditDonor = canEditDonor,
                canDeleteDonor = canDeleteDonor,
                canRecordDonation = canRecordDonation,
                canViewReports = canViewReports,
                canExportData = canExportData,
                existingCreatedAt = targetDevice?.createdAt,
                existingCreatedBy = targetDevice?.createdBy
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Device ${deviceNameInput.trim()} ($adminNameInput) updated ($statusInput)."
            }.onFailure { err ->
                _statusBanner.value = "Device update failed: ${err.localizedMessage}"
            }
        }
    }

    fun saveSettings(settings: AppSettings, adminName: String) {
        _isBusy.value = true
        viewModelScope.launch {
            val result = repository.saveAppSettings(settings, adminName, installationId)
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Akhada settings synchronized with Cloud Firestore."
            }.onFailure { err ->
                _statusBanner.value = "Failed to save settings: ${err.localizedMessage}"
            }
        }
    }

    fun importCsvDonors(
        csvContent: String,
        existingDonors: List<Donor>,
        adminName: String
    ) {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            _statusBanner.value = "No rows found in CSV/Excel input."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            var importedCount = 0
            var baseIndex = existingDonors.size
            for (line in lines) {
                if (line.startsWith("DonorCode", ignoreCase = true) || line.startsWith("FullName", ignoreCase = true)) {
                    continue
                }
                val parts = line.split(",", "\t").map { it.trim() }
                if (parts.size >= 2) {
                    val name = parts[0].ifBlank { "Patron ${baseIndex + 1}" }
                    val mobile = parts.getOrNull(1)?.ifBlank { "9812300000" } ?: "9812300000"
                    val village = parts.getOrNull(2) ?: "Chhara"
                    val city = parts.getOrNull(3) ?: "Jhajjar"
                    val father = parts.getOrNull(4) ?: ""
                    baseIndex++
                    val code = String.format(Locale.US, "DONOR-%04d", baseIndex)
                    val res = repository.createDonor(
                        donorCode = code,
                        fullName = name,
                        fatherName = father,
                        mobileNumber = mobile,
                        whatsappNumber = mobile,
                        address = "$village, $city",
                        village = village,
                        city = city,
                        district = city,
                        state = "Haryana",
                        pinCode = "124504",
                        addedByAdmin = adminName,
                        deviceId = installationId
                    )
                    if (res.isSuccess) importedCount++
                }
            }
            _isBusy.value = false
            _statusBanner.value = "Imported $importedCount donors into Cloud Firestore."
        }
    }

    companion object {
        private const val TAG = "ChandaViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun getOrCreateInstallationId(context: Context): String {
            val prefs = context.getSharedPreferences("chanda_device_identity", Context.MODE_PRIVATE)
            val existing = prefs.getString("installation_id", null)
            if (!existing.isNullOrBlank()) return existing
            val generated = "AKH-" + UUID.randomUUID().toString().uppercase(Locale.US).take(8)
            prefs.edit().putString("installation_id", generated).apply()
            return generated
        }

        fun getDeviceModelDescription(): String {
            val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
            val model = Build.MODEL ?: "Admin Terminal"
            return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        }
    }
}
