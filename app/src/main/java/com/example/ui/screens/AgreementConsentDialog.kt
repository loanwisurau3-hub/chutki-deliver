package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.AdminSettings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
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
fun AgreementConsentDialog(
    adminSettings: AdminSettings,
    initialTab: Int = 0,
    isMandatory: Boolean,
    canAccept: Boolean = true,
    alreadyAccepted: Boolean,
    acceptedAtMs: Long = 0L,
    isBusy: Boolean = false,
    onAccept: () -> Unit,
    onDeclineOrClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val docScrollState = rememberScrollState()
    LaunchedEffect(selectedTab) {
        docScrollState.scrollTo(0)
    }
    var agreementChecked by remember(adminSettings.computeAgreementDocHash(), alreadyAccepted) {
        mutableStateOf(alreadyAccepted && !isMandatory)
    }
    var privacyChecked by remember(adminSettings.computeAgreementDocHash(), alreadyAccepted) {
        mutableStateOf(alreadyAccepted && !isMandatory)
    }

    val updatedLabel = remember(adminSettings.agreementUpdatedAt) {
        if (adminSettings.agreementUpdatedAt > 0L) {
            runCatching {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(adminSettings.agreementUpdatedAt))
            }.getOrDefault("")
        } else ""
    }

    val acceptedDateLabel = remember(acceptedAtMs) {
        if (acceptedAtMs > 0L) {
            runCatching {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(acceptedAtMs))
            }.getOrDefault("")
        } else ""
    }

    Dialog(
        onDismissRequest = {
            if (!isMandatory && !isBusy) onDeclineOrClose()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory && !isBusy,
            dismissOnClickOutside = !isMandatory && !isBusy,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, if (isMandatory) EmeraldPrimary.copy(alpha = 0.7f) else TerminalBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("agreement_popup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.16f))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isMandatory) "Agreement & Privacy Checkup" else "Rider Legal Documents",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "v${adminSettings.agreementVersion} • ADMIN LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyInfo
                                )
                                if (updatedLabel.isNotBlank()) {
                                    Text(
                                        text = "• Updated $updatedLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    if (!isMandatory) {
                        IconButton(
                            onClick = onDeclineOrClose,
                            modifier = Modifier.testTag("agreement_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close_modal),
                                tint = TextMuted
                            )
                        }
                    }
                }

                if (isMandatory) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberWarning.copy(alpha = 0.12f))
                            .border(1.dp, AmberWarning.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Please review and accept the latest Rider Agreement & Privacy Policy from Admin to continue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }
                } else if (alreadyAccepted) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (acceptedDateLabel.isNotBlank()) {
                                "Accepted v${adminSettings.agreementVersion} on $acceptedDateLabel"
                            } else {
                                "You have accepted the current agreement (v${adminSettings.agreementVersion})."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldBright
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2-Tab Switcher: Rider Agreement | Privacy Policy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalBg)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    LegalDocTabChip(
                        label = "Rider Agreement",
                        icon = Icons.Default.Description,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f).testTag("agreement_tab_terms")
                    )
                    LegalDocTabChip(
                        label = "Privacy Policy",
                        icon = Icons.Default.Policy,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f).testTag("agreement_tab_privacy")
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Document Scrollable Viewport
                val docTitle = if (selectedTab == 0) adminSettings.agreementTitle else adminSettings.privacyPolicyTitle
                val docBody = if (selectedTab == 0) adminSettings.agreementContent else adminSettings.privacyPolicyContent
                val docUrl = if (selectedTab == 0) adminSettings.agreementUrl else adminSettings.privacyPolicyUrl

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 250.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TerminalCard)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(14.dp))
                        .verticalScroll(docScrollState)
                        .padding(14.dp)
                ) {
                    Text(
                        text = docTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldBright,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = docBody,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = TextPrimary
                    )

                    if (docUrl.startsWith("http://") || docUrl.startsWith("https://")) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(docUrl)))
                                }
                            },
                            border = BorderStroke(1.dp, SkyInfo),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("agreement_open_url_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Full Web Document", color = SkyInfo, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (canAccept && (isMandatory || !alreadyAccepted)) {
                    Spacer(Modifier.height(12.dp))

                    AgreementCheckRow(
                        checked = agreementChecked,
                        onCheckedChange = { agreementChecked = it },
                        text = "I have read and accept the ${adminSettings.agreementTitle} (v${adminSettings.agreementVersion}).",
                        testTag = "agreement_checkbox_terms"
                    )
                    Spacer(Modifier.height(6.dp))
                    AgreementCheckRow(
                        checked = privacyChecked,
                        onCheckedChange = { privacyChecked = it },
                        text = "I agree to the ${adminSettings.privacyPolicyTitle}, including live 10s GPS tracking while On Duty.",
                        testTag = "agreement_checkbox_privacy"
                    )

                    Spacer(Modifier.height(14.dp))

                    val bothChecked = agreementChecked && privacyChecked
                    Button(
                        onClick = onAccept,
                        enabled = bothChecked && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("agreement_accept_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Accept & Continue", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDeclineOrClose,
                        enabled = !isBusy,
                        border = BorderStroke(1.dp, if (isMandatory) CrimsonLight.copy(alpha = 0.5f) else TerminalBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("agreement_decline_button")
                    ) {
                        Text(
                            text = if (isMandatory) "Decline & Sign Out" else "Close",
                            color = if (isMandatory) CrimsonLight else TextMuted,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onDeclineOrClose,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("agreement_done_button")
                    ) {
                        Text(if (alreadyAccepted) "Done" else "Close", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalDocTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) EmeraldPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else TextMuted,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextMuted
        )
    }
}

@Composable
private fun AgreementCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) EmeraldPrimary.copy(alpha = 0.09f) else TerminalBg)
            .border(
                width = 1.dp,
                color = if (checked) EmeraldPrimary.copy(alpha = 0.55f) else TerminalBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = EmeraldPrimary,
                uncheckedColor = TextMuted
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (checked) TextPrimary else TextMuted,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}
