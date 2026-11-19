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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.data.RiderEarningsCalculator
import com.example.data.RiderPayoutRecord
import com.example.data.RiderProfile
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.SkyDark
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun EarningsScreen(
    rider: RiderProfile,
    adminSettings: AdminSettings,
    availablePayoutBalance: Double,
    pendingPayoutAmount: Double = 0.0,
    totalRiderEarned: Double,
    totalRiderPaidPayout: Double,
    payouts: List<RiderPayoutRecord>,
    onOpenWithdrawModal: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("earnings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = SkyInfo,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_earnings),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        // Earnings Payout Hero Card (Withdraw Only)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0E2A47), Color(0xFF0B192E))))
                    .border(1.5.dp, SkyInfo, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SkyInfo,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "AVAILABLE EARNINGS FOR WITHDRAWAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyInfo
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "₹%.2f".format(availablePayoutBalance),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SkyInfo
                            )
                            if (pendingPayoutAmount > 0.0) {
                                Text(
                                    text = "Pending Withdrawal Approval: ₹%.2f".format(pendingPayoutAmount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Payout UPI: ${rider.upiId.ifBlank { "Not Set" }} • Limits: ₹${adminSettings.minRiderPayout.toInt()} - ₹${adminSettings.maxRiderPayout.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onOpenWithdrawModal,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyDark),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("withdraw_earnings_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Withdraw", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = SkyInfo.copy(alpha = 0.25f))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Commission: ₹%.2f".format(totalRiderEarned),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Text(
                            "Paid by Admin: ₹%.2f".format(totalRiderPaidPayout),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldBright,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Active Admin Rate: ${RiderEarningsCalculator.formatRateBadge(adminSettings)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyInfo,
                        modifier = Modifier.testTag("earnings_admin_rate_label")
                    )
                    if (rider.upiId.isBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = AmberWarning.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add your Payout UPI ID to withdraw earnings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onOpenProfile,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("earnings_set_upi_button")
                                ) {
                                    Text("Set UPI", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Earnings Payout History
        item {
            Text(
                text = "Earnings Payout History (From Admin)",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }
        if (payouts.isEmpty()) {
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
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No earnings payout requests yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Complete deliveries and tap Withdraw to transfer earnings to your UPI ID.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            items(payouts, key = { "pay_${it.id}" }) { p ->
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
                                "₹%.2f".format(p.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyInfo
                            )
                            val detailLabel = if (p.riderUpiId == "COD_DEPOSIT_OFFSET") {
                                "Earnings → COD Deposit" + if (p.adminRef.isNotBlank()) " • ${p.adminRef}" else ""
                            } else {
                                "UPI: ${p.riderUpiId}" + if (p.adminRef.isNotBlank()) " • Ref: ${p.adminRef}" else ""
                            }
                            Text(
                                text = detailLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        StatusPill(
                            text = p.status,
                            bgColor = if (p.status == "PAID") EmeraldBright.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                            textColor = if (p.status == "PAID") EmeraldBright else Color(0xFFFBBF24),
                            borderColor = if (p.status == "PAID") EmeraldBright else AmberWarning
                        )
                    }
                }
            }
        }
    }
}
