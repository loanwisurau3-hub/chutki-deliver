package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.SavedAuthPrefs
import com.example.data.SecurityUtils
import com.example.ui.components.FleetTextField
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun AuthScreen(
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    authError: String?,
    isResetLoading: Boolean,
    resetMessage: String?,
    savedAuthPrefs: SavedAuthPrefs,
    agreementVersion: String,
    onClearError: () -> Unit,
    onClearResetMessage: () -> Unit,
    onOpenAgreement: (initialTab: Int) -> Unit,
    onSignIn: (identifier: String, password: String, rememberMe: Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSendPasswordReset: (identifier: String) -> Unit,
    onRegister: (name: String, email: String, phone: String, upiId: String, vehicle: String, password: String) -> Unit
) {
    var isLoginTab by rememberSaveable { mutableStateOf(true) }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }

    var loginIdentifier by rememberSaveable { mutableStateOf(savedAuthPrefs.rememberedEmail) }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(savedAuthPrefs.rememberMe) }
    var loginPwdVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedAuthPrefs.rememberedEmail) {
        if (loginIdentifier.isBlank() && savedAuthPrefs.rememberedEmail.isNotBlank()) {
            loginIdentifier = savedAuthPrefs.rememberedEmail
        }
    }

    var regName by rememberSaveable { mutableStateOf("") }
    var regEmail by rememberSaveable { mutableStateOf("") }
    var regPhone by rememberSaveable { mutableStateOf("") }
    var regUpi by rememberSaveable { mutableStateOf("") }
    var regVehicle by rememberSaveable { mutableStateOf("") }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var regConfirmPassword by rememberSaveable { mutableStateOf("") }
    var regPwdVisible by rememberSaveable { mutableStateOf(false) }
    var regConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var regAgreementAccepted by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF071124), TerminalBg)
                )
            )
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Security Emblem Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SkyInfo.copy(alpha = 0.1f))
                    .border(1.dp, SkyInfo.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "PROTECTED RIDER AUTHENTICATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyInfo
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.16f))
                    .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = stringResource(R.string.cd_brand_icon),
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.brand_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            if (isLoginTab && savedAuthPrefs.lastRiderName.isNotBlank() && savedAuthPrefs.rememberedEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.1f))
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back, ${savedAuthPrefs.lastRiderName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldBright
                        )
                        Text(
                            text = savedAuthPrefs.rememberedEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TerminalBg)
                            .border(1.dp, TerminalBorder, RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        AuthTabButton(
                            label = stringResource(R.string.auth_sign_in),
                            selected = isLoginTab,
                            onClick = {
                                isLoginTab = true
                                onClearError()
                            },
                            modifier = Modifier.weight(1f).testTag("auth_tab_login")
                        )
                        AuthTabButton(
                            label = stringResource(R.string.auth_register_rider),
                            selected = !isLoginTab,
                            onClick = {
                                isLoginTab = false
                                onClearError()
                            },
                            modifier = Modifier.weight(1f).testTag("auth_tab_signup")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AnimatedContent(targetState = isLoginTab, label = "auth_tab_anim") { loginSelected ->
                        if (loginSelected) {
                            Column {
                                FleetTextField(
                                    value = loginIdentifier,
                                    onValueChange = { raw ->
                                        loginIdentifier = raw.filterNot { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
                                        if (!authError.isNullOrBlank()) onClearError()
                                    },
                                    label = "Email or Saved 10-Digit Mobile",
                                    placeholder = "rider@gossaigaonbazar.com or 9876543210",
                                    leadingIcon = {
                                        Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Email
                                    ),
                                    modifier = Modifier.testTag("login_email_input")
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                FleetTextField(
                                    value = loginPassword,
                                    onValueChange = { raw ->
                                        loginPassword = raw.replace("\r", "").replace("\n", "")
                                        if (!authError.isNullOrBlank()) onClearError()
                                    },
                                    label = "Password",
                                    placeholder = "••••••••",
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { loginPwdVisible = !loginPwdVisible },
                                            modifier = Modifier.testTag("login_toggle_password")
                                        ) {
                                            Icon(
                                                imageVector = if (loginPwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Password
                                    ),
                                    visualTransformation = if (loginPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.testTag("login_password_input")
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { rememberMe = !rememberMe }
                                            .padding(end = 6.dp)
                                    ) {
                                        Checkbox(
                                            checked = rememberMe,
                                            onCheckedChange = { rememberMe = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = EmeraldPrimary,
                                                uncheckedColor = TextMuted
                                            )
                                        )
                                        Text(
                                            text = "Remember me",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            onClearResetMessage()
                                            showForgotDialog = true
                                        },
                                        modifier = Modifier.testTag("forgot_password_button")
                                    ) {
                                        Text(
                                            text = "Forgot Password?",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SkyInfo
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onSignIn(loginIdentifier, loginPassword, rememberMe) },
                                    enabled = !isLoading && !isGoogleLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("login_submit_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.auth_start_shift), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        } else {
                            val phoneValid = regPhone.isEmpty() || SecurityUtils.isValidIndianMobile(regPhone)
                            val upiValid = regUpi.isEmpty() || SecurityUtils.isValidUpiId(regUpi)
                            val pwdMatch = regConfirmPassword.isEmpty() || regPassword == regConfirmPassword
                            val strength = calculatePasswordStrength(regPassword)

                            Column {
                                FleetTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    label = "Rider Full Name",
                                    placeholder = "e.g. Bipin Brahma",
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("signup_name_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FleetTextField(
                                    value = regEmail,
                                    onValueChange = { raw ->
                                        regEmail = raw.filterNot { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
                                    },
                                    label = "Email Address",
                                    placeholder = "rider@gmail.com",
                                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Email
                                    ),
                                    modifier = Modifier.testTag("signup_email_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FleetTextField(
                                    value = regPhone,
                                    onValueChange = { if (it.length <= 10) regPhone = it.filter { ch -> ch.isDigit() } },
                                    label = "10-Digit Mobile Number (+91)",
                                    placeholder = "9876543210",
                                    isError = !phoneValid,
                                    supportingText = if (!phoneValid) "Enter a valid 10-digit Indian mobile starting with 6-9" else null,
                                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.testTag("signup_phone_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FleetTextField(
                                    value = regUpi,
                                    onValueChange = { raw -> regUpi = raw.filterNot { it.isWhitespace() } },
                                    label = "Your UPI ID for Earnings Payout",
                                    placeholder = "e.g. 9876543210@paytm",
                                    isError = !upiValid,
                                    supportingText = if (!upiValid) "Format: username@bankhandle" else null,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Email
                                    ),
                                    modifier = Modifier.testTag("signup_upi_input")
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("@paytm", "@ybl", "@oksbi", "@okaxis").forEach { suffix ->
                                        QuickChip(label = suffix) {
                                            val prefix = regUpi.substringBefore("@").ifBlank { regPhone }
                                            if (prefix.isNotBlank()) regUpi = "$prefix$suffix"
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                FleetTextField(
                                    value = regVehicle,
                                    onValueChange = { regVehicle = it },
                                    label = "Vehicle Details (Bike / Scooter / Cycle)",
                                    placeholder = "Hero Splendor - AS-16-XXXX",
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("signup_vehicle_input")
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Motorcycle - AS-16-", "Scooter - AS-16-", "EV / Bicycle").forEach { preset ->
                                        QuickChip(label = preset.substringBefore(" ")) {
                                            regVehicle = preset
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                FleetTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it },
                                    label = "Set Password (min 6 chars)",
                                    placeholder = "••••••••",
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { regPwdVisible = !regPwdVisible }) {
                                            Icon(
                                                imageVector = if (regPwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Password
                                    ),
                                    visualTransformation = if (regPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.testTag("signup_password_input")
                                )
                                if (regPassword.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { strength.progress },
                                            color = strength.color,
                                            trackColor = TerminalBorder,
                                            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = strength.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = strength.color
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                FleetTextField(
                                    value = regConfirmPassword,
                                    onValueChange = { regConfirmPassword = it.replace("\r", "").replace("\n", "") },
                                    label = "Confirm Password",
                                    placeholder = "••••••••",
                                    isError = !pwdMatch,
                                    supportingText = if (!pwdMatch) "Passwords do not match" else null,
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { regConfirmVisible = !regConfirmVisible }) {
                                            Icon(
                                                imageVector = if (regConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle confirm password visibility",
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Password
                                    ),
                                    visualTransformation = if (regConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.testTag("signup_confirm_password_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                InlineLegalCheckupBox(
                                    checked = regAgreementAccepted,
                                    onCheckedChange = { regAgreementAccepted = it },
                                    agreementVersion = agreementVersion,
                                    onOpenAgreement = { onOpenAgreement(0) },
                                    onOpenPrivacy = { onOpenAgreement(1) },
                                    testTagPrefix = "signup"
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (regPassword != regConfirmPassword || !regAgreementAccepted) {
                                            return@Button
                                        }
                                        onRegister(regName, regEmail, regPhone, regUpi, regVehicle, regPassword)
                                    },
                                    enabled = !isLoading && !isGoogleLoading && regPassword == regConfirmPassword && regAgreementAccepted,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("signup_submit_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.auth_create_account), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = TerminalBorder
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = TerminalBorder
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        enabled = !isLoading && !isGoogleLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TerminalBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TerminalCard,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = EmeraldBright,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Connecting Google...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            GoogleLogoMark(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auth_continue_google),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onOpenAgreement(0) },
                            modifier = Modifier.testTag("auth_view_agreement_button")
                        ) {
                            Text("Rider Agreement (v$agreementVersion)", style = MaterialTheme.typography.labelSmall, color = SkyInfo)
                        }
                        Text("•", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        TextButton(
                            onClick = { onOpenAgreement(1) },
                            modifier = Modifier.testTag("auth_view_privacy_button")
                        ) {
                            Text("Privacy Policy", style = MaterialTheme.typography.labelSmall, color = SkyInfo)
                        }
                    }

                    if (!authError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CrimsonDanger.copy(alpha = 0.14f))
                                .border(1.dp, CrimsonDanger.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("auth_error_banner")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonLight, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = authError,
                                    color = CrimsonLight,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isLoginTab) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            onClearResetMessage()
                                            showForgotDialog = true
                                        },
                                        modifier = Modifier.testTag("error_reset_password_button")
                                    ) {
                                        Text("Reset Password", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
                                    }
                                    TextButton(
                                        onClick = {
                                            isLoginTab = false
                                            onClearError()
                                        },
                                        modifier = Modifier.testTag("error_register_rider_button")
                                    ) {
                                        Text("Register Rider", color = EmeraldBright, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            initialIdentifier = loginIdentifier,
            isLoading = isResetLoading,
            resetMessage = resetMessage,
            errorMessage = authError,
            onDismiss = {
                showForgotDialog = false
                onClearResetMessage()
            },
            onSendReset = onSendPasswordReset
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    initialIdentifier: String,
    isLoading: Boolean,
    resetMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var emailOrPhone by remember { mutableStateOf(initialIdentifier) }
    BackHandler(enabled = !isLoading) { onDismiss() }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Rider Password", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_modal), tint = TextMuted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enter your registered email address to receive a password reset link from Firebase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(14.dp))
                FleetTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    label = "Registered Email",
                    placeholder = "rider@gossaigaonbazar.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.testTag("reset_email_input")
                )

                if (!resetMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(resetMessage, style = MaterialTheme.typography.bodySmall, color = EmeraldBright)
                    }
                }
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = CrimsonLight)
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Close", color = TextPrimary)
                    }
                    Button(
                        onClick = { onSendReset(emailOrPhone) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("send_reset_button")
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Send Reset Link")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPinUnlockScreen(
    riderName: String,
    email: String,
    pinError: String?,
    onUnlock: (String) -> Unit,
    onSwitchAccount: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071124), TerminalBg)))
            .statusBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TerminalSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.16f))
                        .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Unlock Shift Terminal", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = riderName.ifBlank { email },
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldBright
                )
                if (riderName.isNotBlank() && email.isNotBlank()) {
                    Text(email, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        val digits = it.filter { ch -> ch.isDigit() }
                        if (digits.length <= 4) {
                            pin = digits
                            if (digits.length == 4) onUnlock(digits)
                        }
                    },
                    placeholder = {
                        Text("••••", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TextMuted)
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 12.sp,
                        textAlign = TextAlign.Center,
                        color = SkyInfo
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF090D16),
                        unfocusedContainerColor = Color(0xFF090D16),
                        focusedBorderColor = SkyInfo,
                        unfocusedBorderColor = TerminalBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("quick_pin_unlock_input")
                )

                if (!pinError.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(pinError, style = MaterialTheme.typography.bodySmall, color = CrimsonLight, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onUnlock(pin) },
                    enabled = pin.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("quick_pin_unlock_button")
                ) {
                    Text("Unlock Terminal", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onSwitchAccount,
                    modifier = Modifier.testTag("quick_pin_switch_account_button")
                ) {
                    Text("Sign in with Password / Switch Rider", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun GoogleOnboardingScreen(
    email: String,
    initialName: String,
    agreementVersion: String,
    isLoading: Boolean,
    authError: String?,
    onClearError: () -> Unit,
    onOpenAgreement: (initialTab: Int) -> Unit,
    onSubmit: (name: String, phone: String, upiId: String, vehicle: String) -> Unit,
    onSignOut: () -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf("") }
    var upiId by rememberSaveable { mutableStateOf("") }
    var vehicle by rememberSaveable { mutableStateOf("") }
    var agreementAccepted by rememberSaveable { mutableStateOf(false) }

    val phoneValid = phone.isEmpty() || SecurityUtils.isValidIndianMobile(phone)
    val upiValid = upiId.isEmpty() || SecurityUtils.isValidUpiId(upiId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071124), TerminalBg)))
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoogleLogoMark(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (email.isNotBlank()) "GOOGLE VERIFIED • $email" else "GOOGLE RIDER SIGN-IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldBright
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.auth_complete_onboarding),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.auth_onboarding_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FleetTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onClearError()
                        },
                        label = "Rider Full Name",
                        placeholder = "e.g. Bipin Brahma",
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("onboarding_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FleetTextField(
                        value = phone,
                        onValueChange = {
                            if (it.length <= 10) {
                                phone = it.filter { ch -> ch.isDigit() }
                                onClearError()
                            }
                        },
                        label = "10-Digit Mobile Number (+91)",
                        placeholder = "9876543210",
                        isError = !phoneValid,
                        supportingText = if (!phoneValid) "Enter a valid 10-digit Indian mobile starting with 6-9" else null,
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.testTag("onboarding_phone_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FleetTextField(
                        value = upiId,
                        onValueChange = {
                            upiId = it
                            onClearError()
                        },
                        label = "Your UPI ID for Earnings Payout",
                        placeholder = "e.g. 9876543210@paytm",
                        isError = !upiValid,
                        supportingText = if (!upiValid) "Format: username@bankhandle" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.testTag("onboarding_upi_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("@paytm", "@ybl", "@oksbi", "@okaxis").forEach { suffix ->
                            QuickChip(label = suffix) {
                                val prefix = upiId.substringBefore("@").ifBlank { phone }
                                if (prefix.isNotBlank()) upiId = "$prefix$suffix"
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    FleetTextField(
                        value = vehicle,
                        onValueChange = {
                            vehicle = it
                            onClearError()
                        },
                        label = "Vehicle Details (Bike / Scooter / Cycle)",
                        placeholder = "Hero Splendor - AS-16-XXXX",
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("onboarding_vehicle_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Motorcycle - AS-16-", "Scooter - AS-16-", "EV / Bicycle").forEach { preset ->
                            QuickChip(label = preset.substringBefore(" ")) {
                                vehicle = preset
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    InlineLegalCheckupBox(
                        checked = agreementAccepted,
                        onCheckedChange = { agreementAccepted = it },
                        agreementVersion = agreementVersion,
                        onOpenAgreement = { onOpenAgreement(0) },
                        onOpenPrivacy = { onOpenAgreement(1) },
                        testTagPrefix = "onboarding"
                    )

                    if (!authError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CrimsonDanger.copy(alpha = 0.14f))
                                .border(1.dp, CrimsonDanger.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = authError,
                                color = CrimsonLight,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onSubmit(name, phone, upiId, vehicle) },
                        enabled = !isLoading && agreementAccepted,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_onboarding_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save & Enter Terminal", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onSignOut,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_onboarding_signout_button")
                    ) {
                        Text("Use a different account", color = SkyInfo, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineLegalCheckupBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    agreementVersion: String,
    onOpenAgreement: () -> Unit,
    onOpenPrivacy: () -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) EmeraldPrimary.copy(alpha = 0.09f) else TerminalBg)
            .border(
                width = 1.dp,
                color = if (checked) EmeraldPrimary.copy(alpha = 0.55f) else TerminalBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .testTag("${testTagPrefix}_legal_checkbox")
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldPrimary,
                    uncheckedColor = TextMuted
                )
            )
            Text(
                text = "I accept the Rider Agreement (v$agreementVersion) & Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) TextPrimary else TextMuted
            )
        }
        Row(
            modifier = Modifier.padding(start = 42.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Read Agreement",
                style = MaterialTheme.typography.labelSmall,
                color = SkyInfo,
                modifier = Modifier
                    .clickable(onClick = onOpenAgreement)
                    .padding(vertical = 2.dp)
                    .testTag("${testTagPrefix}_read_agreement_link")
            )
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = SkyInfo,
                modifier = Modifier
                    .clickable(onClick = onOpenPrivacy)
                    .padding(vertical = 2.dp)
                    .testTag("${testTagPrefix}_read_privacy_link")
            )
        }
    }
}

@Composable
private fun GoogleLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)
        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -140f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue right arc + crossbar
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -5f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        val midY = size.height / 2f
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, midY),
            end = Offset(size.width - inset * 0.4f, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurfaceLight)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SkyInfo)
    }
}

@Composable
private fun AuthTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EmeraldPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else TextMuted
        )
    }
}

private data class PasswordStrength(val label: String, val progress: Float, val color: Color)

private fun calculatePasswordStrength(pwd: String): PasswordStrength {
    var score = 0
    if (pwd.length >= 6) score++
    if (pwd.length >= 8) score++
    if (pwd.any { it.isDigit() }) score++
    if (pwd.any { it.isUpperCase() } || pwd.any { !it.isLetterOrDigit() }) score++
    return when {
        score <= 1 -> PasswordStrength("Weak", 0.3f, CrimsonDanger)
        score <= 2 -> PasswordStrength("Fair", 0.6f, AmberWarning)
        else -> PasswordStrength("Strong", 1.0f, EmeraldBright)
    }
}
