package com.example

import com.example.data.AdminSettings
import com.example.data.OrderRecord
import com.example.data.RiderAnalyticsEngine
import com.example.data.TripGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class RiderAnalyticsEngineTest {

    private fun makeTrip(
        id: String,
        status: String,
        distanceKm: Double,
        deliveredAt: Long
    ): TripGroup {
        return TripGroup(
            tripId = id,
            customerUid = "cust_$id",
            phone = "9876543210",
            address = "Gossaigaon Town",
            customerLat = null,
            customerLng = null,
            locationLink = "",
            paymentMode = "COD",
            paymentStatus = "COD",
            emergencyAlert = false,
            emergencyDetails = "",
            assignedAt = deliveredAt - 600_000L,
            orders = listOf(
                OrderRecord(
                    orderId = "ord_$id",
                    status = status,
                    distanceKm = distanceKm,
                    deliveredAt = deliveredAt
                )
            )
        )
    }

    @Test
    fun `estimateEtaMinutes returns null for invalid distances and realistic minutes for valid trips`() {
        assertNull(RiderAnalyticsEngine.estimateEtaMinutes(null))
        assertNull(RiderAnalyticsEngine.estimateEtaMinutes(0.0))
        assertNull(RiderAnalyticsEngine.estimateEtaMinutes(-2.5))
        assertNull(RiderAnalyticsEngine.estimateEtaMinutes(Double.NaN))

        // Short 0.2 km hop -> minimum clamped to 4 min
        assertEquals(4, RiderAnalyticsEngine.estimateEtaMinutes(0.2))
        // 2.2 km at 22 km/h = 6 min + 3 min buffer = 9 min
        assertEquals(9, RiderAnalyticsEngine.estimateEtaMinutes(2.2))
        // 11.0 km at 22 km/h = 30 min + 3 min buffer = 33 min
        assertEquals(33, RiderAnalyticsEngine.estimateEtaMinutes(11.0))
    }

    @Test
    fun `computePeriodAnalytics aggregates distance, averages, completion rate, and bonus`() {
        val now = System.currentTimeMillis()
        val settings = AdminSettings(
            riderPerKmRate = 10.0,
            dailyTripTarget = 2,
            dailyBonusAmount = 50.0
        )

        val trips = listOf(
            makeTrip("t1", "DELIVERED", 3.0, now - 3_600_000L), // ₹30
            makeTrip("t2", "DELIVERED", 5.0, now - 1_800_000L), // ₹50
            makeTrip("t3", "CANCELLED", 4.0, now - 900_000L)    // ignored in earnings/distance
        )

        val summary = RiderAnalyticsEngine.computePeriodAnalytics(
            completedTrips = trips,
            stores = emptyMap(),
            settings = settings
        )

        assertEquals(8.0, summary.totalDistanceKm, 0.01)
        assertEquals(10.0, summary.avgEarningPerKm, 0.01)
        assertEquals(40.0, summary.avgEarningPerDelivery, 0.01)
        assertEquals(2, summary.deliveredCount)
        assertEquals(3, summary.totalCompletedCount)
        assertEquals(67, summary.completionRatePercent)
        assertEquals(50.0, summary.estimatedBonusEarned, 0.01)
    }

    @Test
    fun `computeLast7DaysSeries buckets delivered trips into 7 days ending with Today`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 18)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = cal.timeInMillis
        val dayMs = 24L * 3600L * 1000L
        val settings = AdminSettings(riderPerKmRate = 12.0)

        val trips = listOf(
            makeTrip("today1", "DELIVERED", 2.0, nowMs - 1_000_000L),          // ₹24 today
            makeTrip("today2", "DELIVERED", 3.0, nowMs - 500_000L),            // ₹36 today
            makeTrip("todayCancel", "CANCELLED", 5.0, nowMs - 200_000L),       // ignored
            makeTrip("yest1", "DELIVERED", 4.0, nowMs - dayMs),                // ₹48 yesterday
            makeTrip("old", "DELIVERED", 10.0, nowMs - 10L * dayMs)            // outside 7d window
        )

        val series = RiderAnalyticsEngine.computeLast7DaysSeries(
            completedTrips = trips,
            stores = emptyMap(),
            settings = settings,
            nowMs = nowMs
        )

        assertEquals(7, series.size)
        assertEquals("Today", series.last().dayLabel)
        assertEquals(60.0, series.last().earnings, 0.01)
        assertEquals(2, series.last().deliveries)
        assertEquals(5.0, series.last().distanceKm, 0.01)

        val yesterdayPoint = series[5]
        assertEquals(48.0, yesterdayPoint.earnings, 0.01)
        assertEquals(1, yesterdayPoint.deliveries)
        assertEquals(4.0, yesterdayPoint.distanceKm, 0.01)
    }
}
