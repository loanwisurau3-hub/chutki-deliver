package com.example.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

object RiderEarningsCalculator {

    fun round2(value: Double): Double = round(value * 100.0) / 100.0

    fun resolveOrderDistanceKm(
        order: OrderRecord,
        store: StoreInfo?,
        settings: AdminSettings
    ): Double? {
        val explicit = order.distanceKm
        if (explicit != null && explicit.isFinite() && explicit > 0.0) {
            return round2(explicit)
        }
        val rawDist = SecurityUtils.calculateDistanceKm(
            store?.lat,
            store?.lng,
            order.customerLat,
            order.customerLng
        ) ?: return null
        if (!rawDist.isFinite() || rawDist <= 0.0) return null
        val multiplier = settings.distanceRoadMultiplier.coerceIn(1.0, 2.5)
        return round2(rawDist * multiplier)
    }

    fun calculateOrderEarning(
        order: OrderRecord,
        store: StoreInfo?,
        settings: AdminSettings
    ): OrderEarningBreakdown {
        val distanceKm = resolveOrderDistanceKm(order, store, settings)

        // Preserve locked earning on already-delivered orders (or explicit Admin override)
        val lockedEarning = order.riderEarning
        if (lockedEarning != null && lockedEarning.isFinite() && lockedEarning > 0.0) {
            val rate = order.riderPerKmRate?.takeIf { it.isFinite() && it >= 0.0 } ?: settings.riderPerKmRate
            return OrderEarningBreakdown(
                distanceKm = distanceKm,
                perKmRate = rate,
                baseFee = settings.riderDistanceBasePay,
                totalEarning = round2(lockedEarning),
                isDistanceBased = distanceKm != null && rate > 0.0,
                isLocked = true
            )
        }

        // Dynamic distance-based calculation controlled by Admin's per-km rate
        if (settings.distanceEarningsEnabled && settings.riderPerKmRate > 0.0 && distanceKm != null) {
            val billableKm = (distanceKm - settings.riderBaseDistanceKm.coerceAtLeast(0.0)).coerceAtLeast(0.0)
            val basePay = settings.riderDistanceBasePay.coerceAtLeast(0.0)
            val raw = basePay + (billableKm * settings.riderPerKmRate)
            val minFloor = settings.riderMinTripEarning.coerceAtLeast(0.0)
            val finalEarning = round2(max(raw, minFloor))
            return OrderEarningBreakdown(
                distanceKm = distanceKm,
                perKmRate = settings.riderPerKmRate,
                baseFee = basePay,
                totalEarning = finalEarning,
                isDistanceBased = true,
                isLocked = false
            )
        }

        // Fallback when GPS coordinates / distance are unavailable or per-km mode is disabled
        val fallback = order.deliveryCharge?.takeIf { it.isFinite() && it > 0.0 } ?: settings.riderBaseFee
        return OrderEarningBreakdown(
            distanceKm = distanceKm,
            perKmRate = settings.riderPerKmRate,
            baseFee = fallback,
            totalEarning = round2(fallback),
            isDistanceBased = false,
            isLocked = false
        )
    }

    fun calculateTripEarning(
        trip: TripGroup,
        stores: Map<String, StoreInfo>,
        settings: AdminSettings,
        deliveredOnly: Boolean = false
    ): TripEarningSummary {
        val targetOrders = if (deliveredOnly) {
            trip.orders.filter { it.status == "DELIVERED" }
        } else {
            trip.orders
        }
        if (targetOrders.isEmpty()) {
            return TripEarningSummary(
                totalEarning = 0.0,
                totalDistanceKm = null,
                effectivePerKmRate = settings.riderPerKmRate,
                isDistanceBased = false,
                isLocked = false
            )
        }

        val breakdowns = targetOrders.map { order ->
            calculateOrderEarning(order, stores[order.restaurantId], settings)
        }
        val totalEarning = round2(breakdowns.sumOf { it.totalEarning })
        val distances = breakdowns.mapNotNull { it.distanceKm }
        val totalDistance = if (distances.isNotEmpty()) round2(distances.sum()) else null
        val isDistanceBased = breakdowns.any { it.isDistanceBased }
        val isLocked = breakdowns.all { it.isLocked }
        val effectiveRate = breakdowns.firstOrNull { it.perKmRate > 0.0 }?.perKmRate ?: settings.riderPerKmRate

        return TripEarningSummary(
            totalEarning = totalEarning,
            totalDistanceKm = totalDistance,
            effectivePerKmRate = effectiveRate,
            isDistanceBased = isDistanceBased,
            isLocked = isLocked
        )
    }

    fun formatRupees(amount: Double): String {
        val rounded = round2(amount)
        return if (abs(rounded - round(rounded)) < 0.005) {
            String.format(Locale.US, "₹%.0f", rounded)
        } else {
            String.format(Locale.US, "₹%.2f", rounded)
        }
    }

    fun formatKm(distanceKm: Double): String =
        String.format(Locale.US, "%.1f km", distanceKm)

    fun formatRateBadge(settings: AdminSettings): String {
        if (!settings.distanceEarningsEnabled || settings.riderPerKmRate <= 0.0) {
            return "${formatRupees(settings.riderBaseFee)} flat/order"
        }
        val rateText = "${formatRupees(settings.riderPerKmRate)}/km"
        val basePay = settings.riderDistanceBasePay
        val minFloor = settings.riderMinTripEarning
        return when {
            basePay > 0.0 && settings.riderBaseDistanceKm > 0.0 ->
                "${formatRupees(basePay)} (${formatKm(settings.riderBaseDistanceKm)}) + $rateText"
            basePay > 0.0 ->
                "${formatRupees(basePay)} base + $rateText"
            minFloor > 0.0 ->
                "$rateText (min ${formatRupees(minFloor)})"
            else -> rateText
        }
    }

    fun formatTripBreakdown(summary: TripEarningSummary, settings: AdminSettings): String {
        val dist = summary.totalDistanceKm
        return if (summary.isDistanceBased && dist != null) {
            val rateStr = "${formatRupees(summary.effectivePerKmRate)}/km"
            if (!summary.isLocked && settings.riderDistanceBasePay > 0.0) {
                "${formatKm(dist)} • ${formatRupees(settings.riderDistanceBasePay)} + $rateStr"
            } else {
                "${formatKm(dist)} × $rateStr"
            }
        } else if (dist != null) {
            "${formatKm(dist)} • Base fee"
        } else {
            "Base fee (no GPS pin)"
        }
    }
}
