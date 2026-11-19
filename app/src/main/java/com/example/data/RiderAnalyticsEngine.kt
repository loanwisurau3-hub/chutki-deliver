package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

data class DailyEarningPoint(
    val dayStartMs: Long,
    val dayLabel: String,
    val earnings: Double,
    val deliveries: Int,
    val distanceKm: Double
)

data class PeriodAnalyticsSummary(
    val totalDistanceKm: Double,
    val avgEarningPerKm: Double,
    val avgEarningPerDelivery: Double,
    val completionRatePercent: Int,
    val deliveredCount: Int,
    val totalCompletedCount: Int,
    val estimatedBonusEarned: Double
)

object RiderAnalyticsEngine {

    private const val DAY_MS = 24L * 3600L * 1000L
    private const val DEFAULT_TOWN_SPEED_KMH = 22.0

    fun estimateEtaMinutes(distanceKm: Double?): Int? {
        if (distanceKm == null || !distanceKm.isFinite() || distanceKm <= 0.0) return null
        val travelMins = (distanceKm / DEFAULT_TOWN_SPEED_KMH) * 60.0
        return ceil(travelMins + 3.0).toInt().coerceIn(4, 180)
    }

    fun computePeriodAnalytics(
        completedTrips: List<TripGroup>,
        stores: Map<String, StoreInfo>,
        settings: AdminSettings,
        cutoffMs: Long? = null
    ): PeriodAnalyticsSummary {
        val scopedTrips = if (cutoffMs == null) {
            completedTrips
        } else {
            completedTrips.filter { trip ->
                val ts = trip.orders.maxOfOrNull {
                    it.deliveredAt ?: it.updatedAt ?: it.assignedAt ?: it.createdAt ?: 0L
                } ?: trip.assignedAt
                ts >= cutoffMs
            }
        }

        var totalDistance = 0.0
        var totalEarned = 0.0
        var deliveredTrips = 0

        for (trip in scopedTrips) {
            val isDelivered = trip.orders.any { it.status == "DELIVERED" }
            if (isDelivered) {
                deliveredTrips++
                val summary = RiderEarningsCalculator.calculateTripEarning(
                    trip = trip,
                    stores = stores,
                    settings = settings,
                    deliveredOnly = true
                )
                totalEarned += summary.totalEarning
                totalDistance += summary.totalDistanceKm ?: 0.0
            }
        }

        val totalCount = scopedTrips.size
        val completionPct = if (totalCount > 0) {
            ((deliveredTrips.toDouble() / totalCount.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            100
        }
        val avgPerKm = if (totalDistance > 0.0) RiderEarningsCalculator.round2(totalEarned / totalDistance) else 0.0
        val avgPerDrop = if (deliveredTrips > 0) RiderEarningsCalculator.round2(totalEarned / deliveredTrips) else 0.0
        val bonus = if (
            settings.dailyTripTarget > 0 &&
            settings.dailyBonusAmount > 0.0 &&
            deliveredTrips >= settings.dailyTripTarget
        ) {
            settings.dailyBonusAmount
        } else {
            0.0
        }

        return PeriodAnalyticsSummary(
            totalDistanceKm = RiderEarningsCalculator.round2(totalDistance),
            avgEarningPerKm = avgPerKm,
            avgEarningPerDelivery = avgPerDrop,
            completionRatePercent = completionPct,
            deliveredCount = deliveredTrips,
            totalCompletedCount = totalCount,
            estimatedBonusEarned = bonus
        )
    }

    fun computeLast7DaysSeries(
        completedTrips: List<TripGroup>,
        stores: Map<String, StoreInfo>,
        settings: AdminSettings,
        nowMs: Long = System.currentTimeMillis()
    ): List<DailyEarningPoint> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val labelFmt = SimpleDateFormat("EEE", Locale.getDefault())

        val buckets = (6 downTo 0).map { offset ->
            val start = todayStart - offset * DAY_MS
            start to MutableBucket(
                dayStartMs = start,
                dayLabel = if (offset == 0) "Today" else labelFmt.format(Date(start)).take(3)
            )
        }.toMap(LinkedHashMap())

        val earliest = todayStart - 6L * DAY_MS

        for (trip in completedTrips) {
            val isDelivered = trip.orders.any { it.status == "DELIVERED" }
            if (!isDelivered) continue
            val ts = trip.orders.maxOfOrNull {
                it.deliveredAt ?: it.updatedAt ?: it.assignedAt ?: it.createdAt ?: 0L
            } ?: trip.assignedAt
            if (ts < earliest) continue

            val bucketCal = Calendar.getInstance().apply {
                timeInMillis = ts
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val bucket = buckets[bucketCal.timeInMillis] ?: continue
            val summary = RiderEarningsCalculator.calculateTripEarning(
                trip = trip,
                stores = stores,
                settings = settings,
                deliveredOnly = true
            )
            bucket.earnings += summary.totalEarning
            bucket.deliveries += 1
            bucket.distanceKm += summary.totalDistanceKm ?: 0.0
        }

        return buckets.values.map {
            DailyEarningPoint(
                dayStartMs = it.dayStartMs,
                dayLabel = it.dayLabel,
                earnings = RiderEarningsCalculator.round2(it.earnings),
                deliveries = it.deliveries,
                distanceKm = RiderEarningsCalculator.round2(it.distanceKm)
            )
        }
    }

    private data class MutableBucket(
        val dayStartMs: Long,
        val dayLabel: String,
        var earnings: Double = 0.0,
        var deliveries: Int = 0,
        var distanceKm: Double = 0.0
    )
}
