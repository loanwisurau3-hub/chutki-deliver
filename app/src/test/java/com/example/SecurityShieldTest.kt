package com.example

import com.example.data.SecurityUtils
import com.example.data.SettlementRecord
import com.example.security.ActionRateLimiter
import com.example.security.SecurityShield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityShieldTest {

    @Test
    fun `validateLocationFix blocks mock GPS, null-island, and impossible teleportation jumps`() {
        val mockCheck = SecurityShield.validateLocationFix(
            lat = 26.4530,
            lng = 89.9690,
            isMockProvider = true,
            prevLat = null,
            prevLng = null,
            prevFixAtMs = 0L,
            nowMs = 10_000L
        )
        assertFalse(mockCheck.isValid)
        assertTrue(mockCheck.isMocked)

        val nullIsland = SecurityShield.validateLocationFix(
            lat = 0.0,
            lng = 0.0,
            isMockProvider = false,
            prevLat = null,
            prevLng = null,
            prevFixAtMs = 0L,
            nowMs = 10_000L
        )
        assertFalse(nullIsland.isValid)

        // Teleporting ~20 km in 10 seconds -> implied speed > 7000 km/h
        val teleport = SecurityShield.validateLocationFix(
            lat = 26.6330,
            lng = 89.9690,
            isMockProvider = false,
            prevLat = 26.4530,
            prevLng = 89.9690,
            prevFixAtMs = 10_000L,
            nowMs = 20_000L
        )
        assertFalse(teleport.isValid)
        assertTrue(teleport.isTeleported)

        // Genuine motorbike ride: ~120 meters in 10 seconds (~43 km/h)
        val normalRide = SecurityShield.validateLocationFix(
            lat = 26.4540,
            lng = 89.9690,
            isMockProvider = false,
            prevLat = 26.4530,
            prevLng = 89.9690,
            prevFixAtMs = 10_000L,
            nowMs = 20_000L
        )
        assertTrue(normalRide.isValid)
    }

    @Test
    fun `inspectTextForSpamOrInjection blocks XSS, phishing URLs, repeated spam, and oversized payloads`() {
        assertFalse(SecurityShield.inspectTextForSpamOrInjection("<script>alert(1)</script>", "Subject").safe)
        assertFalse(SecurityShield.inspectTextForSpamOrInjection("javascript:steal()", "Note").safe)
        assertFalse(SecurityShield.inspectTextForSpamOrInjection("Claim bonus at https://evil.example/win", "Message").safe)
        assertFalse(SecurityShield.inspectTextForSpamOrInjection("Click bit.ly/scam123", "Message").safe)
        assertFalse(SecurityShield.inspectTextForSpamOrInjection("aaaaaaaaaa help", "Message").safe)
        assertFalse(SecurityShield.inspectTextForSpamOrInjection("a".repeat(350), "Message", maxLength = 300).safe)

        assertTrue(SecurityShield.inspectTextForSpamOrInjection("Customer phone switched off at Gossaigaon Market.", "Message").safe)
    }

    @Test
    fun `weak shift PINs and trivial passwords are rejected`() {
        assertTrue(SecurityShield.isWeakShiftPin("0000"))
        assertTrue(SecurityShield.isWeakShiftPin("1234"))
        assertTrue(SecurityShield.isWeakShiftPin("1111"))
        assertTrue(SecurityShield.isWeakShiftPin("4321"))
        assertFalse(SecurityShield.isWeakShiftPin("7392"))

        assertTrue(SecurityShield.isWeakPassword("123456"))
        assertTrue(SecurityShield.isWeakPassword("password"))
        assertTrue(SecurityShield.isWeakPassword("000000"))
        assertFalse(SecurityShield.isWeakPassword("Rider@9842"))
    }

    @Test
    fun `hardened UTR validator blocks periodic, descending, low-entropy, and duplicate scam UTRs`() {
        val existing = listOf(SettlementRecord(utr = "412893471923"))
        assertFalse(SecurityUtils.validateSettlementUtr("987654321098", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("121212121212", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("123123123123", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("111122221111", existing).valid)
        assertFalse(SecurityUtils.validateSettlementUtr("412893471923", existing).valid)
        assertTrue(SecurityUtils.validateSettlementUtr("539184726104", existing).valid)
    }

    @Test
    fun `ActionRateLimiter enforces cooldown, sliding windows, duplicate suppression, and brute-force lockouts`() {
        val limiter = ActionRateLimiter()

        // Cooldown test
        assertTrue(limiter.checkCooldown("payout", 30_000L, nowMs = 1_000L).allowed)
        limiter.recordAction("payout", nowMs = 1_000L)
        assertFalse(limiter.checkCooldown("payout", 30_000L, nowMs = 10_000L).allowed)
        assertTrue(limiter.checkCooldown("payout", 30_000L, nowMs = 32_000L).allowed)

        // Sliding window spam test
        assertTrue(limiter.checkAndRecordWindow("pass", 2, 60_000L, nowMs = 1_000L).allowed)
        assertTrue(limiter.checkAndRecordWindow("pass", 2, 60_000L, nowMs = 2_000L).allowed)
        assertFalse(limiter.checkAndRecordWindow("pass", 2, 60_000L, nowMs = 3_000L).allowed)
        assertTrue(limiter.checkAndRecordWindow("pass", 2, 60_000L, nowMs = 65_000L).allowed)

        // Duplicate ticket suppression
        assertFalse(limiter.isDuplicateSubmission("ticket", "hash1", nowMs = 1_000L))
        limiter.recordSubmissionFingerprint("ticket", "hash1", nowMs = 1_000L)
        assertTrue(limiter.isDuplicateSubmission("ticket", "hash1", nowMs = 5_000L))

        // Brute-force lockout after 3 failures
        assertFalse(limiter.recordBruteForceFailure("login", maxAttempts = 3, baseLockMs = 30_000L, nowMs = 1_000L).first)
        assertFalse(limiter.recordBruteForceFailure("login", maxAttempts = 3, baseLockMs = 30_000L, nowMs = 2_000L).first)
        assertTrue(limiter.recordBruteForceFailure("login", maxAttempts = 3, baseLockMs = 30_000L, nowMs = 3_000L).first)
        assertFalse(limiter.checkBruteForceLock("login", nowMs = 10_000L).allowed)
        assertTrue(limiter.checkBruteForceLock("login", nowMs = 35_000L).allowed)
    }
}
