package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.RiderAnalyticsEngine
import com.example.data.RiderEarningsCalculator
import com.example.data.StoreInfo
import com.example.data.TripGroup
import com.example.ui.components.DailySummaryMetricRow
import com.example.ui.components.EarningsAnalyticsCard
import com.example.ui.components.ShiftDashboardCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private enum class AnalyticsPeriod(val label: String) {
    TODAY("Today"),
    WEEK("7 Days"),
    ALL("All Time")
}

@Composable
fun ShiftDashboardScreen(
    adminSettings: AdminSettings,
    totalRiderEarned: Double,
    todayTripsCount: Int,
    todayEarnings: Double,
    todayCodCollected: Double,
    isOnline: Boolean,
    shiftElapsedSeconds: Long,
    weekTripsCount: Int,
    weekEarnings: Double,
    completedTrips: List<TripGroup>,
    stores: Map<String, StoreInfo> = emptyMap()
) {
    var selectedPeriod by rememberSaveable { mutableStateOf(AnalyticsPeriod.TODAY) }

    val periodTrips = when (selectedPeriod) {
        AnalyticsPeriod.TODAY -> todayTripsCount
        AnalyticsPeriod.WEEK -> weekTripsCount
        AnalyticsPeriod.ALL -> completedTrips.count { t -> t.orders.any { it.status == "DELIVERED" } }
    }
    val periodEarned = when (selectedPeriod) {
        AnalyticsPeriod.TODAY -> todayEarnings
        AnalyticsPeriod.WEEK -> weekEarnings
        AnalyticsPeriod.ALL -> totalRiderEarned
    }
    val periodCutoffMs = remember(selectedPeriod) {
        if (selectedPeriod == AnalyticsPeriod.ALL) {
            null
        } else {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            if (selectedPeriod == AnalyticsPeriod.TODAY) cal.timeInMillis
            else cal.timeInMillis - 6L * 24L * 3600L * 1000L
        }
    }
    val periodAnalytics = remember(completedTrips, stores, adminSettings, periodCutoffMs) {
        RiderAnalyticsEngine.computePeriodAnalytics(completedTrips, stores, adminSettings, periodCutoffMs)
    }
    val weeklySeries = remember(completedTrips, stores, adminSettings) {
        RiderAnalyticsEngine.computeLast7DaysSeries(completedTrips, stores, adminSettings)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shift_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = SkyInfo,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_dashboard),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsPeriod.entries.forEach { period ->
                    val selected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) EmeraldPrimary else TerminalSurface)
                            .border(
                                1.dp,
                                if (selected) EmeraldPrimary else TerminalBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 10.dp)
                            .testTag("dashboard_filter_${period.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else TextMuted
                        )
                    }
                }
            }
        }

        item {
            if (selectedPeriod == AnalyticsPeriod.TODAY) {
                ShiftDashboardCard(
                    todayTripsCount = todayTripsCount,
                    todayEarnings = todayEarnings,
                    todayCodCollected = todayCodCollected,
                    isOnline = isOnline,
                    shiftElapsedSeconds = shiftElapsedSeconds,
                    adminSettings = adminSettings
                )
            } else {
                DailySummaryMetricRow(
                    earningsLabel = "${selectedPeriod.label} Earnings",
                    earningsValue = RiderEarningsCalculator.formatRupees(periodEarned),
                    earningsSubtext = "Rate: ${RiderEarningsCalculator.formatRateBadge(adminSettings)}",
                    deliveriesLabel = "${selectedPeriod.label} Deliveries",
                    deliveriesCount = periodTrips,
                    deliveriesSubtext = if (periodTrips > 0) {
                        "Avg ${RiderEarningsCalculator.formatRupees(periodEarned / periodTrips)} / drop"
                    } else {
                        "No completed drops yet"
                    }
                )
            }
        }

        item {
            EarningsAnalyticsCard(
                periodLabel = selectedPeriod.label,
                analytics = periodAnalytics,
                weeklySeries = weeklySeries
            )
        }
    }
}
