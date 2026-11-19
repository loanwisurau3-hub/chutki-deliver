package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.location.Location
import android.os.Build
import com.example.data.SecurityUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class SecurityThreatLevel { SECURE, WARNING, HIGH_RISK }

data class DeviceSecurityReport(
    val isRootSuspected: Boolean = false,
    val isHookingSuspected: Boolean = false,
    val isBackupDisabled: Boolean = true,
    val isMockLocationActive: Boolean = false,
    val spoofReason: String? = null,
    val threatLevel: SecurityThreatLevel = SecurityThreatLevel.SECURE,
    val lastScannedAtMs: Long = 0L
) {
    val activeShieldCount: Int
        get() {
            var count = 3 // Anti-Scam OTP/UTR + Anti-Spam Rate Limiter + Phishing/XSS Filter always active
            if (!isRootSuspected && !isHookingSuspected) count += 1
            if (isBackupDisabled) count += 1
            if (!isMockLocationActive) count += 1
            return count
        }

    val totalShieldCount: Int get() = 6
}

data class InputInspectionResult(
    val safe: Boolean,
    val reason: String = "",
    val threatCode: String = ""
)

data class RateLimitResult(
    val allowed: Boolean,
    val retryAfterSec: Int = 0,
    val reason: String = ""
)

data class LocationFraudCheck(
    val isValid: Boolean,
    val isMocked: Boolean = false,
    val isTeleported: Boolean = false,
    val reason: String = ""
)

/**
 * Multi-layer client-side Hacker, Scam & Spam Protection Shield for Gossaigaon Fleet.
 */
object SecurityShield {

    private val WEAK_PINS = setOf(
        "0000", "1111", "2222", "3333", "4444",
        "5555", "6666", "7777", "8888", "9999",
        "1234", "2345", "3456", "4567", "5678", "6789",
        "4321", "5432", "6543", "7654", "8765", "9876",
        "0123", "3210", "1122", "2211", "1212", "2121",
        "1010", "2020", "2580", "0852"
    )

    private val WEAK_PASSWORDS = setOf(
        "123456", "1234567", "12345678", "123456789",
        "000000", "111111", "654321", "password", "qwerty", "abcdef"
    )

    private val ROOT_BINARY_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/sbin/.magisk"
    )

    private val HOOKING_CLASSES = listOf(
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XposedHelpers",
        "com.saurik.substrate.MS$2"
    )

    private val INJECTION_PATTERNS = listOf(
        Regex("<\\s*script", RegexOption.IGNORE_CASE),
        Regex("javascript\\s*:", RegexOption.IGNORE_CASE),
        Regex("on(error|load|click|mouseover)\\s*=", RegexOption.IGNORE_CASE),
        Regex("<\\s*(iframe|object|embed|svg)", RegexOption.IGNORE_CASE),
        Regex("data\\s*:\\s*text/html", RegexOption.IGNORE_CASE),
        Regex("eval\\s*\\(", RegexOption.IGNORE_CASE)
    )

    private val PHISHING_URL_PATTERN = Regex(
        "(https?://|www\\.|bit\\.ly/|tinyurl\\.com/|t\\.me/|rb\\.gy/|cutt\\.ly/|shorturl\\.at/)",
        RegexOption.IGNORE_CASE
    )

    private val REPEATED_SPAM_CHAR_PATTERN = Regex("(.)\\1{7,}")

    fun scanDeviceSecurity(
        context: Context,
        isMockLocationActive: Boolean = false,
        spoofReason: String? = null
    ): DeviceSecurityReport {
        val rootSuspected = checkRootIndicators()
        val hookingSuspected = checkHookingIndicators()
        val backupDisabled = runCatching {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) == 0
        }.getOrDefault(true)

        val threatLevel = when {
            isMockLocationActive || hookingSuspected -> SecurityThreatLevel.HIGH_RISK
            rootSuspected || !backupDisabled -> SecurityThreatLevel.WARNING
            else -> SecurityThreatLevel.SECURE
        }

        return DeviceSecurityReport(
            isRootSuspected = rootSuspected,
            isHookingSuspected = hookingSuspected,
            isBackupDisabled = backupDisabled,
            isMockLocationActive = isMockLocationActive,
            spoofReason = spoofReason,
            threatLevel = threatLevel,
            lastScannedAtMs = System.currentTimeMillis()
        )
    }

    private fun checkRootIndicators(): Boolean {
        val testKeys = runCatching { Build.TAGS?.contains("test-keys") == true }.getOrDefault(false)
        if (testKeys) return true
        return ROOT_BINARY_PATHS.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
    }

    private fun checkHookingIndicators(): Boolean {
        val classFound = HOOKING_CLASSES.any { className ->
            runCatching {
                Class.forName(className, false, SecurityShield::class.java.classLoader)
                true
            }.getOrDefault(false)
        }
        if (classFound) return true

        return runCatching {
            Thread.currentThread().stackTrace.any { frame ->
                val cls = frame.className.lowercase()
                cls.contains("xposed") || cls.contains("frida") || cls.contains("substrate")
            }
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    fun isLocationMocked(location: Location): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                location.isFromMockProvider
            }
        }.getOrDefault(false)
    }

    fun validateLocationFix(
        lat: Double,
        lng: Double,
        isMockProvider: Boolean,
        prevLat: Double?,
        prevLng: Double?,
        prevFixAtMs: Long,
        nowMs: Long
    ): LocationFraudCheck {
        if (isMockProvider) {
            return LocationFraudCheck(
                isValid = false,
                isMocked = true,
                reason = "Mock / Fake GPS provider detected! Turn off Mock Location apps."
            )
        }
        if (lat.isNaN() || lng.isNaN() || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return LocationFraudCheck(
                isValid = false,
                isMocked = true,
                reason = "Invalid GPS coordinates rejected."
            )
        }
        if (lat == 0.0 && lng == 0.0) {
            return LocationFraudCheck(
                isValid = false,
                isMocked = true,
                reason = "Null-island (0,0) spoofed coordinates rejected."
            )
        }
        if (prevLat != null && prevLng != null && prevFixAtMs > 0L) {
            val deltaMs = nowMs - prevFixAtMs
            if (deltaMs in 1_000L..60_000L) {
                val distKm = SecurityUtils.calculateDistanceKm(prevLat, prevLng, lat, lng) ?: 0.0
                val impliedSpeedKmh = distKm / (deltaMs / 3_600_000.0)
                if (distKm > 1.5 && impliedSpeedKmh > 180.0) {
                    return LocationFraudCheck(
                        isValid = false,
                        isTeleported = true,
                        reason = "Suspicious GPS jump detected (%.1f km in %ds). Fake GPS blocked.".format(
                            distKm,
                            (deltaMs / 1000L).coerceAtLeast(1L)
                        )
                    )
                }
            }
        }
        return LocationFraudCheck(isValid = true)
    }

    fun isWeakShiftPin(pin: String): Boolean {
        val clean = pin.trim()
        if (!Regex("^\\d{4}$").matches(clean)) return true
        return clean in WEAK_PINS
    }

    fun isWeakPassword(password: String): Boolean {
        val clean = password.trim().lowercase()
        if (clean.length < 6) return true
        if (clean in WEAK_PASSWORDS) return true
        if (clean.toSet().size <= 1) return true
        return false
    }

    fun inspectTextForSpamOrInjection(
        input: String,
        fieldName: String,
        maxLength: Int = 300,
        blockUrls: Boolean = true
    ): InputInspectionResult {
        val trimmed = input.trim()
        if (trimmed.length > maxLength) {
            return InputInspectionResult(
                safe = false,
                reason = "$fieldName is too long (max $maxLength characters).",
                threatCode = "PAYLOAD_FLOOD_BLOCKED"
            )
        }
        if (INJECTION_PATTERNS.any { it.containsMatchIn(trimmed) }) {
            return InputInspectionResult(
                safe = false,
                reason = "Blocked unsafe script or HTML characters in $fieldName.",
                threatCode = "INJECTION_ATTEMPT_BLOCKED"
            )
        }
        if (blockUrls && PHISHING_URL_PATTERN.containsMatchIn(trimmed)) {
            return InputInspectionResult(
                safe = false,
                reason = "External links are blocked in $fieldName for anti-phishing safety.",
                threatCode = "PHISHING_LINK_BLOCKED"
            )
        }
        if (REPEATED_SPAM_CHAR_PATTERN.containsMatchIn(trimmed)) {
            return InputInspectionResult(
                safe = false,
                reason = "Spam pattern detected in $fieldName. Please enter meaningful text.",
                threatCode = "SPAM_PATTERN_BLOCKED"
            )
        }
        return InputInspectionResult(safe = true)
    }
}

/**
 * Thread-safe sliding-window & cooldown rate limiter to stop spam flooding and brute-force attacks.
 */
class ActionRateLimiter {

    private data class BruteForceState(
        var failCount: Int = 0,
        var lockedUntilMs: Long = 0L,
        var lockRounds: Int = 0
    )

    private val lastActionAtMs = ConcurrentHashMap<String, Long>()
    private val windowEvents = ConcurrentHashMap<String, ArrayDeque<Long>>()
    private val recentMessageHashes = ConcurrentHashMap<String, Pair<String, Long>>()
    private val bruteForceStates = ConcurrentHashMap<String, BruteForceState>()

    fun checkCooldown(
        actionKey: String,
        cooldownMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): RateLimitResult {
        val last = lastActionAtMs[actionKey] ?: 0L
        val elapsed = nowMs - last
        if (last > 0L && elapsed < cooldownMs) {
            val waitSec = (((cooldownMs - elapsed) + 999L) / 1000L).toInt().coerceAtLeast(1)
            return RateLimitResult(
                allowed = false,
                retryAfterSec = waitSec,
                reason = "Anti-Spam Protection: Please wait ${waitSec}s before trying again."
            )
        }
        return RateLimitResult(allowed = true)
    }

    fun recordAction(actionKey: String, nowMs: Long = System.currentTimeMillis()) {
        lastActionAtMs[actionKey] = nowMs
    }

    fun checkAndRecordWindow(
        actionKey: String,
        maxEvents: Int,
        windowMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): RateLimitResult {
        val deque = windowEvents.computeIfAbsent(actionKey) { ArrayDeque() }
        synchronized(deque) {
            while (deque.isNotEmpty() && nowMs - deque.first() > windowMs) {
                deque.removeFirst()
            }
            if (deque.size >= maxEvents) {
                val oldest = deque.first()
                val waitSec = (((windowMs - (nowMs - oldest)) + 999L) / 1000L).toInt().coerceAtLeast(1)
                return RateLimitResult(
                    allowed = false,
                    retryAfterSec = waitSec,
                    reason = "Too many rapid actions! Anti-Spam cooldown active (${waitSec}s)."
                )
            }
            deque.addLast(nowMs)
            return RateLimitResult(allowed = true)
        }
    }

    fun isDuplicateSubmission(
        channelKey: String,
        contentFingerprint: String,
        dedupWindowMs: Long = 300_000L,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val prev = recentMessageHashes[channelKey]
        if (prev != null && prev.first == contentFingerprint && (nowMs - prev.second) < dedupWindowMs) {
            return true
        }
        return false
    }

    fun recordSubmissionFingerprint(
        channelKey: String,
        contentFingerprint: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        recentMessageHashes[channelKey] = contentFingerprint to nowMs
    }

    fun checkBruteForceLock(
        key: String,
        nowMs: Long = System.currentTimeMillis()
    ): RateLimitResult {
        val state = bruteForceStates[key] ?: return RateLimitResult(allowed = true)
        synchronized(state) {
            if (nowMs < state.lockedUntilMs) {
                val waitSec = (((state.lockedUntilMs - nowMs) + 999L) / 1000L).toInt().coerceAtLeast(1)
                return RateLimitResult(
                    allowed = false,
                    retryAfterSec = waitSec,
                    reason = "Too many failed attempts! Locked for ${waitSec}s."
                )
            }
        }
        return RateLimitResult(allowed = true)
    }

    /**
     * Records a failed credential/PIN attempt. Returns (isNowLocked, remainingAttempts, lockSeconds).
     */
    fun recordBruteForceFailure(
        key: String,
        maxAttempts: Int = 5,
        baseLockMs: Long = 30_000L,
        maxLockMs: Long = 120_000L,
        nowMs: Long = System.currentTimeMillis()
    ): Triple<Boolean, Int, Int> {
        val state = bruteForceStates.computeIfAbsent(key) { BruteForceState() }
        synchronized(state) {
            state.failCount += 1
            if (state.failCount >= maxAttempts) {
                state.lockRounds = (state.lockRounds + 1).coerceAtMost(4)
                val multiplier = 1L shl (state.lockRounds - 1)
                val lockMs = (baseLockMs * multiplier).coerceAtMost(maxLockMs)
                state.lockedUntilMs = nowMs + lockMs
                state.failCount = 0
                return Triple(true, 0, (lockMs / 1000L).toInt())
            }
            return Triple(false, (maxAttempts - state.failCount).coerceAtLeast(0), 0)
        }
    }

    fun resetBruteForce(key: String) {
        bruteForceStates.remove(key)
    }
}
