package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.RiderProfile
import com.example.location.GpsRadarState
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.ToastEvent
import com.example.viewmodel.ToastType
import kotlinx.coroutines.delay

@Composable
fun SecurityRibbon() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090E1A))
            .border(width = 0.5.dp, color = TerminalBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SatelliteAlt,
            contentDescription = null,
            tint = SkyInfo,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.security_ribbon_text).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = SkyInfo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ConnectivityStatusBanner(
    isNetworkOnline: Boolean,
    isFirebaseConnected: Boolean
) {
    val showBanner = !isNetworkOnline || !isFirebaseConnected
    AnimatedVisibility(
        visible = showBanner,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        val isOffline = !isNetworkOnline
        val bg = if (isOffline) CrimsonDanger.copy(alpha = 0.22f) else AmberWarning.copy(alpha = 0.16f)
        val border = if (isOffline) CrimsonDanger else AmberWarning
        val fg = if (isOffline) CrimsonLight else AmberLight
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .border(0.5.dp, border)
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("connectivity_status_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isOffline) {
                    "OFFLINE MODE — Cached orders visible; auto-syncing once internet returns"
                } else {
                    "RECONNECTING TO GOSSAIGAON FLEET CLOUD..."
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdminAlertsCluster(rider: RiderProfile, adminSettings: AdminSettings) {
    if (rider.suspended) {
        AlertStrip(
            bgColor = CrimsonDanger.copy(alpha = 0.24f),
            borderColor = CrimsonDanger,
            textColor = CrimsonLight,
            icon = Icons.Default.Lock,
            text = stringResource(R.string.suspended_banner_text)
        )
    }
    if (rider.adminNote.isNotBlank()) {
        AlertStrip(
            bgColor = AmberWarning.copy(alpha = 0.16f),
            borderColor = AmberWarning.copy(alpha = 0.45f),
            textColor = AmberLight,
            icon = Icons.Default.Message,
            text = "MESSAGE FROM ADMIN: ${rider.adminNote}"
        )
    }
    if (adminSettings.riderAnnouncement.isNotBlank()) {
        AlertStrip(
            bgColor = SkyInfo.copy(alpha = 0.14f),
            borderColor = SkyInfo.copy(alpha = 0.45f),
            textColor = Color(0xFF7DD3FC),
            icon = Icons.Default.Campaign,
            text = "ADMIN NOTICE: ${adminSettings.riderAnnouncement}"
        )
    }
}

@Composable
private fun AlertStrip(
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(width = 0.5.dp, color = borderColor)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun RiderHeader(
    rider: RiderProfile,
    soundEnabled: Boolean,
    onOpenMenu: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleDuty: () -> Unit
) {
    val isOnline = rider.online && !rider.suspended
    Surface(
        color = TerminalSurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = TerminalBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.14f))
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                        .testTag("open_drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.cd_open_menu),
                        tint = EmeraldBright,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rider.name.ifBlank { "Gossaigaon Rider" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = rider.vehicle.ifBlank { "Bike" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        val ratingText = if (rider.ratingCount > 0) {
                            "%.1f (%d)".format(rider.ratingAvg, rider.ratingCount)
                        } else {
                            "New"
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberWarning.copy(alpha = 0.16f))
                                .border(0.8.dp, AmberWarning.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = ratingText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                        .testTag("sound_toggle_button")
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.cd_sound_toggle),
                        tint = if (soundEnabled) TextPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(
                            1.dp,
                            if (isOnline) EmeraldPrimary.copy(alpha = 0.7f) else TerminalBorder,
                            RoundedCornerShape(22.dp)
                        )
                        .clickable(onClick = onToggleDuty)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("duty_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) EmeraldPrimary else CrimsonDanger)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) stringResource(R.string.status_online) else stringResource(R.string.status_offline),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOnline) EmeraldBright else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun GpsRadarBar(
    gpsState: GpsRadarState,
    speedKmh: Int? = null,
    onRequestPermission: () -> Unit,
    onForceSync: (() -> Unit)? = null
) {
    when (gpsState) {
        GpsRadarState.Hidden -> Unit
        GpsRadarState.Connecting -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                    .border(0.5.dp, EmeraldPrimary.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CONNECTING LIVE GPS...", style = MaterialTheme.typography.labelSmall, color = EmeraldBright)
                }
                Text("Starting", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
            }
        }
        is GpsRadarState.Live -> {
            val netOk = gpsState.isNetworkOnline
            val bg = if (netOk) EmeraldPrimary.copy(alpha = 0.14f) else CrimsonDanger.copy(alpha = 0.2f)
            val fg = if (netOk) EmeraldBright else CrimsonLight
            val accLabel = when {
                !netOk -> "Offline"
                gpsState.accuracyMeters != null && gpsState.accuracyMeters > 120 -> "Low (~${gpsState.accuracyMeters}m)"
                else -> buildString {
                    append("Live")
                    gpsState.accuracyMeters?.let { append(" ±${it}m") }
                    if (speedKmh != null && speedKmh > 0) append(" • ${speedKmh}km/h")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .border(0.5.dp, fg.copy(alpha = 0.3f))
                    .let { mod -> if (onForceSync != null) mod.clickable(onClick = onForceSync) else mod }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (netOk) "LIVE 10s GPS TRACKING" else "NETWORK OFFLINE — ORDER PROTECTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = fg
                    )
                }
                Text(accLabel, style = MaterialTheme.typography.labelSmall, color = fg)
            }
        }
        is GpsRadarState.Error -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CrimsonDanger.copy(alpha = 0.2f))
                    .border(0.5.dp, CrimsonDanger.copy(alpha = 0.5f))
                    .clickable(onClick = onRequestPermission)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.GpsOff, contentDescription = null, tint = CrimsonLight, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(gpsState.message, style = MaterialTheme.typography.labelSmall, color = CrimsonLight)
                }
                Text(gpsState.badge, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

@Composable
fun CashLimitBanner(
    netCashInHand: Double,
    maxCashLimit: Double,
    onDepositClick: () -> Unit
) {
    if (netCashInHand < maxCashLimit) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrimsonDanger.copy(alpha = 0.22f))
            .border(0.8.dp, CrimsonDanger)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "CASH LIMIT EXCEEDED (₹${maxCashLimit.toInt()}+): Deposit COD cash to Admin to unlock pickups!",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = CrimsonLight
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onDepositClick,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("banner_deposit_button")
        ) {
            Text("Deposit", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ApprovalPendingBanner(approved: Boolean) {
    if (approved) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberWarning.copy(alpha = 0.15f))
            .border(0.8.dp, AmberWarning)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${stringResource(R.string.approval_pending_title)} ${stringResource(R.string.approval_pending_body)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFEF08A)
        )
    }
}

@Composable
fun FleetToastOverlay(toast: ToastEvent?, onDismiss: () -> Unit) {
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(4200L)
            onDismiss()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            if (toast != null) {
                val accent = when (toast.type) {
                    ToastType.SUCCESS -> EmeraldPrimary
                    ToastType.DANGER -> CrimsonDanger
                    ToastType.INFO -> SkyInfo
                    ToastType.WARNING -> AmberWarning
                }
                val icon = when (toast.type) {
                    ToastType.SUCCESS -> Icons.Default.CheckCircle
                    ToastType.DANGER -> Icons.Default.Error
                    ToastType.INFO -> Icons.Default.Info
                    ToastType.WARNING -> Icons.Default.Warning
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TerminalCard.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, accent),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = toast.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_modal), tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FleetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    borderColor: Color = TerminalBorder,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isError) CrimsonLight else TextMuted,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted.copy(alpha = 0.6f)) },
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TerminalSurface,
                unfocusedContainerColor = TerminalSurface,
                disabledContainerColor = Color(0xFF090D16),
                focusedBorderColor = if (isError) CrimsonDanger else EmeraldPrimary,
                unfocusedBorderColor = if (isError) CrimsonDanger.copy(alpha = 0.7f) else borderColor,
                disabledBorderColor = TerminalBorder,
                errorBorderColor = CrimsonDanger,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) CrimsonLight else TextMuted,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )
        }
    }
}

@Composable
fun StatusPill(text: String, bgColor: Color, textColor: Color, borderColor: Color = bgColor) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
