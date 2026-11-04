package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChandaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- REALTIME STREAMS ---

    fun observeDonors(userId: String = requireUserId()): Flow<List<Donor>> = flow {
        val path = "donors"
        emitAllConfigured(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Donor::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lifetimeDonation }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeDonations(userId: String = requireUserId()): Flow<List<Donation>> = flow {
        val path = "donations"
        emitAllConfigured(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeReceipts(userId: String = requireUserId()): Flow<List<ReceiptRecord>> = flow {
        val path = "receipts"
        emitAllConfigured(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ReceiptRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAuthorizedDevices(userId: String = requireUserId()): Flow<List<AuthorizedDevice>> = flow {
        val path = "authorized_devices"
        emitAllConfigured(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AuthorizedDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeActivityLogs(userId: String = requireUserId()): Flow<List<ActivityLog>> = flow {
        val path = "activity_logs"
        emitAllConfigured(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAppSettings(userId: String = requireUserId()): Flow<AppSettings?> = flow {
        val path = "app_settings"
        emitAllConfigured(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .firstOrNull()
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAllConfigured(upstream: Flow<T>) {
        upstream.collect { emit(it) }
    }

    // --- DIRECT QUERIES FOR TESTING & VALIDATION ---

    suspend fun getDonorsForUser(userId: String = requireUserId()): Result<List<Donor>> = runCatching {
        val path = "donors"
        try {
            val snapshot = db.collection(path).whereEqualTo("userId", userId).get().await()
            snapshot.toObjects(Donor::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getDonorById(donorId: String): Result<Donor?> = runCatching {
        val docRef = db.collection("donors").document(donorId)
        try {
            docRef.get().await().toObject(Donor::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    // --- WRITE OPERATIONS ---

    suspend fun createDonor(
        donorCode: String,
        fullName: String,
        fatherName: String,
        mobileNumber: String,
        whatsappNumber: String,
        alternateMobile: String? = null,
        address: String,
        village: String,
        city: String,
        district: String,
        state: String,
        pinCode: String,
        occupation: String? = null,
        birthday: String? = null,
        notes: String? = null,
        photoUri: String? = null,
        addedByAdmin: String,
        isFavorite: Boolean = false,
        consentGiven: Boolean = true,
        deviceId: String = "PRIMARY_DEVICE"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val donorId = "donor_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("donors").document(donorId)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "donorCode" to donorCode.trim(),
            "fullName" to fullName.trim(),
            "fatherName" to fatherName.trim(),
            "mobileNumber" to mobileNumber.trim(),
            "whatsappNumber" to whatsappNumber.trim().ifEmpty { mobileNumber.trim() },
            "address" to address.trim(),
            "village" to village.trim(),
            "city" to city.trim(),
            "district" to district.trim(),
            "state" to state.trim(),
            "pinCode" to pinCode.trim(),
            "addedByAdmin" to addedByAdmin.trim(),
            "lifetimeDonation" to 0L,
            "donationCount" to 0L,
            "isFavorite" to isFavorite,
            "consentGiven" to consentGiven,
            "createdBy" to uid,
            "updatedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!alternateMobile.isNullOrBlank()) payload["alternateMobile"] = alternateMobile.trim()
        if (!occupation.isNullOrBlank()) payload["occupation"] = occupation.trim()
        if (!birthday.isNullOrBlank()) payload["birthday"] = birthday.trim()
        if (!notes.isNullOrBlank()) payload["notes"] = notes.trim()
        if (!photoUri.isNullOrBlank()) payload["photoUri"] = photoUri.trim()

        try {
            docRef.set(payload).await()
            logActivityInternal(
                uid = uid,
                actionType = "DONOR_ADDED",
                description = "Registered donor ${fullName.trim()} ($donorCode)",
                adminName = addedByAdmin.trim(),
                deviceId = deviceId,
                entityType = "DONOR",
                entityId = donorId
            )
            donorId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDonor(donor: Donor, adminName: String, deviceId: String = "PRIMARY_DEVICE"): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(donor.id.isNotBlank()) { "Donor ID must not be blank" }
        requireNotNull(donor.createdAt) { "Donor createdAt must not be null for update" }

        val docRef = db.collection("donors").document(donor.id)
        val payload = mutableMapOf<String, Any>(
            "userId" to donor.userId,
            "donorCode" to donor.donorCode,
            "fullName" to donor.fullName.trim(),
            "fatherName" to donor.fatherName.trim(),
            "mobileNumber" to donor.mobileNumber.trim(),
            "whatsappNumber" to donor.whatsappNumber.trim(),
            "address" to donor.address.trim(),
            "village" to donor.village.trim(),
            "city" to donor.city.trim(),
            "district" to donor.district.trim(),
            "state" to donor.state.trim(),
            "pinCode" to donor.pinCode.trim(),
            "addedByAdmin" to donor.addedByAdmin,
            "lifetimeDonation" to donor.lifetimeDonation,
            "donationCount" to donor.donationCount,
            "isFavorite" to donor.isFavorite,
            "consentGiven" to donor.consentGiven,
            "createdBy" to donor.createdBy,
            "updatedBy" to uid,
            "createdAt" to donor.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!donor.alternateMobile.isNullOrBlank()) payload["alternateMobile"] = donor.alternateMobile.trim()
        if (!donor.occupation.isNullOrBlank()) payload["occupation"] = donor.occupation.trim()
        if (!donor.birthday.isNullOrBlank()) payload["birthday"] = donor.birthday.trim()
        if (!donor.notes.isNullOrBlank()) payload["notes"] = donor.notes.trim()
        if (!donor.photoUri.isNullOrBlank()) payload["photoUri"] = donor.photoUri.trim()

        try {
            docRef.set(payload).await()
            logActivityInternal(
                uid = uid,
                actionType = "DONOR_UPDATED",
                description = "Updated profile of ${donor.fullName} (${donor.donorCode})",
                adminName = adminName,
                deviceId = deviceId,
                entityType = "DONOR",
                entityId = donor.id
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDonor(donor: Donor, adminName: String, deviceId: String = "PRIMARY_DEVICE"): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(donor.id.isNotBlank()) { "Donor ID must not be blank" }
        val docRef = db.collection("donors").document(donor.id)
        try {
            docRef.delete().await()
            logActivityInternal(
                uid = uid,
                actionType = "DONOR_DELETED",
                description = "Deleted donor ${donor.fullName} (${donor.donorCode})",
                adminName = adminName,
                deviceId = deviceId,
                entityType = "DONOR",
                entityId = donor.id
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun recordCashDonation(
        donor: Donor,
        amount: Long,
        donationDate: String,
        purpose: String,
        receivedByAdmin: String,
        notes: String? = null,
        akhadaName: String,
        akhadaAddress: String,
        thankYouMessage: String,
        deviceId: String = "PRIMARY_DEVICE"
    ): Result<ReceiptRecord> = runCatching {
        val uid = requireUserId()
        require(amount > 0L) { "Donation amount must be strictly greater than ₹0" }

        val donationId = "don_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val receiptId = "rcp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val shortHash = UUID.randomUUID().toString().uppercase(Locale.US).take(4)
        val yearPart = donationDate.take(4).ifEmpty { "2026" }
        val receiptNumber = "AKD-$yearPart-$shortHash"
        val amountWords = IndianCurrencyFormatter.amountToWords(amount)

        val donationRef = db.collection("donations").document(donationId)
        val receiptRef = db.collection("receipts").document(receiptId)
        val donorRef = db.collection("donors").document(donor.id)

        val donationPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "donorId" to donor.id,
            "donorCode" to donor.donorCode,
            "donorName" to donor.fullName,
            "donorMobile" to donor.mobileNumber,
            "amount" to amount,
            "amountInWords" to amountWords,
            "donationDate" to donationDate,
            "purpose" to purpose.trim().ifEmpty { "General Akhada Chanda" },
            "paymentMethod" to "CASH",
            "receiptNumber" to receiptNumber,
            "receivedByAdmin" to receivedByAdmin.trim(),
            "status" to "CONFIRMED",
            "createdBy" to uid,
            "updatedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!notes.isNullOrBlank()) donationPayload["notes"] = notes.trim()

        val qrPayload = "CHANDA|RCP:$receiptNumber|DONOR:${donor.donorCode}|NAME:${donor.fullName}|AMT:INR $amount|MODE:CASH|DATE:$donationDate"
        val receiptPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "receiptNumber" to receiptNumber,
            "donationId" to donationId,
            "donorId" to donor.id,
            "donorCode" to donor.donorCode,
            "donorName" to donor.fullName,
            "donorMobile" to donor.mobileNumber,
            "amount" to amount,
            "amountInWords" to amountWords,
            "donationDate" to donationDate,
            "purpose" to purpose.trim().ifEmpty { "General Akhada Chanda" },
            "paymentMethod" to "CASH",
            "receivedByAdmin" to receivedByAdmin.trim(),
            "akhadaName" to akhadaName,
            "akhadaAddress" to akhadaAddress,
            "thankYouMessage" to thankYouMessage,
            "qrPayload" to qrPayload,
            "createdBy" to uid,
            "updatedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            donationRef.set(donationPayload).await()
            receiptRef.set(receiptPayload).await()

            if (donor.id.isNotBlank() && donor.createdAt != null) {
                val updatedDonor = donor.copy(
                    lifetimeDonation = donor.lifetimeDonation + amount,
                    donationCount = donor.donationCount + 1L
                )
                updateDonor(updatedDonor, receivedByAdmin, deviceId)
            }

            logActivityInternal(
                uid = uid,
                actionType = "DONATION_RECORDED",
                description = "CASH Chanda ₹$amount from ${donor.fullName} (Receipt $receiptNumber)",
                adminName = receivedByAdmin.trim(),
                deviceId = deviceId,
                entityType = "DONATION",
                entityId = donationId
            )

            ReceiptRecord(
                id = receiptId,
                userId = uid,
                receiptNumber = receiptNumber,
                donationId = donationId,
                donorId = donor.id,
                donorCode = donor.donorCode,
                donorName = donor.fullName,
                donorMobile = donor.mobileNumber,
                amount = amount,
                amountInWords = amountWords,
                donationDate = donationDate,
                purpose = purpose.trim().ifEmpty { "General Akhada Chanda" },
                paymentMethod = "CASH",
                receivedByAdmin = receivedByAdmin.trim(),
                akhadaName = akhadaName,
                akhadaAddress = akhadaAddress,
                thankYouMessage = thankYouMessage,
                qrPayload = qrPayload,
                createdBy = uid,
                updatedBy = uid
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, donationRef.path)
            throw e
        }
    }

    suspend fun correctOrReverseDonation(
        donation: Donation,
        donor: Donor?,
        newAmount: Long,
        newPurpose: String,
        newStatus: String, // CORRECTED or REVERSED
        correctionNote: String,
        adminName: String,
        deviceId: String = "PRIMARY_DEVICE"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(donation.id.isNotBlank()) { "Donation ID required" }
        requireNotNull(donation.createdAt) { "Donation createdAt required" }

        val effectiveAmount = if (newStatus == "REVERSED") donation.amount else newAmount
        val amountWords = IndianCurrencyFormatter.amountToWords(effectiveAmount)
        val donationRef = db.collection("donations").document(donation.id)

        val payload = mutableMapOf<String, Any>(
            "userId" to donation.userId,
            "donorId" to donation.donorId,
            "donorCode" to donation.donorCode,
            "donorName" to donation.donorName,
            "donorMobile" to donation.donorMobile,
            "amount" to effectiveAmount,
            "amountInWords" to amountWords,
            "donationDate" to donation.donationDate,
            "purpose" to newPurpose.trim().ifEmpty { donation.purpose },
            "paymentMethod" to "CASH",
            "receiptNumber" to donation.receiptNumber,
            "receivedByAdmin" to donation.receivedByAdmin,
            "status" to newStatus,
            "correctionNote" to correctionNote.trim(),
            "createdBy" to donation.createdBy,
            "updatedBy" to uid,
            "createdAt" to donation.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!donation.notes.isNullOrBlank()) payload["notes"] = donation.notes

        try {
            donationRef.set(payload).await()
            if (donor != null && donor.createdAt != null) {
                val delta = when {
                    donation.status != "REVERSED" && newStatus == "REVERSED" -> -donation.amount
                    donation.status == "REVERSED" && newStatus != "REVERSED" -> effectiveAmount
                    else -> effectiveAmount - donation.amount
                }
                val countDelta = when {
                    donation.status != "REVERSED" && newStatus == "REVERSED" -> -1L
                    donation.status == "REVERSED" && newStatus != "REVERSED" -> 1L
                    else -> 0L
                }
                updateDonor(
                    donor.copy(
                        lifetimeDonation = (donor.lifetimeDonation + delta).coerceAtLeast(0L),
                        donationCount = (donor.donationCount + countDelta).coerceAtLeast(0L)
                    ),
                    adminName,
                    deviceId
                )
            }
            logActivityInternal(
                uid = uid,
                actionType = if (newStatus == "REVERSED") "DONATION_REVERSED" else "DONATION_CORRECTED",
                description = "$newStatus receipt ${donation.receiptNumber}: ${correctionNote.trim()}",
                adminName = adminName,
                deviceId = deviceId,
                entityType = "DONATION",
                entityId = donation.id
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, donationRef.path)
            throw e
        }
    }

    // --- DEVICE AUTHORIZATION (UP TO 6 ADMIN DEVICES) ---

    suspend fun enrollOrUpdateAuthorizedDevice(
        deviceId: String,
        installationId: String,
        deviceName: String,
        adminName: String,
        role: String,
        status: String,
        canAddDonor: Boolean = true,
        canEditDonor: Boolean = true,
        canDeleteDonor: Boolean = role == "OWNER_ADMIN",
        canRecordDonation: Boolean = true,
        canViewReports: Boolean = true,
        canExportData: Boolean = role == "OWNER_ADMIN",
        existingCreatedAt: com.google.firebase.Timestamp? = null,
        existingCreatedBy: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val sanitizedId = deviceId.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").take(64)
        val docRef = db.collection("authorized_devices").document(sanitizedId)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "installationId" to installationId,
            "deviceName" to deviceName.trim(),
            "adminName" to adminName.trim(),
            "role" to role,
            "status" to status,
            "canAddDonor" to canAddDonor,
            "canEditDonor" to canEditDonor,
            "canDeleteDonor" to canDeleteDonor,
            "canRecordDonation" to canRecordDonation,
            "canViewReports" to canViewReports,
            "canExportData" to canExportData,
            "lastActiveAt" to IndianCurrencyFormatter.nowReadable(),
            "createdBy" to (existingCreatedBy ?: uid),
            "updatedBy" to uid,
            "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            logActivityInternal(
                uid = uid,
                actionType = "DEVICE_${status}",
                description = "Device ${deviceName.trim()} ($adminName) status set to $status ($role)",
                adminName = adminName.trim(),
                deviceId = installationId,
                entityType = "DEVICE",
                entityId = sanitizedId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingCreatedAt == null) OperationType.CREATE else OperationType.UPDATE,
                docRef.path
            )
            throw e
        }
    }

    suspend fun saveAppSettings(
        settings: AppSettings,
        adminName: String,
        deviceId: String = "PRIMARY_DEVICE"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docId = settings.id.ifBlank { "settings_$uid" }.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
        val docRef = db.collection("app_settings").document(docId)

        val payload = mapOf(
            "userId" to uid,
            "akhadaName" to settings.akhadaName.trim(),
            "akhadaRegNumber" to settings.akhadaRegNumber.trim(),
            "akhadaAddress" to settings.akhadaAddress.trim(),
            "ownerAdminName" to settings.ownerAdminName.trim(),
            "ownerAdminPhone" to settings.ownerAdminPhone.trim(),
            "vaultAllocationId" to settings.vaultAllocationId.trim(),
            "activeSeason" to settings.activeSeason.trim(),
            "language" to settings.language,
            "darkMode" to settings.darkMode,
            "maxAuthorizedDevices" to settings.maxAuthorizedDevices.coerceIn(1L, 6L),
            "thankYouTemplateHi" to settings.thankYouTemplateHi.trim(),
            "thankYouTemplateEn" to settings.thankYouTemplateEn.trim(),
            "reminderTemplateHi" to settings.reminderTemplateHi.trim(),
            "reminderTemplateEn" to settings.reminderTemplateEn.trim(),
            "createdBy" to settings.createdBy.ifBlank { uid },
            "updatedBy" to uid,
            "createdAt" to (settings.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            logActivityInternal(
                uid = uid,
                actionType = "SETTINGS_UPDATED",
                description = "Updated Akhada settings (${settings.akhadaName})",
                adminName = adminName,
                deviceId = deviceId,
                entityType = "SETTINGS",
                entityId = docId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (settings.createdAt == null) OperationType.CREATE else OperationType.UPDATE,
                docRef.path
            )
            throw e
        }
    }

    private suspend fun logActivityInternal(
        uid: String,
        actionType: String,
        description: String,
        adminName: String,
        deviceId: String,
        entityType: String,
        entityId: String
    ) {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val logRef = db.collection("activity_logs").document(logId)
        val payload = mapOf(
            "userId" to uid,
            "actionType" to actionType.take(60),
            "description" to description.take(500),
            "adminName" to adminName.ifBlank { "Owner Admin" }.take(100),
            "deviceId" to deviceId.ifBlank { "PRIMARY" }.take(128),
            "entityType" to entityType,
            "entityId" to entityId.ifBlank { "NONE" }.take(128),
            "createdBy" to uid,
            "updatedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            logRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logRef.path)
        }
    }
}
