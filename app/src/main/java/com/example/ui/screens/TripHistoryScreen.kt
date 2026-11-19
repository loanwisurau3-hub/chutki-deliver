package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.RiderEarningsCalculator
import com.example.data.StoreInfo
import com.example.data.TripGroup
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private enum class HistoryPeriod(val label: String) {
    TODAY("Today"),
    WEEK("7 Days"),
    ALL("All Time")
}

@Composable
fun TripHistoryScreen(
    adminSettings: AdminSettings,
    completedTrips: List<TripGroup>,
    stores: Map<String, StoreInfo> = emptyMap()
) {
    var selectedPeriod by rememberSaveable { mutableStateOf(HistoryPeriod.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredTrips = remember(completedTrips, selectedPeriod, searchQuery) {
        val periodFiltered = if (selectedPeriod == HistoryPeriod.ALL) {
            completedTrips
        } else {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val cutoff = if (selectedPeriod == HistoryPeriod.TODAY) {
                cal.timeInMillis
            } else {
                cal.timeInMillis - 6L * 24L * 3600L * 1000L
            }
            completedTrips.filter { trip ->
                val ts = trip.orders.maxOfOrNull {
                    it.deliveredAt ?: it.updatedAt ?: it.assignedAt ?: it.createdAt ?: 0L
                } ?: trip.assignedAt
                ts >= cutoff
            }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) periodFiltered
        else periodFiltered.filter { trip ->
            trip.tripId.lowercase().contains(q) ||
                trip.address.lowercase().contains(q) ||
                trip.orders.any { it.orderId.lowercase().contains(q) }
        }
    }

    val totalFilteredEarnings = remember(filteredTrips, stores, adminSettings) {
        filteredTrips.sumOf { trip ->
            RiderEarningsCalculator.calculateTripEarning(
                trip = trip,
                stores = stores,
                settings = adminSettings,
                deliveredOnly = true
            ).totalEarning
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("trip_history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_trip_history),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryPeriod.entries.forEach { period ->
                    val selected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) EmeraldPrimary else TerminalSurface)
                            .border(
                                1.dp,
                                if (selected) EmeraldPrimary else TerminalBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 10.dp)
                            .testTag("history_filter_${period.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else TextMuted
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { if (it.length <= 60) searchQuery = it },
                placeholder = { Text("Search by Trip ID or address...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = TerminalSurface,
                    unfocusedContainerColor = TerminalSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trip_history_search_input")
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredTrips.size} Trips (${selectedPeriod.label})",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Earned: ${RiderEarningsCalculator.formatRupees(totalFilteredEarnings)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldBright
                    )
                }
            }
        }

        if (filteredTrips.isEmpty()) {
            item {
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No completed trips for ${selectedPeriod.label.lowercase()}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredTrips, key = { "comp_${it.tripId}" }) { trip ->
                val summary = RiderEarningsCalculator.calculateTripEarning(
                    trip = trip,
                    stores = stores,
                    settings = adminSettings,
                    deliveredOnly = true
                )
                val earned = summary.totalEarning
                val breakdownText = RiderEarningsCalculator.formatTripBreakdown(summary, adminSettings)
                val status = trip.orders.firstOrNull()?.status ?: "DELIVERED"
                Surface(
                    color = TerminalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${trip.tripId.takeLast(6)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            if (status == "DELIVERED") {
                                StatusPill(
                                    text = "+${RiderEarningsCalculator.formatRupees(earned)} EARNED",
                                    bgColor = EmeraldBright.copy(alpha = 0.2f),
                                    textColor = EmeraldBright
                                )
                            } else {
                                StatusPill(
                                    text = status,
                                    bgColor = AmberWarning.copy(alpha = 0.2f),
                                    textColor = Color(0xFFFBBF24),
                                    borderColor = AmberWarning
                                )
                            }
                        }
                        if (status == "DELIVERED") {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = breakdownText,
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyInfo
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(trip.address, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
    }
}
