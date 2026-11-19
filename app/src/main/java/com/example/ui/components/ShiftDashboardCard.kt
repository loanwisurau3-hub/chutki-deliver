package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.RiderEarningsCalculator
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftDashboardCard(
    todayTripsCount: Int,
    todayEarnings: Double,
    todayCodCollected: Double = 0.0,
    isOnline: Boolean,
    shiftElapsedSeconds: Long,
    adminSettings: AdminSettings,
    onOpenEarnings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shiftLabel = if (!isOnline) {
        "Off Duty"
    } else {
        val h = shiftElapsedSeconds / 3600L
        val m = (shiftElapsedSeconds % 3600L) / 60L
        if (h > 0L) "On Duty • ${h}h ${m}m" else "On Duty • ${m}m"
    }
    val todayDateLabel = remember {
        SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
    }

    val target = adminSettings.dailyTripTarget
    val bonus = adminSettings.dailyBonusAmount
    val showBonus = target > 0 && bonus > 0.0
    val rawProgress = if (target > 0) (todayTripsCount.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "daily_target_progress"
    )
    val unlocked = target > 0 && todayTripsCount >= target
    val rateBadge = RiderEarningsCalculator.formatRateBadge(adminSettings)
    val avgPerDeliveryText = if (todayTripsCount > 0) {
        "Avg ${RiderEarningsCalculator.formatRupees(todayEarnings / todayTripsCount)} / drop"
    } else {
        stringResource(R.string.summary_ready_for_first_drop)
    }

    val colorScheme = MaterialTheme.colorScheme

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (unlocked) colorScheme.primary.copy(alpha = 0.7f) else colorScheme.outline,
                shape = MaterialTheme.shapes.large
            )
            .testTag("shift_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Today's Summary + Date + Live Duty Timer Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = EmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.summary_dashboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = todayDateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnline) {
                        colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        colorScheme.background.copy(alpha = 0.7f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isOnline) colorScheme.primary.copy(alpha = 0.55f) else colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isOnline) EmeraldBright else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = shiftLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) EmeraldBright else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Primary 2-Card Material 3 Summary Grid: Today's Earnings & Completed Deliveries
            DailySummaryMetricRow(
                earningsLabel = stringResource(R.string.summary_today_earnings_label),
                earningsValue = RiderEarningsCalculator.formatRupees(todayEarnings),
                earningsSubtext = if (todayCodCollected > 0.0) {
                    "${stringResource(R.string.summary_today_cod_label)}: ${RiderEarningsCalculator.formatRupees(todayCodCollected)}"
                } else {
                    "Rate: $rateBadge"
                },
                deliveriesLabel = stringResource(R.string.summary_completed_deliveries_label),
                deliveriesCount = todayTripsCount,
                deliveriesSubtext = avgPerDeliveryText,
                onCardClick = onOpenEarnings
            )

            // Live Admin Per-KM Earning Rate Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.background.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.30f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_per_km_rate_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = EmeraldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.summary_distance_rate_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurface
                        )
                    }
                    Text(
                        text = rateBadge,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldBright
                    )
                }
            }

            // Daily Bonus Target Progress
            if (showBonus) {
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.5f))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (unlocked) EmeraldBright else AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (unlocked) {
                                    "Daily Bonus Unlocked (+₹${bonus.toInt()})"
                                } else {
                                    "Daily Goal: $todayTripsCount / $target deliveries"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (unlocked) EmeraldBright else colorScheme.onSurface
                            )
                        }
                        if (!unlocked) {
                            Text(
                                text = "+₹${bonus.toInt()} Bonus",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberLight,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = if (unlocked) EmeraldBright else AmberWarning,
                        trackColor = Color.White.copy(alpha = 0.10f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailySummaryMetricRow(
    earningsLabel: String,
    earningsValue: String,
    earningsSubtext: String,
    deliveriesLabel: String,
    deliveriesCount: Int,
    deliveriesSubtext: String,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_summary_dashboard_card"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryStatCard(
            icon = Icons.Default.AccountBalanceWallet,
            iconContentDescription = stringResource(R.string.cd_today_earnings_icon),
            label = earningsLabel,
            value = earningsValue,
            subtext = earningsSubtext,
            accentColor = EmeraldBright,
            tintBackground = EmeraldPrimary.copy(alpha = 0.12f),
            borderColor = EmeraldPrimary.copy(alpha = 0.45f),
            onClick = onCardClick,
            modifier = Modifier
                .weight(1f)
                .testTag("summary_today_earnings_card")
        )
        SummaryStatCard(
            icon = Icons.Default.TaskAlt,
            iconContentDescription = stringResource(R.string.cd_completed_deliveries_icon),
            label = deliveriesLabel,
            value = deliveriesCount.toString(),
            subtext = deliveriesSubtext,
            accentColor = SkyInfo,
            tintBackground = SkyInfo.copy(alpha = 0.11f),
            borderColor = SkyInfo.copy(alpha = 0.42f),
            onClick = onCardClick,
            modifier = Modifier
                .weight(1f)
                .testTag("summary_completed_deliveries_card")
        )
    }
}

@Composable
private fun SummaryStatCard(
    icon: ImageVector,
    iconContentDescription: String,
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    tintBackground: Color,
    borderColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
    val cardBorder = BorderStroke(1.dp, borderColor)

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(tintBackground, Color.Transparent)
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = iconContentDescription,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_open_earnings_details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            colors = cardColors,
            border = cardBorder,
            modifier = modifier
        ) {
            content()
        }
    } else {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = cardColors,
            border = cardBorder,
            modifier = modifier
        ) {
            content()
        }
    }
}

@Composable
fun MaintenanceOverlayScreen(
    adminSettings: AdminSettings
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071124), TerminalBg)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, AmberWarning.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .testTag("maintenance_overlay")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AmberWarning.copy(alpha = 0.16f))
                        .border(1.2.dp, AmberWarning, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.maintenance_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = adminSettings.maintenanceMessage.ifBlank {
                        stringResource(R.string.maintenance_default_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                if (adminSettings.supportPhone.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${adminSettings.supportPhone}"))
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("maintenance_call_helpline_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Call Admin Helpline", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
