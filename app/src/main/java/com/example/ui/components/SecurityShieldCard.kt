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
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.security.DeviceSecurityReport
import com.example.security.SecurityThreatLevel
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SecurityThreatBanner(
    report: DeviceSecurityReport,
    onOpenSecuritySettings: () -> Unit
) {
    val showBanner = report.isMockLocationActive || report.isHookingSuspected || report.isRootSuspected
    if (!showBanner) return

    val isHighRisk = report.isMockLocationActive || report.isHookingSuspected
    val accent = if (isHighRisk) CrimsonDanger else AmberWarning
    val textAccent = if (isHighRisk) CrimsonLight else AmberLight
    val message = when {
        report.isMockLocationActive -> report.spoofReason ?: "FAKE GPS DETECTED — Turn off Mock Location apps to continue deliveries."
        report.isHookingSuspected -> "SECURITY ALERT — Tampering / Hooking framework detected on device."
        else -> "DEVICE WARNING — Rooted or custom test-keys environment detected."
    }

    Surface(
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag("security_threat_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (report.isMockLocationActive) Icons.Default.LocationOff else Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = textAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textAccent
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onOpenSecuritySettings,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("banner_security_inspect_button")
            ) {
                Text("Inspect", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SecurityShieldCard(
    report: DeviceSecurityReport,
    hasQuickPinEnabled: Boolean,
    blockedThreatsCount: Int,
    onRunSecurityScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (shieldIcon, badgeText, accent) = when (report.threatLevel) {
        SecurityThreatLevel.SECURE -> Triple(Icons.Default.GppGood, "PROTECTED (${report.activeShieldCount}/${report.totalShieldCount})", EmeraldBright)
        SecurityThreatLevel.WARNING -> Triple(Icons.Default.GppMaybe, "WARNING (${report.activeShieldCount}/${report.totalShieldCount})", AmberWarning)
        SecurityThreatLevel.HIGH_RISK -> Triple(Icons.Default.GppBad, "THREAT BLOCKED", CrimsonLight)
    }

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.2.dp, accent.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .testTag("security_shield_card")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = shieldIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(21.dp)
                    )
                    Column {
                        Text(
                            text = "Hacker, Scam & Spam Shield",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (blockedThreatsCount > 0) {
                                "$blockedThreatsCount suspicious attempt(s) blocked this session"
                            } else {
                                "Real-time anti-tamper, anti-spoof & rate-limit guard"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                StatusPill(
                    text = badgeText,
                    bgColor = accent.copy(alpha = 0.16f),
                    textColor = accent,
                    borderColor = accent.copy(alpha = 0.55f)
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
                ShieldLayerRow(
                    icon = Icons.Default.PhonelinkLock,
                    label = "Anti-Hacker / Root & Hook",
                    status = when {
                        report.isHookingSuspected -> "Hook Detected"
                        report.isRootSuspected -> "Root Warning"
                        else -> "Verified Clean"
                    },
                    accent = when {
                        report.isHookingSuspected -> CrimsonLight
                        report.isRootSuspected -> AmberLight
                        else -> EmeraldBright
                    }
                )
                ShieldLayerRow(
                    icon = Icons.Default.LocationOff,
                    label = "Anti-Fake GPS / Spoofing",
                    status = if (report.isMockLocationActive) "Spoof Blocked" else "Active Guard",
                    accent = if (report.isMockLocationActive) CrimsonLight else EmeraldBright
                )
                ShieldLayerRow(
                    icon = Icons.Default.VerifiedUser,
                    label = "Anti-Scam OTP & UTR Guard",
                    status = "SHA-256 + Lockout",
                    accent = EmeraldBright
                )
                ShieldLayerRow(
                    icon = Icons.Default.Shield,
                    label = "Anti-Spam & Phishing Filter",
                    status = "Rate-Limit ON",
                    accent = SkyInfo
                )
                ShieldLayerRow(
                    icon = Icons.Default.Lock,
                    label = "Backup & Shift PIN Lock",
                    status = buildString {
                        append(if (report.isBackupDisabled) "Backup Blocked" else "Backup Open")
                        append(" • ")
                        append(if (hasQuickPinEnabled) "PIN ON" else "PIN Off")
                    },
                    accent = if (report.isBackupDisabled && hasQuickPinEnabled) EmeraldBright else AmberLight
                )
            }

            OutlinedButton(
                onClick = onRunSecurityScan,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("run_security_scan_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Run Live Security Shield Scan", color = EmeraldBright, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ShieldLayerRow(
    icon: ImageVector,
    label: String,
    status: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}
