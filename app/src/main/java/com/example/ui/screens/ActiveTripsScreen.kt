package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AdminSettings
import com.example.data.EmergencyRequestRecord
import com.example.data.OrderRecord
import com.example.data.RiderAnalyticsEngine
import com.example.data.RiderEarningsCalculator
import com.example.data.SecurityUtils
import com.example.data.StoreInfo
import com.example.data.StoreStop
import com.example.data.TripGroup
import com.example.location.RiderLocationForegroundService
import com.example.location.RiderMapsNavigator
import com.example.ui.components.StatusPill
import kotlinx.coroutines.launch
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueStop
import com.example.ui.theme.BlueStopLight
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ActiveTripsScreen(
    activeTrips: List<TripGroup>,
    stores: Map<String, StoreInfo>,
    adminSettings: AdminSettings,
    countdownByTrip: Map<String, Int>,
    emergencyRequests: List<EmergencyRequestRecord>,
    riderLat: Double?,
    riderLng: Double?,
    getRiderFee: (OrderRecord) -> Double,
    onLocationPermissionGranted: () -> Unit = {},
    onAcceptTrip: (String) -> Unit,
    onPassTripDirect: (String, String) -> Unit,
    onOpenPassModal: (String) -> Unit,
    onOpenPickupVerify: (orderId: String, storeName: String) -> Unit,
    onStartDoorstep: (String) -> Unit,
    onOpenDeliveryVerify: (tripId: String, totalAmt: Double, isPrepaid: Boolean) -> Unit,
    onOpenSosModal: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "assigned_trips_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assigned Trips Queue",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalSurfaceLight)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${activeTrips.size} Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        if (activeTrips.isEmpty()) {
            item(key = "empty_trips_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.empty_trips_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.empty_trips_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeTrips, key = { it.tripId }) { trip ->
                val isAccepted = trip.orders.all { it.riderAccepted == true }
                val isPostPickupLocked = SecurityUtils.isTripPostPickupLocked(trip)
                if (!isAccepted && !isPostPickupLocked) {
                    IncomingTripCard(
                        trip = trip,
                        stores = stores,
                        adminSettings = adminSettings,
                        riderLat = riderLat,
                        riderLng = riderLng,
                        timeoutSec = adminSettings.riderAcceptTimeoutSec,
                        remainingSec = countdownByTrip[trip.tripId] ?: adminSettings.riderAcceptTimeoutSec,
                        allowPass = adminSettings.allowRiderPassTrip,
                        getRiderFee = getRiderFee,
                        onAccept = { onAcceptTrip(trip.tripId) },
                        onPass = { onPassTripDirect(trip.tripId, "Rider declined") }
                    )
                } else {
                    ActiveMultiStoreTripCard(
                        trip = trip,
                        stores = stores,
                        adminSettings = adminSettings,
                        emergencyRequests = emergencyRequests,
                        riderLat = riderLat,
                        riderLng = riderLng,
                        allowPass = adminSettings.allowRiderPassTrip,
                        allowSos = adminSettings.allowRiderSos,
                        getRiderFee = getRiderFee,
                        onLocationPermissionGranted = onLocationPermissionGranted,
                        onOpenPickupVerify = onOpenPickupVerify,
                        onStartDoorstep = { onStartDoorstep(trip.tripId) },
                        onOpenDeliveryVerify = { total, prepaid ->
                            onOpenDeliveryVerify(trip.tripId, total, prepaid)
                        },
                        onOpenSos = { onOpenSosModal(trip.tripId) },
                        onOpenPassModal = { onOpenPassModal(trip.tripId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingTripCard(
    trip: TripGroup,
    stores: Map<String, StoreInfo>,
    adminSettings: AdminSettings,
    riderLat: Double?,
    riderLng: Double?,
    timeoutSec: Int,
    remainingSec: Int,
    allowPass: Boolean,
    getRiderFee: (OrderRecord) -> Double,
    onAccept: () -> Unit,
    onPass: () -> Unit
) {
    val isMultiStore = trip.orders.size > 1
    val totalBill = trip.orders.sumOf { it.totalAmount }
    val earningSummary = RiderEarningsCalculator.calculateTripEarning(trip, stores, adminSettings)
    val totalFee = earningSummary.totalEarning
    val earningBreakdownText = RiderEarningsCalculator.formatTripBreakdown(earningSummary, adminSettings)
    val allItems = trip.orders.flatMap { it.items }
    val isUpi = trip.paymentMode == "UPI"
    val isWallet = trip.paymentMode == "WALLET"
    val isPrepaidVerified = isWallet || (isUpi && trip.orders.all { it.paymentStatus == "VERIFIED" })

    Surface(
        color = TerminalSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(2.dp, AmberWarning),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMultiStore) "⚡ ${trip.orders.size}-STORES COMBO TRIP" else "⚡ NEW TRIP ASSIGNED",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFEF08A)
                    )
                    Text(
                        text = "#${trip.tripId.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                StatusPill(
                    text = "ACCEPT REQUIRED",
                    bgColor = AmberWarning.copy(alpha = 0.2f),
                    textColor = Color(0xFFFBBF24),
                    borderColor = AmberWarning
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto-Pass in:", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
                }
                Text("${remainingSec}s", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (remainingSec.toFloat() / timeoutSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                color = AmberWarning,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.12f))
                    .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("YOUR EARNING", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = "+${RiderEarningsCalculator.formatRupees(totalFee)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = earningBreakdownText,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBright
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PAYMENT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = when {
                            isWallet -> "Wallet Paid"
                            isUpi -> if (isPrepaidVerified) "UPI Paid" else "UPI Hold"
                            else -> "COD: ₹%.0f".format(totalBill)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                trip.orders.forEachIndexed { idx, order ->
                    val st = stores[order.restaurantId] ?: StoreInfo()
                    val d = SecurityUtils.calculateDistanceKm(riderLat, riderLng, st.lat, st.lng)
                    val distStr = if (d != null) " (~%.1fkm)".format(d) else ""
                    StatusPill(
                        text = "Store ${idx + 1}: ${st.name}$distStr",
                        bgColor = Color(0xFF1D4ED8),
                        textColor = Color.White
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(10.dp)
            ) {
                val itemsText = allItems.joinToString(", ") { "${it.qty}× ${it.name}" }
                Text(
                    text = "Items (${allItems.size}): $itemsText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowPass) {
                    OutlinedButton(
                        onClick = onPass,
                        border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("pass_incoming_trip_${trip.tripId}")
                    ) {
                        Text("Pass", color = CrimsonDanger, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.5f).height(48.dp).testTag("accept_trip_${trip.tripId}")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Accept (+${RiderEarningsCalculator.formatRupees(totalFee)})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private data class PendingMapNavigationRequest(
    val destLat: Double?,
    val destLng: Double?,
    val destLabelFallback: String,
    val locationLink: String? = null
)

@Composable
private fun ActiveMultiStoreTripCard(
    trip: TripGroup,
    stores: Map<String, StoreInfo>,
    adminSettings: AdminSettings,
    emergencyRequests: List<EmergencyRequestRecord>,
    riderLat: Double?,
    riderLng: Double?,
    allowPass: Boolean,
    allowSos: Boolean,
    getRiderFee: (OrderRecord) -> Double,
    onLocationPermissionGranted: () -> Unit,
    onOpenPickupVerify: (orderId: String, storeName: String) -> Unit,
    onStartDoorstep: () -> Unit,
    onOpenDeliveryVerify: (totalAmt: Double, isPrepaid: Boolean) -> Unit,
    onOpenSos: () -> Unit,
    onOpenPassModal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isOpeningMap by remember(trip.tripId) { mutableStateOf(false) }
    var pendingMapRequest by remember(trip.tripId) { mutableStateOf<PendingMapNavigationRequest?>(null) }

    val fallbackRiderCoords = remember(riderLat, riderLng, trip.orders) {
        if (RiderMapsNavigator.isValidCoord(riderLat, riderLng)) {
            riderLat to riderLng
        } else {
            trip.orders.firstNotNullOfOrNull { order ->
                if (RiderMapsNavigator.isValidCoord(order.riderLat, order.riderLng)) {
                    order.riderLat to order.riderLng
                } else {
                    null
                }
            } ?: (null to null)
        }
    }

    fun launchMapsNow(req: PendingMapNavigationRequest) {
        if (isOpeningMap) return
        scope.launch {
            isOpeningMap = true
            try {
                RiderMapsNavigator.openDirectionsWithExactRiderLocation(
                    context = context,
                    fallbackOriginLat = fallbackRiderCoords.first,
                    fallbackOriginLng = fallbackRiderCoords.second,
                    destLat = req.destLat,
                    destLng = req.destLng,
                    destLabelFallback = req.destLabelFallback,
                    locationLink = req.locationLink
                )
            } finally {
                isOpeningMap = false
            }
        }
    }

    val mapLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onLocationPermissionGranted()
        }
        val req = pendingMapRequest
        pendingMapRequest = null
        if (req != null) {
            launchMapsNow(req)
        }
    }

    fun requestLocationAndOpenMaps(req: PendingMapNavigationRequest) {
        if (isOpeningMap) return
        if (RiderLocationForegroundService.hasLocationPermission(context)) {
            launchMapsNow(req)
        } else {
            pendingMapRequest = req
            mapLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    val isPostPickupLocked = SecurityUtils.isTripPostPickupLocked(trip)
    val isMultiStore = trip.orders.size > 1
    val totalBill = trip.orders.sumOf { it.totalAmount }
    val earningSummary = RiderEarningsCalculator.calculateTripEarning(trip, stores, adminSettings)
    val totalFee = earningSummary.totalEarning
    val earningBreakdownText = RiderEarningsCalculator.formatTripBreakdown(earningSummary, adminSettings)
    val isUpi = trip.paymentMode == "UPI"
    val isWallet = trip.paymentMode == "WALLET"
    val isPrepaidVerified = isWallet || (isUpi && trip.orders.all { it.paymentStatus == "VERIFIED" })
    val isUpiPending = isUpi && !isPrepaidVerified

    val storeStops = trip.orders.map { o ->
        val st = stores[o.restaurantId] ?: StoreInfo()
        val dist = SecurityUtils.calculateDistanceKm(riderLat, riderLng, st.lat, st.lng) ?: 999.0
        StoreStop(
            order = o,
            store = st,
            distanceKm = dist,
            isPickedUp = SecurityUtils.isOrderPostPickupLocked(o)
        )
    }.sortedWith(
        compareBy<StoreStop> { it.isPickedUp }
            .thenBy { !SecurityUtils.isOrderReadyForPickup(it.order) }
            .thenBy { it.distanceKm }
    )

    val currentActiveIndex = storeStops.indexOfFirst { !it.isPickedUp }
    val allStoresPickedUp = currentActiveIndex == -1
    val isEmergencyPending = trip.orders.any {
        it.status in setOf("CANCELLATION_REQUESTED", "EMERGENCY_INVESTIGATION") || it.cancellationRequested
    } || emergencyRequests.any { req ->
        req.status == "PENDING" && trip.orders.any { it.orderId == req.orderId }
    }

    Surface(
        color = TerminalSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.2.dp, if (allStoresPickedUp) EmeraldPrimary else BlueStop),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMultiStore) "🏪 ${trip.orders.size}-STORES TRIP" else "SINGLE STORE TRIP",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Earning: +${RiderEarningsCalculator.formatRupees(totalFee)} • $earningBreakdownText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = SkyInfo
                    )
                }
                StatusPill(
                    text = when {
                        allStoresPickedUp -> "IN TRANSIT • CANCEL LOCKED"
                        isPostPickupLocked -> "PICKED UP • CANCEL LOCKED"
                        else -> "PICKING STORES"
                    },
                    bgColor = if (isPostPickupLocked) EmeraldPrimary.copy(alpha = 0.2f) else BlueStop.copy(alpha = 0.2f),
                    textColor = if (isPostPickupLocked) EmeraldBright else BlueStopLight,
                    borderColor = if (isPostPickupLocked) EmeraldPrimary else BlueStop
                )
            }

            Spacer(Modifier.height(10.dp))
            val isOutForDeliveryStage = trip.orders.any { it.status == "OUT_FOR_DELIVERY" }
            TripStageProgressStepper(
                pickupCompleted = allStoresPickedUp,
                inTransitActive = isOutForDeliveryStage
            )
            Spacer(Modifier.height(12.dp))

            if (isPostPickupLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonDanger.copy(alpha = 0.16f))
                        .border(1.2.dp, CrimsonDanger, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .testTag("post_pickup_cancel_lock_banner_${trip.tripId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Cancellation Locked", tint = CrimsonLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ORDER PICKED UP — CANCEL LOCKED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimsonLight
                        )
                        Text(
                            text = "Cancellation / Pass is permanently locked after pickup. Only Admin can cancel this order.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonLight.copy(alpha = 0.9f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (isEmergencyPending) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberWarning.copy(alpha = 0.18f))
                        .border(1.dp, AmberWarning, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Emergency Request Pending: Waiting for Admin review. Do not abandon items.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberLight
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // Store Stop Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                storeStops.forEachIndexed { idx, stop ->
                    val isCurrent = !allStoresPickedUp && idx == currentActiveIndex
                    val stopPickupCode = SecurityUtils.getDisplayablePickupCode(stop.order)
                    when {
                        stop.isPickedUp -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                                    .border(1.2.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            text = "STOP ${idx + 1}: PICKED UP & CANCEL LOCKED",
                                            bgColor = Color(0xFF15803D),
                                            textColor = Color.White
                                        )
                                        if (!stopPickupCode.isNullOrBlank()) {
                                            Text(
                                                text = "Code: $stopPickupCode",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldBright
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(stop.store.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                }
                            }
                        }
                        isCurrent -> {
                            val itemsText = stop.order.items.joinToString(", ") { "${it.qty}× ${it.name}" }
                            val isReadyForPickup = SecurityUtils.isOrderReadyForPickup(stop.order)
                            val prepStatusLabel = SecurityUtils.formatOrderPrepStatus(stop.order)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isReadyForPickup) EmeraldPrimary.copy(alpha = 0.08f) else BlueStop.copy(alpha = 0.09f))
                                    .border(1.5.dp, if (isReadyForPickup) EmeraldPrimary else BlueStop, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(
                                        text = if (isReadyForPickup) {
                                            "STOP ${idx + 1}: RESTAURANT READY"
                                        } else {
                                            "STOP ${idx + 1}: WAITING FOR RESTAURANT"
                                        },
                                        bgColor = if (isReadyForPickup) Color(0xFF15803D) else AmberWarning.copy(alpha = 0.22f),
                                        textColor = if (isReadyForPickup) Color.White else AmberLight,
                                        borderColor = if (isReadyForPickup) EmeraldBright else AmberWarning
                                    )
                                    if (stop.distanceKm <= RiderMapsNavigator.MAX_DRIVABLE_DISTANCE_KM) {
                                        Text(
                                            text = "~%.1f km".format(stop.distanceKm),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BlueStopLight
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(stop.store.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(stop.store.address, style = MaterialTheme.typography.bodySmall, color = TextMuted)

                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            requestLocationAndOpenMaps(
                                                PendingMapNavigationRequest(
                                                    destLat = stop.store.lat,
                                                    destLng = stop.store.lng,
                                                    destLabelFallback = "${stop.store.name}, ${stop.store.address.ifBlank { "Gossaigaon" }}"
                                                )
                                            )
                                        },
                                        enabled = !isOpeningMap,
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueStop),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("store_maps_button_${stop.order.orderId}")
                                    ) {
                                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (isOpeningMap) "Getting GPS..." else "Store Maps",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    if (stop.store.phone.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${stop.store.phone}"))) }
                                            },
                                            border = BorderStroke(1.dp, TerminalBorder),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("Call Store", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                if (!stopPickupCode.isNullOrBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SkyInfo.copy(alpha = 0.14f))
                                            .border(1.2.dp, SkyInfo, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("store_pickup_code_card_${stop.order.orderId}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Key,
                                                        contentDescription = null,
                                                        tint = SkyInfo,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = "STORE PICKUP CODE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SkyInfo
                                                    )
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = stopPickupCode.toCharArray().joinToString(" "),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 26.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 3.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.testTag("store_pickup_code_value_${stop.order.orderId}")
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    runCatching {
                                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                        cm?.setPrimaryClip(ClipData.newPlainText("Pickup Code", stopPickupCode))
                                                        Toast.makeText(context, "Pickup Code $stopPickupCode copied!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("copy_pickup_code_${stop.order.orderId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy store pickup code",
                                                    tint = SkyInfo,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Show this 4-digit Pickup Code at store counter. Confirming pickup locks cancellation.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFBAE6FD)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SkyInfo.copy(alpha = 0.12f))
                                            .border(1.dp, SkyInfo, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "Ask Storekeeper for the 4-Digit Protected Pickup OTP when collecting bag.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7DD3FC)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Items: $itemsText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                                if (!isReadyForPickup) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AmberWarning.copy(alpha = 0.14f))
                                            .border(1.dp, AmberWarning.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .testTag("waiting_restaurant_banner_${stop.order.orderId}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "Waiting for restaurant",
                                            tint = AmberWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Restaurant is preparing ($prepStatusLabel). Pickup will unlock automatically once Restaurant marks it READY FOR PICKUP.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberLight
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("pickup_locked_waiting_restaurant_${stop.order.orderId}")
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = "Pickup locked until restaurant ready", tint = AmberWarning, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Pickup Locked • Waiting for Restaurant Ready", color = AmberWarning, style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    Button(
                                        onClick = { onOpenPickupVerify(stop.order.orderId, stop.store.name) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_pickup_button_${stop.order.orderId}")
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Restaurant Ready • Confirm Pickup", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(0.75f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, TerminalBorder, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(
                                        text = "STOP ${idx + 1}: NEXT STORE",
                                        bgColor = Color(0xFF334155),
                                        textColor = TextMuted
                                    )
                                    if (!stopPickupCode.isNullOrBlank()) {
                                        Text(
                                            text = "Pickup Code: $stopPickupCode",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SkyInfo
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(stop.store.name, style = MaterialTheme.typography.titleSmall, color = TextMuted)
                            }
                        }
                    }
                }

                // Final Customer Drop-Off Stop
                if (!allStoresPickedUp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.65f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, TerminalBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        StatusPill(
                            text = "FINAL STOP: CUSTOMER (LOCKED)",
                            bgColor = Color(0xFF334155),
                            textColor = TextMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Verify store pickup OTP first to unlock doorstep route.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                } else {
                    val distCustomer = SecurityUtils.calculateDistanceKm(riderLat, riderLng, trip.customerLat, trip.customerLng)
                    val waDigits = SecurityUtils.formatWhatsAppDigits(trip.phone)
                    val isOutForDelivery = trip.orders.any { it.status == "OUT_FOR_DELIVERY" }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.08f))
                            .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                text = "FINAL STOP: DELIVER TO CUSTOMER",
                                bgColor = Color(0xFF15803D),
                                textColor = Color.White
                            )
                            if (distCustomer != null && distCustomer <= RiderMapsNavigator.MAX_DRIVABLE_DISTANCE_KM) {
                                val etaMin = RiderAnalyticsEngine.estimateEtaMinutes(distCustomer)
                                val etaText = if (etaMin != null) " • ~${etaMin}m" else ""
                                Text(
                                    text = "~%.1f km%s".format(distCustomer, etaText),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldBright
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(trip.address, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            }
                            IconButton(
                                onClick = {
                                    runCatching {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("Delivery Address", trip.address))
                                        Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp).testTag("copy_address_button_${trip.tripId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy customer address",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    requestLocationAndOpenMaps(
                                        PendingMapNavigationRequest(
                                            destLat = trip.customerLat,
                                            destLng = trip.customerLng,
                                            destLabelFallback = trip.address,
                                            locationLink = trip.locationLink
                                        )
                                    )
                                },
                                enabled = !isOpeningMap,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("customer_maps_button_${trip.tripId}")
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isOpeningMap) "Getting GPS..." else "Maps",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (trip.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${trip.phone}"))) }
                                    },
                                    border = BorderStroke(1.dp, TerminalBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (waDigits.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val msg = Uri.encode("Namaste! Gossaigaon Bazar rider arriving with your order.")
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waDigits?text=$msg"))) }
                                    },
                                    border = BorderStroke(1.dp, EmeraldBright.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WA", color = EmeraldBright, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        val payBg = when {
                            isPrepaidVerified -> SkyInfo.copy(alpha = 0.12f)
                            isUpiPending -> AmberWarning.copy(alpha = 0.15f)
                            else -> EmeraldPrimary.copy(alpha = 0.12f)
                        }
                        val payBorder = when {
                            isPrepaidVerified -> SkyInfo
                            isUpiPending -> CrimsonDanger
                            else -> EmeraldPrimary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(payBg)
                                .border(1.dp, payBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PAYMENT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = when {
                                        isWallet -> "Wallet Paid"
                                        isUpi -> if (isPrepaidVerified) "UPI Online Paid" else "UPI Unverified"
                                        else -> "Cash on Delivery"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "₹%.0f".format(totalBill),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        if (!isOutForDelivery) {
                            Button(
                                onClick = onStartDoorstep,
                                colors = ButtonDefaults.buttonColors(containerColor = SkyInfo, contentColor = Color.Black),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("start_transit_button_${trip.tripId}")
                            ) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("START IN-TRANSIT ROUTE", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Button(
                                onClick = { onOpenDeliveryVerify(totalBill, isPrepaidVerified) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("enter_pin_button_${trip.tripId}")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ENTER CUSTOMER PIN & COMPLETE", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (allowSos) {
                    Button(
                        onClick = onOpenSos,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sos_button_${trip.tripId}")
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Emergency Help", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (!isPostPickupLocked && allowPass) {
                    OutlinedButton(
                        onClick = onOpenPassModal,
                        border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(48.dp).testTag("pass_prepickup_button_${trip.tripId}")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Pass Pre-Pickup", color = CrimsonDanger, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        border = BorderStroke(1.2.dp, CrimsonDanger.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(48.dp).testTag("cancel_locked_button_${trip.tripId}")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Cancellation locked after pickup", tint = CrimsonLight, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isPostPickupLocked) "Cancel Locked (Picked Up)" else "Cancel Locked",
                            color = CrimsonLight,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripStageProgressStepper(
    pickupCompleted: Boolean,
    inTransitActive: Boolean
) {
    val step1Done = pickupCompleted
    val step2Active = pickupCompleted && !inTransitActive
    val step2Done = inTransitActive
    val step3Active = inTransitActive

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface.copy(alpha = 0.75f))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StageNode(
            stepNumber = "1",
            label = "Pickup",
            isDone = step1Done,
            isActive = !step1Done
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .padding(horizontal = 6.dp)
                .background(if (step1Done) EmeraldBright else TerminalBorder)
        )
        StageNode(
            stepNumber = "2",
            label = "Transit",
            isDone = step2Done,
            isActive = step2Active
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .padding(horizontal = 6.dp)
                .background(if (step2Done) SkyInfo else TerminalBorder)
        )
        StageNode(
            stepNumber = "3",
            label = "Drop PIN",
            isDone = false,
            isActive = step3Active
        )
    }
}

@Composable
private fun StageNode(
    stepNumber: String,
    label: String,
    isDone: Boolean,
    isActive: Boolean
) {
    val badgeBg = when {
        isDone -> EmeraldPrimary
        isActive -> SkyInfo
        else -> TerminalSurfaceLight
    }
    val badgeBorder = when {
        isDone -> EmeraldBright
        isActive -> SkyInfo
        else -> TerminalBorder
    }
    val textColor = when {
        isDone -> EmeraldBright
        isActive -> TextPrimary
        else -> TextMuted
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(badgeBg)
                .border(1.dp, badgeBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "$label completed",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.Black else TextMuted
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

