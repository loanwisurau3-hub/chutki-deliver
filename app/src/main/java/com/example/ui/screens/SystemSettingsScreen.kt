package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.RiderProfile
import com.example.location.GpsRadarState
import com.example.security.DeviceSecurityReport
import com.example.ui.components.FleetTextField
import com.example.ui.components.SecurityShieldCard
import com.example.ui.components.SystemHealthCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SystemSettingsScreen(
    rider: RiderProfile,
    adminSettings: AdminSettings,
    hasQuickPinEnabled: Boolean,
    backgroundAlertsEnabled: Boolean,
    hasNotificationPermission: Boolean,
    isBackgroundServiceActive: Boolean,
    isNetworkOnline: Boolean,
    isFirebaseConnected: Boolean,
    gpsState: GpsRadarState,
    riderSpeedKmh: Int?,
    batteryPct: Int?,
    securityReport: DeviceSecurityReport,
    blockedThreatsCount: Int,
    onRunSecurityScan: () -> Unit,
    onForceGpsSync: () -> Unit,
    onToggleBackgroundAlerts: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenAgreement: (initialTab: Int) -> Unit,
    onSetQuickPin: (String) -> Unit,
    onDisableQuickPin: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var newQuickPin by rememberSaveable(rider.uid) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("system_settings_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.section_settings),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(14.dp))

        // Hacker, Scam & Spam Protection Shield Card
        SecurityShieldCard(
            report = securityReport,
            hasQuickPinEnabled = hasQuickPinEnabled,
            blockedThreatsCount = blockedThreatsCount,
            onRunSecurityScan = onRunSecurityScan
        )

        Spacer(Modifier.height(16.dp))

        // Live Terminal System Diagnostics Card
        SystemHealthCard(
            isNetworkOnline = isNetworkOnline,
            isFirebaseConnected = isFirebaseConnected,
            gpsState = gpsState,
            riderSpeedKmh = riderSpeedKmh,
            batteryPct = batteryPct,
            isBackgroundServiceActive = isBackgroundServiceActive,
            kycStatus = rider.kyc.kycStatus,
            onForceGpsSync = onForceGpsSync
        )

        Spacer(Modifier.height(16.dp))

        // Background Notifications & Shift Monitor Card
        val notifStatusLabel = when {
            !hasNotificationPermission -> "PERMISSION REQUIRED"
            !backgroundAlertsEnabled -> "PAUSED"
            isBackgroundServiceActive -> "LIVE IN BACKGROUND"
            !rider.online -> "STANDBY (GO ONLINE)"
            else -> "READY"
        }
        val notifAccent = when {
            !hasNotificationPermission -> AmberWarning
            isBackgroundServiceActive -> EmeraldBright
            backgroundAlertsEnabled -> SkyInfo
            else -> TextMuted
        }

        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, notifAccent.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (backgroundAlertsEnabled && hasNotificationPermission) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                            tint = notifAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Background Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = notifStatusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = notifAccent
                            )
                        }
                    }
                    Switch(
                        checked = backgroundAlertsEnabled,
                        onCheckedChange = { checked ->
                            onToggleBackgroundAlerts(checked)
                            if (checked && !hasNotificationPermission) {
                                onRequestNotificationPermission()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = TerminalBorder
                        ),
                        modifier = Modifier.testTag("background_notifications_switch")
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Receive instant sound & vibration alerts for new grocery trips, Admin broadcasts, account approvals, and payouts even when the app is minimized.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasNotificationPermission) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    onRequestNotificationPermission()
                                } else {
                                    runCatching {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("allow_notifications_button")
                        ) {
                            Text("Allow Notifications", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSendTestNotification,
                            border = BorderStroke(1.dp, SkyInfo),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_notification_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Test Alert", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            border = BorderStroke(1.dp, TerminalBorder),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("system_notif_settings_button")
                        ) {
                            Text("Channel Settings", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Shift Terminal Quick Lock PIN Security Card
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, if (hasQuickPinEnabled) EmeraldPrimary.copy(alpha = 0.6f) else TerminalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (hasQuickPinEnabled) EmeraldBright else SkyInfo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasQuickPinEnabled) "Quick Shift PIN Lock (Active)" else "Set 4-Digit Quick Shift Lock PIN",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (hasQuickPinEnabled) EmeraldBright else TextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Protect your active rider terminal with a 4-digit PIN whenever you reopen the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FleetTextField(
                        value = newQuickPin,
                        onValueChange = { if (it.length <= 4) newQuickPin = it.filter { ch -> ch.isDigit() } },
                        label = if (hasQuickPinEnabled) "New 4-Digit PIN" else "4-Digit PIN",
                        placeholder = "1234",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_quick_pin_input")
                    )
                    Button(
                        onClick = {
                            onSetQuickPin(newQuickPin)
                            newQuickPin = ""
                        },
                        enabled = newQuickPin.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("save_quick_pin_button")
                    ) {
                        Text(if (hasQuickPinEnabled) "Update" else "Enable")
                    }
                }
                if (hasQuickPinEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDisableQuickPin,
                        border = BorderStroke(1.dp, TerminalBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disable_quick_pin_button")
                    ) {
                        Text("Disable Quick PIN Lock", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Legal Agreement & Privacy Policy Card
        val legalAccepted = rider.hasAcceptedLatestAgreement(adminSettings)
        val legalAccent = if (legalAccepted) EmeraldBright else AmberWarning
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, legalAccent.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = legalAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Agreement & Privacy Policy",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (legalAccepted) {
                                "ACCEPTED • v${adminSettings.agreementVersion} (ADMIN LIVE)"
                            } else {
                                "REVIEW REQUIRED • v${adminSettings.agreementVersion}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = legalAccent
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Admin-managed Rider Delivery Agreement and Privacy Policy. Whenever Admin updates the document, you will be prompted to review it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onOpenAgreement(0) },
                        border = BorderStroke(1.dp, SkyInfo),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_view_agreement_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Agreement", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onOpenAgreement(1) },
                        border = BorderStroke(1.dp, TerminalBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_view_privacy_button")
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Privacy Policy", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSignOut,
            border = BorderStroke(1.2.dp, CrimsonDanger),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_sign_out), color = CrimsonDanger, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(24.dp))
    }
}
