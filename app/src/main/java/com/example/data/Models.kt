package com.example.data

data class RiderKycInfo(
    val licenseNumber: String = "",
    val vehicleRegNumber: String = "",
    val idLast4: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val kycStatus: String = "NOT_SUBMITTED",
    val kycNote: String = "",
    val idDocumentPhoto: String = "",
    val selfiePhoto: String = "",
    val submittedAt: Long = 0L
)

data class RiderProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val upiId: String = "",
    val vehicle: String = "",
    val approved: Boolean = false,
    val online: Boolean = false,
    val suspended: Boolean = false,
    val adminNote: String = "",
    val ratingAvg: Double = 5.0,
    val ratingCount: Int = 0,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Int? = null,
    val lastSeen: Long = 0L,
    val shiftStartedAt: Long = 0L,
    val kyc: RiderKycInfo = RiderKycInfo(),
    val agreementAccepted: Boolean = false,
    val privacyPolicyAccepted: Boolean = false,
    val agreementAcceptedVersion: String = "",
    val agreementAcceptedHash: String = "",
    val agreementAcceptedAt: Long = 0L
) {
    fun hasAcceptedLatestAgreement(settings: AdminSettings): Boolean {
        if (!settings.requireAgreementAcceptance) return true
        if (!agreementAccepted || !privacyPolicyAccepted) return false
        val versionMatches = agreementAcceptedVersion.trim() == settings.agreementVersion.trim()
        if (!versionMatches) return false
        val latestHash = settings.computeAgreementDocHash()
        return agreementAcceptedHash.isBlank() || agreementAcceptedHash == latestHash
    }
}

data class StoreInfo(
    val id: String = "",
    val name: String = "Store",
    val address: String = "Gossaigaon Bazar",
    val phone: String = "",
    val lat: Double? = null,
    val lng: Double? = null
)

data class OrderItem(
    val name: String = "",
    val qty: Int = 1
)

data class OrderRecord(
    val orderId: String = "",
    val batchGroupId: String = "",
    val customerUid: String = "",
    val restaurantId: String = "",
    val phone: String = "",
    val address: String = "Gossaigaon",
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val locationLink: String = "",
    val paymentMode: String = "COD",
    val paymentStatus: String = "COD",
    val status: String = "",
    val totalAmount: Double = 0.0,
    val deliveryCharge: Double? = null,
    val distanceKm: Double? = null,
    val riderPerKmRate: Double? = null,
    val riderEarning: Double? = null,
    val deliveryUid: String? = null,
    val assignedRiderId: String? = null,
    val riderAccepted: Boolean? = null,
    val riderAcceptedAt: Long? = null,
    val assignedAt: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deliveredAt: Long? = null,
    val deliveryProofPhoto: String = "",
    val pickupOtp: String? = null,
    val pickupOtpHash: String? = null,
    val readyForPickup: Boolean = false,
    val readyForPickupAt: Long? = null,
    val pickupVerified: Boolean = false,
    val cancellationLocked: Boolean = false,
    val deliveryOtp: String? = null,
    val deliveryOtpHash: String? = null,
    val deliveryOtpExpiresAt: Long? = null,
    val emergencyAlert: Boolean = false,
    val emergencyDetails: String = "",
    val cancellationRequested: Boolean = false,
    val cancellationReason: String = "",
    val lastLocationUpdate: Long? = null,
    val riderLat: Double? = null,
    val riderLng: Double? = null,
    val rejectedByRiders: Set<String> = emptySet(),
    val items: List<OrderItem> = emptyList()
)

data class OrderEarningBreakdown(
    val distanceKm: Double?,
    val perKmRate: Double,
    val baseFee: Double,
    val totalEarning: Double,
    val isDistanceBased: Boolean,
    val isLocked: Boolean
)

data class TripEarningSummary(
    val totalEarning: Double,
    val totalDistanceKm: Double?,
    val effectivePerKmRate: Double,
    val isDistanceBased: Boolean,
    val isLocked: Boolean
)

data class StoreStop(
    val order: OrderRecord,
    val store: StoreInfo,
    val distanceKm: Double,
    val isPickedUp: Boolean
)

data class TripGroup(
    val tripId: String,
    val customerUid: String,
    val phone: String,
    val address: String,
    val customerLat: Double?,
    val customerLng: Double?,
    val locationLink: String,
    val paymentMode: String,
    val paymentStatus: String,
    val emergencyAlert: Boolean,
    val emergencyDetails: String,
    val assignedAt: Long,
    val orders: List<OrderRecord>
)

data class AdminSettings(
    val upiId: String = "gossaigaonbazar@apl",
    val riderBaseFee: Double = 25.0,
    val distanceEarningsEnabled: Boolean = true,
    val riderPerKmRate: Double = 10.0,
    val riderDistanceBasePay: Double = 0.0,
    val riderBaseDistanceKm: Double = 0.0,
    val riderMinTripEarning: Double = 0.0,
    val distanceRoadMultiplier: Double = 1.0,
    val maxRiderCashLimit: Double = 3000.0,
    val riderAcceptTimeoutSec: Int = 45,
    val minRiderPayout: Double = 10.0,
    val maxRiderPayout: Double = 10000.0,
    val minRiderCodDeposit: Double = 10.0,
    val maxRiderCodDeposit: Double = 25000.0,
    val allowRiderPassTrip: Boolean = true,
    val allowRiderSos: Boolean = true,
    val riderAnnouncement: String = "",
    val supportPhone: String = "",
    val supportWhatsapp: String = "",
    val supportHours: String = "",
    val agreementVersion: String = LegalDocumentDefaults.DEFAULT_VERSION,
    val agreementTitle: String = LegalDocumentDefaults.DEFAULT_AGREEMENT_TITLE,
    val agreementContent: String = LegalDocumentDefaults.DEFAULT_AGREEMENT_TEXT,
    val privacyPolicyTitle: String = LegalDocumentDefaults.DEFAULT_PRIVACY_TITLE,
    val privacyPolicyContent: String = LegalDocumentDefaults.DEFAULT_PRIVACY_TEXT,
    val agreementUrl: String = "",
    val privacyPolicyUrl: String = "",
    val agreementUpdatedAt: Long = 0L,
    val requireAgreementAcceptance: Boolean = true,
    val dailyTripTarget: Int = 10,
    val dailyBonusAmount: Double = 100.0,
    val requireDeliveryPhoto: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = ""
) {
    fun computeAgreementDocHash(): String =
        SecurityUtils.sha256Hex(
            "${agreementVersion.trim()}|${agreementTitle.trim()}|${agreementContent.trim()}|${privacyPolicyTitle.trim()}|${privacyPolicyContent.trim()}|${agreementUrl.trim()}|${privacyPolicyUrl.trim()}"
        ).take(12)
}

object LegalDocumentDefaults {
    const val DEFAULT_VERSION = "1.0"
    const val DEFAULT_AGREEMENT_TITLE = "Chutki Rider Partner Agreement"
    const val DEFAULT_PRIVACY_TITLE = "Privacy Policy & Live GPS Data Notice"

    val DEFAULT_AGREEMENT_TEXT = """
        1. Delivery Partner Engagement
        By activating your shift on Chutki Rider, you agree to operate as an independent delivery partner for Gossaigaon Bazar multi-store grocery orders and follow all dispatch instructions managed by Admin.

        2. Order Verification & Anti-Scam Policy
        • Always verify the 4-digit Store Pickup OTP with the merchant before collecting items.
        • Once pickup is verified, post-pickup cancellation locks are active. Only Gossaigaon Bazar Admin may cancel or reassign locked orders.
        • Collect the Customer 4-digit Delivery PIN at the doorstep before completing delivery.

        3. Cash-on-Delivery (COD) & Settlement Discipline
        • Cash collected from COD orders must remain within the Admin cash limit and be settled via verified 12-digit UPI UTR reference.
        • Submitting duplicate, altered, or false UTR references may result in immediate account suspension.

        4. Safety, Conduct & Updates
        • Ride safely with a valid driving license, helmet, and registered vehicle details.
        • Admin may update this agreement at any time; continued shift access requires reviewing and accepting the latest version.
    """.trimIndent()

    val DEFAULT_PRIVACY_TEXT = """
        1. Information Collected
        We collect your Rider Name, Email Address, 10-digit Mobile Number, Payout UPI ID, Vehicle Details, delivery history, COD settlement UTRs, and support tickets to operate the Chutki Rider dispatch terminal.

        2. Live GPS Location Tracking (Foreground Service)
        • When you toggle Duty ON or carry an active picked-up order, Chutki Rider runs a visible foreground location service that updates your latitude, longitude, and GPS accuracy to Firebase Realtime Database every 10 seconds.
        • Location data is used solely for nearest-rider trip assignment, live delivery tracking, route-safety verification, and Admin dispatch support.
        • GPS tracking stops automatically when you turn Duty OFF (with no active picked-up orders) or sign out.

        3. Background Notifications & Security Audit Logs
        • While On Duty, a background notification monitor alerts you to new trips, Admin broadcasts, approvals, and payout updates.
        • Security events (failed OTPs, GPS interruptions, SOS alerts, and agreement acceptance timestamps) are logged for fraud prevention and dispute resolution.

        4. Data Access & Control
        • Your profile and payout UPI details can be updated anytime in the Profile tab.
        • For account deletion or privacy inquiries, contact Gossaigaon Bazar Admin via the Helpline in Profile.
    """.trimIndent()
}

data class SettlementRecord(
    val id: String = "",
    val riderUid: String = "",
    val riderName: String = "",
    val amount: Double = 0.0,
    val mode: String = "UPI",
    val utr: String = "",
    val status: String = "PENDING",
    val createdAt: Long = 0L
)

data class RiderPayoutRecord(
    val id: String = "",
    val riderUid: String = "",
    val riderName: String = "",
    val riderPhone: String = "",
    val riderUpiId: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING",
    val adminRef: String = "",
    val createdAt: Long = 0L
)

data class RiderRating(
    val id: String = "",
    val customerName: String = "Customer",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = 0L
)

data class FeedbackRecord(
    val id: String = "",
    val role: String = "RIDER",
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val adminReply: String = "",
    val createdAt: Long = 0L
)

data class EmergencyRequestRecord(
    val id: String = "",
    val orderId: String = "",
    val riderId: String = "",
    val reason: String = "",
    val description: String = "",
    val status: String = "PENDING",
    val createdAt: Long = 0L
)
