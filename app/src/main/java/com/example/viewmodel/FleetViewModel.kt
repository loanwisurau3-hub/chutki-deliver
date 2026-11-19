package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AdminSettings
import com.example.data.AuthPrefsStore
import com.example.data.ConnectivityMonitor
import com.example.data.EmergencyRequestRecord
import com.example.data.FeedbackRecord
import com.example.data.FirebaseFleetRepository
import com.example.data.OrderEarningBreakdown
import com.example.data.OrderRecord
import com.example.data.RiderEarningsCalculator
import com.example.data.RiderPayoutRecord
import com.example.data.RiderProfile
import com.example.data.RiderRating
import com.example.data.SavedAuthPrefs
import com.example.data.SecurityUtils
import com.example.data.SettlementRecord
import com.example.data.StoreInfo
import com.example.data.TripGroup
import com.example.location.GpsRadarState
import com.example.location.LiveLocationTelemetry
import com.example.location.LocationTracker
import com.example.location.RiderLocationForegroundService
import com.example.notifications.FleetBackgroundService
import com.example.notifications.FleetNotificationHelper
import com.example.security.ActionRateLimiter
import com.example.security.DeviceSecurityReport
import com.example.security.SecurityShield
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RiderTab {
    ACTIVE,
    DASHBOARD,
    EARNINGS,
    COD_CASH,
    TRIP_HISTORY,
    RATINGS,
    PROFILE,
    KYC,
    SUPPORT,
    SETTINGS
}
enum class ToastType { SUCCESS, DANGER, INFO, WARNING }

data class ToastEvent(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.SUCCESS
)

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(
        val uid: String,
        val email: String,
        val displayName: String = ""
    ) : AuthUiState
}

sealed interface ActiveModal {
    data object None : ActiveModal
    data class PickupVerify(val orderId: String, val storeName: String, val pickupCode: String? = null) : ActiveModal
    data class DeliveryVerify(val tripId: String, val totalAmount: Double, val isPrepaid: Boolean) : ActiveModal
    data class PassTrip(val tripId: String) : ActiveModal
    data class EmergencySos(val tripId: String) : ActiveModal
    data object PayoutWithdraw : ActiveModal
    data object CodDeposit : ActiveModal
    data object Feedback : ActiveModal
}

data class FleetUiState(
    val authState: AuthUiState = AuthUiState.Checking,
    val authError: String? = null,
    val isAuthLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isProfileLoaded: Boolean = false,
    val needsGoogleOnboarding: Boolean = false,
    val resetMessage: String? = null,
    val isResetLoading: Boolean = false,
    val savedAuthPrefs: SavedAuthPrefs = SavedAuthPrefs(),
    val isPinLocked: Boolean = false,
    val quickPinError: String? = null,
    val rider: RiderProfile = RiderProfile(),
    val adminSettings: AdminSettings = AdminSettings(),
    val stores: Map<String, StoreInfo> = emptyMap(),
    val assignedOrders: Map<String, OrderRecord> = emptyMap(),
    val activeTrips: List<TripGroup> = emptyList(),
    val completedTrips: List<TripGroup> = emptyList(),
    val countdownByTrip: Map<String, Int> = emptyMap(),
    val ratings: List<RiderRating> = emptyList(),
    val feedbacks: List<FeedbackRecord> = emptyList(),
    val emergencyRequests: List<EmergencyRequestRecord> = emptyList(),
    val riderSettlements: List<SettlementRecord> = emptyList(),
    val allSettlements: List<SettlementRecord> = emptyList(),
    val riderPayouts: List<RiderPayoutRecord> = emptyList(),
    val totalCodCollected: Double = 0.0,
    val totalDeposited: Double = 0.0,
    val pendingCodSettlementAmount: Double = 0.0,
    val netCashInHand: Double = 0.0,
    val totalRiderEarned: Double = 0.0,
    val totalRiderPaidPayout: Double = 0.0,
    val pendingPayoutAmount: Double = 0.0,
    val availablePayoutBalance: Double = 0.0,
    val riderLat: Double? = null,
    val riderLng: Double? = null,
    val riderAccuracy: Int? = null,
    val riderSpeedKmh: Int? = null,
    val batteryPct: Int? = null,
    val lastGpsSyncAtMs: Long = 0L,
    val isNetworkOnline: Boolean = true,
    val isFirebaseConnected: Boolean = true,
    val gpsState: GpsRadarState = GpsRadarState.Hidden,
    val soundEnabled: Boolean = true,
    val chimeCounter: Int = 0,
    val selectedTab: RiderTab = RiderTab.ACTIVE,
    val activeModal: ActiveModal = ActiveModal.None,
    val isModalBusy: Boolean = false,
    val toast: ToastEvent? = null,
    val needsLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isBackgroundServiceActive: Boolean = false,
    val needsAgreementAcceptance: Boolean = false,
    val isAgreementSaving: Boolean = false,
    val agreementViewerTab: Int? = null,
    val todayTripsCount: Int = 0,
    val todayEarnings: Double = 0.0,
    val todayCodCollected: Double = 0.0,
    val weekTripsCount: Int = 0,
    val weekEarnings: Double = 0.0,
    val shiftElapsedSeconds: Long = 0L,
    val isKycSaving: Boolean = false,
    val securityReport: DeviceSecurityReport = DeviceSecurityReport(),
    val mockLocationDetected: Boolean = false,
    val spoofAlertMessage: String? = null,
    val blockedThreatsCount: Int = 0
)

private data class BruteForceGuard(var fails: Int = 0, var lockedUntilMs: Long = 0L)

class FleetViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseFleetRepository(application)
    private val authPrefs = AuthPrefsStore(application)
    private val locationTracker = LocationTracker(application)
    private val connectivityMonitor = ConnectivityMonitor(application)

    private val _uiState = MutableStateFlow(FleetUiState())
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    private var dataJobs: List<Job> = emptyList()
    private var tickerJob: Job? = null
    private var gpsMonitorJob: Job? = null
    private var recomputeJob: Job? = null
    private var prevActiveCount: Int? = null

    private val timedOutTrips = mutableSetOf<String>()
    private val pickupGuards = mutableMapOf<String, BruteForceGuard>()
    private val pinGuards = mutableMapOf<String, BruteForceGuard>()
    private val pickupInitialDist = mutableMapOf<String, Double>()
    private val alertThrottleTs = mutableMapOf<String, Long>()
    private val rateLimiter = ActionRateLimiter()

    private var sessionUnlockedInProcess = false
    private var locationPermissionDeniedInSession = false
    @Volatile
    private var isRegisteringProfile = false

    init {
        FleetNotificationHelper.ensureChannels(application)
        val initialSecurity = SecurityShield.scanDeviceSecurity(application)
        _uiState.update {
            it.copy(
                hasNotificationPermission = FleetNotificationHelper.hasNotificationPermission(application),
                securityReport = initialSecurity
            )
        }
        viewModelScope.launch {
            FleetBackgroundService.isRunning.collect { running ->
                _uiState.update { it.copy(isBackgroundServiceActive = running) }
            }
        }
        viewModelScope.launch {
            RiderLocationForegroundService.telemetry.collect { telemetry ->
                handleForegroundLocationTelemetry(telemetry)
            }
        }
        viewModelScope.launch {
            authPrefs.prefsFlow
                .flowOn(Dispatchers.IO)
                .collect { prefs ->
                    _uiState.update { it.copy(savedAuthPrefs = prefs) }
                    evaluateBackgroundNotificationService()
                }
        }
        viewModelScope.launch {
            connectivityMonitor.observeOnline().collect { online ->
                _uiState.update { state ->
                    val updatedGps = when (val g = state.gpsState) {
                        is GpsRadarState.Live -> g.copy(isNetworkOnline = online)
                        else -> g
                    }
                    state.copy(isNetworkOnline = online, gpsState = updatedGps)
                }
            }
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.warmUp()
            }
            launch {
                repo.observeFirebaseConnected().collect { connected ->
                    _uiState.update { it.copy(isFirebaseConnected = connected) }
                }
            }
            launch {
                repo.observeAdminSettings().collect { settings ->
                    _uiState.update { state ->
                        val signedIn = state.authState is AuthUiState.SignedIn
                        val needsAgreement = signedIn &&
                            state.isProfileLoaded &&
                            !state.needsGoogleOnboarding &&
                            !state.rider.hasAcceptedLatestAgreement(settings)
                        state.copy(
                            adminSettings = settings,
                            needsAgreementAcceptance = needsAgreement
                        )
                    }
                    recomputeTripsAndFinancials()
                    evaluateGpsRequirement()
                    evaluateBackgroundNotificationService()
                }
            }
            repo.observeAuthState().collect { user ->
                cancelUserListeners()
                if (user == null) {
                    locationTracker.stop()
                    RiderLocationForegroundService.stop(getApplication())
                    FleetBackgroundService.stop(getApplication())
                    prevActiveCount = null
                    sessionUnlockedInProcess = false
                    locationPermissionDeniedInSession = false
                    val saved = withContext(Dispatchers.IO) { authPrefs.prefsFlow.first() }
                    _uiState.update {
                        FleetUiState(
                            authState = AuthUiState.SignedOut,
                            savedAuthPrefs = saved,
                            adminSettings = it.adminSettings,
                            soundEnabled = it.soundEnabled,
                            hasNotificationPermission = FleetNotificationHelper.hasNotificationPermission(getApplication())
                        )
                    }
                } else {
                    val saved = withContext(Dispatchers.IO) { authPrefs.prefsFlow.first() }
                    val requiresPin = !sessionUnlockedInProcess &&
                        saved.quickPinUid == user.uid &&
                        saved.quickPinHash.isNotBlank()
                    _uiState.update {
                        it.copy(
                            authState = AuthUiState.SignedIn(
                                uid = user.uid,
                                email = user.email.orEmpty(),
                                displayName = user.displayName.orEmpty()
                            ),
                            authError = null,
                            isAuthLoading = false,
                            isGoogleLoading = false,
                            isProfileLoaded = false,
                            needsGoogleOnboarding = false,
                            needsAgreementAcceptance = false,
                            savedAuthPrefs = saved,
                            isPinLocked = requiresPin
                        )
                    }
                    startUserListeners(user.uid)
                }
            }
        }
    }

    private fun cancelUserListeners() {
        dataJobs.forEach { it.cancel() }
        dataJobs = emptyList()
        tickerJob?.cancel()
        gpsMonitorJob?.cancel()
        recomputeJob?.cancel()
    }

    private fun startUserListeners(uid: String) {
        val j1 = viewModelScope.launch {
            repo.observeRiderProfile(uid).collect { profile ->
                val needsOnboarding = profile.phone.isBlank() || profile.vehicle.isBlank() || profile.upiId.isBlank()
                if (isRegisteringProfile && needsOnboarding) {
                    return@collect
                }
                _uiState.update { state ->
                    val needsAgreement = !needsOnboarding && !profile.hasAcceptedLatestAgreement(state.adminSettings)
                    state.copy(
                        rider = profile,
                        isProfileLoaded = true,
                        needsGoogleOnboarding = needsOnboarding,
                        needsAgreementAcceptance = needsAgreement
                    )
                }
                if (profile.phone.isNotBlank() && profile.email.isNotBlank()) {
                    authPrefs.linkPhoneToEmail(profile.phone, profile.email, profile.name)
                }
                evaluateGpsRequirement()
                evaluateBackgroundNotificationService()
            }
        }
        val j2 = viewModelScope.launch {
            repo.observeStores().collect { stores ->
                _uiState.update { it.copy(stores = stores) }
                recomputeTripsAndFinancials()
            }
        }
        val j4 = viewModelScope.launch {
            repo.observeRiderRatings(uid).collect { ratings ->
                _uiState.update { it.copy(ratings = ratings) }
            }
        }
        val j5 = viewModelScope.launch {
            repo.observeFeedbacks(uid).collect { feedbacks ->
                _uiState.update { it.copy(feedbacks = feedbacks) }
            }
        }
        val j6 = viewModelScope.launch {
            repo.observeEmergencyRequests(uid).collect { reqs ->
                _uiState.update { it.copy(emergencyRequests = reqs) }
            }
        }
        val j7 = viewModelScope.launch {
            var initializedReadySet = false
            val knownReadyOrderIds = mutableSetOf<String>()
            repo.observeAssignedOrders(uid).collect { orders ->
                val activeCount = orders.values.count { !SecurityUtils.isOrderCompleted(it) }
                val shouldChime = prevActiveCount != null && activeCount > prevActiveCount!!
                prevActiveCount = activeCount

                val currentReadyOrders = orders.values.filter { SecurityUtils.isOrderReadyForPickup(it) }
                val newlyReadyOrder = if (initializedReadySet) {
                    currentReadyOrders.firstOrNull { it.orderId !in knownReadyOrderIds }
                } else {
                    initializedReadySet = true
                    null
                }
                knownReadyOrderIds.clear()
                knownReadyOrderIds.addAll(currentReadyOrders.map { it.orderId })

                val activePickupModal = _uiState.value.activeModal as? ActiveModal.PickupVerify
                val shouldClosePickupModal = activePickupModal != null &&
                    orders[activePickupModal.orderId]?.let { !SecurityUtils.isOrderReadyForPickup(it) } == true

                val triggerChime = shouldChime || newlyReadyOrder != null
                _uiState.update { state ->
                    state.copy(
                        assignedOrders = orders,
                        chimeCounter = if (triggerChime) state.chimeCounter + 1 else state.chimeCounter,
                        activeModal = if (shouldClosePickupModal) ActiveModal.None else state.activeModal,
                        isModalBusy = if (shouldClosePickupModal) false else state.isModalBusy
                    )
                }
                if (shouldChime) {
                    showToast("New order trip assigned!", ToastType.SUCCESS)
                } else if (newlyReadyOrder != null) {
                    val storeName = _uiState.value.stores[newlyReadyOrder.restaurantId]?.name ?: "Restaurant"
                    showToast("$storeName marked Order #${newlyReadyOrder.orderId.takeLast(5)} READY FOR PICKUP!", ToastType.SUCCESS)
                } else if (shouldClosePickupModal) {
                    showToast("Restaurant updated order status — pickup locked until Ready.", ToastType.WARNING)
                }
                recomputeTripsAndFinancials()
                evaluateGpsRequirement()
                evaluateBackgroundNotificationService()
            }
        }
        val j8 = viewModelScope.launch {
            repo.observeAllSettlements().collect { all ->
                val mine = all.filter { it.riderUid == uid }
                _uiState.update { it.copy(allSettlements = all, riderSettlements = mine) }
                recomputeTripsAndFinancials()
            }
        }
        val j9 = viewModelScope.launch {
            repo.observeRiderPayouts(uid).collect { payouts ->
                _uiState.update { it.copy(riderPayouts = payouts) }
                recomputeTripsAndFinancials()
            }
        }
        dataJobs = listOf(j1, j2, j4, j5, j6, j7, j8, j9)

        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                updateCountdownsAndAutoPass()
            }
        }

        gpsMonitorJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                monitorGpsHealthAndOutages()
            }
        }
    }

    fun runSecurityShieldScan(showResultToast: Boolean = true) {
        val state = _uiState.value
        val report = SecurityShield.scanDeviceSecurity(
            context = getApplication(),
            isMockLocationActive = state.mockLocationDetected,
            spoofReason = state.spoofAlertMessage
        )
        _uiState.update { it.copy(securityReport = report) }
        if (showResultToast) {
            val uid = (state.authState as? AuthUiState.SignedIn)?.uid
            if (report.isRootSuspected || report.isHookingSuspected) {
                repo.logSecurityEvent(
                    uid,
                    "DEVICE_INTEGRITY",
                    "TAMPER_OR_ROOT_WARNING",
                    "Root=${report.isRootSuspected}, Hook=${report.isHookingSuspected}",
                    "FLAGGED"
                )
            }
            val msg = when {
                report.isMockLocationActive -> "Alert: Fake GPS / Mock Location detected! Disable spoofing apps."
                report.isHookingSuspected -> "Alert: Instrumentation hook detected on device."
                report.isRootSuspected -> "Warning: Rooted/custom ROM detected (${report.activeShieldCount}/${report.totalShieldCount} shields active)."
                else -> "Security Shield verified: ${report.activeShieldCount}/${report.totalShieldCount} protections active!"
            }
            showToast(msg, if (report.isMockLocationActive || report.isHookingSuspected) ToastType.DANGER else ToastType.SUCCESS)
        }
    }

    private fun recordBlockedThreat(eventType: String, orderOrTargetId: String, details: String) {
        val uid = (_uiState.value.authState as? AuthUiState.SignedIn)?.uid
        _uiState.update { it.copy(blockedThreatsCount = it.blockedThreatsCount + 1) }
        repo.logSecurityEvent(uid, orderOrTargetId, eventType, details, "BLOCKED")
    }

    fun signIn(identifier: String, password: String, rememberMe: Boolean = true) {
        val lockCheck = rateLimiter.checkBruteForceLock("auth_login")
        if (!lockCheck.allowed) {
            _uiState.update { it.copy(authError = lockCheck.reason) }
            return
        }
        val cleanId = identifier
            .filterNot { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
        val cleanPassword = password
            .replace("\r", "")
            .replace("\n", "")
            .replace("\u200B", "")
            .replace("\uFEFF", "")

        if (cleanId.isBlank() || cleanPassword.isBlank()) {
            _uiState.update { it.copy(authError = "Enter your registered email (or saved mobile) and password.") }
            return
        }
        if (cleanId.length > 120 || cleanPassword.length > 128) {
            _uiState.update { it.copy(authError = "Input length exceeded maximum allowed security limit.") }
            return
        }
        if (cleanPassword.length < 6) {
            _uiState.update { it.copy(authError = "Password must be at least 6 characters. Double-check or tap Forgot Password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            var secondaryEmailFromPhone: String? = null
            val resolvedEmail = if (cleanId.contains("@")) {
                if (!SecurityUtils.isValidEmail(cleanId)) {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = "Please enter a valid email address (e.g. rider@gmail.com)."
                        )
                    }
                    return@launch
                }
                cleanId.lowercase()
            } else if (SecurityUtils.isValidIndianMobile(cleanId)) {
                val digits = cleanId.filter { it.isDigit() }.takeLast(10)
                val remoteEmail = repo.lookupEmailByPhone(digits)
                val localEmail = authPrefs.resolveEmailForPhone(digits)
                if (!remoteEmail.isNullOrBlank() && !localEmail.isNullOrBlank() && !remoteEmail.equals(localEmail, ignoreCase = true)) {
                    secondaryEmailFromPhone = localEmail
                }
                remoteEmail ?: localEmail
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = "Enter a valid email address or 10-digit registered mobile number."
                    )
                }
                return@launch
            }

            if (resolvedEmail.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = "Mobile number not found locally. Please sign in with your registered email once."
                    )
                }
                return@launch
            }

            sessionUnlockedInProcess = true
            var activeEmail = resolvedEmail
            var result = repo.signIn(activeEmail, cleanPassword)

            // Recover cleanly if pasted password had accidental leading/trailing spaces
            val trimmedPassword = cleanPassword.trim()
            if (result.isFailure && trimmedPassword != cleanPassword && trimmedPassword.length >= 6) {
                result = repo.signIn(activeEmail, trimmedPassword)
            }

            // If phone lookup had a secondary email mapping and primary failed, try the secondary once
            if (result.isFailure && !secondaryEmailFromPhone.isNullOrBlank()) {
                val retry = repo.signIn(secondaryEmailFromPhone, trimmedPassword.ifEmpty { cleanPassword })
                if (retry.isSuccess) {
                    activeEmail = secondaryEmailFromPhone
                    result = retry
                }
            }

            result.onSuccess {
                rateLimiter.resetBruteForce("auth_login")
                authPrefs.saveLoginPreference(activeEmail, rememberMe)
            }.onFailure { err ->
                sessionUnlockedInProcess = false
                val (locked, remaining, lockSec) = rateLimiter.recordBruteForceFailure(
                    key = "auth_login",
                    maxAttempts = 5,
                    baseLockMs = 30_000L,
                    maxLockMs = 120_000L
                )
                val baseMsg = err.message ?: "Sign in failed"
                val suffix = if (locked) {
                    " Too many failed attempts — login paused for ${lockSec}s."
                } else {
                    " ($remaining attempts left before security cooldown)"
                }
                _uiState.update { it.copy(isAuthLoading = false, authError = baseMsg + suffix) }
            }
        }
    }

    fun sendPasswordReset(identifier: String) {
        val cooldown = rateLimiter.checkCooldown("password_reset", 60_000L)
        if (!cooldown.allowed) {
            _uiState.update { it.copy(authError = cooldown.reason) }
            return
        }
        val cleanId = identifier.trim()
        if (cleanId.isBlank()) {
            _uiState.update { it.copy(authError = "Enter your registered email address to reset password.") }
            return
        }
        rateLimiter.recordAction("password_reset")
        viewModelScope.launch {
            _uiState.update { it.copy(isResetLoading = true, resetMessage = null, authError = null) }
            val resolvedEmail = if (cleanId.contains("@")) {
                cleanId
            } else if (SecurityUtils.isValidIndianMobile(cleanId)) {
                val digits = cleanId.filter { it.isDigit() }.takeLast(10)
                authPrefs.resolveEmailForPhone(digits) ?: repo.lookupEmailByPhone(digits)
            } else null

            if (resolvedEmail.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isResetLoading = false,
                        authError = "Enter a valid registered email address."
                    )
                }
                return@launch
            }

            repo.sendPasswordResetEmail(resolvedEmail)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isResetLoading = false,
                            resetMessage = "Password reset link sent to $resolvedEmail. Check inbox or spam."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isResetLoading = false,
                            authError = err.message ?: "Could not send reset email."
                        )
                    }
                }
        }
    }

    fun clearResetMessage() {
        _uiState.update { it.copy(resetMessage = null, authError = null) }
    }

    fun registerRider(
        name: String,
        email: String,
        phone: String,
        upiId: String,
        vehicle: String,
        password: String
    ) {
        val regCooldown = rateLimiter.checkCooldown("register_rider", 20_000L)
        if (!regCooldown.allowed) {
            _uiState.update { it.copy(authError = regCooldown.reason) }
            return
        }
        if (name.isBlank() || email.isBlank() || vehicle.isBlank() || password.length < 6) {
            _uiState.update { it.copy(authError = "Complete all fields (password min 6 chars).") }
            return
        }
        if (SecurityShield.isWeakPassword(password)) {
            _uiState.update { it.copy(authError = "Password is too weak (avoid 123456, 000000, or 'password'). Choose a stronger password.") }
            return
        }
        val nameCheck = SecurityShield.inspectTextForSpamOrInjection(name, "Full Name", maxLength = 80)
        if (!nameCheck.safe) {
            _uiState.update { it.copy(authError = nameCheck.reason) }
            return
        }
        val vehicleCheck = SecurityShield.inspectTextForSpamOrInjection(vehicle, "Vehicle Info", maxLength = 80)
        if (!vehicleCheck.safe) {
            _uiState.update { it.copy(authError = vehicleCheck.reason) }
            return
        }
        if (!SecurityUtils.isValidEmail(email)) {
            _uiState.update { it.copy(authError = "Enter a valid email address (e.g. rider@gmail.com).") }
            return
        }
        if (!SecurityUtils.isValidIndianMobile(phone)) {
            _uiState.update { it.copy(authError = "Enter a valid 10-digit Indian mobile number.") }
            return
        }
        if (!SecurityUtils.isValidUpiId(upiId)) {
            _uiState.update { it.copy(authError = "Enter a valid UPI ID (e.g. 9876543210@paytm).") }
            return
        }
        rateLimiter.recordAction("register_rider")
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            sessionUnlockedInProcess = true
            isRegisteringProfile = true
            val currentSettings = _uiState.value.adminSettings
            try {
                val result = repo.registerRider(
                    name = name,
                    email = email,
                    phone = phone,
                    upiId = upiId,
                    vehicle = vehicle,
                    password = password,
                    acceptedSettings = currentSettings
                )
                result.onSuccess {
                    authPrefs.saveLoginPreference(email, true, name)
                    authPrefs.linkPhoneToEmail(phone, email, name)
                }.onFailure { err ->
                    sessionUnlockedInProcess = false
                    _uiState.update { it.copy(isAuthLoading = false, authError = err.message ?: "Registration failed") }
                }
            } finally {
                isRegisteringProfile = false
            }
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun unlockWithQuickPin(enteredPin: String) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val lockCheck = rateLimiter.checkBruteForceLock("quick_pin_$uid")
        if (!lockCheck.allowed) {
            _uiState.update { it.copy(quickPinError = lockCheck.reason) }
            return
        }
        val expectedHash = state.savedAuthPrefs.quickPinHash
        if (expectedHash.isBlank() || state.savedAuthPrefs.quickPinUid != uid) {
            sessionUnlockedInProcess = true
            _uiState.update { it.copy(isPinLocked = false, quickPinError = null) }
            evaluateGpsRequirement()
            evaluateBackgroundNotificationService()
            return
        }
        val computed = SecurityUtils.sha256Hex("$uid:${enteredPin.trim()}")
        if (computed == expectedHash) {
            rateLimiter.resetBruteForce("quick_pin_$uid")
            sessionUnlockedInProcess = true
            _uiState.update { it.copy(isPinLocked = false, quickPinError = null) }
            evaluateGpsRequirement()
            evaluateBackgroundNotificationService()
            showToast("Shift terminal unlocked!", ToastType.SUCCESS)
        } else {
            val (locked, remaining, lockSec) = rateLimiter.recordBruteForceFailure(
                key = "quick_pin_$uid",
                maxAttempts = 5,
                baseLockMs = 60_000L,
                maxLockMs = 180_000L
            )
            if (locked) {
                recordBlockedThreat("QUICK_PIN_BRUTE_FORCE", uid, "5 failed Quick Shift PIN unlock attempts")
                _uiState.update { it.copy(quickPinError = "Too many wrong PINs! Terminal locked for ${lockSec}s.") }
            } else {
                _uiState.update { it.copy(quickPinError = "Incorrect 4-digit Shift PIN ($remaining attempts left).") }
            }
        }
    }

    fun setQuickShiftPin(pin: String) {
        val uid = (_uiState.value.authState as? AuthUiState.SignedIn)?.uid ?: return
        val cleanPin = pin.trim()
        if (!Regex("^\\d{4}$").matches(cleanPin)) {
            showToast("Enter a 4-digit numeric PIN.", ToastType.DANGER)
            return
        }
        if (SecurityShield.isWeakShiftPin(cleanPin)) {
            showToast("Weak PIN blocked! Avoid easy PINs like 0000, 1111, or 1234.", ToastType.DANGER)
            return
        }
        viewModelScope.launch {
            authPrefs.setQuickPin(uid, cleanPin)
            sessionUnlockedInProcess = true
            showToast("4-Digit Quick Shift Lock PIN enabled!", ToastType.SUCCESS)
        }
    }

    fun disableQuickShiftPin() {
        viewModelScope.launch {
            authPrefs.clearQuickPin()
            _uiState.update { it.copy(isPinLocked = false, quickPinError = null) }
            showToast("Quick Shift Lock PIN removed.", ToastType.INFO)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveGoogleWebClientId(): String {
        val fromSecrets = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            .takeUnless { it.isBlank() || it == "OPTIONAL_GOOGLE_WEB_CLIENT_ID" }
        if (!fromSecrets.isNullOrBlank()) return fromSecrets

        val app = getApplication<Application>()
        val resId = app.resources.getIdentifier("default_web_client_id", "string", app.packageName)
        if (resId != 0) {
            return runCatching { app.getString(resId).trim() }.getOrDefault("")
        }
        return ""
    }

    fun signInWithGoogle(activity: Activity) {
        if (_uiState.value.isGoogleLoading || _uiState.value.isAuthLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, authError = null) }
            sessionUnlockedInProcess = true

            val webClientId = resolveGoogleWebClientId()
            var credentialError: String? = null
            if (webClientId.isNotEmpty()) {
                val credentialManager = CredentialManager.create(activity)
                val option = GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                try {
                    val cred = credentialManager.getCredential(activity, request).credential
                    if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                        val res = repo.signInWithGoogleIdToken(idToken)
                        res.onSuccess { user ->
                            user.email?.let { authPrefs.saveLoginPreference(it, true, user.displayName) }
                        }.onFailure { err ->
                            sessionUnlockedInProcess = false
                            _uiState.update {
                                it.copy(isGoogleLoading = false, authError = err.message ?: "Google Sign-In failed")
                            }
                        }
                        return@launch
                    }
                } catch (e: GetCredentialCancellationException) {
                    Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                    sessionUnlockedInProcess = false
                    _uiState.update { it.copy(isGoogleLoading = false) }
                    return@launch
                } catch (e: Exception) {
                    Log.w("Auth", "CredentialManager failed: ${e.message}", e)
                    credentialError = e.message
                }
            }

            // OAuthProvider requires an Android Firebase App ID (not :web:) registered in test-2a0d0
            if (repo.hasAndroidFirebaseAppId()) {
                val oauthResult = repo.signInWithGoogleOAuth(activity)
                oauthResult.onSuccess { user ->
                    user.email?.let { authPrefs.saveLoginPreference(it, true, user.displayName) }
                }.onFailure { err ->
                    sessionUnlockedInProcess = false
                    _uiState.update {
                        it.copy(isGoogleLoading = false, authError = err.message ?: "Google Sign-In failed")
                    }
                }
            } else {
                sessionUnlockedInProcess = false
                val msg = if (!credentialError.isNullOrBlank()) {
                    "Google Sign-In failed ($credentialError). Ensure Android app ${BuildConfig.APPLICATION_ID} (SHA-1 30:EA:37:F5:28:0C:58:0F:00:7A:A4:9D:81:64:5E:BA:DD:8D:EA:02) is registered in Firebase project test-2a0d0."
                } else {
                    "Google Sign-In needs Firebase Android setup: add app ${BuildConfig.APPLICATION_ID} with SHA-1 30:EA:37:F5:28:0C:58:0F:00:7A:A4:9D:81:64:5E:BA:DD:8D:EA:02 in Firebase Console (test-2a0d0) and set GOOGLE_WEB_CLIENT_ID in Secrets (or add google-services.json). Email/Password login works now."
                }
                _uiState.update { it.copy(isGoogleLoading = false, authError = msg) }
            }
        }
    }

    fun completeGoogleOnboarding(name: String, phone: String, upiId: String, vehicle: String) {
        val signedIn = _uiState.value.authState as? AuthUiState.SignedIn ?: return
        if (name.isBlank() || vehicle.isBlank()) {
            _uiState.update { it.copy(authError = "Enter your full name and vehicle details.") }
            return
        }
        if (!SecurityUtils.isValidIndianMobile(phone)) {
            _uiState.update { it.copy(authError = "Enter a valid 10-digit Indian mobile number.") }
            return
        }
        if (!SecurityUtils.isValidUpiId(upiId)) {
            _uiState.update { it.copy(authError = "Enter a valid UPI ID (e.g. 9876543210@paytm).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            val currentSettings = _uiState.value.adminSettings
            repo.completeGoogleRiderOnboarding(
                uid = signedIn.uid,
                name = name,
                email = signedIn.email,
                phone = phone,
                upiId = upiId,
                vehicle = vehicle,
                acceptedSettings = currentSettings
            ).onSuccess {
                authPrefs.saveLoginPreference(signedIn.email, true, name)
                authPrefs.linkPhoneToEmail(phone, signedIn.email, name)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        needsGoogleOnboarding = false,
                        needsAgreementAcceptance = false
                    )
                }
                showToast("Rider profile completed! Awaiting Admin verification.", ToastType.SUCCESS)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isAuthLoading = false, authError = err.message ?: "Failed to save rider details")
                }
            }
        }
    }

    fun openAgreementModal(initialTab: Int = 0) {
        _uiState.update { it.copy(agreementViewerTab = initialTab.coerceIn(0, 1)) }
    }

    fun closeAgreementModal() {
        _uiState.update { it.copy(agreementViewerTab = null) }
    }

    fun acceptRiderAgreement() {
        val state = _uiState.value
        val signedIn = state.authState as? AuthUiState.SignedIn
        if (signedIn == null) {
            closeAgreementModal()
            return
        }
        if (state.isAgreementSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAgreementSaving = true) }
            repo.acceptLegalAgreements(signedIn.uid, state.adminSettings)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isAgreementSaving = false,
                            needsAgreementAcceptance = false,
                            agreementViewerTab = null
                        )
                    }
                    evaluateGpsRequirement()
                    evaluateBackgroundNotificationService()
                    showToast(
                        "Agreement v${state.adminSettings.agreementVersion} & Privacy Policy accepted!",
                        ToastType.SUCCESS
                    )
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isAgreementSaving = false) }
                    showToast("Failed to save agreement: ${err.message}", ToastType.DANGER)
                }
        }
    }

    fun signOut() {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid
        if (hasActivePostPickupOrder(state.assignedOrders)) {
            val lockedOrderId = state.assignedOrders.values
                .firstOrNull { SecurityUtils.isOrderPostPickupLocked(it) && !SecurityUtils.isOrderCompleted(it) }
                ?.orderId.orEmpty()
            repo.logSecurityEvent(
                uid = uid,
                orderId = lockedOrderId,
                eventType = "UNAUTHORIZED_SIGNOUT_ATTEMPT",
                attemptedAction = "Rider attempted sign-out while carrying picked-up order",
                result = "BLOCKED"
            )
            showToast("LOCKED: Cannot sign out while carrying a picked-up order!", ToastType.DANGER)
            return
        }
        val credentialManager = CredentialManager.create(getApplication())
        viewModelScope.launch {
            withContext(NonCancellable) {
                locationTracker.stop()
                RiderLocationForegroundService.stop(getApplication())
                FleetBackgroundService.stop(getApplication())
                sessionUnlockedInProcess = false
                runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                repo.signOut(uid)
            }
        }
    }

    fun selectTab(tab: RiderTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectTabByName(tabName: String?) {
        val target = when (tabName?.uppercase()) {
            FleetNotificationHelper.TAB_EARNINGS.uppercase(), "EARNINGS" -> RiderTab.EARNINGS
            FleetNotificationHelper.TAB_PROFILE.uppercase(), "PROFILE" -> RiderTab.PROFILE
            FleetNotificationHelper.TAB_ACTIVE.uppercase(), "ACTIVE" -> RiderTab.ACTIVE
            "DASHBOARD" -> RiderTab.DASHBOARD
            "COD_CASH" -> RiderTab.COD_CASH
            "TRIP_HISTORY" -> RiderTab.TRIP_HISTORY
            "RATINGS" -> RiderTab.RATINGS
            "KYC" -> RiderTab.KYC
            "SUPPORT" -> RiderTab.SUPPORT
            "SETTINGS" -> RiderTab.SETTINGS
            else -> return
        }
        _uiState.update { it.copy(selectedTab = target) }
    }

    fun refreshNotificationPermission() {
        val granted = FleetNotificationHelper.hasNotificationPermission(getApplication())
        _uiState.update { it.copy(hasNotificationPermission = granted) }
        evaluateBackgroundNotificationService()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasNotificationPermission = granted) }
        if (granted) {
            showToast("Background notifications enabled!", ToastType.SUCCESS)
            evaluateBackgroundNotificationService()
        } else {
            showToast("Notification permission denied — background alerts won't pop up.", ToastType.WARNING)
        }
    }

    fun setBackgroundAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            authPrefs.setBackgroundAlertsEnabled(enabled)
            if (enabled) {
                showToast("Background trip & Admin notifications ON.", ToastType.SUCCESS)
            } else {
                FleetBackgroundService.stop(getApplication())
                showToast("Background notifications paused.", ToastType.INFO)
            }
            evaluateBackgroundNotificationService()
        }
    }

    fun sendTestNotification() {
        val cooldown = rateLimiter.checkCooldown("test_notif", 10_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.INFO)
            return
        }
        val app = getApplication<Application>()
        if (!FleetNotificationHelper.hasNotificationPermission(app)) {
            _uiState.update { it.copy(hasNotificationPermission = false) }
            showToast("Grant notification permission first to receive alerts.", ToastType.WARNING)
            return
        }
        rateLimiter.recordAction("test_notif")
        val sent = FleetNotificationHelper.notifyTestAlert(app)
        if (sent) {
            showToast("Test notification sent! Check your status bar.", ToastType.SUCCESS)
        } else {
            showToast("Could not post notification. Check system notification settings.", ToastType.DANGER)
        }
    }

    private fun evaluateBackgroundNotificationService() {
        val app = getApplication<Application>()
        val state = _uiState.value
        val signedIn = state.authState as? AuthUiState.SignedIn
        val alertsAllowed = state.savedAuthPrefs.backgroundAlertsEnabled &&
            FleetNotificationHelper.hasNotificationPermission(app)
        val hasActiveWork = state.rider.online || hasActivePostPickupOrder(state.assignedOrders)
        val shouldRun = signedIn != null &&
            state.isProfileLoaded &&
            !state.needsGoogleOnboarding &&
            !state.isPinLocked &&
            !state.needsAgreementAcceptance &&
            alertsAllowed &&
            hasActiveWork &&
            !state.rider.suspended

        if (shouldRun) {
            FleetBackgroundService.start(
                context = app,
                uid = signedIn.uid,
                riderName = state.rider.name.ifBlank { signedIn.displayName }
            )
        } else {
            FleetBackgroundService.stop(app)
        }
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        _uiState.update { it.copy(soundEnabled = next) }
        showToast(if (next) "Trip alert sound: ON" else "Trip alert sound: MUTED", ToastType.INFO)
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }

    fun showToast(message: String, type: ToastType = ToastType.SUCCESS) {
        _uiState.update { it.copy(toast = ToastEvent(System.currentTimeMillis(), message, type)) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        locationPermissionDeniedInSession = !granted
        _uiState.update { it.copy(needsLocationPermission = false) }
        if (granted) {
            evaluateGpsRequirement()
        } else {
            _uiState.update {
                it.copy(gpsState = GpsRadarState.Error("GPS PERMISSION DENIED — TAP TO ENABLE", "Enable"))
            }
        }
    }

    fun refreshLocationPermission() {
        if (RiderLocationForegroundService.hasLocationPermission(getApplication())) {
            locationPermissionDeniedInSession = false
            evaluateGpsRequirement()
        }
    }

    fun retryLocationPermission() {
        locationPermissionDeniedInSession = false
        evaluateGpsRequirement()
    }

    fun forceGpsSync() {
        val cooldown = rateLimiter.checkCooldown("force_gps_sync", 8_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.INFO)
            return
        }
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        if (!RiderLocationForegroundService.hasLocationPermission(getApplication())) {
            retryLocationPermission()
            return
        }
        if (!state.rider.online && !hasActivePostPickupOrder(state.assignedOrders)) {
            showToast("Turn Duty ON to activate live 10s GPS sync.", ToastType.INFO)
            return
        }
        rateLimiter.recordAction("force_gps_sync")
        RiderLocationForegroundService.requestImmediateSync(
            context = getApplication(),
            uid = uid,
            online = state.rider.online
        )
        showToast("Syncing live GPS coordinates to Admin Hub...", ToastType.INFO)
    }

    fun toggleDutyStatus() {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        if (state.rider.suspended) {
            showToast("Account suspended by Admin!", ToastType.DANGER)
            return
        }
        val newStatus = !state.rider.online
        if (newStatus && state.netCashInHand >= state.adminSettings.maxRiderCashLimit) {
            showToast(
                "Cash Limit Rs.${state.adminSettings.maxRiderCashLimit.toInt()} exceeded! Deposit cash first.",
                ToastType.DANGER
            )
            return
        }
        if (!newStatus && hasActivePostPickupOrder(state.assignedOrders)) {
            showToast("Cannot go offline while carrying a picked-up order!", ToastType.DANGER)
            return
        }
        viewModelScope.launch {
            val startedAt = if (newStatus) System.currentTimeMillis() else state.rider.shiftStartedAt
            runCatching { repo.updateRiderOnline(uid, newStatus, state.rider.shiftStartedAt) }
                .onSuccess {
                    if (newStatus) locationPermissionDeniedInSession = false
                    _uiState.update {
                        it.copy(
                            rider = it.rider.copy(
                                online = newStatus,
                                shiftStartedAt = if (newStatus) startedAt else it.rider.shiftStartedAt
                            ),
                            shiftElapsedSeconds = if (newStatus) 0L else 0L
                        )
                    }
                    if (newStatus) evaluateGpsRequirement() else stopGpsIfAllowed()
                    evaluateBackgroundNotificationService()
                    showToast(
                        if (newStatus) "Duty ON: Shift timer, GPS & alerts active!" else "Duty OFF: Shift logged & ended.",
                        if (newStatus) ToastType.SUCCESS else ToastType.INFO
                    )
                }
                .onFailure { showToast("Failed to update duty status: ${it.message}", ToastType.DANGER) }
        }
    }

    private fun hasActivePostPickupOrder(orders: Map<String, OrderRecord>): Boolean =
        orders.values.any { SecurityUtils.isOrderPostPickupLocked(it) && !SecurityUtils.isOrderCompleted(it) }

    private fun evaluateGpsRequirement() {
        val state = _uiState.value
        val signedIn = state.authState as? AuthUiState.SignedIn ?: return
        val blockedByGate = state.rider.suspended ||
            !state.isProfileLoaded ||
            state.needsGoogleOnboarding ||
            state.isPinLocked ||
            state.needsAgreementAcceptance
        val shouldTrack = (state.rider.online || hasActivePostPickupOrder(state.assignedOrders)) && !blockedByGate
        if (!shouldTrack) {
            stopGpsIfAllowed(force = blockedByGate)
            return
        }
        if (!RiderLocationForegroundService.hasLocationPermission(getApplication())) {
            _uiState.update {
                it.copy(
                    needsLocationPermission = !locationPermissionDeniedInSession,
                    gpsState = GpsRadarState.Error("LOCATION PERMISSION REQUIRED — TAP TO ENABLE", "Enable")
                )
            }
            return
        }
        if (state.gpsState is GpsRadarState.Hidden || state.gpsState is GpsRadarState.Error) {
            _uiState.update { it.copy(gpsState = GpsRadarState.Connecting) }
        }
        RiderLocationForegroundService.start(
            context = getApplication(),
            uid = signedIn.uid,
            online = state.rider.online
        )
    }

    private fun stopGpsIfAllowed(force: Boolean = false) {
        val state = _uiState.value
        if (!force && hasActivePostPickupOrder(state.assignedOrders)) return
        locationTracker.stop()
        RiderLocationForegroundService.stop(getApplication())
        _uiState.update { it.copy(gpsState = GpsRadarState.Hidden) }
    }

    private fun handleForegroundLocationTelemetry(telemetry: LiveLocationTelemetry) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return

        if (telemetry.permissionMissing) {
            _uiState.update {
                it.copy(
                    needsLocationPermission = !locationPermissionDeniedInSession,
                    gpsState = GpsRadarState.Error("GPS PERMISSION DENIED — TAP TO ENABLE", "Enable")
                )
            }
            return
        }

        if (telemetry.mockLocationDetected) {
            val wasMock = state.mockLocationDetected
            val updatedReport = SecurityShield.scanDeviceSecurity(
                context = getApplication(),
                isMockLocationActive = true,
                spoofReason = telemetry.spoofReason
            )
            _uiState.update {
                it.copy(
                    mockLocationDetected = true,
                    spoofAlertMessage = telemetry.spoofReason ?: "Fake GPS / Mock Location detected!",
                    securityReport = updatedReport,
                    blockedThreatsCount = if (!wasMock) it.blockedThreatsCount + 1 else it.blockedThreatsCount,
                    gpsState = GpsRadarState.Error("FAKE GPS BLOCKED — DISABLE MOCK LOCATION", "Alert")
                )
            }
            if (!wasMock) {
                showToast(
                    telemetry.spoofReason ?: "ANTI-SCAM ALERT: Mock GPS detected & blocked!",
                    ToastType.DANGER
                )
            }
            return
        }

        if (!telemetry.isRunning) return

        val lat = telemetry.lat ?: return
        val lng = telemetry.lng ?: return
        val accuracy = telemetry.accuracyMeters
        val netOnline = state.isNetworkOnline && locationTracker.isNetworkOnline()

        _uiState.update {
            val clearedReport = if (it.mockLocationDetected) {
                SecurityShield.scanDeviceSecurity(getApplication(), isMockLocationActive = false)
            } else {
                it.securityReport
            }
            it.copy(
                riderLat = lat,
                riderLng = lng,
                riderAccuracy = accuracy,
                riderSpeedKmh = telemetry.speedKmh,
                batteryPct = telemetry.batteryPct ?: it.batteryPct,
                lastGpsSyncAtMs = telemetry.lastFirebaseSyncAtMs.takeIf { ts -> ts > 0L } ?: it.lastGpsSyncAtMs,
                mockLocationDetected = false,
                spoofAlertMessage = null,
                securityReport = clearedReport,
                gpsState = GpsRadarState.Live(accuracy, netOnline)
            )
        }

        val now = System.currentTimeMillis()
        state.assignedOrders.values.forEach { order ->
            if (order.status in setOf("PICKED_UP", "OUT_FOR_DELIVERY", "CANCELLATION_REQUESTED", "EMERGENCY_INVESTIGATION")) {
                val lastLocUpdate = order.lastLocationUpdate
                if (lastLocUpdate != null && (now - lastLocUpdate) > 180_000L) {
                    val key = "${order.orderId}_outage"
                    if (now - (alertThrottleTs[key] ?: 0L) > 300_000L) {
                        alertThrottleTs[key] = now
                        repo.logSecurityEvent(
                            uid,
                            order.orderId,
                            "GPS_INTERRUPTION_DETECTED",
                            "Location updates paused for ${(now - lastLocUpdate) / 60_000L} mins during active delivery",
                            "LOGGED"
                        )
                    }
                }

                if (order.customerLat != null && order.customerLng != null && (accuracy == null || accuracy <= 100)) {
                    val distNow = SecurityUtils.calculateDistanceKm(lat, lng, order.customerLat, order.customerLng)
                    if (distNow != null) {
                        val initDist = pickupInitialDist.getOrPut(order.orderId) { distNow }
                        if (distNow - initDist > 2.5) {
                            val devKey = "${order.orderId}_dev"
                            if (now - (alertThrottleTs[devKey] ?: 0L) > 300_000L) {
                                alertThrottleTs[devKey] = now
                                repo.logSecurityEvent(
                                    uid,
                                    order.orderId,
                                    "SUSPICIOUS_ROUTE_DEVIATION",
                                    "Rider moved %.1fkm away from customer (initial %.1fkm)".format(distNow, initDist),
                                    "LOGGED"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun monitorGpsHealthAndOutages() {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val now = System.currentTimeMillis()
        if (!locationTracker.isNetworkOnline() && state.gpsState !is GpsRadarState.Hidden) {
            _uiState.update {
                it.copy(gpsState = GpsRadarState.Error("INTERNET DISCONNECTED — LOCKED ORDERS PROTECTED", "Offline"))
            }
        }
        state.assignedOrders.values.forEach { order ->
            if (order.status in setOf("PICKED_UP", "OUT_FOR_DELIVERY")) {
                val lastUp = order.lastLocationUpdate ?: return@forEach
                if ((now - lastUp) > 180_000L) {
                    val key = "${order.orderId}_stale"
                    if (now - (alertThrottleTs[key] ?: 0L) > 300_000L) {
                        alertThrottleTs[key] = now
                        repo.logSecurityEvent(
                            uid,
                            order.orderId,
                            "GPS_INTERRUPTION_DETECTED",
                            "No GPS location update received for >3 mins",
                            "LOGGED"
                        )
                    }
                }
            }
        }
    }

    fun getOrderEarningBreakdown(order: OrderRecord): OrderEarningBreakdown {
        val state = _uiState.value
        return RiderEarningsCalculator.calculateOrderEarning(
            order = order,
            store = state.stores[order.restaurantId],
            settings = state.adminSettings
        )
    }

    fun getRiderFeeForOrder(order: OrderRecord): Double =
        getOrderEarningBreakdown(order).totalEarning

    private fun recomputeTripsAndFinancials() {
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val activeMap = linkedMapOf<String, MutableList<OrderRecord>>()
            val completedMap = linkedMapOf<String, MutableList<OrderRecord>>()

            for (order in state.assignedOrders.values) {
                val key = order.batchGroupId.ifBlank { "${order.customerUid}_${order.createdAt ?: order.orderId}" }
                val target = if (SecurityUtils.isOrderCompleted(order)) completedMap else activeMap
                target.getOrPut(key) { mutableListOf() }.add(order)
            }

            fun buildTrip(tripId: String, orders: List<OrderRecord>): TripGroup {
                val first = orders.first()
                return TripGroup(
                    tripId = tripId,
                    customerUid = first.customerUid,
                    phone = first.phone,
                    address = first.address,
                    customerLat = first.customerLat,
                    customerLng = first.customerLng,
                    locationLink = first.locationLink,
                    paymentMode = first.paymentMode,
                    paymentStatus = first.paymentStatus,
                    emergencyAlert = orders.any { it.emergencyAlert },
                    emergencyDetails = first.emergencyDetails,
                    assignedAt = first.assignedAt ?: first.createdAt ?: System.currentTimeMillis(),
                    orders = orders
                )
            }

            val activeTrips = activeMap.map { (k, v) -> buildTrip(k, v) }
            val completedTrips = completedMap.map { (k, v) -> buildTrip(k, v) }
                .sortedByDescending { it.assignedAt }

            val startOfTodayMs = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val sevenDaysAgoMs = startOfTodayMs - 6L * 24L * 3600L * 1000L

            var totalCodCollected = 0.0
            var totalRiderEarned = 0.0
            var todayEarnings = 0.0
            var todayCodCollected = 0.0
            var weekEarnings = 0.0
            val todayTripKeys = mutableSetOf<String>()
            val weekTripKeys = mutableSetOf<String>()

            for (o in state.assignedOrders.values) {
                if (o.status == "DELIVERED") {
                    val fee = RiderEarningsCalculator.calculateOrderEarning(
                        order = o,
                        store = state.stores[o.restaurantId],
                        settings = state.adminSettings
                    ).totalEarning
                    totalRiderEarned += fee
                    val isCod = o.paymentMode != "UPI" && o.paymentMode != "WALLET"
                    if (isCod) {
                        totalCodCollected += o.totalAmount
                    }
                    val completedTs = o.deliveredAt ?: o.updatedAt ?: o.assignedAt ?: o.createdAt ?: 0L
                    val tripKey = o.batchGroupId.ifBlank { "${o.customerUid}_${o.createdAt ?: o.orderId}" }
                    if (completedTs >= startOfTodayMs) {
                        todayEarnings += fee
                        if (isCod) todayCodCollected += o.totalAmount
                        todayTripKeys.add(tripKey)
                    }
                    if (completedTs >= sevenDaysAgoMs) {
                        weekEarnings += fee
                        weekTripKeys.add(tripKey)
                    }
                }
            }
            val totalDeposited = state.riderSettlements.filter { it.status == "APPROVED" }.sumOf { it.amount }
            val pendingCodSettlement = state.riderSettlements.filter { it.status == "PENDING" }.sumOf { it.amount }
            val netCashInHand = (totalCodCollected - totalDeposited).coerceAtLeast(0.0)
            val totalPaidPayout = state.riderPayouts.filter { it.status == "PAID" }.sumOf { it.amount }
            val pendingPayout = state.riderPayouts.filter { it.status == "PENDING" }.sumOf { it.amount }
            val availablePayout = (totalRiderEarned - totalPaidPayout - pendingPayout).coerceAtLeast(0.0)

            _uiState.update {
                it.copy(
                    activeTrips = activeTrips,
                    completedTrips = completedTrips,
                    totalCodCollected = totalCodCollected,
                    totalDeposited = totalDeposited,
                    pendingCodSettlementAmount = pendingCodSettlement,
                    netCashInHand = netCashInHand,
                    totalRiderEarned = totalRiderEarned,
                    totalRiderPaidPayout = totalPaidPayout,
                    pendingPayoutAmount = pendingPayout,
                    availablePayoutBalance = availablePayout,
                    todayTripsCount = todayTripKeys.size,
                    todayEarnings = todayEarnings,
                    todayCodCollected = todayCodCollected,
                    weekTripsCount = weekTripKeys.size,
                    weekEarnings = weekEarnings
                )
            }
        }
    }

    private fun updateCountdownsAndAutoPass() {
        val state = _uiState.value
        val timeoutSec = state.adminSettings.riderAcceptTimeoutSec
        val now = System.currentTimeMillis()
        val map = mutableMapOf<String, Int>()
        val toAutoPass = mutableListOf<String>()

        for (trip in state.activeTrips) {
            if (SecurityUtils.isTripPostPickupLocked(trip)) continue
            if (trip.orders.all { it.riderAccepted == true }) continue
            val elapsedSec = ((now - trip.assignedAt) / 1000L).toInt()
            val remaining = (timeoutSec - elapsedSec).coerceAtLeast(0)
            map[trip.tripId] = remaining
            if (remaining <= 0 && trip.tripId !in timedOutTrips) {
                timedOutTrips.add(trip.tripId)
                toAutoPass.add(trip.tripId)
            }
        }
        val elapsedShiftSec = if (state.rider.online) {
            val baseStart = state.rider.shiftStartedAt
            if (baseStart > 0L) ((now - baseStart) / 1000L).coerceAtLeast(0L)
            else state.shiftElapsedSeconds + 1L
        } else {
            0L
        }
        _uiState.update { it.copy(countdownByTrip = map, shiftElapsedSeconds = elapsedShiftSec) }
        for (tripId in toAutoPass) {
            passTripToNextRider(tripId, "Auto-Timeout (${timeoutSec}s)")
        }
    }

    fun acceptTrip(tripId: String) {
        val state = _uiState.value
        if (state.rider.suspended) {
            showToast("Account suspended by Admin!", ToastType.DANGER)
            return
        }
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId } ?: return
        viewModelScope.launch {
            runCatching { repo.acceptOrders(trip.orders.map { it.orderId }) }
                .onSuccess { showToast("Trip Accepted!", ToastType.SUCCESS) }
                .onFailure { showToast("Error accepting trip: ${it.message}", ToastType.DANGER) }
        }
    }

    fun passTripToNextRider(tripId: String, reason: String) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId } ?: return
        if (SecurityUtils.isTripPostPickupLocked(trip)) {
            repo.logSecurityEvent(
                uid,
                trip.orders.firstOrNull()?.orderId ?: tripId,
                "UNAUTHORIZED_CANCEL_ATTEMPT",
                "Rider attempted post-pickup pass/cancel ($reason)",
                "BLOCKED"
            )
            showToast("BLOCKED: Post-pickup cancellation lock is active! Only Admin can cancel.", ToastType.DANGER)
            return
        }
        showToast("Finding next nearest online rider...", ToastType.INFO)
        viewModelScope.launch {
            runCatching {
                repo.passTripToNextNearestRider(
                    orders = trip.orders,
                    currentUid = uid,
                    riderName = state.rider.name,
                    riderPhone = state.rider.phone,
                    reason = reason,
                    stores = state.stores,
                    fallbackLat = state.riderLat,
                    fallbackLng = state.riderLng
                )
            }.onSuccess { nextUid ->
                if (nextUid != null) {
                    showToast("Trip passed to next nearest rider!", ToastType.SUCCESS)
                } else {
                    showToast("No other rider online. Sent to Admin queue.", ToastType.WARNING)
                }
            }.onFailure { err ->
                showToast("Error passing trip: ${err.message}", ToastType.DANGER)
            }
        }
    }

    // Modals
    fun closeModal() {
        _uiState.update { it.copy(activeModal = ActiveModal.None, isModalBusy = false) }
    }

    fun openPickupVerifyModal(orderId: String, storeName: String) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid
        val order = state.assignedOrders[orderId]
        if (order == null || !SecurityUtils.isOrderReadyForPickup(order)) {
            repo.logSecurityEvent(uid, orderId, "UNAUTHORIZED_STATUS_CHANGE", "Attempted pickup before Restaurant marked READY_FOR_PICKUP", "BLOCKED")
            showToast("Restaurant has not marked this order Ready for Pickup yet!", ToastType.DANGER)
            return
        }
        val displayCode = SecurityUtils.getDisplayablePickupCode(order)
        _uiState.update { it.copy(activeModal = ActiveModal.PickupVerify(orderId, storeName, displayCode)) }
    }

    fun confirmPickupOtp(orderId: String, enteredOtp: String) {
        val state = _uiState.value
        if (state.isModalBusy) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        if (state.rider.suspended) {
            recordBlockedThreat("SUSPENDED_ACTION_BLOCKED", orderId, "Suspended rider attempted pickup verification")
            closeModal()
            showToast("Account suspended by Admin!", ToastType.DANGER)
            return
        }
        if (state.mockLocationDetected) {
            recordBlockedThreat("MOCK_GPS_PICKUP_BLOCKED", orderId, "Blocked pickup OTP while Fake GPS active")
            showToast("BLOCKED: Turn off Fake GPS / Mock Location app before verifying pickup!", ToastType.DANGER)
            return
        }
        val order = state.assignedOrders[orderId]
        if (order == null || !SecurityUtils.isOrderReadyForPickup(order)) {
            recordBlockedThreat("UNAUTHORIZED_STATUS_CHANGE", orderId, "Confirmed pickup while order not READY_FOR_PICKUP")
            closeModal()
            showToast("Restaurant has not marked this order Ready for Pickup yet!", ToastType.DANGER)
            return
        }
        val guard = pickupGuards.getOrPut(orderId) { BruteForceGuard() }
        val now = System.currentTimeMillis()
        if (now < guard.lockedUntilMs) {
            val sec = ((guard.lockedUntilMs - now) / 1000L + 1).toInt()
            showToast("Too many wrong Pickup OTP attempts! Wait ${sec}s.", ToastType.DANGER)
            return
        }
        val cleanOtp = enteredOtp.trim()
        if (!Regex("^\\d{4}$").matches(cleanOtp)) {
            showToast("Enter 4-digit Pickup OTP from Storekeeper!", ToastType.DANGER)
            return
        }
        val matched = if (!order.pickupOtpHash.isNullOrBlank()) {
            SecurityUtils.sha256Hex("${orderId}:$cleanOtp") == order.pickupOtpHash
        } else {
            cleanOtp == SecurityUtils.getStorePickupCode(order, orderId)
        }
        if (!matched) {
            guard.fails += 1
            repo.logSecurityEvent(uid, orderId, "PICKUP_OTP_FAILED", "Wrong Store Pickup OTP attempt #${guard.fails}", "BLOCKED")
            if (guard.fails >= 3) {
                guard.lockedUntilMs = now + 60_000L
                guard.fails = 0
                showToast("3 Wrong Pickup OTP Attempts! Locked 60s.", ToastType.DANGER)
            } else {
                showToast("Wrong Store Pickup OTP! (${3 - guard.fails} left)", ToastType.DANGER)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isModalBusy = true) }
            evaluateGpsRequirement()
            runCatching {
                repo.verifyAndLockPickup(order, uid, state.riderLat, state.riderLng, state.riderAccuracy)
            }.onSuccess {
                closeModal()
                showToast("Pickup Verified & Cancellation Lock Activated!", ToastType.SUCCESS)
            }.onFailure { err ->
                _uiState.update { it.copy(isModalBusy = false) }
                showToast("Error: ${err.message}", ToastType.DANGER)
            }
        }
    }

    fun startCustomerDoorstepTrip(tripId: String) {
        val state = _uiState.value
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId } ?: return
        val allReady = trip.orders.all {
            it.pickupVerified || it.status == "PICKED_UP" || it.status == "OUT_FOR_DELIVERY"
        }
        if (!allReady) {
            showToast("Verify store pickup first!", ToastType.DANGER)
            return
        }
        evaluateGpsRequirement()
        viewModelScope.launch {
            runCatching {
                repo.startDoorstepInTransit(trip.orders.map { it.orderId }, state.riderLat, state.riderLng)
            }.onSuccess {
                showToast("In-Transit route started!", ToastType.INFO)
            }.onFailure {
                showToast("Failed to start transit: ${it.message}", ToastType.DANGER)
            }
        }
    }

    fun openDeliveryVerifyModal(tripId: String, totalAmount: Double, isPrepaid: Boolean) {
        val state = _uiState.value
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId } ?: return
        val blockedStatus = trip.orders.any {
            it.status in setOf("CANCELLATION_REQUESTED", "EMERGENCY_INVESTIGATION", "RETURNED_TO_STORE")
        }
        if (blockedStatus) {
            showToast("Order is under Admin Emergency Review. Wait for Admin decision.", ToastType.WARNING)
            return
        }
        _uiState.update { it.copy(activeModal = ActiveModal.DeliveryVerify(tripId, totalAmount, isPrepaid)) }
    }

    fun confirmDeliveryPin(tripId: String, enteredPin: String, deliveryProofPhoto: String? = null) {
        val state = _uiState.value
        if (state.isModalBusy) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        if (state.rider.suspended) {
            recordBlockedThreat("SUSPENDED_ACTION_BLOCKED", tripId, "Suspended rider attempted delivery completion")
            closeModal()
            showToast("Account suspended by Admin!", ToastType.DANGER)
            return
        }
        if (state.mockLocationDetected) {
            recordBlockedThreat("MOCK_GPS_DELIVERY_BLOCKED", tripId, "Blocked delivery PIN while Fake GPS active")
            showToast("BLOCKED: Disable Fake GPS / Mock Location app before completing delivery!", ToastType.DANGER)
            return
        }
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId } ?: return
        val cleanPin = enteredPin.trim()
        if (!Regex("^\\d{4}$").matches(cleanPin)) {
            showToast("Enter Customer 4-Digit Delivery PIN!", ToastType.DANGER)
            return
        }
        if (state.adminSettings.requireDeliveryPhoto && deliveryProofPhoto.isNullOrBlank()) {
            showToast("Admin requires a Proof of Delivery photo before completing!", ToastType.WARNING)
            return
        }

        val guard = pinGuards.getOrPut(tripId) { BruteForceGuard() }
        val now = System.currentTimeMillis()
        if (now < guard.lockedUntilMs) {
            val sec = ((guard.lockedUntilMs - now) / 1000L + 1).toInt()
            showToast("Locked: Wait ${sec}s.", ToastType.DANGER)
            return
        }

        for (o in trip.orders) {
            val expiresAt = o.deliveryOtpExpiresAt
            if (expiresAt != null && now > expiresAt) {
                repo.logSecurityEvent(uid, o.orderId, "DELIVERY_OTP_EXPIRED", "Attempted delivery with expired OTP", "BLOCKED")
                showToast("Delivery OTP expired! Contact Admin for Admin Exception.", ToastType.DANGER)
                return
            }
            val matched = when {
                !o.deliveryOtpHash.isNullOrBlank() -> SecurityUtils.sha256Hex("${o.orderId}:$cleanPin") == o.deliveryOtpHash
                !o.deliveryOtp.isNullOrBlank() -> cleanPin == o.deliveryOtp
                else -> false
            }
            if (!matched) {
                guard.fails += 1
                repo.logSecurityEvent(uid, o.orderId, "DELIVERY_OTP_FAILED", "Wrong Customer Delivery PIN attempt #${guard.fails}", "BLOCKED")
                if (guard.fails >= 3) {
                    guard.lockedUntilMs = now + 60_000L
                    guard.fails = 0
                    showToast("3 Wrong Attempts! Locked 60s.", ToastType.DANGER)
                } else {
                    showToast("Wrong PIN! (${3 - guard.fails} left)", ToastType.DANGER)
                }
                return
            }
        }

        val orderEarnings = trip.orders.associate { order ->
            order.orderId to RiderEarningsCalculator.calculateOrderEarning(
                order = order,
                store = state.stores[order.restaurantId],
                settings = state.adminSettings
            )
        }
        val tripEarnedTotal = orderEarnings.values.sumOf { it.totalEarning }

        viewModelScope.launch {
            _uiState.update { it.copy(isModalBusy = true) }
            runCatching {
                repo.completeDelivery(
                    orderIds = trip.orders.map { it.orderId },
                    uid = uid,
                    deliveryProofPhoto = deliveryProofPhoto,
                    orderEarnings = orderEarnings
                )
            }
                .onSuccess {
                    closeModal()
                    showToast(
                        "PIN Verified! Trip Completed (+${RiderEarningsCalculator.formatRupees(tripEarnedTotal)})!",
                        ToastType.SUCCESS
                    )
                }.onFailure { err ->
                    _uiState.update { it.copy(isModalBusy = false) }
                    showToast("Error completing trip: ${err.message}", ToastType.DANGER)
                }
        }
    }

    fun openPassTripModal(tripId: String) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId }
        if (trip != null && SecurityUtils.isTripPostPickupLocked(trip)) {
            repo.logSecurityEvent(
                uid,
                trip.orders.firstOrNull()?.orderId ?: tripId,
                "UNAUTHORIZED_CANCEL_ATTEMPT",
                "Tried opening cancel modal after pickup",
                "BLOCKED"
            )
            showToast("ORDER PICKED UP — CANCELLATION LOCK ACTIVE. Only Admin can cancel.", ToastType.DANGER)
            return
        }
        if (!state.adminSettings.allowRiderPassTrip) {
            showToast("Passing trips disabled by Admin.", ToastType.INFO)
            return
        }
        _uiState.update { it.copy(activeModal = ActiveModal.PassTrip(tripId)) }
    }

    fun confirmPassTrip(tripId: String, reason: String, note: String) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId }
        if (trip != null && SecurityUtils.isTripPostPickupLocked(trip)) {
            closeModal()
            recordBlockedThreat(
                "UNAUTHORIZED_CANCEL_ATTEMPT",
                trip.orders.firstOrNull()?.orderId ?: tripId,
                "Tried confirming cancel/pass after pickup"
            )
            showToast("BLOCKED: Order picked up — cancellation lock is active!", ToastType.DANGER)
            return
        }
        val passWindow = rateLimiter.checkAndRecordWindow("manual_pass_trip", maxEvents = 4, windowMs = 300_000L)
        if (!passWindow.allowed) {
            recordBlockedThreat("PASS_TRIP_SPAM_DETECTED", tripId, "Excessive trip passing in 5 minutes")
            showToast(passWindow.reason, ToastType.DANGER)
            return
        }
        if (note.isNotBlank()) {
            val noteCheck = SecurityShield.inspectTextForSpamOrInjection(note, "Pass Note", maxLength = 200)
            if (!noteCheck.safe) {
                recordBlockedThreat(noteCheck.threatCode, tripId, noteCheck.reason)
                showToast(noteCheck.reason, ToastType.DANGER)
                return
            }
        }
        closeModal()
        val combined = if (note.isNotBlank()) "$reason (${SecurityUtils.sanitizeText(note.trim(), 200)})" else reason
        passTripToNextRider(tripId, combined)
    }

    fun openEmergencySosModal(tripId: String) {
        if (!_uiState.value.adminSettings.allowRiderSos) {
            showToast("Emergency Help disabled by Admin.", ToastType.INFO)
            return
        }
        _uiState.update { it.copy(activeModal = ActiveModal.EmergencySos(tripId)) }
    }

    fun submitEmergencySos(tripId: String, reason: String, description: String) {
        val state = _uiState.value
        if (state.isModalBusy) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val cooldown = rateLimiter.checkCooldown("sos_submit", 60_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.WARNING)
            return
        }
        val descCheck = SecurityShield.inspectTextForSpamOrInjection(description, "SOS Description", maxLength = 300)
        if (!descCheck.safe) {
            recordBlockedThreat(descCheck.threatCode, tripId, descCheck.reason)
            showToast(descCheck.reason, ToastType.DANGER)
            return
        }
        val trip = state.activeTrips.firstOrNull { it.tripId == tripId } ?: return
        val isPostPickup = SecurityUtils.isTripPostPickupLocked(trip)
        val now = System.currentTimeMillis()
        val recentCount = state.emergencyRequests.count { it.createdAt > 0L && (now - it.createdAt) < 24L * 3600L * 1000L }

        rateLimiter.recordAction("sos_submit")
        viewModelScope.launch {
            _uiState.update { it.copy(isModalBusy = true) }
            runCatching {
                repo.submitEmergencyRequest(
                    orders = trip.orders,
                    uid = uid,
                    riderName = state.rider.name,
                    riderPhone = state.rider.phone,
                    reason = reason,
                    description = SecurityUtils.sanitizeText(description, 300),
                    isPostPickup = isPostPickup,
                    recentCount24h = recentCount
                )
            }.onSuccess {
                closeModal()
                showToast("Emergency request sent to Admin! Order remains locked until Admin reviews.", ToastType.WARNING)
            }.onFailure { err ->
                _uiState.update { it.copy(isModalBusy = false) }
                showToast("Error sending alert: ${err.message}", ToastType.DANGER)
            }
        }
    }

    fun openPayoutModal() {
        val state = _uiState.value
        val min = state.adminSettings.minRiderPayout
        if (state.availablePayoutBalance < min) {
            showToast("Minimum Rs.${min.toInt()} balance required!", ToastType.INFO)
            return
        }
        _uiState.update { it.copy(activeModal = ActiveModal.PayoutWithdraw) }
    }

    fun submitPayoutRequest(amount: Double, upiId: String) {
        val state = _uiState.value
        if (state.isModalBusy) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        if (state.rider.suspended) {
            recordBlockedThreat("SUSPENDED_PAYOUT_BLOCKED", uid, "Suspended rider attempted payout withdrawal")
            closeModal()
            showToast("Account suspended by Admin! Payouts are locked.", ToastType.DANGER)
            return
        }
        val cooldown = rateLimiter.checkCooldown("payout_request", 30_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.WARNING)
            return
        }
        val min = state.adminSettings.minRiderPayout
        val max = state.adminSettings.maxRiderPayout
        if (amount < min || amount > max) {
            showToast("Allowed withdrawal: Rs.${min.toInt()} to Rs.${max.toInt()}!", ToastType.DANGER)
            return
        }
        if (amount > state.availablePayoutBalance) {
            showToast("Insufficient earnings balance!", ToastType.DANGER)
            return
        }
        if (!SecurityUtils.isValidUpiId(upiId)) {
            showToast("Enter a valid UPI ID!", ToastType.DANGER)
            return
        }
        rateLimiter.recordAction("payout_request")
        viewModelScope.launch {
            _uiState.update { it.copy(isModalBusy = true) }
            runCatching {
                repo.submitPayoutRequest(uid, state.rider.name, state.rider.phone, upiId.trim(), amount)
            }.onSuccess {
                closeModal()
                showToast("Rs.%.2f Payout Request sent to Admin!".format(amount), ToastType.SUCCESS)
            }.onFailure { err ->
                _uiState.update { it.copy(isModalBusy = false) }
                showToast("Error: ${err.message}", ToastType.DANGER)
            }
        }
    }

    fun openCodDepositModal() {
        val state = _uiState.value
        val remainingCodDue = (state.netCashInHand - state.pendingCodSettlementAmount).coerceAtLeast(0.0)
        if (state.netCashInHand <= 0.0) {
            showToast("No COD cash balance to deposit!", ToastType.INFO)
            return
        }
        if (remainingCodDue <= 0.0) {
            showToast("Your full COD cash deposit is already pending Admin approval.", ToastType.INFO)
            return
        }
        _uiState.update { it.copy(activeModal = ActiveModal.CodDeposit) }
    }

    fun submitCodDeposit(amount: Double, mode: String, utr: String) {
        val state = _uiState.value
        if (state.isModalBusy) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val cooldown = rateLimiter.checkCooldown("cod_deposit", 25_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.WARNING)
            return
        }
        val normalizedMode = mode.trim().uppercase()
        if (normalizedMode != "UPI" && normalizedMode != "CASH") {
            recordBlockedThreat("INVALID_COD_MODE", uid, "Attempted unsupported COD deposit mode: $mode")
            showToast("Only collected COD cash (UPI or Office Cash) can be deposited!", ToastType.DANGER)
            return
        }
        val remainingCodDue = (state.netCashInHand - state.pendingCodSettlementAmount).coerceAtLeast(0.0)
        if (remainingCodDue <= 0.0) {
            showToast("No remaining COD cash to deposit!", ToastType.INFO)
            return
        }
        if (amount <= 0.0) {
            showToast("Enter a valid COD deposit amount!", ToastType.DANGER)
            return
        }
        if (amount > remainingCodDue + 0.01) {
            showToast(
                "Amount exceeds remaining COD cash (Rs.%.2f)!".format(remainingCodDue),
                ToastType.DANGER
            )
            return
        }
        val min = state.adminSettings.minRiderCodDeposit
        val max = state.adminSettings.maxRiderCodDeposit
        val effectiveMin = if (remainingCodDue < min) remainingCodDue else min
        if (amount + 0.01 < effectiveMin || amount > max) {
            showToast(
                if (remainingCodDue < min) {
                    "Deposit full remaining COD cash (Rs.%.2f)!".format(remainingCodDue)
                } else {
                    "COD Deposit limit: Rs.${min.toInt()} to Rs.${max.toInt()}!"
                },
                ToastType.DANGER
            )
            return
        }
        if (normalizedMode == "UPI") {
            val check = SecurityUtils.validateSettlementUtr(utr, state.allSettlements)
            if (!check.valid) {
                recordBlockedThreat("FAKE_OR_DUPLICATE_UTR", uid, "Rejected UTR '${utr.trim()}': ${check.reason}")
                showToast(check.reason, ToastType.DANGER)
                return
            }
        }
        rateLimiter.recordAction("cod_deposit")
        val clampedAmount = amount.coerceAtMost(remainingCodDue)
        viewModelScope.launch {
            _uiState.update { it.copy(isModalBusy = true) }
            runCatching {
                repo.submitCodSettlement(uid, state.rider.name, clampedAmount, normalizedMode, utr.trim())
            }.onSuccess {
                closeModal()
                showToast("COD cash deposit submitted to Admin for approval!", ToastType.SUCCESS)
            }.onFailure { err ->
                _uiState.update { it.copy(isModalBusy = false) }
                showToast("Error: ${err.message}", ToastType.DANGER)
            }
        }
    }

    fun saveRiderProfile(name: String, phone: String, upiId: String, vehicle: String) {
        val state = _uiState.value
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val cooldown = rateLimiter.checkCooldown("profile_save", 12_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.INFO)
            return
        }
        val nameCheck = SecurityShield.inspectTextForSpamOrInjection(name, "Driver Name", maxLength = 80)
        if (!nameCheck.safe) {
            recordBlockedThreat(nameCheck.threatCode, uid, nameCheck.reason)
            showToast(nameCheck.reason, ToastType.DANGER)
            return
        }
        val vehicleCheck = SecurityShield.inspectTextForSpamOrInjection(vehicle, "Vehicle Info", maxLength = 80)
        if (!vehicleCheck.safe) {
            recordBlockedThreat(vehicleCheck.threatCode, uid, vehicleCheck.reason)
            showToast(vehicleCheck.reason, ToastType.DANGER)
            return
        }
        if (!SecurityUtils.isValidIndianMobile(phone)) {
            showToast("Enter a valid 10-digit mobile number!", ToastType.DANGER)
            return
        }
        if (!SecurityUtils.isValidUpiId(upiId)) {
            showToast("Enter a valid UPI ID!", ToastType.DANGER)
            return
        }
        if (name.isBlank() || vehicle.isBlank()) {
            showToast("Name and vehicle cannot be empty!", ToastType.DANGER)
            return
        }
        rateLimiter.recordAction("profile_save")
        viewModelScope.launch {
            runCatching {
                repo.updateRiderProfile(
                    uid,
                    SecurityUtils.sanitizeText(name, 80),
                    phone.filter { it.isDigit() }.takeLast(10),
                    upiId.trim(),
                    SecurityUtils.sanitizeText(vehicle, 80)
                )
            }
                .onSuccess { showToast("Rider profile & UPI ID saved!", ToastType.SUCCESS) }
                .onFailure { showToast("Save failed: ${it.message}", ToastType.DANGER) }
        }
    }

    fun openFeedbackModal() {
        _uiState.update { it.copy(activeModal = ActiveModal.Feedback) }
    }

    fun submitFeedback(subject: String, message: String) {
        val state = _uiState.value
        if (state.isModalBusy) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val cooldown = rateLimiter.checkCooldown("feedback_ticket", 45_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.WARNING)
            return
        }
        if (subject.isBlank() || message.isBlank()) {
            showToast("Subject and message are required!", ToastType.DANGER)
            return
        }
        val subjectCheck = SecurityShield.inspectTextForSpamOrInjection(subject, "Subject", maxLength = 100)
        if (!subjectCheck.safe) {
            recordBlockedThreat(subjectCheck.threatCode, uid, subjectCheck.reason)
            showToast(subjectCheck.reason, ToastType.DANGER)
            return
        }
        val messageCheck = SecurityShield.inspectTextForSpamOrInjection(message, "Message", maxLength = 400)
        if (!messageCheck.safe) {
            recordBlockedThreat(messageCheck.threatCode, uid, messageCheck.reason)
            showToast(messageCheck.reason, ToastType.DANGER)
            return
        }
        val fingerprint = SecurityUtils.sha256Hex("${subject.trim().lowercase()}|${message.trim().lowercase()}")
        if (rateLimiter.isDuplicateSubmission("feedback_$uid", fingerprint)) {
            recordBlockedThreat("DUPLICATE_SPAM_TICKET", uid, "Blocked duplicate feedback submission")
            showToast("Duplicate ticket blocked by Anti-Spam protection!", ToastType.WARNING)
            return
        }
        rateLimiter.recordAction("feedback_ticket")
        rateLimiter.recordSubmissionFingerprint("feedback_$uid", fingerprint)
        viewModelScope.launch {
            _uiState.update { it.copy(isModalBusy = true) }
            runCatching {
                repo.submitFeedback(
                    uid,
                    state.rider.name,
                    state.rider.phone,
                    SecurityUtils.sanitizeText(subject, 100),
                    SecurityUtils.sanitizeText(message, 400)
                )
            }.onSuccess {
                closeModal()
                showToast("Ticket sent to Admin Hub!", ToastType.SUCCESS)
            }.onFailure { err ->
                _uiState.update { it.copy(isModalBusy = false) }
                showToast("Failed: ${err.message}", ToastType.DANGER)
            }
        }
    }

    fun submitRiderKyc(
        licenseNumber: String,
        vehicleRegNumber: String,
        idLast4: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        idDocumentPhoto: String,
        selfiePhoto: String
    ) {
        val state = _uiState.value
        if (state.isKycSaving) return
        val uid = (state.authState as? AuthUiState.SignedIn)?.uid ?: return
        val cooldown = rateLimiter.checkCooldown("kyc_submit", 30_000L)
        if (!cooldown.allowed) {
            showToast(cooldown.reason, ToastType.WARNING)
            return
        }
        val dlCheck = SecurityShield.inspectTextForSpamOrInjection(licenseNumber, "Driving License", maxLength = 40)
        val rcCheck = SecurityShield.inspectTextForSpamOrInjection(vehicleRegNumber, "Vehicle RC", maxLength = 40)
        val ecCheck = SecurityShield.inspectTextForSpamOrInjection(emergencyContactName, "Emergency Contact", maxLength = 80)
        if (!dlCheck.safe || !rcCheck.safe || !ecCheck.safe) {
            val firstErr = listOf(dlCheck, rcCheck, ecCheck).first { !it.safe }
            recordBlockedThreat(firstErr.threatCode, uid, firstErr.reason)
            showToast(firstErr.reason, ToastType.DANGER)
            return
        }
        if (licenseNumber.trim().length < 5 || vehicleRegNumber.trim().length < 4) {
            showToast("Enter valid Driving License and Vehicle RC numbers.", ToastType.DANGER)
            return
        }
        val cleanId4 = idLast4.filter { it.isDigit() }
        if (cleanId4.length != 4) {
            showToast("Enter the last 4 digits of your Aadhaar / Govt ID.", ToastType.DANGER)
            return
        }
        if (emergencyContactName.isBlank() || !SecurityUtils.isValidIndianMobile(emergencyContactPhone)) {
            showToast("Enter Emergency Contact Name & valid 10-digit mobile.", ToastType.DANGER)
            return
        }
        rateLimiter.recordAction("kyc_submit")
        viewModelScope.launch {
            _uiState.update { it.copy(isKycSaving = true) }
            runCatching {
                repo.submitRiderKyc(
                    uid = uid,
                    licenseNumber = SecurityUtils.sanitizeText(licenseNumber, 40),
                    vehicleRegNumber = SecurityUtils.sanitizeText(vehicleRegNumber, 40),
                    idLast4 = cleanId4,
                    emergencyContactName = SecurityUtils.sanitizeText(emergencyContactName, 80),
                    emergencyContactPhone = emergencyContactPhone.filter { it.isDigit() }.takeLast(10),
                    idDocumentPhoto = idDocumentPhoto,
                    selfiePhoto = selfiePhoto
                )
            }.onSuccess {
                _uiState.update { it.copy(isKycSaving = false) }
                showToast("KYC submitted! Waiting for Admin verification.", ToastType.SUCCESS)
            }.onFailure { err ->
                _uiState.update { it.copy(isKycSaving = false) }
                showToast("KYC upload failed: ${err.message}", ToastType.DANGER)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stop()
    }
}
