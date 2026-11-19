package com.example.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.AuthPrefsStore
import com.example.data.FirebaseFleetRepository
import com.example.data.RiderEarningsCalculator
import com.example.data.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FleetBackgroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.notifications.ACTION_START_DUTY_MONITOR"
        const val ACTION_STOP = "com.example.notifications.ACTION_STOP_DUTY_MONITOR"
        const val ACTION_PAUSE_BY_USER = "com.example.notifications.ACTION_PAUSE_BY_USER"
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_RIDER_NAME = "extra_rider_name"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, uid: String, riderName: String) {
            if (uid.isBlank()) return
            val intent = Intent(context, FleetBackgroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_UID, uid)
                putExtra(EXTRA_RIDER_NAME, riderName)
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { err ->
                Log.w("FleetBgService", "Could not start foreground service: ${err.message}")
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, FleetBackgroundService::class.java))
            }
            _isRunning.value = false
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: FirebaseFleetRepository
    private lateinit var authPrefs: AuthPrefsStore

    private var activeUid: String? = null
    private var riderName: String = ""
    private var activeTripCount: Int = 0
    private var monitorJobs: List<Job> = emptyList()

    override fun onCreate() {
        super.onCreate()
        repo = FirebaseFleetRepository(applicationContext)
        authPrefs = AuthPrefsStore(applicationContext)
        FleetNotificationHelper.ensureChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_BY_USER -> {
                serviceScope.launch {
                    runCatching { authPrefs.setBackgroundAlertsEnabled(false) }
                }
                stopMonitoringAndSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopMonitoringAndSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                val uid = intent?.getStringExtra(EXTRA_UID).orEmpty()
                val name = intent?.getStringExtra(EXTRA_RIDER_NAME).orEmpty()
                if (name.isNotBlank()) riderName = name

                promoteToForeground()

                if (uid.isNotBlank() && uid != activeUid) {
                    activeUid = uid
                    startFirebaseMonitors(uid)
                } else if (uid.isBlank() && activeUid == null) {
                    stopMonitoringAndSelf()
                    return START_NOT_STICKY
                }
                return START_REDELIVER_INTENT
            }
            else -> return START_NOT_STICKY
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w("FleetBgService", "Foreground service dataSync time limit reached; stopping cleanly.")
        stopMonitoringAndSelf()
    }

    private fun promoteToForeground() {
        val notif = FleetNotificationHelper.buildDutyForegroundNotification(
            context = this,
            riderName = riderName,
            activeTripCount = activeTripCount
        )
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this,
                FleetNotificationHelper.NOTIF_ID_DUTY,
                notif,
                fgsType
            )
            _isRunning.value = true
        }.onFailure { err ->
            Log.e("FleetBgService", "startForeground failed: ${err.message}", err)
            stopSelf()
        }
    }

    private fun updateDutyNotification() {
        if (!_isRunning.value) return
        if (!FleetNotificationHelper.hasNotificationPermission(this)) return
        val notif = FleetNotificationHelper.buildDutyForegroundNotification(
            context = this,
            riderName = riderName,
            activeTripCount = activeTripCount
        )
        runCatching {
            NotificationManagerCompat.from(this).notify(FleetNotificationHelper.NOTIF_ID_DUTY, notif)
        }
    }

    private fun startFirebaseMonitors(uid: String) {
        monitorJobs.forEach { it.cancel() }
        monitorJobs = emptyList()

        // 1. Assigned Orders / New Trips & Restaurant Ready-for-Pickup Alerts
        var ordersInitialized = false
        val knownActiveOrderIds = mutableSetOf<String>()
        val knownReadyOrderIds = mutableSetOf<String>()
        val jOrders = serviceScope.launch {
            repo.warmUp()
            repo.observeAssignedOrders(uid).collect { orders ->
                val activeOrders = orders.values.filter { !SecurityUtils.isOrderCompleted(it) }
                val readyOrders = activeOrders.filter { SecurityUtils.isOrderReadyForPickup(it) }
                val newTripGroups = activeOrders
                    .groupBy { it.batchGroupId.ifBlank { "${it.customerUid}_${it.createdAt ?: it.orderId}" } }
                activeTripCount = newTripGroups.size
                updateDutyNotification()

                if (!ordersInitialized) {
                    ordersInitialized = true
                    knownActiveOrderIds.addAll(activeOrders.map { it.orderId })
                    knownReadyOrderIds.addAll(readyOrders.map { it.orderId })
                } else {
                    val newlyArrived = activeOrders.filter { it.orderId !in knownActiveOrderIds }
                    val newlyReady = readyOrders.filter { it.orderId !in knownReadyOrderIds && it.orderId in knownActiveOrderIds }

                    knownActiveOrderIds.clear()
                    knownActiveOrderIds.addAll(activeOrders.map { it.orderId })
                    knownReadyOrderIds.clear()
                    knownReadyOrderIds.addAll(readyOrders.map { it.orderId })

                    if (newlyArrived.isNotEmpty() && !FleetAppForegroundTracker.isInForeground) {
                        val first = newlyArrived.first()
                        val total = newlyArrived.sumOf { it.totalAmount }
                        val isPrepaid = first.paymentMode == "UPI" || first.paymentMode == "WALLET"
                        FleetNotificationHelper.notifyNewTrip(
                            context = this@FleetBackgroundService,
                            orderCount = newlyArrived.size,
                            address = first.address,
                            totalAmount = total,
                            isPrepaid = isPrepaid
                        )
                    } else if (newlyReady.isNotEmpty() && !FleetAppForegroundTracker.isInForeground) {
                        val firstReady = newlyReady.first()
                        val pickupCode = SecurityUtils.getDisplayablePickupCode(firstReady)
                        val codeHint = if (!pickupCode.isNullOrBlank()) " Pickup Code: $pickupCode." else ""
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Order Ready for Pickup!",
                            body = "Restaurant marked Order #${firstReady.orderId.takeLast(5)} READY FOR PICKUP.$codeHint Open app to pick up now.",
                            urgent = true,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ORDER_READY,
                            targetTab = FleetNotificationHelper.TAB_ACTIVE
                        )
                    }
                }
            }
        }

        // 2. Rider Profile (Approval, Suspension, Admin Personal Note, KYC Status)
        var profileInitialized = false
        var prevApproved = false
        var prevSuspended = false
        var prevAdminNote = ""
        var prevKycStatus = ""
        val jProfile = serviceScope.launch {
            repo.warmUp()
            repo.observeRiderProfile(uid).collect { profile ->
                if (profile.name.isNotBlank() && profile.name != riderName) {
                    riderName = profile.name
                    updateDutyNotification()
                }
                if (!profileInitialized) {
                    profileInitialized = true
                    prevApproved = profile.approved
                    prevSuspended = profile.suspended
                    prevAdminNote = profile.adminNote
                    prevKycStatus = profile.kyc.kycStatus
                } else {
                    if (!prevApproved && profile.approved) {
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Rider Account Approved!",
                            body = "Gossaigaon Bazar Admin verified your account. You can now accept live trips.",
                            urgent = true,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ACCOUNT_STATUS,
                            targetTab = FleetNotificationHelper.TAB_ACTIVE
                        )
                    }
                    if (!prevSuspended && profile.suspended) {
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Account Suspended by Admin",
                            body = "Your rider account was temporarily locked by Admin. Open Profile to contact Helpline.",
                            urgent = true,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ACCOUNT_STATUS,
                            targetTab = FleetNotificationHelper.TAB_PROFILE
                        )
                    }
                    if (profile.adminNote.isNotBlank() && profile.adminNote != prevAdminNote) {
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Direct Notice from Admin",
                            body = profile.adminNote,
                            urgent = true,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ADMIN_BROADCAST,
                            targetTab = FleetNotificationHelper.TAB_ACTIVE
                        )
                    }
                    val currentKyc = profile.kyc.kycStatus
                    if (currentKyc != prevKycStatus && currentKyc in setOf("VERIFIED", "REJECTED")) {
                        val isVerified = currentKyc == "VERIFIED"
                        val noteSuffix = profile.kyc.kycNote.takeIf { it.isNotBlank() }?.let { " Note: $it" }.orEmpty()
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = if (isVerified) "KYC Documents Verified!" else "KYC Review Required",
                            body = if (isVerified) {
                                "Your Driving License & ID verification was approved by Admin.$noteSuffix"
                            } else {
                                "Admin requested KYC updates. Open Profile to resubmit.$noteSuffix"
                            },
                            urgent = !isVerified,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ACCOUNT_STATUS + 2,
                            targetTab = FleetNotificationHelper.TAB_PROFILE
                        )
                    }
                    prevApproved = profile.approved
                    prevSuspended = profile.suspended
                    prevAdminNote = profile.adminNote
                    prevKycStatus = currentKyc
                }
            }
        }

        // 3. Global Admin Broadcasts, Agreement Updates, Per-KM Rate Changes & Maintenance Mode
        var settingsInitialized = false
        var prevAnnouncement = ""
        var prevAgreementHash = ""
        var prevPerKmRate = -1.0
        var prevMaintenance = false
        val jSettings = serviceScope.launch {
            repo.warmUp()
            repo.observeAdminSettings().collect { settings ->
                val currentHash = settings.computeAgreementDocHash()
                if (!settingsInitialized) {
                    settingsInitialized = true
                    prevAnnouncement = settings.riderAnnouncement
                    prevAgreementHash = currentHash
                    prevPerKmRate = settings.riderPerKmRate
                    prevMaintenance = settings.maintenanceMode
                } else {
                    if (settings.riderAnnouncement.isNotBlank() && settings.riderAnnouncement != prevAnnouncement) {
                        prevAnnouncement = settings.riderAnnouncement
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Fleet Broadcast from Admin",
                            body = settings.riderAnnouncement,
                            urgent = false,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ADMIN_BROADCAST,
                            targetTab = FleetNotificationHelper.TAB_ACTIVE
                        )
                    }
                    if (settings.requireAgreementAcceptance && currentHash != prevAgreementHash) {
                        prevAgreementHash = currentHash
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Updated Rider Agreement (v${settings.agreementVersion})",
                            body = "Admin updated the Rider Agreement or Privacy Policy. Tap to review and accept.",
                            urgent = false,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ADMIN_BROADCAST + 10,
                            targetTab = FleetNotificationHelper.TAB_ACTIVE
                        )
                    }
                    if (prevPerKmRate >= 0.0 && settings.riderPerKmRate != prevPerKmRate) {
                        prevPerKmRate = settings.riderPerKmRate
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Earning Rate Updated by Admin",
                            body = "New active distance rate: ${RiderEarningsCalculator.formatRateBadge(settings)}",
                            urgent = false,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ADMIN_BROADCAST + 11,
                            targetTab = FleetNotificationHelper.TAB_EARNINGS
                        )
                    }
                    if (!prevMaintenance && settings.maintenanceMode) {
                        FleetNotificationHelper.notifyAdminUpdate(
                            context = this@FleetBackgroundService,
                            title = "Fleet Terminal Maintenance Active",
                            body = settings.maintenanceMessage.ifBlank {
                                "Admin has enabled scheduled maintenance mode."
                            },
                            urgent = true,
                            notificationId = FleetNotificationHelper.NOTIF_ID_ADMIN_BROADCAST + 12,
                            targetTab = FleetNotificationHelper.TAB_ACTIVE
                        )
                    }
                    prevMaintenance = settings.maintenanceMode
                }
            }
        }

        // 4. Payout Status Updates
        var payoutsInitialized = false
        val payoutStatuses = mutableMapOf<String, String>()
        val jPayouts = serviceScope.launch {
            repo.observeRiderPayouts(uid).collect { payouts ->
                if (!payoutsInitialized) {
                    payoutsInitialized = true
                    payouts.forEach { payoutStatuses[it.id] = it.status }
                } else {
                    for (p in payouts) {
                        val old = payoutStatuses[p.id]
                        if (old != null && old != p.status && p.status in setOf("PAID", "REJECTED")) {
                            val title = if (p.status == "PAID") "Payout Sent: Rs.${p.amount.toInt()}" else "Payout Request Rejected"
                            val body = if (p.status == "PAID") {
                                "Admin marked your Rs.${p.amount.toInt()} payout as PAID${if (p.adminRef.isNotBlank()) " (Ref: ${p.adminRef})" else ""}."
                            } else {
                                "Your Rs.${p.amount.toInt()} payout request was rejected by Admin."
                            }
                            FleetNotificationHelper.notifyAdminUpdate(
                                context = this@FleetBackgroundService,
                                title = title,
                                body = body,
                                urgent = false,
                                notificationId = FleetNotificationHelper.NOTIF_ID_PAYOUT_UPDATE,
                                targetTab = FleetNotificationHelper.TAB_EARNINGS
                            )
                        }
                        payoutStatuses[p.id] = p.status
                    }
                }
            }
        }

        // 5. COD Settlement Approvals / Rejections
        var settlementsInitialized = false
        val settlementStatuses = mutableMapOf<String, String>()
        val jSettlements = serviceScope.launch {
            repo.observeAllSettlements().collect { all ->
                val mine = all.filter { it.riderUid == uid }
                if (!settlementsInitialized) {
                    settlementsInitialized = true
                    mine.forEach { settlementStatuses[it.id] = it.status }
                } else {
                    for (s in mine) {
                        val old = settlementStatuses[s.id]
                        if (old != null && old != s.status && s.status in setOf("APPROVED", "REJECTED")) {
                            val title = if (s.status == "APPROVED") "COD Deposit Approved" else "COD Deposit Rejected"
                            val body = "Admin marked your Rs.${s.amount.toInt()} deposit (UTR: ${s.utr}) as ${s.status}."
                            FleetNotificationHelper.notifyAdminUpdate(
                                context = this@FleetBackgroundService,
                                title = title,
                                body = body,
                                urgent = false,
                                notificationId = FleetNotificationHelper.NOTIF_ID_PAYOUT_UPDATE + 1,
                                targetTab = FleetNotificationHelper.TAB_EARNINGS
                            )
                        }
                        settlementStatuses[s.id] = s.status
                    }
                }
            }
        }

        // 6. Admin Feedback / Support Ticket Replies
        var feedbackInitialized = false
        val feedbackReplies = mutableMapOf<String, String>()
        val jFeedback = serviceScope.launch {
            repo.observeFeedbacks(uid).collect { list ->
                if (!feedbackInitialized) {
                    feedbackInitialized = true
                    list.forEach { feedbackReplies[it.id] = it.adminReply }
                } else {
                    for (fb in list) {
                        val oldReply = feedbackReplies[fb.id].orEmpty()
                        if (fb.adminReply.isNotBlank() && fb.adminReply != oldReply) {
                            FleetNotificationHelper.notifyAdminUpdate(
                                context = this@FleetBackgroundService,
                                title = "Admin Replied: ${fb.subject}",
                                body = fb.adminReply,
                                urgent = false,
                                notificationId = FleetNotificationHelper.NOTIF_ID_FEEDBACK_REPLY,
                                targetTab = FleetNotificationHelper.TAB_PROFILE
                            )
                        }
                        feedbackReplies[fb.id] = fb.adminReply
                    }
                }
            }
        }

        monitorJobs = listOf(jOrders, jProfile, jSettings, jPayouts, jSettlements, jFeedback)
    }

    private fun stopMonitoringAndSelf() {
        monitorJobs.forEach { it.cancel() }
        monitorJobs = emptyList()
        activeUid = null
        _isRunning.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        monitorJobs.forEach { it.cancel() }
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
