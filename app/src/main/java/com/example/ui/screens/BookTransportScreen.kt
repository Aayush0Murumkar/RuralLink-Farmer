package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransportRequest
import com.example.services.VoiceService
import com.example.ui.components.RuralLinkButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoiceBookingDialog
import com.example.ui.components.VoiceBookingOrb
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class BookingStep {
    METHOD_SELECTION,
    MANUAL_FORM,
    REVIEW,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookTransportScreen(
    initialVoiceData: VoiceService.VoiceParsedRequest? = null,
    onBack: () -> Unit,
    onViewRequestsClick: () -> Unit,
    onBackToDashboardClick: () -> Unit,
    onSubmitRequest: suspend (
        materialType: String,
        weight: Double,
        unit: String,
        pickup: String,
        drop: String,
        date: String,
        time: String,
        notes: String
    ) -> TransportRequest?
) {
    var currentStep by remember {
        mutableStateOf(if (initialVoiceData != null) BookingStep.MANUAL_FORM else BookingStep.METHOD_SELECTION)
    }

    // Form Field States
    var pickupPoint by remember { mutableStateOf(initialVoiceData?.pickupPoint ?: "Kopargaon Farm Yard") }
    var dropPoint by remember { mutableStateOf(initialVoiceData?.dropPoint ?: "Nashik APMC Mandi") }
    var weightInput by remember { mutableStateOf(initialVoiceData?.weight?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "20") }
    var weightUnit by remember { mutableStateOf(initialVoiceData?.weightUnit ?: "quintal") }
    var selectedMaterial by remember { mutableStateOf(initialVoiceData?.materialType ?: "Onion") }
    var customMaterial by remember { mutableStateOf("") }
    var requiredDate by remember { mutableStateOf(initialVoiceData?.requiredDate ?: "12 August 2026") }
    var requiredTime by remember { mutableStateOf(initialVoiceData?.requiredTime ?: "09:00 AM") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var createdRequest by remember { mutableStateOf<TransportRequest?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialVoiceData) {
        if (initialVoiceData != null) {
            currentStep = BookingStep.MANUAL_FORM
            pickupPoint = initialVoiceData.pickupPoint ?: "Kopargaon Farm Yard"
            dropPoint = initialVoiceData.dropPoint ?: "Nashik APMC Mandi"
            weightInput = initialVoiceData.weight?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "20"
            weightUnit = initialVoiceData.weightUnit ?: "quintal"
            selectedMaterial = initialVoiceData.materialType ?: "Onion"
            requiredDate = initialVoiceData.requiredDate ?: "12 August 2026"
            requiredTime = initialVoiceData.requiredTime ?: "09:00 AM"
        }
    }

    val materialOptions = listOf(
        "Wheat", "Rice", "Onion", "Potato", "Tomato",
        "Sugarcane", "Grapes", "Fruits", "Vegetables", "Other"
    )
    val unitOptions = listOf("kg", "quintal", "tonne")
    val quickDates = listOf("Today (11 Aug)", "12 August 2026", "13 August 2026", "14 August 2026")
    val quickTimes = listOf("07:00 AM", "09:00 AM", "01:00 PM", "05:00 PM", "08:00 PM")

    val effectiveMaterial = if (selectedMaterial == "Other") customMaterial.trim() else selectedMaterial

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            BookingStep.METHOD_SELECTION -> "Book Transport"
                            BookingStep.MANUAL_FORM -> "Book Transport"
                            BookingStep.REVIEW -> "Review Request"
                            BookingStep.SUCCESS -> "Request Created"
                        },
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                },
                navigationIcon = {
                    if (currentStep != BookingStep.SUCCESS) {
                        IconButton(onClick = {
                            when (currentStep) {
                                BookingStep.REVIEW -> currentStep = BookingStep.MANUAL_FORM
                                BookingStep.MANUAL_FORM -> {
                                    if (initialVoiceData == null) {
                                        currentStep = BookingStep.METHOD_SELECTION
                                    } else {
                                        onBack()
                                    }
                                }
                                BookingStep.METHOD_SELECTION -> onBack()
                                BookingStep.SUCCESS -> onBackToDashboardClick()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = EmeraldGreen
                            )
                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (currentStep) {

                // ==================== 1. BOOKING METHOD SELECTION SCREEN ====================
                BookingStep.METHOD_SELECTION -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Choose Booking Method",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select how you would like to book your transport request",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // MANUAL BOOKING CARD (Elevated White Card with Emerald Green line art)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentStep = BookingStep.MANUAL_FORM }
                            .testTag("method_manual_card")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "MANUAL BOOKING",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = "Enter Details Manually",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDarkText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Fill in your pickup, dropoff, produce weight and schedule step-by-step.",
                                fontSize = 13.sp,
                                color = SecondaryText,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            RuralLinkButton(
                                text = "Enter Manually",
                                onClick = { currentStep = BookingStep.MANUAL_FORM },
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "method_manual_button"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // VOICE BOOKING CARD WITH CIRCULAR AI ORB
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVoiceDialog = true }
                            .testTag("method_voice_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Circular AI Voice Orb Hero
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .clickable { showVoiceDialog = true }
                            ) {
                                VoiceBookingOrb(
                                    size = 88.dp,
                                    isListening = false,
                                    showMicIcon = true,
                                    onClick = { showVoiceDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryGreenContainer.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI VOICE ASSISTANT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryGreenDark,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Tap Orb to Speak",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Speak naturally in Marathi, Hindi, or English to auto-fill booking details.",
                                fontSize = 12.5.sp,
                                color = SecondaryText,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            RuralLinkButton(
                                text = "Start Voice Booking",
                                onClick = { showVoiceDialog = true },
                                icon = Icons.Default.Mic,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "method_voice_button"
                            )
                        }
                    }
                }

                // ==================== 2. MANUAL BOOKING FORM ====================
                BookingStep.MANUAL_FORM -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Transport Details",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "Provide route, material specifications, and timing.",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Scenarios Quick Bar
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Test Presets",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            pickupPoint = "Nashik APMC Mandi Yard, Gate 2"
                                            dropPoint = "Vashi Wholesale Market, Navi Mumbai"
                                            weightInput = "45"
                                            weightUnit = "quintal"
                                            selectedMaterial = "Tomato"
                                            requiredDate = "28 August 2026"
                                            requiredTime = "08:00 AM"
                                            notes = "Dummy request test payload"
                                            errorMessage = null
                                        },
                                        label = { Text("Fill Dummy", fontSize = 11.sp) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            if (!isSubmitting) {
                                                coroutineScope.launch {
                                                    isSubmitting = true
                                                    errorMessage = null
                                                    try {
                                                        val req = onSubmitRequest(
                                                            "Fresh Tomatoes & Onions",
                                                            45.0,
                                                            "quintal",
                                                            "Nashik APMC Mandi Yard, Gate 2",
                                                            "Vashi Wholesale Market, Navi Mumbai",
                                                            "28 August 2026",
                                                            "08:00 AM",
                                                            "Dummy transport request registered in Supabase backend."
                                                        )
                                                        if (req != null) {
                                                            createdRequest = req
                                                            currentStep = BookingStep.SUCCESS
                                                        } else {
                                                            errorMessage = "Failed to register dummy request in Supabase."
                                                        }
                                                    } catch (e: Exception) {
                                                        val msg = e.message ?: "Registration error"
                                                        errorMessage = msg
                                                        android.util.Log.e("BookTransportScreen", "Dummy registration error: $msg", e)
                                                    } finally {
                                                        isSubmitting = false
                                                    }
                                                }
                                            }
                                        },
                                        label = { Text("🚀 Submit Dummy to Supabase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PICKUP POINT
                        Text(
                            text = "Pickup Point *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pickupPoint,
                            onValueChange = {
                                pickupPoint = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = { Text("e.g. Kopargaon, Farm Yard, Gate 2") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pickup_location_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // DROP POINT
                        Text(
                            text = "Drop Point *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dropPoint,
                            onValueChange = {
                                dropPoint = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = { Text("e.g. Nashik APMC Wholesale Mandi") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = EmeraldGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drop_location_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // MATERIAL WEIGHT & UNIT
                        Text(
                            text = "Material Weight *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = {
                                    weightInput = it
                                    if (errorMessage != null) errorMessage = null
                                },
                                label = { Text("Quantity") },
                                placeholder = { Text("e.g. 20") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weight_input")
                            )

                            var unitExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = unitExpanded,
                                onExpandedChange = { unitExpanded = !unitExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = weightUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unit") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = CardBorder,
                                        focusedContainerColor = CardBackground,
                                        unfocusedContainerColor = CardBackground
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("unit_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = unitExpanded,
                                    onDismissRequest = { unitExpanded = false }
                                ) {
                                    unitOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                weightUnit = opt
                                                unitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // MATERIAL TYPE
                        Text(
                            text = "What are you transporting? *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        var materialExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = materialExpanded,
                            onExpandedChange = { materialExpanded = !materialExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedMaterial,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Material Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("material_type_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = materialExpanded,
                                onDismissRequest = { materialExpanded = false }
                            ) {
                                materialOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            selectedMaterial = opt
                                            materialExpanded = false
                                            if (errorMessage != null) errorMessage = null
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedMaterial == "Other") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customMaterial,
                                onValueChange = {
                                    customMaterial = it
                                    if (errorMessage != null) errorMessage = null
                                },
                                label = { Text("Enter material type *") },
                                placeholder = { Text("e.g. Cotton Bales / Fertilizer") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_material_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // DATE
                        Text(
                            text = "When do you need the transport? *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = requiredDate,
                            onValueChange = {
                                requiredDate = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Required Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("date_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickDates.forEach { qd ->
                                FilterChip(
                                    selected = requiredDate == qd,
                                    onClick = { requiredDate = qd },
                                    label = { Text(qd, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrightLeafLight,
                                        selectedLabelColor = EmeraldGreen
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // TIME
                        Text(
                            text = "Required Time *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = requiredTime,
                            onValueChange = {
                                requiredTime = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Pickup Time") },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("time_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickTimes.forEach { qt ->
                                FilterChip(
                                    selected = requiredTime == qt,
                                    onClick = { requiredTime = qt },
                                    label = { Text(qt, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrightLeafLight,
                                        selectedLabelColor = EmeraldGreen
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Additional Notes (Optional)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Special Requirements / Notes (Optional)") },
                            placeholder = { Text("e.g. Perishable item, waterproof tarpaulin required") },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_input")
                        )

                        // Validation Error Display
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // REVIEW BUTTON
                        RuralLinkButton(
                            text = "Review Request",
                            onClick = {
                                val wVal = weightInput.toDoubleOrNull()
                                when {
                                    pickupPoint.isBlank() -> {
                                        errorMessage = "Please enter where the material should be picked up."
                                    }
                                    dropPoint.isBlank() -> {
                                        errorMessage = "Please enter where the material should be delivered."
                                    }
                                    weightInput.isBlank() -> {
                                        errorMessage = "Please enter the material weight."
                                    }
                                    wVal == null || wVal <= 0 -> {
                                        errorMessage = "Weight must be greater than zero."
                                    }
                                    selectedMaterial.isBlank() -> {
                                        errorMessage = "Please select what you are transporting."
                                    }
                                    selectedMaterial == "Other" && customMaterial.isBlank() -> {
                                        errorMessage = "Please enter material type."
                                    }
                                    requiredDate.isBlank() -> {
                                        errorMessage = "Please select when you need the transport."
                                    }
                                    requiredTime.isBlank() -> {
                                        errorMessage = "Please select when you need the transport."
                                    }
                                    else -> {
                                        errorMessage = null
                                        currentStep = BookingStep.REVIEW
                                    }
                                }
                            },
                            icon = Icons.Default.RateReview,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "review_request_button"
                        )
                    }
                }

                // ==================== 3. REVIEW SCREEN ====================
                BookingStep.REVIEW -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Review Request",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "Please verify your transport details before submitting.",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Review Details Card (Elevated White Card)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {

                                ReviewDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    iconTint = EmeraldGreen,
                                    label = "Pickup Point",
                                    value = pickupPoint
                                )

                                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                                ReviewDetailRow(
                                    icon = Icons.Default.Place,
                                    iconTint = EmeraldGreen,
                                    label = "Drop Point",
                                    value = dropPoint
                                )

                                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                                ReviewDetailRow(
                                    icon = Icons.Default.Category,
                                    iconTint = EmeraldGreen,
                                    label = "Material",
                                    value = effectiveMaterial
                                )

                                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                                ReviewDetailRow(
                                    icon = Icons.Default.Scale,
                                    iconTint = EmeraldGreen,
                                    label = "Weight",
                                    value = "$weightInput $weightUnit"
                                )

                                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                                ReviewDetailRow(
                                    icon = Icons.Default.CalendarToday,
                                    iconTint = EmeraldGreen,
                                    label = "Required Date",
                                    value = requiredDate
                                )

                                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                                ReviewDetailRow(
                                    icon = Icons.Default.Schedule,
                                    iconTint = EmeraldGreen,
                                    label = "Required Time",
                                    value = requiredTime
                                )

                                if (notes.isNotBlank()) {
                                    HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))
                                    ReviewDetailRow(
                                        icon = Icons.AutoMirrored.Filled.Notes,
                                        iconTint = SecondaryText,
                                        label = "Special Instructions",
                                        value = notes
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Review Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = BookingStep.MANUAL_FORM },
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("edit_details_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Details", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }

                            Button(
                                onClick = {
                                    if (!isSubmitting) {
                                        isSubmitting = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            try {
                                                val wVal = weightInput.toDoubleOrNull() ?: 1.0
                                                val result = onSubmitRequest(
                                                    effectiveMaterial,
                                                    wVal,
                                                    weightUnit,
                                                    pickupPoint,
                                                    dropPoint,
                                                    requiredDate,
                                                    requiredTime,
                                                    notes
                                                )
                                                isSubmitting = false
                                                if (result != null) {
                                                    createdRequest = result
                                                    currentStep = BookingStep.SUCCESS
                                                } else {
                                                    errorMessage = "An unexpected error occurred. Request was not created."
                                                }
                                            } catch (e: Exception) {
                                                isSubmitting = false
                                                val detailedMsg = if (e is com.example.services.SupabaseApiException) {
                                                    "Supabase Error (HTTP ${e.httpStatusCode}${if (!e.errorCode.isNullOrBlank()) ", Code: ${e.errorCode}" else ""}): ${e.serverMessage}"
                                                } else {
                                                    e.message ?: "Something went wrong while creating your request."
                                                }
                                                android.util.Log.e("BookTransportScreen", "Booking failed: $detailedMsg", e)
                                                errorMessage = detailedMsg
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("confirm_request_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = BrightLeaf,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrightLeaf)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm Request", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // ==================== 4. SUCCESS SCREEN ====================
                BookingStep.SUCCESS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BrightLeafContainer)
                                .border(2.dp, BrightLeaf, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Transport Request Created",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Your request has been submitted successfully to nearby transporters.",
                            fontSize = 13.5.sp,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Request Info Card (Elevated White Card)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "REQUEST ID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryText,
                                    letterSpacing = 0.8.sp
                                )
                                val displayId = remember(createdRequest) {
                                    val req = createdRequest
                                    if (req == null) "RL-REQ-0001"
                                    else if (req.id.startsWith("RL-REQ")) req.id
                                    else {
                                        val regex = "RL-REQ-\\d{4}".toRegex()
                                        regex.find(req.notes)?.value ?: "RL-REQ-${req.id.take(4).uppercase()}"
                                    }
                                }
                                Text(
                                    text = displayId,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    StatusBadge(status = createdRequest?.status ?: "SEARCHING")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Finding suitable transporters near you",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryText
                                )

                                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(vertical = 16.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Route:", fontSize = 13.sp, color = SecondaryText)
                                        Text("$pickupPoint ➔ $dropPoint", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkText)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Material & Weight:", fontSize = 13.sp, color = SecondaryText)
                                        Text("$effectiveMaterial, $weightInput $weightUnit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkText)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Schedule:", fontSize = 13.sp, color = SecondaryText)
                                        Text("$requiredDate at $requiredTime", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkText)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Success Actions
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RuralLinkButton(
                                text = "View Request",
                                onClick = onViewRequestsClick,
                                icon = Icons.Default.Visibility,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "view_created_request_button"
                            )

                            OutlinedButton(
                                onClick = onBackToDashboardClick,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("success_back_dashboard_button")
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Back to Dashboard", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val syncStatusText = remember(createdRequest) {
                                val notes = createdRequest?.notes ?: ""
                                when {
                                    notes.contains("[Supabase Synced]") -> "Saved to Supabase public.transport_requests"
                                    notes.contains("RLS Policy active") -> "Saved to local Room DB (Supabase RLS policy requires authenticated session)"
                                    else -> "Saved locally in Room DB & queued for Supabase sync"
                                }
                            }
                            com.example.ui.components.SupabaseSyncFooter(
                                statusText = syncStatusText
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVoiceDialog) {
        VoiceBookingDialog(
            onDismiss = { showVoiceDialog = false },
            onConfirmBooking = { voiceReq ->
                showVoiceDialog = false
                val mType = voiceReq.materialType ?: "Onion"
                val wVal = voiceReq.weight ?: 20.0
                val wUnit = voiceReq.weightUnit ?: "quintal"
                val pPoint = voiceReq.pickupPoint ?: "Kopargaon"
                val dPoint = voiceReq.dropPoint ?: "Nashik"
                val rDate = voiceReq.requiredDate ?: "12 August 2026"
                val rTime = voiceReq.requiredTime ?: "09:00 AM"

                selectedMaterial = mType
                weightInput = if (wVal % 1 == 0.0) wVal.toInt().toString() else wVal.toString()
                weightUnit = wUnit
                pickupPoint = pPoint
                dropPoint = dPoint
                requiredDate = rDate
                requiredTime = rTime

                coroutineScope.launch {
                    try {
                        val result = onSubmitRequest(
                            mType,
                            wVal,
                            wUnit,
                            pPoint,
                            dPoint,
                            rDate,
                            rTime,
                            notes
                        )
                        if (result != null) {
                            createdRequest = result
                            currentStep = BookingStep.SUCCESS
                        }
                    } catch (e: Exception) {
                        val detailedMsg = if (e is com.example.services.SupabaseApiException) {
                            "Supabase Error (HTTP ${e.httpStatusCode}${if (!e.errorCode.isNullOrBlank()) ", Code: ${e.errorCode}" else ""}): ${e.serverMessage}"
                        } else {
                            e.message ?: "Something went wrong while creating your request."
                        }
                        android.util.Log.e("BookTransportScreen", "Voice Booking failed: $detailedMsg", e)
                        errorMessage = detailedMsg
                    }
                }
            },
            onEditInManualForm = { voiceReq ->
                showVoiceDialog = false
                voiceReq.materialType?.let { selectedMaterial = it }
                voiceReq.weight?.let { weightInput = if (it % 1 == 0.0) it.toInt().toString() else it.toString() }
                voiceReq.weightUnit?.let { weightUnit = it }
                voiceReq.pickupPoint?.let { pickupPoint = it }
                voiceReq.dropPoint?.let { dropPoint = it }
                voiceReq.requiredDate?.let { requiredDate = it }
                voiceReq.requiredTime?.let { requiredTime = it }
                currentStep = BookingStep.MANUAL_FORM
            },
            onEnterManually = {
                showVoiceDialog = false
                currentStep = BookingStep.MANUAL_FORM
            }
        )
    }
}

@Composable
private fun ReviewDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EmeraldGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = SecondaryText
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDarkText
            )
        }
    }
}
