package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ImageCompressUtils
import com.example.data.RiderKycInfo
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun KycVerificationCard(
    riderUid: String,
    kyc: RiderKycInfo,
    isSaving: Boolean,
    onSubmitKyc: (
        licenseNumber: String,
        vehicleRegNumber: String,
        idLast4: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        idDocumentPhoto: String,
        selfiePhoto: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var expanded by rememberSaveable(riderUid) {
        mutableStateOf(kyc.kycStatus == "NOT_SUBMITTED" || kyc.kycStatus == "REJECTED")
    }
    var licenseNo by rememberSaveable(riderUid) { mutableStateOf(kyc.licenseNumber) }
    var vehicleReg by rememberSaveable(riderUid) { mutableStateOf(kyc.vehicleRegNumber) }
    var idLast4 by rememberSaveable(riderUid) { mutableStateOf(kyc.idLast4) }
    var emergencyName by rememberSaveable(riderUid) { mutableStateOf(kyc.emergencyContactName) }
    var emergencyPhone by rememberSaveable(riderUid) { mutableStateOf(kyc.emergencyContactPhone) }
    var idDocPhoto by remember(riderUid, kyc.idDocumentPhoto) { mutableStateOf(kyc.idDocumentPhoto) }
    var selfiePhoto by remember(riderUid, kyc.selfiePhoto) { mutableStateOf(kyc.selfiePhoto) }
    var isCompressing by remember { mutableStateOf(false) }

    LaunchedEffect(riderUid, kyc.licenseNumber, kyc.vehicleRegNumber, kyc.idLast4, kyc.emergencyContactName, kyc.emergencyContactPhone) {
        if (kyc.licenseNumber.isNotBlank()) licenseNo = kyc.licenseNumber
        if (kyc.vehicleRegNumber.isNotBlank()) vehicleReg = kyc.vehicleRegNumber
        if (kyc.idLast4.isNotBlank()) idLast4 = kyc.idLast4
        if (kyc.emergencyContactName.isNotBlank()) emergencyName = kyc.emergencyContactName
        if (kyc.emergencyContactPhone.isNotBlank()) emergencyPhone = kyc.emergencyContactPhone
    }

    val idPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCompressing = true
                val encoded = ImageCompressUtils.compressUriToDataUrl(context, uri)
                if (!encoded.isNullOrBlank()) idDocPhoto = encoded
                isCompressing = false
            }
        }
    }

    val selfiePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCompressing = true
                val encoded = ImageCompressUtils.compressUriToDataUrl(context, uri)
                if (!encoded.isNullOrBlank()) selfiePhoto = encoded
                isCompressing = false
            }
        }
    }

    val (statusLabel, accent) = when (kyc.kycStatus) {
        "VERIFIED" -> "VERIFIED BY ADMIN" to EmeraldBright
        "PENDING" -> "UNDER ADMIN REVIEW" to SkyInfo
        "REJECTED" -> "ACTION REQUIRED (REJECTED)" to CrimsonLight
        else -> "NOT SUBMITTED" to AmberWarning
    }

    Surface(
        color = TerminalSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("kyc_verification_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("kyc_toggle_expand"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (kyc.kycStatus == "VERIFIED") Icons.Default.VerifiedUser else Icons.Default.Badge,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Rider KYC & Emergency Contact",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse KYC form" else "Expand KYC form",
                    tint = TextMuted
                )
            }

            if (kyc.kycNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (kyc.kycStatus == "REJECTED") CrimsonDanger.copy(alpha = 0.14f)
                            else AmberWarning.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (kyc.kycStatus == "REJECTED") CrimsonDanger else AmberWarning,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Admin KYC Note: ${kyc.kycNote}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (kyc.kycStatus == "REJECTED") CrimsonLight else AmberLight
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Submit your Driving License, Vehicle RC, ID proof & Emergency Contact for fast Admin approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(12.dp))

                    FleetTextField(
                        value = licenseNo,
                        onValueChange = { licenseNo = it },
                        label = "Driving License Number",
                        placeholder = "e.g. AS1620210012345",
                        modifier = Modifier.testTag("kyc_license_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    FleetTextField(
                        value = vehicleReg,
                        onValueChange = { vehicleReg = it },
                        label = "Vehicle Registration / RC Number",
                        placeholder = "e.g. AS-16-D-4821",
                        modifier = Modifier.testTag("kyc_vehicle_reg_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    FleetTextField(
                        value = idLast4,
                        onValueChange = { if (it.length <= 4) idLast4 = it.filter { ch -> ch.isDigit() } },
                        label = "Aadhaar / Govt ID (Last 4 Digits)",
                        placeholder = "e.g. 4829",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("kyc_id_last4_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    FleetTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        label = "Emergency Contact Name (Family)",
                        placeholder = "e.g. Raju Basumatary",
                        modifier = Modifier.testTag("kyc_emergency_name_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    FleetTextField(
                        value = emergencyPhone,
                        onValueChange = { if (it.length <= 10) emergencyPhone = it.filter { ch -> ch.isDigit() } },
                        label = "Emergency Contact 10-Digit Mobile",
                        placeholder = "9876543210",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.testTag("kyc_emergency_phone_input")
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "DOCUMENT PHOTOS (OPTIONAL — SECURE PHOTO PICKER)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                idPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isCompressing && !isSaving,
                            border = BorderStroke(1.dp, if (idDocPhoto.isNotBlank()) EmeraldPrimary else SkyInfo),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                            modifier = Modifier.weight(1f).testTag("kyc_pick_id_photo_button")
                        ) {
                            Icon(
                                imageVector = if (idDocPhoto.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = if (idDocPhoto.isNotBlank()) EmeraldBright else SkyInfo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (idDocPhoto.isNotBlank()) "DL / ID Ready" else "Upload DL / ID",
                                color = if (idDocPhoto.isNotBlank()) EmeraldBright else SkyInfo,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                selfiePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isCompressing && !isSaving,
                            border = BorderStroke(1.dp, if (selfiePhoto.isNotBlank()) EmeraldPrimary else TerminalBorder),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                            modifier = Modifier.weight(1f).testTag("kyc_pick_selfie_button")
                        ) {
                            Icon(
                                imageVector = if (selfiePhoto.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (selfiePhoto.isNotBlank()) EmeraldBright else TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (selfiePhoto.isNotBlank()) "Selfie Ready" else "Rider Photo",
                                color = if (selfiePhoto.isNotBlank()) EmeraldBright else TextPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    val idBmp = remember(idDocPhoto) { ImageCompressUtils.decodeDataUrlToBitmap(idDocPhoto) }
                    val selfieBmp = remember(selfiePhoto) { ImageCompressUtils.decodeDataUrlToBitmap(selfiePhoto) }
                    if (idBmp != null || selfieBmp != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (idBmp != null) {
                                Image(
                                    bitmap = idBmp.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_kyc_id_preview),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TerminalBg)
                                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                )
                            }
                            if (selfieBmp != null) {
                                Image(
                                    bitmap = selfieBmp.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_kyc_selfie_preview),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TerminalBg)
                                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onSubmitKyc(
                                licenseNo,
                                vehicleReg,
                                idLast4,
                                emergencyName,
                                emergencyPhone,
                                idDocPhoto,
                                selfiePhoto
                            )
                        },
                        enabled = !isSaving && !isCompressing,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("kyc_submit_button")
                    ) {
                        if (isSaving || isCompressing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (kyc.kycStatus == "NOT_SUBMITTED") "Submit KYC to Admin" else "Update & Resubmit KYC",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
