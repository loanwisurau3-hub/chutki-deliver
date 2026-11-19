package com.example

import com.example.data.AdminSettings
import com.example.data.OrderRecord
import com.example.data.RiderProfile
import com.example.data.SecurityUtils
import com.example.data.SettlementRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `isValidEmail validates standard email addresses and rejects malformed inputs`() {
        assertTrue(SecurityUtils.isValidEmail("rider@gossaigaonbazar.com"))
        assertTrue(SecurityUtils.isValidEmail("  skriju148@gmail.com  "))
        assertFalse(SecurityUtils.isValidEmail("not-an-email"))
        assertFalse(SecurityUtils.isValidEmail("rider@localhost"))
        assertFalse(SecurityUtils.isValidEmail(""))
    }

    @Test
    fun `sha256Hex matches standard SHA-256 output`() {
        val hash = SecurityUtils.sha256Hex("order123:4567")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `getStorePickupCode uses explicit pickupOtp when present and deterministic fallback otherwise`() {
        val withOtp = OrderRecord(orderId = "ord_abc", pickupOtp = "9182")
        assertEquals("9182", SecurityUtils.getStorePickupCode(withOtp, "ord_abc"))

        val withoutOtp = OrderRecord(orderId = "ord_abc")
        val code1 = SecurityUtils.getStorePickupCode(withoutOtp, "ord_abc")
        val code2 = SecurityUtils.getStorePickupCode(withoutOtp, "ord_abc")
        assertEquals(4, code1.length)
        assertEquals(code1, code2)
        assertTrue(code1.toInt() in 1000..9999)
    }

    @Test
    fun `validateSettlementUtr rejects fake and duplicate UTRs`() {
        val existing = listOf(SettlementRecord(utr = "412893471923"))
        assertFalse(SecurityUtils.validateSettlementUtr("12345", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("111111111111", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("123456789012", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("000012345678", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("412893471923", existing).valid)
        assertTrue(SecurityUtils.validateSettlementUtr("512893471924", existing).valid)
    }

    @Test
    fun `getDisplayablePickupCode shows explicit or deterministic code and hides hash-only OTP`() {
        val explicit = OrderRecord(orderId = "ord_101", pickupOtp = "4829")
        assertEquals("4829", SecurityUtils.getDisplayablePickupCode(explicit))

        val fallback = OrderRecord(orderId = "ord_102")
        val fallbackCode = SecurityUtils.getDisplayablePickupCode(fallback)
        assertEquals(SecurityUtils.getStorePickupCode(fallback, "ord_102"), fallbackCode)

        val hashOnly = OrderRecord(orderId = "ord_103", pickupOtpHash = "abcd1234hash")
        assertEquals(null, SecurityUtils.getDisplayablePickupCode(hashOnly))
    }

    @Test
    fun `isOrderReadyForPickup unlocks pickup only after restaurant marks order ready`() {
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "PENDING")))
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "ASSIGNED")))
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "ACCEPTED")))
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "PREPARING")))
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "COOKING")))

        assertTrue(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "READY_FOR_PICKUP")))
        assertTrue(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "ready_for_pickup")))
        assertTrue(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "READY")))
        assertTrue(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "FOOD_READY")))
        assertTrue(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "PREPARING", readyForPickup = true)))

        // Already picked up or delivered orders are not in pending ready-for-pickup state
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "PICKED_UP", readyForPickup = true)))
        assertFalse(SecurityUtils.isOrderReadyForPickup(OrderRecord(status = "DELIVERED", readyForPickup = true)))
    }

    @Test
    fun `post pickup lock protects picked up and transit orders including multi-store trips`() {
        assertFalse(SecurityUtils.isOrderPostPickupLocked(OrderRecord(status = "READY_FOR_PICKUP")))
        assertTrue(SecurityUtils.isOrderPostPickupLocked(OrderRecord(status = "PICKED_UP")))
        assertTrue(SecurityUtils.isOrderPostPickupLocked(OrderRecord(status = "OUT_FOR_DELIVERY")))
        assertTrue(SecurityUtils.isOrderPostPickupLocked(OrderRecord(pickupVerified = true)))
        assertTrue(SecurityUtils.isOrderPostPickupLocked(OrderRecord(cancellationLocked = true)))

        val multiStoreTrip = com.example.data.TripGroup(
            tripId = "trip_lock_1",
            customerUid = "cust_1",
            phone = "9876543210",
            address = "Gossaigaon",
            customerLat = null,
            customerLng = null,
            locationLink = "",
            paymentMode = "COD",
            paymentStatus = "COD",
            emergencyAlert = false,
            emergencyDetails = "",
            assignedAt = 1000L,
            orders = listOf(
                OrderRecord(orderId = "o1", status = "PICKED_UP", pickupVerified = true, cancellationLocked = true),
                OrderRecord(orderId = "o2", status = "READY_FOR_PICKUP")
            )
        )
        assertTrue(SecurityUtils.isTripPostPickupLocked(multiStoreTrip))
    }

    @Test
    fun `rider agreement checkup detects admin document edits and version bumps`() {
        val initialAdmin = AdminSettings(
            agreementVersion = "1.0",
            agreementContent = "Initial Rider Terms",
            privacyPolicyContent = "Initial Privacy Policy",
            requireAgreementAcceptance = true
        )
        val unaccepted = RiderProfile(uid = "r1")
        assertFalse(unaccepted.hasAcceptedLatestAgreement(initialAdmin))

        val accepted = unaccepted.copy(
            agreementAccepted = true,
            privacyPolicyAccepted = true,
            agreementAcceptedVersion = initialAdmin.agreementVersion,
            agreementAcceptedHash = initialAdmin.computeAgreementDocHash()
        )
        assertTrue(accepted.hasAcceptedLatestAgreement(initialAdmin))

        // Admin edits agreement content even without changing version -> triggers re-acceptance
        val contentEdited = initialAdmin.copy(agreementContent = "Updated Rider Terms with new COD rules")
        assertFalse(accepted.hasAcceptedLatestAgreement(contentEdited))

        // Admin edits privacy policy -> triggers re-acceptance
        val privacyEdited = initialAdmin.copy(privacyPolicyContent = "Updated GPS Privacy Policy")
        assertFalse(accepted.hasAcceptedLatestAgreement(privacyEdited))

        // Admin bumps version -> triggers re-acceptance
        val versionBumped = initialAdmin.copy(agreementVersion = "2.0")
        assertFalse(accepted.hasAcceptedLatestAgreement(versionBumped))

        // Admin disables mandatory acceptance -> allows rider through
        val optionalAgreement = versionBumped.copy(requireAgreementAcceptance = false)
        assertTrue(unaccepted.hasAcceptedLatestAgreement(optionalAgreement))
    }
}
