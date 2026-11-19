package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyEarningPoint
import com.example.data.PeriodAnalyticsSummary
import com.example.data.RiderEarningsCalculator
import com.example.ui.theme.AmberLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun EarningsAnalyticsCard(
    periodLabel: String,
    analytics: PeriodAnalyticsSummary,
    weeklySeries: List<DailyEarningPoint>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outline, MaterialTheme.shapes.large)
            .testTag("earnings_analytics_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = SkyInfo,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Performance & 7-Day Trend",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Text(
                    text = periodLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyInfo
                )
            }

            // 3-Column Performance Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMiniTile(
                    icon = Icons.Default.Route,
                    label = "DISTANCE",
                    value = RiderEarningsCalculator.formatKm(analytics.totalDistanceKm),
                    accent = SkyInfo,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMiniTile(
                    icon = Icons.Default.Speed,
                    label = "AVG / KM",
                    value = if (analytics.avgEarningPerKm > 0.0) {
                        RiderEarningsCalculator.formatRupees(analytics.avgEarningPerKm)
                    } else "—",
                    accent = EmeraldBright,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMiniTile(
                    icon = Icons.Default.CheckCircle,
                    label = "SUCCESS",
                    value = "${analytics.completionRatePercent}%",
                    accent = if (analytics.completionRatePercent >= 90) EmeraldBright else AmberLight,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = TerminalBorder.copy(alpha = 0.6f))

            // 7-Day Interactive Smooth Line Chart
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LAST 7 DAYS EARNINGS TREND",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                DailyEarningsLineChart(
                    weeklySeries = weeklySeries
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMiniTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalBg.copy(alpha = 0.7f))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                maxLines = 1
            )
        }
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
