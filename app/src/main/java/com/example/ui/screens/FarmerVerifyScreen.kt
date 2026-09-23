package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OTPInput
import com.example.ui.components.RuralLinkButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerVerifyScreen(
    mobileNumber: String,
    onBack: () -> Unit,
    onChangeMobile: () -> Unit,
    onVerifyOtp: (otp: String, onError: (String) -> Unit) -> Unit,
    onResendOtp: (onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>("We sent a verification code to +91 $mobileNumber.") }

    // Frontend Throttling: 60-second countdown timer for resend email / OTP trigger
    var resendCooldownSeconds by remember { mutableIntStateOf(60) }

    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000L)
            resendCooldownSeconds -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Verification", fontWeight = FontWeight.Bold, color = EmeraldGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    titleContentColor = EmeraldGreen,
                    navigationIconContentColor = EmeraldGreen
                )
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = EmeraldGreenContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhonelinkRing,
                        contentDescription = "SMS & Email Verification",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify Your Account",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDarkText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter the 6-digit verification code sent to your registered contact.",
                fontSize = 13.5.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "+91 $mobileNumber",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkText
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onChangeMobile,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("change_mobile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change mobile number",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frontend Throttling Banner & Timer
            AnimatedVisibility(visible = resendCooldownSeconds > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrightLeafLight),
                    border = BorderStroke(1.dp, BrightLeaf.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("resend_cooldown_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Cooldown Timer",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rate Limit Protection Active",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreen
                            ) {
                                Text(
                                    text = "${resendCooldownSeconds}s",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To prevent spamming email triggers and hitting Supabase rate limits, you can request another email/code in $resendCooldownSeconds seconds.",
                            fontSize = 11.5.sp,
                            color = PrimaryDarkText,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (60 - resendCooldownSeconds) / 60f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = EmeraldGreen,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Demo Mode Helper Card
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = "Demo Mode",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Supabase Auth Test Environment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = EmeraldGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Standard verification code: 123456. Connected directly to Supabase Auth.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            otpValue = "123456"
                            errorMessage = null
                        },
                        modifier = Modifier.testTag("autofill_otp_button")
                    ) {
                        Text("⚡ Auto-Fill Demo Code (123456)", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OTPInput(
                otpValue = otpValue,
                onOtpChange = {
                    otpValue = it
                    errorMessage = null
                },
                testTag = "verify_otp_input"
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            if (successMessage != null && errorMessage == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = successMessage ?: "",
                    color = EmeraldGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RuralLinkButton(
                text = if (isVerifying) "Verifying Code..." else "Verify & Continue",
                onClick = {
                    if (otpValue.isBlank() || otpValue.length < 6) {
                        errorMessage = "Please enter the 6-digit verification code."
                    } else {
                        isVerifying = true
                        errorMessage = null
                        onVerifyOtp(otpValue) { err ->
                            isVerifying = false
                            errorMessage = err
                        }
                    }
                },
                icon = Icons.Default.CheckCircle,
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth(),
                testTag = "verify_submit_button"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resend Email / Code button with immediate disabling and 60s countdown throttling
                OutlinedButton(
                    onClick = {
                        if (resendCooldownSeconds == 0 && !isResending) {
                            isResending = true
                            // Immediately start 60s cooldown and disable button
                            resendCooldownSeconds = 60
                            onResendOtp(
                                { msg ->
                                    isResending = false
                                    successMessage = msg
                                    errorMessage = null
                                    otpValue = ""
                                },
                                { err ->
                                    isResending = false
                                    errorMessage = err
                                    // Keep cooldown active even on error to prevent spamming
                                    if (resendCooldownSeconds <= 0) {
                                        resendCooldownSeconds = 60
                                    }
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = resendCooldownSeconds == 0 && !isResending,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmeraldGreen,
                        disabledContentColor = SecondaryText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("resend_otp_button")
                ) {
                    if (isResending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = EmeraldGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sending...", fontSize = 12.sp)
                    } else if (resendCooldownSeconds > 0) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resend (${resendCooldownSeconds}s)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend Email", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = onChangeMobile,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("change_phone_text_button")
                ) {
                    Text("Change Details", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            com.example.ui.components.SupabaseSyncFooter(
                statusText = "Verification status recorded to Supabase farmer profile"
            )
        }
    }
}

