package com.example.ui.components

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.services.AiExtractionService
import com.example.services.SpeechRecognitionService
import com.example.services.StructuredVoiceRequest
import com.example.services.VoiceState
import com.example.ui.theme.*

enum class VoiceDialogStep {
    RECORD_PROMPT,
    LISTENING,
    TRANSCRIPT_REVIEW,
    MISSING_INFO_QA,
    FINAL_CONFIRMATION,
    ERROR_STATE,
    PERMISSION_DENIED
}

private const val TAG_UI = "VoiceBookingUI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceBookingDialog(
    onDismiss: () -> Unit,
    onConfirmBooking: (StructuredVoiceRequest) -> Unit,
    onEditInManualForm: (StructuredVoiceRequest) -> Unit,
    onEnterManually: () -> Unit
) {
    val context = LocalContext.current
    val speechService = remember { SpeechRecognitionService(context) }
    val aiService = remember { AiExtractionService() }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG_UI, "Cleaning up SpeechRecognitionService onDispose")
            speechService.destroy()
        }
    }

    var currentStep by remember { mutableStateOf(VoiceDialogStep.RECORD_PROMPT) }
    var currentTranscript by remember { mutableStateOf("") }
    var manualInputText by remember { mutableStateOf("") }
    var extractedRequest by remember { mutableStateOf<StructuredVoiceRequest?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Missing Field QA State
    var currentMissingFieldIndex by remember { mutableIntStateOf(0) }
    var qaInputValue by remember { mutableStateOf("") }

    // Test Scenarios matching Stage 5 Specs
    val testScenarios = listOf(
        "English: Complete" to "I want to transport 20 quintal onion from Kopargaon Farm Yard to Nashik APMC Mandi tomorrow at 9 AM.",
        "Marathi: Complete" to "Mala Kopargaon madhun Nashik la 20 quintal कांदा पाठवायचा आहे.",
        "Hindi: Complete" to "मुझे कोपरगांव से नासिक 2 टन प्याज भेजना है।",
        "Mixed: Complete" to "Kopargaon se Nashik ला 20 quintal onion send karna hai.",
        "Incomplete (Triggers Q&A)" to "I want to send onions to Nashik."
    )

    // Helper to start listening safely
    fun startListeningFlow() {
        Log.d(TAG_UI, "startListeningFlow triggered")
        speechService.startListening { state ->
            when (state) {
                is VoiceState.Listening -> {
                    Log.d(TAG_UI, "State -> LISTENING")
                    currentStep = VoiceDialogStep.LISTENING
                }
                is VoiceState.Success -> {
                    Log.d(TAG_UI, "State -> SUCCESS: transcript='${state.transcript}'")
                    currentTranscript = state.transcript
                    currentStep = VoiceDialogStep.TRANSCRIPT_REVIEW
                }
                is VoiceState.Error -> {
                    Log.e(TAG_UI, "State -> ERROR: ${state.message}")
                    errorMessage = state.message
                    state.rawPartial?.let { currentTranscript = it }
                    currentStep = VoiceDialogStep.ERROR_STATE
                }
                is VoiceState.PermissionDenied -> {
                    Log.w(TAG_UI, "State -> PERMISSION_DENIED")
                    currentStep = VoiceDialogStep.PERMISSION_DENIED
                }
                is VoiceState.Idle -> {
                    Log.d(TAG_UI, "State -> IDLE")
                }
            }
        }
    }

    // Speech Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG_UI, "Permission request result: isGranted=$isGranted")
        if (isGranted) {
            startListeningFlow()
        } else {
            currentStep = VoiceDialogStep.PERMISSION_DENIED
        }
    }

    // Process Transcript Function
    fun processTranscript(transcript: String) {
        Log.d(TAG_UI, "processTranscript: '$transcript'")
        val result = aiService.extractTransportDetails(transcript)
        extractedRequest = result
        if (result.missingFields.isNotEmpty()) {
            currentMissingFieldIndex = 0
            currentStep = VoiceDialogStep.MISSING_INFO_QA
            Log.d(TAG_UI, "Moving to MISSING_INFO_QA (${result.missingFields.size} missing)")
        } else {
            currentStep = VoiceDialogStep.FINAL_CONFIRMATION
            Log.d(TAG_UI, "Moving to FINAL_CONFIRMATION (all fields extracted)")
        }
    }

    // Function to handle answer to missing question
    fun applyMissingAnswer(answerText: String) {
        val currentReq = extractedRequest ?: return
        val fieldKey = currentReq.missingFields.getOrNull(currentMissingFieldIndex) ?: return

        Log.d(TAG_UI, "applyMissingAnswer fieldKey=$fieldKey, answer='$answerText'")

        val newReq = when (fieldKey) {
            "pickupPoint" -> currentReq.copy(pickupPoint = answerText)
            "dropPoint" -> currentReq.copy(dropPoint = answerText)
            "weight" -> {
                val num = Regex("(\\d+(?:[.,]\\d+)?)").find(answerText)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 20.0
                val unit = if (answerText.lowercase().contains("kg")) "kg" else if (answerText.lowercase().contains("ton")) "tonne" else "quintal"
                currentReq.copy(weight = num, weightUnit = unit)
            }
            "materialType" -> currentReq.copy(materialType = answerText)
            "requiredDate" -> currentReq.copy(requiredDate = answerText)
            "requiredTime" -> currentReq.copy(requiredTime = answerText)
            else -> currentReq
        }

        val updatedMissing = newReq.missingFields.filter { key ->
            when (key) {
                "pickupPoint" -> newReq.pickupPoint.isNullOrBlank()
                "dropPoint" -> newReq.dropPoint.isNullOrBlank()
                "weight" -> newReq.weight == null
                "materialType" -> newReq.materialType.isNullOrBlank()
                "requiredDate" -> newReq.requiredDate.isNullOrBlank()
                "requiredTime" -> newReq.requiredTime.isNullOrBlank()
                else -> false
            }
        }

        val finalReq = newReq.copy(missingFields = updatedMissing)
        extractedRequest = finalReq

        if (updatedMissing.isNotEmpty()) {
            currentMissingFieldIndex = 0
        } else {
            currentStep = VoiceDialogStep.FINAL_CONFIRMATION
        }
        qaInputValue = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("voice_booking_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // HEADER BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoiceBookingOrb(
                            size = 36.dp,
                            isListening = false,
                            showMicIcon = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Voice Assistant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentStep) {

                    // ==================== STEP 1: ENTRY & MIC ====================
                    VoiceDialogStep.RECORD_PROMPT -> {
                        Text(
                            text = "Tell us what you need",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Speak or type naturally. Mention pickup, destination, material, weight, date & time.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Siri-style Circular AI Voice Orb
                        VoiceBookingOrb(
                            size = 110.dp,
                            isListening = false,
                            showMicIcon = true,
                            onClick = {
                                if (speechService.isPermissionGranted()) {
                                    startListeningFlow()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("start_speaking_mic_button")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (speechService.isPermissionGranted()) {
                                    startListeningFlow()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_speaking_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Speaking", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text input fallback directly on main dialog
                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it },
                            placeholder = { Text("Or type request here (e.g. 20 quintal onion Kopargaon to Nashik)") },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (manualInputText.isNotBlank()) {
                                    IconButton(onClick = {
                                        currentTranscript = manualInputText
                                        processTranscript(manualInputText)
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Process text", tint = AgroGreenPrimary)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("voice_direct_text_input")
                        )

                        if (manualInputText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    currentTranscript = manualInputText
                                    processTranscript(manualInputText)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepTeal),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Text("Extract Details from Text", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Phrases
                        Text(
                            text = "🧪 Test Voice Samples (Tap to simulate speech):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            testScenarios.forEach { (label, phrase) ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentTranscript = phrase
                                            processTranscript(phrase)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AgroGreenPrimary
                                        )
                                        Text(
                                            text = "\"$phrase\"",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==================== STEP 2: LISTENING STATE ====================
                    VoiceDialogStep.LISTENING -> {
                        Spacer(modifier = Modifier.height(16.dp))

                        VoiceBookingOrb(
                            size = 120.dp,
                            isListening = true,
                            showMicIcon = true,
                            onClick = {
                                speechService.stopListening()
                                currentStep = VoiceDialogStep.RECORD_PROMPT
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Listening...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Text(
                            text = "Speak naturally in English, Marathi, or Hindi...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                speechService.stopListening()
                                currentStep = VoiceDialogStep.RECORD_PROMPT
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("stop_listening_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }
                    }

                    // ==================== STEP 3: TRANSCRIPT REVIEW ====================
                    VoiceDialogStep.TRANSCRIPT_REVIEW -> {
                        Text(
                            text = "Recognized Speech:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroGreenPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = AgroGreenContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "\"$currentTranscript\"",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = VoiceDialogStep.RECORD_PROMPT },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("speak_again_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speak Again")
                            }

                            Button(
                                onClick = { processTranscript(currentTranscript) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("use_this_transcript_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use This", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ==================== STEP 4: MISSING INFO INTERACTIVE Q&A ====================
                    VoiceDialogStep.MISSING_INFO_QA -> {
                        val currentReq = extractedRequest
                        val missingList = currentReq?.missingFields ?: emptyList()
                        val currentMissingKey = missingList.getOrNull(currentMissingFieldIndex) ?: "pickupPoint"

                        Text(
                            text = "Additional Info Needed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HarvestAmberPrimary
                        )

                        if (currentTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recognized: \"$currentTranscript\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "Question ${currentMissingFieldIndex + 1} of ${missingList.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Question Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        tint = HarvestAmberPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiService.getQuestionForMissingField(currentMissingKey),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Answer Input / Options
                        OutlinedTextField(
                            value = qaInputValue,
                            onValueChange = { qaInputValue = it },
                            placeholder = { Text("Speak or type your answer here...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (speechService.isPermissionGranted()) {
                                        speechService.startListening { state ->
                                            if (state is VoiceState.Success) {
                                                qaInputValue = state.transcript
                                            }
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }) {
                                    Icon(Icons.Default.Mic, contentDescription = "Speak answer", tint = AgroGreenPrimary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qa_answer_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset chips for rapid answer selection
                        val quickChips = when (currentMissingKey) {
                            "pickupPoint" -> listOf("Kopargaon Farm Yard", "Sinnar Village", "Nashik Market Yard")
                            "dropPoint" -> listOf("Nashik APMC Mandi", "Mumbai Wholesale Mandi", "Pune APMC")
                            "weight" -> listOf("20 quintal", "50 quintal", "2 tonne", "2000 kg")
                            "materialType" -> listOf("Onion", "Wheat", "Tomato", "Sugarcane")
                            "requiredDate" -> listOf("14 August 2026", "13 August 2026", "15 August 2026")
                            "requiredTime" -> listOf("09:00 AM", "07:00 AM", "01:00 PM", "05:00 PM")
                            else -> emptyList()
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickChips.forEach { chipText ->
                                FilterChip(
                                    selected = qaInputValue == chipText,
                                    onClick = { qaInputValue = chipText },
                                    label = { Text(chipText, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (qaInputValue.isNotBlank()) {
                                    applyMissingAnswer(qaInputValue)
                                }
                            },
                            enabled = qaInputValue.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_qa_answer_button")
                        ) {
                            Text("Next / Save", fontWeight = FontWeight.Bold)
                        }
                    }

                    // ==================== STEP 5: FINAL CONFIRMATION ====================
                    VoiceDialogStep.FINAL_CONFIRMATION -> {
                        val req = extractedRequest ?: StructuredVoiceRequest()

                        Text(
                            text = "I understood your request as:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroGreenPrimary,
                            textAlign = TextAlign.Center
                        )

                        if (req.rawSpeech.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recognized: \"${req.rawSpeech}\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AgroGreenContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {

                                ConfRow("Pickup:", req.pickupPoint ?: "Not specified")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ConfRow("Drop:", req.dropPoint ?: "Not specified")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ConfRow("Material:", req.materialType ?: "Not specified")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ConfRow("Weight:", "${req.weight?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "20"} ${req.weightUnit ?: "quintal"}")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ConfRow("Date:", req.requiredDate ?: "14 August 2026")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ConfRow("Time:", req.requiredTime ?: "09:00 AM")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onConfirmBooking(req) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("voice_confirm_request_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditInManualForm(req) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("voice_edit_details_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Details", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { currentStep = VoiceDialogStep.RECORD_PROMPT },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("voice_speak_again_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Speak Again", fontSize = 13.sp)
                            }
                        }
                    }

                    // ==================== STEP 6: ERROR & PERMISSION STATES ====================
                    VoiceDialogStep.ERROR_STATE -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = errorMessage ?: "Speech recognition encountered an issue.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        if (currentTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Recognized Speech:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AgroGreenPrimary)
                                    Text("\"$currentTranscript\"", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fallback text input inside error box
                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it },
                            placeholder = { Text("Type transport request here...") },
                            singleLine = false,
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (manualInputText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    currentTranscript = manualInputText
                                    processTranscript(manualInputText)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Text("Extract Details from Typed Text", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Voice Samples inside error view for rapid recovery
                        Text(
                            text = "🧪 Test Voice Samples (Tap to simulate speech):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 140.dp)
                        ) {
                            items(testScenarios) { (label, phrase) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentTranscript = phrase
                                            processTranscript(phrase)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AgroGreenPrimary)
                                        Text(text = "\"$phrase\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEnterManually,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("error_enter_manually_button")
                            ) {
                                Text("Enter Manually")
                            }

                            Button(
                                onClick = {
                                    if (currentTranscript.isNotBlank()) {
                                        processTranscript(currentTranscript)
                                    } else {
                                        currentStep = VoiceDialogStep.RECORD_PROMPT
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("error_try_again_button")
                            ) {
                                Text(if (currentTranscript.isNotBlank()) "Extract Speech" else "Try Again", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    VoiceDialogStep.PERMISSION_DENIED -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Microphone Access Needed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Microphone access is needed for voice booking so you can speak naturally in Marathi, Hindi, or English.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onEnterManually,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("permission_enter_manually_button")
                        ) {
                            Text("Enter Details Manually", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AgroGreenPrimary
        )
    }
}
