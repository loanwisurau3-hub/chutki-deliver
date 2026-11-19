package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RiderProfile
import com.example.ui.components.FleetTextField
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ProfileScreen(
    rider: RiderProfile,
    email: String,
    onOpenKyc: () -> Unit = {},
    onSaveProfile: (name: String, phone: String, upiId: String, vehicle: String) -> Unit
) {
    var name by rememberSaveable(rider.uid) { mutableStateOf(rider.name) }
    var phone by rememberSaveable(rider.uid) { mutableStateOf(rider.phone) }
    var upiId by rememberSaveable(rider.uid) { mutableStateOf(rider.upiId) }
    var vehicle by rememberSaveable(rider.uid) { mutableStateOf(rider.vehicle) }

    LaunchedEffect(rider.uid, rider.name, rider.phone, rider.upiId, rider.vehicle) {
        if (rider.name.isNotBlank()) name = rider.name
        if (rider.phone.isNotBlank()) phone = rider.phone
        if (rider.upiId.isNotBlank()) upiId = rider.upiId
        if (rider.vehicle.isNotBlank()) vehicle = rider.vehicle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = EmeraldBright,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.section_profile),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Manage your driver details, contact number, payout UPI ID, and vehicle info.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(Modifier.height(14.dp))

        // Rider Identity & Verification Summary Card
        val kycStatus = rider.kyc.kycStatus.uppercase()
        val kycAccent = when (kycStatus) {
            "VERIFIED" -> EmeraldBright
            "REJECTED" -> CrimsonLight
            else -> AmberWarning
        }
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (rider.approved) EmeraldPrimary.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = if (rider.approved) EmeraldBright else AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (rider.uid.isNotBlank()) "Rider ID: #${rider.uid.takeLast(6).uppercase()}" else "Rider Account",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (rider.approved) "APPROVED" else "PENDING",
                        bgColor = (if (rider.approved) EmeraldBright else AmberWarning).copy(alpha = 0.16f),
                        textColor = if (rider.approved) EmeraldBright else AmberWarning,
                        borderColor = (if (rider.approved) EmeraldBright else AmberWarning).copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KYC: ${kycStatus.replace('_', ' ')}",
                        style = MaterialTheme.typography.labelMedium,
                        color = kycAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (kycStatus != "VERIFIED") {
                        OutlinedButton(
                            onClick = onOpenKyc,
                            border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("profile_open_kyc_button")
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Verify KYC", color = SkyInfo, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FleetTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Driver Full Name",
                    modifier = Modifier.testTag("profile_name_input")
                )
                Spacer(Modifier.height(12.dp))
                FleetTextField(
                    value = email.ifBlank { rider.email },
                    onValueChange = {},
                    label = "Registered Email",
                    enabled = false
                )
                Spacer(Modifier.height(12.dp))
                FleetTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() } },
                    label = "10-Digit Mobile Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.testTag("profile_phone_input")
                )
                Spacer(Modifier.height(12.dp))
                FleetTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = "Your UPI ID for Earnings Payout",
                    borderColor = SkyInfo,
                    modifier = Modifier.testTag("profile_upi_input")
                )
                Spacer(Modifier.height(12.dp))
                FleetTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = "Vehicle Information",
                    modifier = Modifier.testTag("profile_vehicle_input")
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSaveProfile(name, phone, upiId, vehicle) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Details & UPI ID", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
