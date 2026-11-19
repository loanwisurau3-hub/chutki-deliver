package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.location.GpsRadarState
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SystemHealthCard(
    isNetworkOnline: Boolean,
    isFirebaseConnected: Boolean,
    gpsState: GpsRadarState,
    riderSpeedKmh: Int?,
    batteryPct: Int?,
    isBackgroundServiceActive: Boolean,
    kycStatus: String,
    onForceGpsSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cloudHealthy = isNetworkOnline && isFirebaseConnected
    val cloudLabel = when {
        !isNetworkOnline -> "Offline (Cached)"
        !isFirebaseConnected -> "Reconnecting..."
        else -> "Live Synced"
    }
    val cloudAccent = if (cloudHealthy) EmeraldBright else AmberLight

    val gpsLabel = when (gpsState) {
        is GpsRadarState.Live -> buildString {
            append("Active")
            gpsState.accuracyMeters?.let { append(" • ±${it}m") }
            if (riderSpeedKmh != null && riderSpeedKmh > 0) append(" • ${riderSpeedKmh} km/h")
        }
        GpsRadarState.Connecting -> "Acquiring Fix..."
        is GpsRadarState.Error -> "Action Needed"
        GpsRadarState.Hidden -> "Standby (Off Duty)"
    }
    val gpsAccent = when (gpsState) {
        is GpsRadarState.Live -> EmeraldBright
        GpsRadarState.Connecting -> SkyInfo
        is GpsRadarState.Error -> CrimsonLight
        GpsRadarState.Hidden -> TextMuted
    }

    val batteryLabel = if (batteryPct != null) "$batteryPct%" else "Normal"
    val batteryAccent = when {
        batteryPct == null -> TextPrimary
        batteryPct <= 15 -> CrimsonLight
        batteryPct <= 30 -> AmberLight
        else -> EmeraldBright
    }

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, MaterialTheme.shapes.large)
            .testTag("system_health_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "Terminal System Diagnostics",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                StatusPill(
                    text = if (cloudHealthy) "HEALTHY" else "CHECK",
                    bgColor = cloudAccent.copy(alpha = 0.16f),
                    textColor = cloudAccent,
                    borderColor = cloudAccent.copy(alpha = 0.5f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TerminalBg.copy(alpha = 0.65f))
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticRow(
                    icon = if (cloudHealthy) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    label = "Firebase Cloud",
                    value = cloudLabel,
                    accent = cloudAccent
                )
                DiagnosticRow(
                    icon = Icons.Default.GpsFixed,
                    label = "10s GPS Telemetry",
                    value = gpsLabel,
                    accent = gpsAccent
                )
                DiagnosticRow(
                    icon = Icons.Default.NotificationsActive,
                    label = "Background Alerts",
                    value = if (isBackgroundServiceActive) "Running" else "Standby",
                    accent = if (isBackgroundServiceActive) EmeraldBright else TextMuted
                )
                DiagnosticRow(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Battery & KYC",
                    value = "$batteryLabel • KYC ${kycStatus.replace('_', ' ')}",
                    accent = batteryAccent
                )
            }

            OutlinedButton(
                onClick = onForceGpsSync,
                border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("force_gps_sync_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync Live GPS & Telemetry Now", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}
