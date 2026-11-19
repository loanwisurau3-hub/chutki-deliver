package com.example.data

import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object SecurityUtils {

    private val MOBILE_REGEX = Regex("^[6-9]\\d{9}$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val UPI_REGEX = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")
    private val UTR_DIGITS_REGEX = Regex("^\\d{12}$")
    private val UTR_REPEATED_REGEX = Regex("^(\\d)\\1{11}$")
    private val UTR_PERIODIC_REGEX = Regex("^(\\d{2,4})\\1{2,}$")
    private val SANITIZE_REGEX = Regex("[<>\"'`\\u0000-\\u001F\\u007F\\u200B\\uFEFF]")

    private val LOCKED_STATUSES = setOf(
        "PICKED_UP",
        "OUT_FOR_DELIVERY",
        "CANCELLATION_REQUESTED",
        "EMERGENCY_INVESTIGATION",
        "RETURNED_TO_STORE",
        "DELIVERY_FAILED",
        "ADMIN_CANCELLATION_APPROVED"
    )

    private val COMPLETED_STATUSES = setOf(
        "DELIVERED",
        "CANCELLED",
        "ADMIN_CANCELLATION_APPROVED",
        "RETURNED_TO_STORE",
        "DELIVERY_FAILED"
    )

    val READY_FOR_PICKUP_STATUSES = setOf(
        "READY_FOR_PICKUP",
        "READY",
        "FOOD_READY",
        "PACKED_FOR_PICKUP"
    )

    fun sanitizeText(input: String?, maxLength: Int = 300): String {
        val cleaned = (input ?: "").replace(SANITIZE_REGEX, "").trim()
        return if (cleaned.length > maxLength) cleaned.take(maxLength) else cleaned
    }

    fun isValidIndianMobile(phone: String?): Boolean {
        val digits = (phone ?: "").filter { it.isDigit() }
        val last10 = if (digits.length >= 10) digits.takeLast(10) else digits
        return MOBILE_REGEX.matches(last10)
    }

    fun isValidEmail(email: String?): Boolean =
        EMAIL_REGEX.matches((email ?: "").trim())

    fun isValidUpiId(upi: String?): Boolean =
        UPI_REGEX.matches((upi ?: "").trim())

    fun formatWhatsAppDigits(rawPhone: String?): String {
        val digits = (rawPhone ?: "").filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return if (digits.length == 10) "91$digits" else digits
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun getStorePickupCode(order: OrderRecord?, orderId: String): String {
        val explicit = order?.pickupOtp?.trim()
        if (!explicit.isNullOrBlank()) return explicit
        var hash = 0
        for (ch in orderId) {
            hash = (hash shl 5) - hash + ch.code
        }
        val positiveMod = if (hash == Int.MIN_VALUE) 0 else abs(hash) % 9000
        return (positiveMod + 1000).toString()
    }

    /**
     * Returns the 4-digit store pickup code to display to the rider:
     * - Explicit `pickupOtp` / `pickupCode` from Firebase when present
     * - `null` when only an irreversible `pickupOtpHash` is stored on the order
     * - Deterministic 4-digit fallback code matching [getStorePickupCode] otherwise
     */
    fun getDisplayablePickupCode(order: OrderRecord): String? {
        val explicit = order.pickupOtp?.trim()
        if (!explicit.isNullOrBlank()) return explicit
        if (!order.pickupOtpHash.isNullOrBlank()) return null
        return getStorePickupCode(order, order.orderId)
    }

    fun calculateDistanceKm(lat1: Double?, lon1: Double?, lat2: Double?, lon2: Double?): Double? {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)).coerceIn(0.0, 1.0)
        return r * (2 * atan2(sqrt(a), sqrt(1.0 - a)))
    }

    fun isOrderReadyForPickup(order: OrderRecord): Boolean {
        if (isOrderPostPickupLocked(order) || isOrderCompleted(order)) return false
        val normStatus = order.status.trim().uppercase()
        return normStatus in READY_FOR_PICKUP_STATUSES || order.readyForPickup
    }

    fun formatOrderPrepStatus(order: OrderRecord): String {
        val norm = order.status.trim().uppercase()
        return when {
            isOrderReadyForPickup(order) -> "READY FOR PICKUP"
            norm.isBlank() || norm == "PENDING" || norm == "PLACED" -> "PENDING"
            norm == "ACCEPTED" || norm == "CONFIRMED" -> "ACCEPTED BY RESTAURANT"
            norm == "PREPARING" || norm == "COOKING" || norm == "PACKING" -> "PREPARING ORDER"
            norm == "ASSIGNED" -> "WAITING FOR RESTAURANT"
            else -> norm.replace('_', ' ')
        }
    }

    fun isOrderPostPickupLocked(order: OrderRecord): Boolean =
        order.cancellationLocked || order.pickupVerified || order.status in LOCKED_STATUSES

    fun isOrderCompleted(order: OrderRecord): Boolean =
        order.status in COMPLETED_STATUSES

    fun isTripPostPickupLocked(trip: TripGroup): Boolean =
        trip.orders.any { isOrderPostPickupLocked(it) }

    data class ValidationResult(val valid: Boolean, val reason: String = "")

    fun validateSettlementUtr(utr: String?, existingSettlements: List<SettlementRecord>): ValidationResult {
        val clean = (utr ?: "").trim()
        if (!UTR_DIGITS_REGEX.matches(clean)) {
            return ValidationResult(false, "UTR number must be exactly 12 digits.")
        }
        val knownFakeUtrs = setOf(
            "123456789012",
            "012345678901",
            "987654321098",
            "123456123456",
            "112233445566"
        )
        if (UTR_REPEATED_REGEX.matches(clean) ||
            UTR_PERIODIC_REGEX.matches(clean) ||
            clean in knownFakeUtrs ||
            clean.startsWith("0000") ||
            clean.toSet().size < 3
        ) {
            return ValidationResult(false, "Invalid or suspicious fake UTR pattern is not allowed.")
        }
        if (existingSettlements.any { it.utr.trim() == clean }) {
            return ValidationResult(false, "This UTR number has already been used.")
        }
        return ValidationResult(true)
    }
}
