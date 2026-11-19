package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RiderProfile
import com.example.ui.components.KycVerificationCard
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun KycScreen(
    rider: RiderProfile,
    isKycSaving: Boolean,
    onSubmitKyc: (
        licenseNumber: String,
        vehicleRegNumber: String,
        idLast4: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        idDocumentPhoto: String,
        selfiePhoto: String
    ) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("kyc_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = null,
                tint = SkyInfo,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.section_kyc),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Submit your driving license, vehicle registration, ID proof, selfie, and emergency contact for Admin verification.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(Modifier.height(14.dp))

        KycVerificationCard(
            riderUid = rider.uid,
            kyc = rider.kyc,
            isSaving = isKycSaving,
            onSubmitKyc = onSubmitKyc
        )
        Spacer(Modifier.height(24.dp))
    }
}
