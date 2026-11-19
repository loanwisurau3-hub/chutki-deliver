package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object FleetAppForegroundTracker {
    private val foregroundFlag = AtomicBoolean(false)
    val isInForeground: Boolean
        get() = foregroundFlag.get()

    fun setForeground(active: Boolean) {
        foregroundFlag.set(active)
    }
}

object FleetNotificationHelper {
    const val CHANNEL_TRIP_DISPATCH = "fleet_trip_dispatch"
    const val CHANNEL_ADMIN_UPDATES = "fleet_admin_updates"
    const val CHANNEL_DUTY_SERVICE = "fleet_duty_service"
    const val CHANNEL_LOCATION_TRACKING = "fleet_location_tracking"

    const val NOTIF_ID_DUTY = 1001
    const val NOTIF_ID_ADMIN_BROADCAST = 1002
    const val NOTIF_ID_ACCOUNT_STATUS = 1003
    const val NOTIF_ID_PAYOUT_UPDATE = 1004
    const val NOTIF_ID_FEEDBACK_REPLY = 1005
    const val NOTIF_ID_LOCATION = 1006
    const val NOTIF_ID_ORDER_READY = 1007
    const val NOTIF_ID_TEST = 1009

    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val TAB_ACTIVE = "ACTIVE"
    const val TAB_EARNINGS = "EARNINGS"
    const val TAB_PROFILE = "PROFILE"

    private val tripNotifCounter = AtomicInteger(2000)

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val tripChannel = NotificationChannel(
            CHANNEL_TRIP_DISPATCH,
            context.getString(R.string.notif_channel_trips_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_trips_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 280L, 140L, 280L)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(alarmSound, audioAttrs)
        }

        val adminChannel = NotificationChannel(
            CHANNEL_ADMIN_UPDATES,
            context.getString(R.string.notif_channel_admin_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_admin_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val dutyChannel = NotificationChannel(
            CHANNEL_DUTY_SERVICE,
            context.getString(R.string.notif_channel_duty_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_duty_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        val locationChannel = NotificationChannel(
            CHANNEL_LOCATION_TRACKING,
            context.getString(R.string.notif_channel_location_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_location_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannels(listOf(tripChannel, adminChannel, dutyChannel, locationChannel))
    }

    private fun openAppPendingIntent(
        context: Context,
        targetTab: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_TAB, targetTab)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildDutyForegroundNotification(
        context: Context,
        riderName: String,
        activeTripCount: Int
    ): Notification {
        ensureChannels(context)
        val stopIntent = Intent(context, FleetBackgroundService::class.java).apply {
            action = FleetBackgroundService.ACTION_PAUSE_BY_USER
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            9001,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = when {
            activeTripCount > 0 ->
                "$activeTripCount active trip${if (activeTripCount > 1) "s" else ""} in progress • Live dispatch alerts active"
            riderName.isNotBlank() ->
                "$riderName is Online • Monitoring Admin dispatches in background"
            else ->
                "Online • Monitoring live grocery dispatches & Admin alerts"
        }

        return NotificationCompat.Builder(context, CHANNEL_DUTY_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_fleet)
            .setContentTitle(context.getString(R.string.notif_duty_title))
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppPendingIntent(context, TAB_ACTIVE, 9000))
            .addAction(
                R.drawable.ic_stat_fleet,
                context.getString(R.string.notif_duty_action_stop),
                stopPendingIntent
            )
            .build()
    }

    fun buildLocationForegroundNotification(
        context: Context,
        lat: Double?,
        lng: Double?,
        accuracyMeters: Int?
    ): Notification {
        ensureChannels(context)
        val subtitle = if (lat != null && lng != null) {
            val acc = accuracyMeters?.let { " • ±${it}m" }.orEmpty()
            String.format(java.util.Locale.US, "Syncing every 10s: %.5f, %.5f%s", lat, lng, acc)
        } else {
            "Acquiring GPS fix • Updating Firebase every 10s"
        }

        return NotificationCompat.Builder(context, CHANNEL_LOCATION_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_fleet)
            .setContentTitle(context.getString(R.string.notif_location_title))
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppPendingIntent(context, TAB_ACTIVE, 9050))
            .build()
    }

    fun notifyNewTrip(
        context: Context,
        orderCount: Int,
        address: String,
        totalAmount: Double,
        isPrepaid: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val paymentLabel = if (isPrepaid) "PREPAID" else "COD Rs.${totalAmount.toInt()}"
        val title = if (orderCount > 1) {
            "New Multi-Store Trip ($orderCount orders)"
        } else {
            "New Grocery Trip Assigned!"
        }
        val body = "${address.ifBlank { "Gossaigaon" }} • $paymentLabel — Tap to accept before timer expires."

        val notif = NotificationCompat.Builder(context, CHANNEL_TRIP_DISPATCH)
            .setSmallIcon(R.drawable.ic_stat_fleet)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openAppPendingIntent(context, TAB_ACTIVE, 9100))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(tripNotifCounter.incrementAndGet(), notif)
        }
    }

    fun notifyAdminUpdate(
        context: Context,
        title: String,
        body: String,
        urgent: Boolean,
        notificationId: Int,
        targetTab: String = TAB_ACTIVE
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val channel = if (urgent) CHANNEL_TRIP_DISPATCH else CHANNEL_ADMIN_UPDATES
        val priority = if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_fleet)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent(context, targetTab, notificationId))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notif)
        }
    }

    fun notifyTestAlert(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val body = "Background dispatch alerts are working! You will receive new trips & Admin notices even when the app is minimized."
        val notif = NotificationCompat.Builder(context, CHANNEL_TRIP_DISPATCH)
            .setSmallIcon(R.drawable.ic_stat_fleet)
            .setContentTitle("Chutki Rider — Alerts Active")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openAppPendingIntent(context, TAB_ACTIVE, NOTIF_ID_TEST))
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_TEST, notif)
            true
        }.getOrDefault(false)
    }
}
