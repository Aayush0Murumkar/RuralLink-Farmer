package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.AadhaarUtils
import com.example.services.ValidationUtils
import com.example.ui.components.PhoneInput
import com.example.ui.components.RuralLinkButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerRegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: (name: String, address: String, mobile: String, aadhaar: String, email: String, password: String) -> Unit,
    onSignInSuccess: ((email: String, password: String) -> Unit)? = null,
    errorMessageExternal: String? = null
) {
    var isSignUpMode by remember { mutableStateOf(true) }

    var fullName by remember { mutableStateOf("Ramesh Patil") }
    var address by remember { mutableStateOf("Pimplgaon Baswant, Nashik District") }
    var mobileNumber by remember { mutableStateOf("9876543210") }
    var aadhaarNumber by remember { mutableStateOf("999988881234") }
    var email by remember { mutableStateOf("ramesh.farmer@gmail.com") }
    var password by remember { mutableStateOf("rural123") }
    var showPassword by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showTestCasePanel by remember { mutableStateOf(false) }

    // Frontend Throttling: 60-second cooldown timer to protect email triggers from hitting Supabase limits
    var signUpCooldownSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(signUpCooldownSeconds) {
        if (signUpCooldownSeconds > 0) {
            delay(1000L)
            signUpCooldownSeconds -= 1
        }
    }

    LaunchedEffect(errorMessageExternal) {
        if (errorMessageExternal != null) {
            isSubmitting = false
            // If rate limit error received from server, activate or extend the 60s cooldown
            val isRateLimit = errorMessageExternal.contains("rate limit", ignoreCase = true) ||
                    errorMessageExternal.contains("over_email_send_rate_limit", ignoreCase = true) ||
                    errorMessageExternal.contains("too many", ignoreCase = true) ||
                    errorMessageExternal.contains("429")
            if (isRateLimit && signUpCooldownSeconds <= 0) {
                signUpCooldownSeconds = 60
            }
        }
    }

    val rawError = errorMessageExternal ?: errorMessage
    val displayedError = remember(rawError) {
        if (rawError == null) null
        else if (rawError.contains("email_address_invalid", ignoreCase = true) || (rawError.contains("Email address", ignoreCase = true) && rawError.contains("invalid", ignoreCase = true))) {
            "This email domain is restricted or invalid for Supabase Auth. Please use a valid email address (e.g. ramesh.farmer@gmail.com)."
        } else if (rawError.contains("over_email_send_rate_limit", ignoreCase = true) || rawError.contains("rate limit", ignoreCase = true)) {
            "Email rate limit reached. Please wait a minute before requesting another confirmation email."
        } else if (rawError.contains("Invalid login credentials", ignoreCase = true)) {
            "Invalid email or password. Please check your credentials and try again."
        } else {
            rawError
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSignUpMode) "Farmer Registration" else "Farmer Login",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldGreen
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTestCasePanel = !showTestCasePanel }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test Presets",
                            tint = EmeraldGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    titleContentColor = EmeraldGreen,
                    navigationIconContentColor = EmeraldGreen,
                    actionIconContentColor = EmeraldGreen
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Mode Switch Segmented Control (Sign Up vs Sign In)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EmeraldGreenContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Surface(
                        onClick = {
                            isSignUpMode = true
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSignUpMode) EmeraldGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_account_tab")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Create Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isSignUpMode) Color.White else EmeraldGreen
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            isSignUpMode = false
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isSignUpMode) EmeraldGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sign_in_tab")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sign In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (!isSignUpMode) Color.White else EmeraldGreen
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isSignUpMode) "Create Your Farmer Account" else "Welcome Back, Farmer",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDarkText
            )
            Text(
                text = if (isSignUpMode)
                    "Register with Supabase Auth to start booking agricultural transport."
                else
                    "Sign in to your Supabase authenticated RuralLink account.",
                fontSize = 13.5.sp,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Test Case QA Quick Presets (Collapsible)
            AnimatedVisibility(visible = showTestCasePanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🧪 QA Test Presets & Auth Scenarios",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    isSignUpMode = true
                                    fullName = ""
                                    address = ""
                                    mobileNumber = ""
                                    aadhaarNumber = ""
                                    email = ""
                                    password = ""
                                    errorMessage = null
                                },
                                label = { Text("1. Empty Form", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    isSignUpMode = true
                                    mobileNumber = "12345"
                                    errorMessage = null
                                },
                                label = { Text("2. Bad Mobile", fontSize = 11.sp) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    email = "invalid_email@"
                                    errorMessage = null
                                },
                                label = { Text("3. Bad Email", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    password = "123"
                                    errorMessage = null
                                },
                                label = { Text("4. Short Pass (<6)", fontSize = 11.sp) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    isSignUpMode = true
                                    fullName = "Ramesh Patil"
                                    address = "Pimplgaon Baswant, Nashik District"
                                    mobileNumber = "9876543210"
                                    aadhaarNumber = "999988881234"
                                    email = "ramesh.farmer@gmail.com"
                                    password = "rural123"
                                    errorMessage = null
                                },
                                label = { Text("5. Valid Demo Farmer", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = !isSignUpMode,
                                onClick = {
                                    isSignUpMode = !isSignUpMode
                                    errorMessage = null
                                },
                                label = { Text(if (isSignUpMode) "Switch to Sign In" else "Switch to Register", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            if (isSignUpMode) {
                // Supabase Privacy Notice Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrightLeafLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrightLeaf.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy Shield",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Supabase Auth & Privacy Protected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "Passwords and contact details are securely managed by Supabase Auth with Row Level Security.",
                                fontSize = 12.sp,
                                color = PrimaryDarkText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        errorMessage = null
                    },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = EmeraldGreen) },
                    singleLine = true,
                    isError = displayedError == "Please enter your full name." || displayedError?.contains("full name") == true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        errorMessage = null
                    },
                    label = { Text("Farm Address / Village / Mandi *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = EmeraldGreen) },
                    singleLine = true,
                    isError = displayedError == "Please enter your address." || displayedError?.contains("address") == true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone Input
                PhoneInput(
                    value = mobileNumber,
                    onValueChange = {
                        mobileNumber = it
                        errorMessage = null
                    },
                    testTag = "mobile_input"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Aadhaar
                OutlinedTextField(
                    value = aadhaarNumber,
                    onValueChange = { input ->
                        if (input.length <= 12 && input.all { it.isDigit() }) {
                            aadhaarNumber = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Aadhaar Number (12 Digits - Demo)") },
                    placeholder = { Text("999988881234") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = EmeraldGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = displayedError?.contains("Aadhaar") == true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aadhaar_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Email Field (Used for both Sign Up and Sign In)
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text(if (isSignUpMode) "Email Address *" else "Account Email *") },
                leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = EmeraldGreen) },
                singleLine = true,
                isError = displayedError?.contains("email", ignoreCase = true) == true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text(if (isSignUpMode) "Password (min 6 characters) *" else "Password *") },
                leadingIcon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = EmeraldGreen) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = EmeraldGreen
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = displayedError?.contains("password", ignoreCase = true) == true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input")
            )

            if (displayedError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("error_message_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = Color(0xFFC62828),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayedError,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.testTag("error_message_text")
                        )
                    }
                }
            }

            // Frontend Throttling: 60-second Countdown Timer UI Banner
            AnimatedVisibility(visible = isSignUpMode && signUpCooldownSeconds > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrightLeafLight),
                    border = BorderStroke(1.dp, BrightLeaf.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_cooldown_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Email Trigger Throttle",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Email Rate Limit Protection",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreen
                            ) {
                                Text(
                                    text = "${signUpCooldownSeconds}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To prevent spamming email triggers and hitting Supabase send limits, please wait ${signUpCooldownSeconds}s before sending another confirmation request.",
                            fontSize = 12.sp,
                            color = PrimaryDarkText,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (60 - signUpCooldownSeconds) / 60f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EmeraldGreen,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val isSignUpThrottled = isSignUpMode && signUpCooldownSeconds > 0
            val isButtonEnabled = !isSubmitting && !isSignUpThrottled

            RuralLinkButton(
                text = when {
                    isSubmitting -> if (isSignUpMode) "Creating Account..." else "Signing In..."
                    isSignUpThrottled -> "Sign Up (Wait ${signUpCooldownSeconds}s)"
                    isSignUpMode -> "Create Account & Continue"
                    else -> "Sign In to RuralLink"
                },
                onClick = {
                    if (isSignUpMode) {
                        // Sign up validation
                        if (!ValidationUtils.isValidName(fullName)) {
                            errorMessage = "Please enter your full name."
                            return@RuralLinkButton
                        }
                        if (!ValidationUtils.isValidAddress(address)) {
                            errorMessage = "Please enter your address."
                            return@RuralLinkButton
                        }
                        if (!ValidationUtils.isValidMobile(mobileNumber)) {
                            errorMessage = "Please enter a valid 10-digit mobile number."
                            return@RuralLinkButton
                        }
                        if (email.isBlank() || !ValidationUtils.isValidEmail(email)) {
                            errorMessage = "Please enter a valid email address."
                            return@RuralLinkButton
                        }
                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters long."
                            return@RuralLinkButton
                        }
                        if (aadhaarNumber.isNotBlank() && !AadhaarUtils.isValidAadhaar(aadhaarNumber)) {
                            errorMessage = "Please enter a valid 12-digit Aadhaar number."
                            return@RuralLinkButton
                        }

                        // Immediately disable button and engage 60-second cooldown
                        errorMessage = null
                        isSubmitting = true
                        signUpCooldownSeconds = 60
                        onRegisterSuccess(
                            fullName.trim(),
                            address.trim(),
                            mobileNumber.filter { it.isDigit() },
                            aadhaarNumber,
                            email.trim(),
                            password
                        )
                    } else {
                        // Sign in validation
                        if (email.isBlank() || !ValidationUtils.isValidEmail(email)) {
                            errorMessage = "Please enter a valid email address."
                            return@RuralLinkButton
                        }
                        if (password.isBlank()) {
                            errorMessage = "Please enter your password."
                            return@RuralLinkButton
                        }

                        errorMessage = null
                        isSubmitting = true
                        if (onSignInSuccess != null) {
                            onSignInSuccess(email.trim(), password)
                        } else {
                            onRegisterSuccess(
                                fullName.trim(),
                                address.trim(),
                                mobileNumber.filter { it.isDigit() },
                                aadhaarNumber,
                                email.trim(),
                                password
                            )
                        }
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth(),
                testTag = "register_continue_button"
            )

            if (isSubmitting) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = EmeraldGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSignUpMode) "Registering with Supabase Auth..." else "Authenticating session...",
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch Mode Link
            TextButton(
                onClick = {
                    isSignUpMode = !isSignUpMode
                    errorMessage = null
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isSignUpMode)
                        "Already have an account? Sign In"
                    else
                        "New to RuralLink? Create Account",
                    color = EmeraldGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            com.example.ui.components.SupabaseSyncFooter(
                statusText = "Secured with Supabase GoTrue Auth & RLS"
            )
        }
    }
}
