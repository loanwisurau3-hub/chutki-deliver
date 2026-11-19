package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.FleetNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chutki Rider", appName)
  }

  @Test
  fun `notification channels and foreground notifications initialize cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FleetNotificationHelper.ensureChannels(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertNotNull(nm.getNotificationChannel(FleetNotificationHelper.CHANNEL_TRIP_DISPATCH))
    assertNotNull(nm.getNotificationChannel(FleetNotificationHelper.CHANNEL_ADMIN_UPDATES))
    assertNotNull(nm.getNotificationChannel(FleetNotificationHelper.CHANNEL_DUTY_SERVICE))
    assertNotNull(nm.getNotificationChannel(FleetNotificationHelper.CHANNEL_LOCATION_TRACKING))

    val dutyNotif = FleetNotificationHelper.buildDutyForegroundNotification(context, "Bipin", 2)
    assertNotNull(dutyNotif)
    assertEquals(FleetNotificationHelper.CHANNEL_DUTY_SERVICE, dutyNotif.channelId)

    val locNotif = FleetNotificationHelper.buildLocationForegroundNotification(context, 26.4472, 89.9693, 8)
    assertNotNull(locNotif)
    assertEquals(FleetNotificationHelper.CHANNEL_LOCATION_TRACKING, locNotif.channelId)
    assertEquals(10_000L, com.example.location.RiderLocationForegroundService.UPDATE_INTERVAL_MS)
  }
}
