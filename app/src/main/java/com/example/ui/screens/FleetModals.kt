package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AdminSettings
import com.example.data.ImageCompressUtils
import com.example.data.RiderProfile
import kotlinx.coroutines.launch
import com.example.ui.components.FleetTextField
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyDark
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.ActiveModal
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FleetModalsHost(
    activeModal: ActiveModal,
    isBusy: Boolean,
    rider: RiderProfile,
    adminSettings: AdminSettings,
    availablePayoutBalance: Double,
    netCashInHand: Double,
    pendingCodSettlementAmount: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirmPickupOtp: (orderId: String, otp: String) -> Unit,
    onConfirmDeliveryPin: (tripId: String, pin: String, deliveryProofPhoto: String?) -> Unit,
    onConfirmPassTrip: (tripId: String, reason: String, note: String) -> Unit,
    onSubmitEmergencySos: (tripId: String, reason: String, description: String) -> Unit,
    onSubmitPayout: (amount: Double, upiId: String) -> Unit,
    onSubmitCodDeposit: (amount: Double, mode: String, utr: String) -> Unit,
    onSubmitFeedback: (subject: String, message: String) -> Unit
) {
    if (activeModal is ActiveModal.None) return

    BackHandler(enabled = !isBusy) { onDismiss() }

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(horizontal = 18.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                when (activeModal) {
                    is ActiveModal.PickupVerify -> PickupVerifyModalContent(
                        orderId = activeModal.orderId,
                        storeName = activeModal.storeName,
                        pickupCode = activeModal.pickupCode,
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onConfirm = { otp -> onConfirmPickupOtp(activeModal.orderId, otp) }
                    )
                    is ActiveModal.DeliveryVerify -> DeliveryVerifyModalContent(
                        tripId = activeModal.tripId,
                        totalAmount = activeModal.totalAmount,
                        isPrepaid = activeModal.isPrepaid,
                        requireDeliveryPhoto = adminSettings.requireDeliveryPhoto,
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onConfirm = { pin, photo -> onConfirmDeliveryPin(activeModal.tripId, pin, photo) }
                    )
                    is ActiveModal.PassTrip -> PassTripModalContent(
                        tripId = activeModal.tripId,
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onConfirm = { reason, note -> onConfirmPassTrip(activeModal.tripId, reason, note) }
                    )
                    is ActiveModal.EmergencySos -> EmergencySosModalContent(
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onSubmit = { reason, desc -> onSubmitEmergencySos(activeModal.tripId, reason, desc) }
                    )
                    ActiveModal.PayoutWithdraw -> PayoutWithdrawModalContent(
                        rider = rider,
                        adminSettings = adminSettings,
                        availableBalance = availablePayoutBalance,
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onSubmit = onSubmitPayout
                    )
                    ActiveModal.CodDeposit -> CodDepositModalContent(
                        adminSettings = adminSettings,
                        netCashInHand = (netCashInHand - pendingCodSettlementAmount).coerceAtLeast(0.0),
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onSubmit = onSubmitCodDeposit
                    )
                    ActiveModal.Feedback -> FeedbackModalContent(
                        isBusy = isBusy,
                        onDismiss = onDismiss,
                        onSubmit = onSubmitFeedback
                    )
                    ActiveModal.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun ModalHeader(title: String, titleColor: Color, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = titleColor, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_modal), tint = TextMuted)
        }
    }
}

@Composable
private fun OtpInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SkyInfo.copy(alpha = 0.08f))
            .border(1.dp, SkyInfo, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SkyInfo)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 4) onValueChange(it.filter { ch -> ch.isDigit() }) },
            placeholder = {
                Text(
                    "----",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = SkyInfo.copy(alpha = 0.4f)
                )
            },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 10.sp,
                textAlign = TextAlign.Center,
                color = SkyInfo
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF090D16),
                unfocusedContainerColor = Color(0xFF090D16),
                focusedBorderColor = SkyInfo,
                unfocusedBorderColor = SkyInfo.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth().testTag(testTag)
        )
    }
}

@Composable
private fun PickupVerifyModalContent(
    orderId: String,
    storeName: String,
    pickupCode: String?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var otp by remember(orderId) { mutableStateOf("") }
    ModalHeader("Verify Store Pickup Code", SkyInfo, onDismiss)
    Spacer(Modifier.height(6.dp))
    Text("Store: $storeName • Order #${orderId.takeLast(6)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    Spacer(Modifier.height(12.dp))
    if (!pickupCode.isNullOrBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SkyInfo.copy(alpha = 0.14f))
                .border(1.2.dp, SkyInfo, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("modal_pickup_code_box"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ORDER PICKUP CODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SkyInfo
                )
                Text(
                    text = pickupCode.toCharArray().joinToString(" "),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = Color.White
                )
            }
            OutlinedButton(
                onClick = { otp = pickupCode },
                enabled = !isBusy,
                border = BorderStroke(1.dp, SkyInfo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).testTag("autofill_pickup_code_button")
            ) {
                Text("Fill Code", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CrimsonDanger.copy(alpha = 0.14f))
            .border(1.dp, CrimsonDanger, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "POST-PICKUP CANCEL LOCK: Once pickup is confirmed, order cancellation & pass are strictly locked. Only Admin can cancel after pickup.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = AmberLight
        )
    }
    Spacer(Modifier.height(14.dp))
    OtpInputBox(
        value = otp,
        onValueChange = { otp = it },
        label = "Enter 4-Digit Store Pickup Code",
        testTag = "pickup_otp_input"
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDismiss, enabled = !isBusy, modifier = Modifier.weight(1f).height(48.dp)) {
            Text("Back", color = TextPrimary)
        }
        Button(
            onClick = { onConfirm(otp) },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_pickup_otp_button")
        ) {
            if (isBusy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirm & Lock")
            }
        }
    }
}

@Composable
private fun DeliveryVerifyModalContent(
    tripId: String,
    totalAmount: Double,
    isPrepaid: Boolean,
    requireDeliveryPhoto: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (pin: String, deliveryProofPhoto: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember(tripId) { mutableStateOf("") }
    var tenderedStr by remember(tripId) { mutableStateOf("") }
    var proofPhotoDataUrl by remember(tripId) { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    val tendered = tenderedStr.toDoubleOrNull() ?: 0.0
    val change = tendered - totalAmount

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                isEncodingPhoto = true
                proofPhotoDataUrl = ImageCompressUtils.compressBitmapToDataUrl(bitmap)
                isEncodingPhoto = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isEncodingPhoto = true
                proofPhotoDataUrl = ImageCompressUtils.compressUriToDataUrl(context, uri)
                isEncodingPhoto = false
            }
        }
    }

    ModalHeader("Verify PIN & Complete Trip", TextPrimary, onDismiss)
    Text("Trip: #${tripId.takeLast(6)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    Spacer(Modifier.height(12.dp))
    OtpInputBox(
        value = pin,
        onValueChange = { pin = it },
        label = "Enter Customer's 4-Digit Delivery PIN",
        testTag = "delivery_pin_input"
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "Max 3 attempts allowed before 60s security lock.",
        style = MaterialTheme.typography.labelSmall,
        color = AmberWarning,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (requireDeliveryPhoto && proofPhotoDataUrl.isNullOrBlank()) AmberWarning else TerminalBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROOF OF DELIVERY PHOTO",
                style = MaterialTheme.typography.labelSmall,
                color = if (requireDeliveryPhoto) AmberLight else TextMuted
            )
            Text(
                text = if (requireDeliveryPhoto) "REQUIRED BY ADMIN" else "OPTIONAL",
                style = MaterialTheme.typography.labelSmall,
                color = if (requireDeliveryPhoto) AmberWarning else SkyInfo
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { runCatching { cameraLauncher.launch(null) } },
                enabled = !isBusy && !isEncodingPhoto,
                border = BorderStroke(1.dp, SkyInfo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("pod_take_photo_button")
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Camera", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !isBusy && !isEncodingPhoto,
                border = BorderStroke(1.dp, TerminalBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("pod_pick_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Gallery", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
        }
        val podBmp = remember(proofPhotoDataUrl) { ImageCompressUtils.decodeDataUrlToBitmap(proofPhotoDataUrl) }
        if (podBmp != null) {
            Spacer(Modifier.height(8.dp))
            Image(
                bitmap = podBmp.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_pod_photo_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, EmeraldPrimary, RoundedCornerShape(10.dp))
            )
        }
    }

    if (!isPrepaid) {
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EmeraldPrimary.copy(alpha = 0.12f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TOTAL COD CASH TO COLLECT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("₹%.2f".format(totalAmount), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
        }
        Spacer(Modifier.height(10.dp))
        FleetTextField(
            value = tenderedStr,
            onValueChange = { tenderedStr = it },
            label = "Cash Tendered by Customer (Calculator)",
            placeholder = "e.g. 500",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Change to Return:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                text = if (change >= 0) "₹%.2f".format(change) else "Need ₹%.2f more".format(-change),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (change >= 0) EmeraldPrimary else CrimsonDanger
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onConfirm(pin, proofPhotoDataUrl) },
        enabled = !isBusy && !isEncodingPhoto,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_delivery_pin_button")
    ) {
        if (isBusy || isEncodingPhoto) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Verify PIN & Complete Trip", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PassTripModalContent(
    tripId: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, note: String) -> Unit
) {
    val reasons = listOf(
        "Vehicle breakdown / Tyre puncture",
        "Heavy rainfall / Severe waterlogging",
        "Personal emergency / Ending shift",
        "Customer unreachable"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var note by remember { mutableStateOf("") }

    ModalHeader("Pass / Reject Pre-Pickup Trip", CrimsonDanger, onDismiss)
    Text("Trip #${tripId.takeLast(6)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CrimsonDanger.copy(alpha = 0.12f))
            .border(1.dp, CrimsonDanger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            "Pre-pickup only: Passing this trip immediately reassigns it to the next nearest online rider.",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
    Spacer(Modifier.height(10.dp))
    reasons.forEach { reason ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedReason = reason }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedReason == reason,
                onClick = { selectedReason = reason },
                colors = RadioButtonDefaults.colors(selectedColor = CrimsonDanger)
            )
            Text(reason, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }
    Spacer(Modifier.height(8.dp))
    FleetTextField(
        value = note,
        onValueChange = { note = it },
        label = "Extra Note (Optional)",
        singleLine = false,
        maxLines = 3
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
            Text("Go Back", color = TextPrimary)
        }
        Button(
            onClick = { onConfirm(selectedReason, note) },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
            modifier = Modifier.weight(1.3f).height(48.dp).testTag("confirm_pass_trip_button")
        ) {
            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("Confirm Pass")
        }
    }
}

@Composable
private fun EmergencySosModalContent(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, description: String) -> Unit
) {
    val reasons = listOf(
        "Vehicle breakdown",
        "Accident or medical emergency",
        "Customer unavailable",
        "Wrong delivery address",
        "Store or product issue",
        "Other emergency"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var desc by remember { mutableStateOf("") }

    ModalHeader("Emergency Help Request", AmberWarning, onDismiss)
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmberWarning.copy(alpha = 0.12f))
            .border(1.dp, AmberWarning, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            "Submitting Emergency Help notifies Admin immediately. Your order stays assigned & locked until Admin approves cancellation, reassignment, or return to store.",
            style = MaterialTheme.typography.bodySmall,
            color = AmberLight
        )
    }
    Spacer(Modifier.height(10.dp))
    reasons.forEach { reason ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedReason = reason }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedReason == reason,
                onClick = { selectedReason = reason },
                colors = RadioButtonDefaults.colors(selectedColor = AmberWarning)
            )
            Text(reason, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }
    Spacer(Modifier.height(8.dp))
    FleetTextField(
        value = desc,
        onValueChange = { desc = it },
        label = "Describe Situation (Optional)",
        placeholder = "Explain clearly for Admin review...",
        singleLine = false,
        maxLines = 3
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDismiss, enabled = !isBusy, modifier = Modifier.weight(1f).height(48.dp)) {
            Text("Back", color = TextPrimary)
        }
        Button(
            onClick = { onSubmit(selectedReason, desc) },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
            modifier = Modifier.weight(1.4f).height(48.dp).testTag("submit_sos_button")
        ) {
            if (isBusy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
            else {
                Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Send Emergency", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PayoutWithdrawModalContent(
    rider: RiderProfile,
    adminSettings: AdminSettings,
    availableBalance: Double,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, upiId: String) -> Unit
) {
    val defaultAmt = min(floor(availableBalance), adminSettings.maxRiderPayout).toInt().coerceAtLeast(1)
    var amountStr by remember { mutableStateOf(defaultAmt.toString()) }
    var upiId by remember { mutableStateOf(rider.upiId) }

    ModalHeader("Withdraw Earnings to UPI", SkyInfo, onDismiss)
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SkyInfo.copy(alpha = 0.1f))
            .border(1.dp, SkyInfo, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AVAILABLE PAYOUT BALANCE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text("₹%.2f".format(availableBalance), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = SkyInfo)
    }
    Spacer(Modifier.height(14.dp))
    FleetTextField(
        value = amountStr,
        onValueChange = { amountStr = it },
        label = "Withdraw Amount (Min ₹${adminSettings.minRiderPayout.toInt()} - Max ₹${adminSettings.maxRiderPayout.toInt()})",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.testTag("payout_amount_input")
    )
    Spacer(Modifier.height(12.dp))
    FleetTextField(
        value = upiId,
        onValueChange = { upiId = it },
        label = "Verify Your Receiving UPI ID",
        placeholder = "9876543210@paytm",
        modifier = Modifier.testTag("payout_upi_input")
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSubmit(amountStr.toDoubleOrNull() ?: 0.0, upiId) },
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = SkyDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_payout_button")
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Send Withdrawal Request")
        }
    }
}

@Composable
private fun CodDepositModalContent(
    adminSettings: AdminSettings,
    netCashInHand: Double,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, mode: String, utr: String) -> Unit
) {
    val context = LocalContext.current
    val defaultAmt = min(netCashInHand.roundToInt().toDouble(), adminSettings.maxRiderCodDeposit).toInt().coerceAtLeast(1)
    var amountStr by remember { mutableStateOf(defaultAmt.toString()) }
    var mode by remember { mutableStateOf("UPI") }
    var utr by remember { mutableStateOf("") }

    val parsedAmt = max(1.0, amountStr.toDoubleOrNull() ?: netCashInHand)
    val formattedAmt = String.format(java.util.Locale.US, "%.2f", parsedAmt)
    val upiDeepLink = "upi://pay?pa=${Uri.encode(adminSettings.upiId)}&pn=GossaigaonBazarAdmin&am=$formattedAmt&cu=INR"
    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${Uri.encode(upiDeepLink)}"

    ModalHeader("Deposit COD Cash to Admin", TextPrimary, onDismiss)
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EmeraldPrimary.copy(alpha = 0.1f))
            .border(1.dp, EmeraldPrimary, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("COLLECTED COD CASH TO DEPOSIT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text("₹%.2f".format(netCashInHand), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
    }
    Spacer(Modifier.height(12.dp))
    FleetTextField(
        value = amountStr,
        onValueChange = { amountStr = it },
        label = "Deposit Amount (Min ₹${adminSettings.minRiderCodDeposit.toInt()} - Max ₹${adminSettings.maxRiderCodDeposit.toInt()})",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.testTag("deposit_amount_input")
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { mode = "UPI" },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mode == "UPI") EmeraldPrimary else Color.Transparent,
                contentColor = TextPrimary
            ),
            border = if (mode == "UPI") null else BorderStroke(1.dp, TerminalBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("cod_mode_upi")
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("Pay via UPI", style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = { mode = "CASH" },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mode == "CASH") EmeraldPrimary else Color.Transparent,
                contentColor = TextPrimary
            ),
            border = if (mode == "CASH") null else BorderStroke(1.dp, TerminalBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("cod_mode_cash")
        ) {
            Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("Office Cash", style = MaterialTheme.typography.labelMedium)
        }
    }
    Spacer(Modifier.height(12.dp))

    if (mode == "UPI") {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Admin UPI ID: ${adminSettings.upiId}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SkyInfo
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = qrUrl,
                    contentDescription = stringResource(R.string.cd_qr_code),
                    modifier = Modifier.size(150.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upiDeepLink))) }
                },
                border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Open UPI App", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(12.dp))
            FleetTextField(
                value = utr,
                onValueChange = { if (it.length <= 12) utr = it.filter { ch -> ch.isDigit() } },
                label = "Enter Exact 12-Digit UPI UTR No.",
                placeholder = "e.g. 412893471923",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.testTag("deposit_utr_input")
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(14.dp)
        ) {
            Text(
                "Hand over exact collected COD cash at Gossaigaon Bazar Hub Office and submit this request for Admin confirmation.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSubmit(amountStr.toDoubleOrNull() ?: 0.0, mode, utr) },
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_cod_deposit_button")
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Submit Verified Handover")
        }
    }
}

@Composable
private fun FeedbackModalContent(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (subject: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    ModalHeader("Send Feedback / Issue to Admin", SkyInfo, onDismiss)
    Spacer(Modifier.height(12.dp))
    FleetTextField(
        value = subject,
        onValueChange = { subject = it },
        label = "Subject",
        placeholder = "e.g. Payout / Route / App Issue",
        modifier = Modifier.testTag("feedback_subject_input")
    )
    Spacer(Modifier.height(12.dp))
    FleetTextField(
        value = message,
        onValueChange = { message = it },
        label = "Message",
        placeholder = "Write details...",
        singleLine = false,
        maxLines = 4,
        modifier = Modifier.testTag("feedback_message_input")
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSubmit(subject, message) },
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_feedback_button")
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("Send to Admin Hub", style = MaterialTheme.typography.labelLarge)
    }
}
