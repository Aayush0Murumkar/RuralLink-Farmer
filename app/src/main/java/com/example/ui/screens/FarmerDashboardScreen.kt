package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Farmer
import com.example.data.model.NotificationItem
import com.example.data.model.TransportRequest
import com.example.ui.components.NotificationCard
import com.example.ui.components.RequestCard
import com.example.ui.components.RuralLinkButton
import com.example.ui.components.TopBrandHeader
import com.example.ui.components.VoiceBookingDialog
import com.example.ui.components.VoiceBookingOrb
import com.example.ui.theme.*

@Composable
fun FarmerDashboardScreen(
    farmer: Farmer?,
    requests: List<TransportRequest>,
    notifications: List<NotificationItem>,
    isLoading: Boolean = false,
    isError: Boolean = false,
    onRetry: () -> Unit = {},
    onBookTransportClick: () -> Unit,
    onViewAllRequestsClick: () -> Unit,
    onViewNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAgriMatchClick: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    onRequestSelected: (TransportRequest) -> Unit,
    onVoiceRequestParsed: (com.example.services.VoiceService.VoiceParsedRequest) -> Unit
) {
    var showVoiceDialog by remember { mutableStateOf(false) }

    val activeRequests = remember(requests) {
        requests.filter {
            it.status == "Pending" ||
            it.status == "Transporters Responded" ||
            it.status == "Matched" ||
            it.status == "SEARCHING" ||
            it.status == "RESPONSES_RECEIVED" ||
            it.status == "TRANSPORTER_SELECTED"
        }
    }

    val unreadNotifCount = remember(notifications) {
        notifications.count { !it.read }
    }

    Scaffold(
        topBar = {
            TopBrandHeader(
                farmerName = farmer?.name,
                location = farmer?.address?.ifBlank { "Pimplgaon Baswant" } ?: "Pimplgaon Baswant",
                farmerRole = "Farmer",
                unreadCount = unreadNotifCount,
                notifications = notifications,
                onNotificationClick = onViewNotificationsClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showVoiceDialog = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Assistant",
                        tint = BrightLeaf
                    )
                },
                text = {
                    Text(
                        "Voice Booking",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("dashboard_voice_fab")
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. WELCOME HERO CARD (Minimal elevated white card with Emerald Green typography)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Welcome back,",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SecondaryText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BrightLeafLight
                                        ) {
                                            Text(
                                                text = "Verified",
                                                color = EmeraldGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = farmer?.name ?: "Ramesh Patil",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ready to move your farm materials?",
                                        fontSize = 13.sp,
                                        color = SecondaryText
                                    )
                                }

                                // Profile Quick Icon Circle
                                Surface(
                                    onClick = onProfileClick,
                                    shape = CircleShape,
                                    color = EmeraldGreenContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("dashboard_header_profile_icon")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Smart Match Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreenContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAgriMatchClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌾🤖✨", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AgriMatch AI", fontWeight = FontWeight.Bold, color = PrimaryGreenDark, fontSize = 16.sp)
                                Text("Find the most profitable buyer & route", color = PrimaryGreenDark, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryGreenDark)
                        }
                    }
                }

                // 2. COMPACT INFORMATION STAT CARDS (Clean elevated white cards with Emerald Green line art)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Active Requests
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onViewAllRequestsClick)
                                .testTag("stat_card_active_requests")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${activeRequests.size}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = "Active Requests",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryText
                                )
                            }
                        }

                        // Card 2: Total Bookings
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onViewAllRequestsClick)
                                .testTag("stat_card_total_bookings")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${requests.size}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = "Total Bookings",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryText
                                )
                            }
                        }

                        // Card 3: Unread Notifications with Bright Leaf badge
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onViewNotificationsClick)
                                .testTag("stat_card_notifications")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EmeraldGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (unreadNotifCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = BrightLeaf,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$unreadNotifCount",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldGreen
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "$unreadNotifCount",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = "Notifications",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }

                // 3. BOOK TRANSPORT & VOICE BOOKING ACTION CARDS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Book Transport Primary Action
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(EmeraldGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = "Book Transport",
                                            tint = BrightLeaf,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Book Transport",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Find verified transport for your harvest materials.",
                                            fontSize = 12.5.sp,
                                            color = SecondaryText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                RuralLinkButton(
                                    text = "Book Transport",
                                    onClick = onBookTransportClick,
                                    icon = Icons.Default.Add,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "dashboard_book_transport_button"
                                )
                            }
                        }

                        // Voice Booking Secondary Action (Elevated Card with circular AI Voice Orb)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVoiceDialog = true }
                                .testTag("dashboard_voice_booking_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VoiceBookingOrb(
                                    size = 56.dp,
                                    isListening = false,
                                    showMicIcon = true,
                                    onClick = { showVoiceDialog = true }
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Voice Assistant Booking",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDarkText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PrimaryGreenContainer
                                        ) {
                                            Text(
                                                text = "AI",
                                                color = PrimaryGreenDark,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Speak in Marathi, Hindi or English to book instantly.",
                                        fontSize = 12.sp,
                                        color = SecondaryText,
                                        lineHeight = 16.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Voice Assistant",
                                    tint = SecondaryText
                                )
                            }
                        }
                    }
                }

                // 4. QUICK ACTIONS SECTION
                item {
                    Column {
                        Text(
                            text = "QUICK ACTIONS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryText,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickActionChip(
                                title = "Book",
                                icon = Icons.Default.AddCircleOutline,
                                iconColor = EmeraldGreen,
                                iconBg = EmeraldGreenContainer,
                                onClick = onBookTransportClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            QuickActionChip(
                                title = "Voice",
                                icon = Icons.Default.MicNone,
                                iconColor = EmeraldGreen,
                                iconBg = BrightLeafContainer,
                                onClick = { showVoiceDialog = true },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            QuickActionChip(
                                title = "Requests",
                                icon = Icons.Default.LocalShipping,
                                iconColor = EmeraldGreen,
                                iconBg = EmeraldGreenContainer,
                                onClick = onViewAllRequestsClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            QuickActionChip(
                                title = "Alerts",
                                icon = Icons.Default.NotificationsNone,
                                iconColor = EmeraldGreen,
                                iconBg = EmeraldGreenContainer,
                                onClick = onViewNotificationsClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            QuickActionChip(
                                title = "Profile",
                                icon = Icons.Default.PersonOutline,
                                iconColor = EmeraldGreen,
                                iconBg = EmeraldGreenContainer,
                                onClick = onProfileClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }

                // 5. LOADING & ERROR STATES
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = EmeraldGreen,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fetching live transport updates...",
                                    fontSize = 12.5.sp,
                                    color = SecondaryText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else if (isError) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Unable to load your requests.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Try Again", color = Color.White)
                                }
                            }
                        }
                    }
                } else {

                    // 6. ACTIVE REQUESTS SECTION
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Requests",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            if (activeRequests.isNotEmpty()) {
                                TextButton(onClick = onViewAllRequestsClick) {
                                    Text("View All (${activeRequests.size})", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }
                        }
                    }

                    // ACTIVE REQUESTS LIST / EMPTY STATE
                    if (activeRequests.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(EmeraldGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No active transport requests",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = PrimaryDarkText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create a transport request when you need to move your farm materials.",
                                        fontSize = 12.5.sp,
                                        color = SecondaryText,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    RuralLinkButton(
                                        text = "Book Transport",
                                        onClick = onBookTransportClick,
                                        icon = Icons.Default.Add,
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        testTag = "active_requests_empty_book_button"
                                    )
                                }
                            }
                        }
                    } else {
                        items(activeRequests) { req ->
                            RequestCard(
                                request = req,
                                onClick = { onRequestSelected(req) }
                            )
                        }
                    }

                    // 7. RECENT REQUESTS SECTION
                    if (requests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Requests",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                TextButton(
                                    onClick = onViewAllRequestsClick,
                                    modifier = Modifier.testTag("dashboard_view_all_requests_button")
                                ) {
                                    Text("View All", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }
                        }

                        items(requests.take(3)) { req ->
                            RequestCard(
                                request = req,
                                onClick = { onRequestSelected(req) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.components.SupabaseSyncFooter(
                                statusText = "Live database sync active via Supabase PostgREST & Auth",
                                onClick = onNavigateToDiagnostics
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
                val parsed = com.example.services.VoiceService.VoiceParsedRequest(
                    materialType = voiceReq.materialType ?: "Onion",
                    weight = voiceReq.weight ?: 20.0,
                    weightUnit = voiceReq.weightUnit ?: "quintal",
                    pickupPoint = voiceReq.pickupPoint ?: "Kopargaon",
                    dropPoint = voiceReq.dropPoint ?: "Nashik",
                    requiredDate = voiceReq.requiredDate ?: "12 August 2026",
                    requiredTime = voiceReq.requiredTime ?: "09:00 AM",
                    rawSpeech = voiceReq.rawSpeech
                )
                onVoiceRequestParsed(parsed)
            },
            onEditInManualForm = { voiceReq ->
                showVoiceDialog = false
                val parsed = com.example.services.VoiceService.VoiceParsedRequest(
                    materialType = voiceReq.materialType ?: "Onion",
                    weight = voiceReq.weight ?: 20.0,
                    weightUnit = voiceReq.weightUnit ?: "quintal",
                    pickupPoint = voiceReq.pickupPoint ?: "Kopargaon",
                    dropPoint = voiceReq.dropPoint ?: "Nashik",
                    requiredDate = voiceReq.requiredDate ?: "12 August 2026",
                    requiredTime = voiceReq.requiredTime ?: "09:00 AM",
                    rawSpeech = voiceReq.rawSpeech
                )
                onVoiceRequestParsed(parsed)
            },
            onEnterManually = {
                showVoiceDialog = false
                onBookTransportClick()
            }
        )
    }
}

@Composable
private fun QuickActionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 2.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryDarkText
            )
        }
    }
}

@Composable
private fun DashboardSkeleton() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(EmeraldGreenContainer.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBorder.copy(alpha = alpha))
        )
    }
}
