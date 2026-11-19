package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.FeedbackRecord
import com.example.data.SecurityUtils
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SupportScreen(
    adminSettings: AdminSettings,
    feedbacks: List<FeedbackRecord>,
    onOpenFeedbackModal: () -> Unit
) {
    val context = LocalContext.current
    val waDigits = SecurityUtils.formatWhatsAppDigits(
        adminSettings.supportWhatsapp.ifBlank { adminSettings.supportPhone }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("support_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = SkyInfo,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_support),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        item {
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SkyInfo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = SkyInfo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Gossaigaon Hub Helpline", style = MaterialTheme.typography.titleSmall, color = SkyInfo)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (adminSettings.supportHours.isNotBlank()) {
                            "Helpline Hours: ${adminSettings.supportHours}"
                        } else {
                            "Need hub assistance or want to report a delivery issue?"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    if (adminSettings.supportPhone.isNotBlank() || waDigits.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (adminSettings.supportPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${adminSettings.supportPhone}"))
                                            )
                                        }
                                    },
                                    border = BorderStroke(1.dp, TerminalBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("call_hub_button")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Call Hub", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (waDigits.isNotBlank()) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waDigits"))
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("whatsapp_hub_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenFeedbackModal,
                        border = BorderStroke(1.dp, SkyInfo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_feedback_button")
                    ) {
                        Text("Send Feedback / Ticket to Admin", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            Text(
                text = "Your Support Tickets (${feedbacks.size})",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }

        if (feedbacks.isEmpty()) {
            item {
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No support tickets submitted yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(feedbacks, key = { it.id.ifBlank { "${it.subject}_${it.createdAt}" } }) { fb ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalSurface)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            fb.subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            fb.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (fb.status == "RESOLVED") EmeraldBright else AmberWarning
                        )
                    }
                    if (fb.message.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(fb.message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    if (fb.adminReply.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = Color.Black.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, SkyInfo.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Admin Reply: ${fb.adminReply}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyInfo,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
