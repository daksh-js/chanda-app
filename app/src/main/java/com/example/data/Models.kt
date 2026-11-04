package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@IgnoreExtraProperties
data class Donor(
    @DocumentId val id: String = "",
    val userId: String = "",
    val donorCode: String = "",
    val fullName: String = "",
    val fatherName: String = "",
    val mobileNumber: String = "",
    val whatsappNumber: String = "",
    val alternateMobile: String? = null,
    val address: String = "",
    val village: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pinCode: String = "",
    val occupation: String? = null,
    val birthday: String? = null,
    val notes: String? = null,
    val photoUri: String? = null,
    val addedByAdmin: String = "",
    val lifetimeDonation: Long = 0L,
    val donationCount: Long = 0L,
    val isFavorite: Boolean = false,
    val consentGiven: Boolean = true,
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Donation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val donorId: String = "",
    val donorCode: String = "",
    val donorName: String = "",
    val donorMobile: String = "",
    val amount: Long = 0L,
    val amountInWords: String = "",
    val donationDate: String = "",
    val purpose: String = "",
    val paymentMethod: String = "CASH",
    val receiptNumber: String = "",
    val receivedByAdmin: String = "",
    val notes: String? = null,
    val status: String = "CONFIRMED", // CONFIRMED, CORRECTED, REVERSED
    val correctionNote: String? = null,
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReceiptRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val receiptNumber: String = "",
    val donationId: String = "",
    val donorId: String = "",
    val donorCode: String = "",
    val donorName: String = "",
    val donorMobile: String = "",
    val amount: Long = 0L,
    val amountInWords: String = "",
    val donationDate: String = "",
    val purpose: String? = null,
    val paymentMethod: String = "CASH",
    val receivedByAdmin: String = "",
    val akhadaName: String = "",
    val akhadaAddress: String = "",
    val thankYouMessage: String? = null,
    val qrPayload: String = "",
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuthorizedDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val installationId: String = "",
    val deviceName: String = "",
    val adminName: String = "",
    val role: String = "OWNER_ADMIN", // OWNER_ADMIN, STAFF_ADMIN
    val status: String = "APPROVED", // APPROVED, PENDING, REVOKED
    val canAddDonor: Boolean = true,
    val canEditDonor: Boolean = true,
    val canDeleteDonor: Boolean = true,
    val canRecordDonation: Boolean = true,
    val canViewReports: Boolean = true,
    val canExportData: Boolean = true,
    val lastActiveAt: String = "",
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actionType: String = "",
    val description: String = "",
    val adminName: String = "",
    val deviceId: String = "",
    val entityType: String = "DONATION", // DONOR, DONATION, RECEIPT, DEVICE, SETTINGS
    val entityId: String = "",
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val akhadaName: String = "Shri Bajrang Kushti Akhada Trust",
    val akhadaRegNumber: String = "Reg #AK-409 • Corpus Vault",
    val akhadaAddress: String = "Main Chopal Arena Road, Chhara, Jhajjar, Haryana - 124504",
    val ownerAdminName: String = "Vikramaditya Sengupta",
    val ownerAdminPhone: String = "+91 98123 45678",
    val vaultAllocationId: String = "8934",
    val activeSeason: String = "Active Season 2026-27",
    val language: String = "BILINGUAL", // EN, HI, BILINGUAL
    val darkMode: Boolean = true,
    val maxAuthorizedDevices: Long = 6L,
    val thankYouTemplateHi: String = "आपके द्वारा हमारे अखाड़े को दिए गए सहयोग के लिए हृदय से धन्यवाद। आपका योगदान हमारे अखाड़े के विकास में महत्वपूर्ण भूमिका निभाएगा।",
    val thankYouTemplateEn: String = "Thank you wholeheartedly for your generous cash contribution to our Akhada. Your support plays a vital role in training our athletes.",
    val reminderTemplateHi: String = "नमस्कार! आपके सहयोग से हमारा अखाड़ा निरंतर प्रगति कर रहा है। आगामी दंगल एवं पहलवानों के आहार कोष हेतु आपके सहयोग की अपेक्षा है।",
    val reminderTemplateEn: String = "Namaskar! Your continued patronage empowers our Akhada wrestlers. We warmly welcome your annual cash contribution for the upcoming season.",
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object IndianCurrencyFormatter {
    private val indianLocale = Locale.forLanguageTag("en-IN")

    fun formatInr(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale)
        return "₹${formatter.format(amount)}"
    }

    fun formatCompactInr(amount: Long): String {
        return when {
            amount >= 10_000_000L -> String.format(Locale.US, "₹%.2f Cr", amount / 10_000_000.0)
            amount >= 100_000L -> String.format(Locale.US, "₹%.2f L", amount / 100_000.0)
            else -> formatInr(amount)
        }
    }

    fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun nowReadable(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    }

    fun amountToWords(amount: Long): String {
        if (amount <= 0L) return "Zero Indian Rupees Only"
        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convertBelowThousand(n: Int): String {
            return when {
                n == 0 -> ""
                n < 20 -> units[n]
                n < 100 -> tens[n / 10] + (if (n % 10 != 0) " " + units[n % 10] else "")
                else -> units[n / 100] + " Hundred" + (if (n % 100 != 0) " " + convertBelowThousand(n % 100) else "")
            }
        }

        var remaining = amount
        val parts = mutableListOf<String>()

        val crores = (remaining / 10_000_000L).toInt()
        remaining %= 10_000_000L
        if (crores > 0) {
            parts.add("${convertBelowThousand(crores)} Crore")
        }

        val lakhs = (remaining / 100_000L).toInt()
        remaining %= 100_000L
        if (lakhs > 0) {
            parts.add("${convertBelowThousand(lakhs)} Lakh")
        }

        val thousands = (remaining / 1_000L).toInt()
        remaining %= 1_000L
        if (thousands > 0) {
            parts.add("${convertBelowThousand(thousands)} Thousand")
        }

        val hundreds = remaining.toInt()
        if (hundreds > 0) {
            parts.add(convertBelowThousand(hundreds))
        }

        return "${parts.joinToString(" ")} Indian Rupees Only"
    }
}
