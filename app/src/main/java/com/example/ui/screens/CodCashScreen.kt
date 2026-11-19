package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.SettlementRecord
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun CodCashScreen(
    adminSettings: AdminSettings,
    netCashInHand: Double,
    pendingCodSettlementAmount: Double,
    totalCodCollected: Double,
    totalDeposited: Double,
    settlements: List<SettlementRecord>,
    onOpenDepositModal: () -> Unit
) {
    val maxLimit = adminSettings.maxRiderCashLimit.coerceAtLeast(1.0)
    val usageRatio = (netCashInHand / maxLimit).toFloat().coerceIn(0f, 1f)
    val barColor = when {
        usageRatio >= 0.9f -> CrimsonDanger
        usageRatio >= 0.7f -> AmberWarning
        else -> EmeraldPrimary
    }
    val remainingToDeposit = (netCashInHand - pendingCodSettlementAmount).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cod_cash_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_cod_cash),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        // COD Cash in Hand Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(TerminalCard, TerminalSurface)))
                    .border(1.2.dp, barColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADMIN COD CASH IN HAND (MAX ₹${adminSettings.maxRiderCashLimit.toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "₹%.2f".format(netCashInHand),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            if (pendingCodSettlementAmount > 0.0) {
                                Text(
                                    text = "Pending Admin Approval: ₹%.2f (Remaining: ₹%.2f)".format(
                                        pendingCodSettlementAmount,
                                        remainingToDeposit
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Deposit Limits: ₹${adminSettings.minRiderCodDeposit.toInt()} - ₹${adminSettings.maxRiderCodDeposit.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onOpenDepositModal,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("deposit_cod_button")
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Deposit COD", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { usageRatio },
                        color = barColor,
                        trackColor = TerminalBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = TerminalBorder)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total COD Collected: ₹%.2f".format(totalCodCollected),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Text(
                            "Settled: ₹%.2f".format(totalDeposited),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldBright,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // COD Cash Handover Log
        item {
            Text(
                text = "COD Cash Handover Log (To Admin)",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }
        if (settlements.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No COD deposit requests yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Cash collected on COD deliveries can be deposited via UPI or Office Cash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            items(settlements, key = { "set_${it.id}" }) { s ->
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "₹%.2f".format(s.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            val modeLabel = if (s.mode == "EARNINGS") "FROM EARNINGS" else s.mode
                            Text(
                                "$modeLabel • ${s.utr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        StatusPill(
                            text = s.status,
                            bgColor = if (s.status == "APPROVED") EmeraldBright.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                            textColor = if (s.status == "APPROVED") EmeraldBright else Color(0xFFFBBF24),
                            borderColor = if (s.status == "APPROVED") EmeraldBright else AmberWarning
                        )
                    }
                }
            }
        }
    }
}
