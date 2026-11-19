package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RiderProfile
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.RiderTab

private data class DrawerMenuItem(
    val tab: RiderTab,
    val labelRes: Int,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val accent: Color,
    val badgeText: String? = null,
    val badgeColor: Color = EmeraldPrimary,
    val testTag: String
)

@Composable
fun FleetNavigationDrawerSheet(
    rider: RiderProfile,
    selectedTab: RiderTab,
    activeTripsCount: Int,
    completedTripsCount: Int,
    availablePayoutBalance: Double,
    netCashInHand: Double,
    maxCashLimit: Double,
    openTicketsCount: Int,
    onSelectTab: (RiderTab) -> Unit,
    onCloseDrawer: () -> Unit,
    onSignOut: () -> Unit
) {
    val isOnline = rider.online && !rider.suspended
    val cashNearLimit = maxCashLimit > 0 && netCashInHand >= maxCashLimit * 0.8
    val ratingBadge = if (rider.ratingCount > 0) "★ %.1f".format(rider.ratingAvg) else "New"
    val (kycBadgeText, kycBadgeColor) = when (rider.kyc.kycStatus.uppercase()) {
        "VERIFIED" -> "Verified" to EmeraldBright
        "PENDING" -> "Pending" to AmberWarning
        "REJECTED" -> "Re-Submit" to CrimsonDanger
        else -> "Action" to AmberWarning
    }

    val dispatchItems = listOf(
        DrawerMenuItem(
            tab = RiderTab.ACTIVE,
            labelRes = R.string.section_active_trips,
            subtitle = "Live order queue & GPS",
            selectedIcon = Icons.Filled.Navigation,
            unselectedIcon = Icons.Outlined.Navigation,
            accent = EmeraldPrimary,
            badgeText = if (activeTripsCount > 0) activeTripsCount.toString() else null,
            badgeColor = EmeraldPrimary,
            testTag = "drawer_item_active"
        ),
        DrawerMenuItem(
            tab = RiderTab.DASHBOARD,
            labelRes = R.string.section_dashboard,
            subtitle = "Shift summary & 7-day chart",
            selectedIcon = Icons.Filled.Insights,
            unselectedIcon = Icons.Outlined.Insights,
            accent = SkyInfo,
            testTag = "drawer_item_dashboard"
        ),
        DrawerMenuItem(
            tab = RiderTab.TRIP_HISTORY,
            labelRes = R.string.section_trip_history,
            subtitle = "Past completed deliveries",
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            accent = EmeraldBright,
            badgeText = if (completedTripsCount > 0) completedTripsCount.toString() else null,
            badgeColor = SkyInfo,
            testTag = "drawer_item_trip_history"
        )
    )

    val financeItems = listOf(
        DrawerMenuItem(
            tab = RiderTab.EARNINGS,
            labelRes = R.string.section_earnings,
            subtitle = "Withdraw commission to UPI",
            selectedIcon = Icons.Filled.AccountBalanceWallet,
            unselectedIcon = Icons.Outlined.AccountBalanceWallet,
            accent = SkyInfo,
            badgeText = "₹${availablePayoutBalance.toInt()}",
            badgeColor = SkyInfo,
            testTag = "drawer_item_earnings"
        ),
        DrawerMenuItem(
            tab = RiderTab.COD_CASH,
            labelRes = R.string.section_cod_cash,
            subtitle = "Deposit collected COD cash",
            selectedIcon = Icons.Filled.CurrencyRupee,
            unselectedIcon = Icons.Outlined.CurrencyRupee,
            accent = if (cashNearLimit) AmberWarning else EmeraldPrimary,
            badgeText = "₹${netCashInHand.toInt()}",
            badgeColor = if (cashNearLimit) CrimsonDanger else EmeraldPrimary,
            testTag = "drawer_item_cod_cash"
        )
    )

    val accountItems = listOf(
        DrawerMenuItem(
            tab = RiderTab.RATINGS,
            labelRes = R.string.section_ratings,
            subtitle = "Customer stars & feedback",
            selectedIcon = Icons.Filled.Star,
            unselectedIcon = Icons.Outlined.StarOutline,
            accent = AmberWarning,
            badgeText = ratingBadge,
            badgeColor = AmberWarning,
            testTag = "drawer_item_ratings"
        ),
        DrawerMenuItem(
            tab = RiderTab.PROFILE,
            labelRes = R.string.section_profile,
            subtitle = "Name, mobile, UPI & bike",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            accent = EmeraldBright,
            testTag = "drawer_item_profile"
        ),
        DrawerMenuItem(
            tab = RiderTab.KYC,
            labelRes = R.string.section_kyc,
            subtitle = "DL, ID card & selfie check",
            selectedIcon = Icons.Filled.Badge,
            unselectedIcon = Icons.Outlined.Badge,
            accent = kycBadgeColor,
            badgeText = kycBadgeText,
            badgeColor = kycBadgeColor,
            testTag = "drawer_item_kyc"
        )
    )

    val terminalItems = listOf(
        DrawerMenuItem(
            tab = RiderTab.SUPPORT,
            labelRes = R.string.section_support,
            subtitle = "Call hub, WhatsApp & tickets",
            selectedIcon = Icons.Filled.SupportAgent,
            unselectedIcon = Icons.Outlined.SupportAgent,
            accent = SkyInfo,
            badgeText = if (openTicketsCount > 0) "$openTicketsCount open" else null,
            badgeColor = AmberWarning,
            testTag = "drawer_item_support"
        ),
        DrawerMenuItem(
            tab = RiderTab.SETTINGS,
            labelRes = R.string.section_settings,
            subtitle = "Health, alerts, PIN & legal",
            selectedIcon = Icons.Filled.Security,
            unselectedIcon = Icons.Outlined.Security,
            accent = EmeraldPrimary,
            testTag = "drawer_item_settings"
        )
    )

    ModalDrawerSheet(
        drawerContainerColor = TerminalBg,
        drawerContentColor = TextPrimary,
        modifier = Modifier
            .width(308.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Rider Identity Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0E2439), TerminalSurface)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.16f))
                                    .border(1.2.dp, EmeraldPrimary, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rider.name.ifBlank { "Gossaigaon Rider" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rider.vehicle.ifBlank { "Delivery Partner" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("drawer_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close_modal),
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isOnline) EmeraldPrimary.copy(alpha = 0.18f)
                                    else CrimsonDanger.copy(alpha = 0.18f)
                                )
                                .border(
                                    1.dp,
                                    if (isOnline) EmeraldPrimary.copy(alpha = 0.6f)
                                    else CrimsonDanger.copy(alpha = 0.55f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) EmeraldBright else CrimsonDanger)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "ON DUTY" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOnline) EmeraldBright else CrimsonLight
                            )
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyInfo
                        )
                    }
                }
            }

            HorizontalDivider(color = TerminalBorder)

            // Scrollable Grouped Sections
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawerGroupHeader(stringResource(R.string.drawer_group_dispatch))
                dispatchItems.forEach { item ->
                    DrawerNavRow(
                        item = item,
                        selected = selectedTab == item.tab,
                        onClick = { onSelectTab(item.tab) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                DrawerGroupHeader(stringResource(R.string.drawer_group_finance))
                financeItems.forEach { item ->
                    DrawerNavRow(
                        item = item,
                        selected = selectedTab == item.tab,
                        onClick = { onSelectTab(item.tab) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                DrawerGroupHeader(stringResource(R.string.drawer_group_account))
                accountItems.forEach { item ->
                    DrawerNavRow(
                        item = item,
                        selected = selectedTab == item.tab,
                        onClick = { onSelectTab(item.tab) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                DrawerGroupHeader(stringResource(R.string.drawer_group_terminal))
                terminalItems.forEach { item ->
                    DrawerNavRow(
                        item = item,
                        selected = selectedTab == item.tab,
                        onClick = { onSelectTab(item.tab) }
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(color = TerminalBorder)

            // Pinned Sign Out Footer
            Surface(
                color = CrimsonDanger.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSignOut)
                    .testTag("drawer_sign_out_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = CrimsonLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_sign_out),
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonLight
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = TextDim,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerNavRow(
    item: DrawerMenuItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) item.accent.copy(alpha = 0.16f) else Color.Transparent
    val borderColor = if (selected) item.accent.copy(alpha = 0.55f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(item.testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) item.accent.copy(alpha = 0.22f) else TerminalCard
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = if (selected) item.accent else TextMuted,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) TextPrimary else TextPrimary.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!item.badgeText.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(item.badgeColor.copy(alpha = 0.18f))
                    .border(0.8.dp, item.badgeColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = item.badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = item.badgeColor,
                    maxLines = 1
                )
            }
        }
    }
}
