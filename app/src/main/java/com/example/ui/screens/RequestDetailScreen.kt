package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransportRequest
import com.example.data.model.TransporterResponse
import com.example.services.RecommendationService
import com.example.services.RequestService
import com.example.services.TransporterResponseService
import com.example.ui.components.RuralLinkButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.getStatusExplanation
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    farmerId: String,
    requestService: RequestService,
    onBack: () -> Unit,
    onBookNewTransport: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val transporterResponseService = remember { TransporterResponseService(context) }
    val recommendationService = remember { RecommendationService() }

    val db = remember { com.example.data.local.AppDatabase.getDatabase(context) }
    val liveRequest by db.requestDao().getRequestByIdFlow(requestId).collectAsStateWithLifecycle(initialValue = null)

    var request by remember { mutableStateOf<TransportRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var accessDenied by remember { mutableStateOf(false) }

    LaunchedEffect(liveRequest) {
        if (liveRequest != null) {
            request = liveRequest
        }
    }

    val responses by transporterResponseService
        .getResponsesForRequest(requestId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Fetch request and seed demo responses if needed
    LaunchedEffect(requestId, farmerId) {
        isLoading = true
        val fetched = requestService.getRequestById(requestId, farmerId)
        if (fetched == null && liveRequest == null) {
            accessDenied = true
        } else {
            accessDenied = false
            transporterResponseService.seedDemoResponsesIfEmpty(requestId)
            coroutineScope.launch {
                transporterResponseService.syncFirestoreResponses(requestId)
            }
        }
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (request != null) "Request #${request?.id}" else "Request Details",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("request_detail_back_button")
                    ) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldGreen)
                }
            } else if (accessDenied || request == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Access Denied / Request Not Found",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This transport request does not exist or belongs to another farmer account.",
                                fontSize = 13.sp,
                                color = SecondaryText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            RuralLinkButton(
                                text = "Back to My Requests",
                                onClick = onBack,
                                testTag = "access_denied_back_button"
                            )
                        }
                    }
                }
            } else {
                val req = request ?: return@Scaffold
                val formattedCreatedAt = remember(req.createdAt) {
                    try {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        sdf.format(Date(req.createdAt))
                    } catch (e: Exception) {
                        "Recently"
                    }
                }

                val aiResult = remember(req, responses) {
                    recommendationService.evaluateResponses(req, responses)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Demo Simulation Controls
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Demo Response Simulator",
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
                                FilterChip(
                                    selected = responses.isEmpty(),
                                    onClick = {
                                        coroutineScope.launch {
                                            transporterResponseService.setDemoResponsesCount(req.id, 0)
                                        }
                                    },
                                    label = { Text("0 (Searching)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = responses.size == 1,
                                    onClick = {
                                        coroutineScope.launch {
                                            transporterResponseService.setDemoResponsesCount(req.id, 1)
                                        }
                                    },
                                    label = { Text("1 Quote", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = responses.size == 2,
                                    onClick = {
                                        coroutineScope.launch {
                                            transporterResponseService.setDemoResponsesCount(req.id, 2)
                                        }
                                    },
                                    label = { Text("2 Quotes", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = responses.size == 3,
                                    onClick = {
                                        coroutineScope.launch {
                                            transporterResponseService.setDemoResponsesCount(req.id, 3)
                                        }
                                    },
                                    label = { Text("3 Quotes", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 1. STATUS CARD (Elevated White Card)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = req.id,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                    Text(
                                        text = "Created: $formattedCreatedAt",
                                        fontSize = 12.sp,
                                        color = SecondaryText
                                    )
                                }
                                StatusBadge(status = req.status)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Status Explanation Box
                            Surface(
                                color = BrightLeafLight,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrightLeaf.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Status: ${req.status}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                        Text(
                                            text = getStatusExplanation(req.status),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryDarkText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. ROUTE & CARGO DETAILS (Elevated White Card)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Route & Material Info",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Material & Weight
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Material Type", fontSize = 12.sp, color = SecondaryText)
                                    Text(
                                        text = req.materialType,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDarkText
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Weight / Quantity", fontSize = 12.sp, color = SecondaryText)
                                    Text(
                                        text = "${req.weight} ${req.weightUnit}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorderSubtle)

                            // Pickup
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pickup",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Pickup Location", fontSize = 11.5.sp, color = SecondaryText)
                                    Text(
                                        text = req.pickupPoint,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryDarkText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Drop
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Drop",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Drop Location", fontSize = 11.5.sp, color = SecondaryText)
                                    Text(
                                        text = req.dropPoint,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryDarkText
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorderSubtle)

                            // Required Date & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Required Date", fontSize = 12.sp, color = SecondaryText)
                                    Text(
                                        text = req.requiredDate,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryDarkText
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Required Time", fontSize = 12.sp, color = SecondaryText)
                                    Text(
                                        text = req.requiredTime,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryDarkText
                                    )
                                }
                            }

                            if (req.notes.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorderSubtle)
                                Text(text = "Special Notes / Instructions", fontSize = 12.sp, color = SecondaryText)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = req.notes,
                                    fontSize = 13.5.sp,
                                    color = PrimaryDarkText
                                )
                            }
                        }
                    }

                    // 3. TRANSPORTERS WHO ACCEPTED
                    Text(
                        text = "Transporter Quotes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )

                    if (responses.isEmpty() && req.status == "SEARCHING") {
                        LaunchedEffect(req.id) {
                            kotlinx.coroutines.delay(1500)
                            transporterResponseService.setDemoResponsesCount(req.id, 2)
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = EmeraldGreen,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Finding suitable transporters...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryDarkText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Broadcasted to transport partners near ${req.pickupPoint}. Fetching competitive quotes...",
                                    fontSize = 12.5.sp,
                                    color = SecondaryText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            transporterResponseService.setDemoResponsesCount(req.id, 2)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = BrightLeaf)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Instant Driver Quotes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        // AI Recommendation Banner (if > 1 responses exist)
                        if (responses.size > 1 && aiResult.recommendedResponseId != null) {
                            val recTransporter = responses.find { it.id == aiResult.recommendedResponseId }
                            val topScore = aiResult.scoreBreakdowns[aiResult.recommendedResponseId]

                            if (recTransporter != null && topScore != null) {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrightLeaf.copy(alpha = 0.8f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BrightLeafLight
                                                ) {
                                                    Text(
                                                        text = "AI RECOMMENDED",
                                                        color = EmeraldGreen,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = EmeraldGreen,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "${topScore.totalScore}/100 Match",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "Why we recommend ${recTransporter.transporterName}:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryDarkText
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        aiResult.explanationPoints.forEach { point ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = EmeraldGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = point,
                                                    fontSize = 12.5.sp,
                                                    color = PrimaryDarkText
                                                )
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorderSubtle)

                                        // Scoring Breakdown
                                        Text(
                                            text = "Distance: ${topScore.distanceScore}/25 • Capacity: ${topScore.capacityScore}/25 • Route: ${topScore.routeScore}/30 • Time: ${topScore.timeScore}/20",
                                            fontSize = 11.5.sp,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                        }

                        // List of Response Cards
                        if (req.status == "TRANSPORTER_SELECTED" || req.status == "PAYMENT_PENDING" || req.paymentStatus == "PAID" || req.status == "PAID" || req.status == "IN_TRANSIT" || req.status == "DELIVERED") {
                            val selectedRes = responses.find { it.transporterId == req.selectedTransporterId || it.status == "SELECTED" }
                            if (selectedRes != null) {
                                TransporterResponseCard(
                                    response = selectedRes,
                                    suitabilityScore = aiResult.scoreBreakdowns[selectedRes.id]?.totalScore ?: selectedRes.matchingScore,
                                    isAiRecommended = false,
                                    isSelected = true,
                                    isSelectionMade = true,
                                    onSelect = { }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Payment UI
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Payment Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDarkText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (req.paymentStatus == "PAID" || req.status == "PAID" || req.status == "IN_TRANSIT" || req.status == "DELIVERED") {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("payment complete thankyou for being our link  dear farmer", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EmeraldGreen, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        } else {
                                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = PrimaryDarkText, modifier = Modifier.size(120.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Scan to Pay: ${selectedRes.priceQuote}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDarkText)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        requestService.updatePaymentStatus(req.id, "PAID", farmerId)
                                                        request = requestService.getRequestById(req.id, farmerId)
                                                        snackbarHostState.showSnackbar("Payment Confirmed! Ride can now start.")
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Simulate QR Scan", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            responses.forEach { res ->
                                val score = aiResult.scoreBreakdowns[res.id]
                                val isRecommended = (res.id == aiResult.recommendedResponseId && responses.size > 1)
                                val isSelected = (req.selectedTransporterId == res.transporterId || res.status == "SELECTED")
                                
                                TransporterResponseCard(
                                    response = res,
                                    suitabilityScore = score?.totalScore ?: res.matchingScore,
                                    isAiRecommended = isRecommended,
                                    isSelected = isSelected,
                                    isSelectionMade = req.status == "TRANSPORTER_SELECTED",
                                    onSelect = {
                                        coroutineScope.launch {
                                            transporterResponseService.selectTransporter(req.id, res.id, farmerId)
                                            requestService.updateRequestStatus(req.id, "PAYMENT_PENDING", farmerId)
                                            request = requestService.getRequestById(req.id, farmerId)
                                            snackbarHostState.showSnackbar("Transporter selected successfully.")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var showBackoutDialog by remember { mutableStateOf(false) }

                    if (req.status != "CANCELLED" && req.status != "BACKED_OUT" && req.status != "COMPLETED") {
                        Button(
                            onClick = { showBackoutDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backout / Decline Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (showBackoutDialog) {
                        com.example.ui.components.OrderBackoutConfirmationDialog(
                            orderId = req.id,
                            onDismiss = { showBackoutDialog = false },
                            onConfirmBackout = { reason ->
                                showBackoutDialog = false
                                coroutineScope.launch {
                                    // Update request status to BACKED_OUT / CANCELLED and optionally note the reason
                                    requestService.updateRequestStatus(req.id, "BACKED_OUT", farmerId)
                                    request = requestService.getRequestById(req.id, farmerId)
                                    snackbarHostState.showSnackbar("Order released back into dispatch pool.")
                                }
                            }
                        )
                    }

                    RuralLinkButton(
                        text = "Book Another Transport",
                        onClick = onBookNewTransport,
                        icon = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "detail_book_another_button"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    com.example.ui.components.SupabaseSyncFooter(
                        statusText = "Quotes & selections synced with Supabase public.transporter_responses"
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TransporterResponseCard(
    response: TransporterResponse,
    suitabilityScore: Int,
    isAiRecommended: Boolean,
    isSelected: Boolean,
    isSelectionMade: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrightLeafLight else CardBackground
        ),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BrightLeaf else CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = response.transporterName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryDarkText
                        )
                        if (isAiRecommended) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = BrightLeafLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Best Match",
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Vehicle: ${response.vehicleNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen
                    )
                }

                Surface(
                    color = EmeraldGreenContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$suitabilityScore/100 Fit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${response.vehicleType} (${response.vehicleCapacity})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryDarkText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${response.estimatedDistance} away",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Specs Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = response.routeMatch,
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ETA: ${response.estimatedArrival}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (response.finalAmount > 0) {
                HorizontalDivider(color = CardBorderSubtle, modifier = Modifier.padding(bottom = 8.dp))
                
                if (response.isReturnTrip) {
                    Text("🎉 Backload Return-Trip Discount Applied!", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Transporter is returning empty; additional discount applied.", color = SecondaryText, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Normal Truck Price:", fontSize = 12.sp, color = SecondaryText)
                    Text("₹%,.0f".format(response.normalPrice), fontSize = 12.sp, color = SecondaryText, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                }
                Text("Estimated cost if you booked the entire truck for this route.", color = SecondaryText, fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Shared Capacity Price:", fontSize = 12.sp, color = SecondaryText)
                    Text("₹%,.0f".format(response.basePrice - response.backloadDiscountAmount), fontSize = 12.sp, color = PrimaryDarkText)
                }
                Text("Prorated fare for using ${(response.capacitySharePercentage * 100).toInt()}% of the truck's capacity.", color = SecondaryText, fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("RuralLink Fee (5%):", fontSize = 12.sp, color = SecondaryText)
                    Text("₹%,.0f".format(response.platformFee), fontSize = 12.sp, color = PrimaryDarkText)
                }
                Text("Platform fee for matchmaking and transaction safety.", color = SecondaryText, fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Your Savings:", fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    Text("₹%,.0f".format(response.savings), fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
                Text("Total savings compared to booking a dedicated truck.", color = EmeraldGreen.copy(alpha = 0.8f), fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Price & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = response.priceQuote,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldGreen
                )

                if (isSelectionMade) {
                    if (isSelected) {
                        Surface(
                            color = EmeraldGreen,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BrightLeaf,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Selected",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = CardBorder,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Not Selected",
                                color = SecondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    RuralLinkButton(
                        text = "Select Transporter",
                        onClick = onSelect,
                        testTag = "select_transporter_${response.id}"
                    )
                }
            }
        }
    }
}
