package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.data.model.TransportRequest
import com.example.ui.theme.*

/**
 * Premium Minimal Top Brand Header with clean white surface, Emerald Green (#203D43) typography,
 * elevated location selector pill, and Bright Leaf (#CDFF9B) live status badge.
 */
@Composable
fun TopBrandHeader(
    farmerName: String? = null,
    location: String? = "Pimplgaon Baswant",
    farmerRole: String? = "Farmer",
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    notifications: List<NotificationItem> = emptyList(),
    onNotificationClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardBackground,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & App Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = "RuralLink Logo",
                            tint = BrightLeaf,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RuralLink",
                            color = EmeraldGreen,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "FARMER LOGISTICS",
                            color = SecondaryText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Quick Location & Supabase Status Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!location.isNull_or_empty_shim()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PrimaryGreenContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 135.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatCleanLocation(location ?: "Pimplgaon"),
                                    color = PrimaryGreenDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Online/Offline Transporter Status Toggle
                    var isOnline by remember { mutableStateOf(true) }
                    ClickableOnlineOfflineButton(
                        isOnline = isOnline,
                        onToggle = { isOnline = it }
                    )

                    // Notification Bell
                    Box {
                        var showNotificationPopup by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showNotificationPopup = !showNotificationPopup },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AppBackground)
                                .border(1.dp, CardBorder, CircleShape)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = ErrorRed) {
                                            Text("$unreadCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = PrimaryGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showNotificationPopup,
                            onDismissRequest = { showNotificationPopup = false },
                            modifier = Modifier
                                .width(320.dp)
                                .background(CardBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PrimaryDarkText
                                )
                                Text(
                                    text = "View All",
                                    fontSize = 12.sp,
                                    color = EmeraldGreen,
                                    modifier = Modifier.clickable {
                                        showNotificationPopup = false
                                        onNotificationClick()
                                    }
                                )
                            }
                            HorizontalDivider(color = CardBorderSubtle)
                            if (notifications.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "No notifications yet.",
                                            fontSize = 13.sp,
                                            color = SecondaryText
                                        )
                                    },
                                    onClick = { showNotificationPopup = false }
                                )
                            } else {
                                notifications.take(5).forEach { notif ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = notif.title,
                                                    fontWeight = if (!notif.read) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = PrimaryDarkText
                                                )
                                                Text(
                                                    text = notif.message,
                                                    fontSize = 11.5.sp,
                                                    color = SecondaryText,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        onClick = {
                                            showNotificationPopup = false
                                            onNotificationClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_empty_shim(): Boolean = this == null || this.trim().isEmpty()

private fun formatCleanLocation(loc: String): String {
    if (loc.isBlank()) return "Nashik"
    val parts = loc.split(",")
    return if (parts.isNotEmpty()) parts.first().trim() else loc
}

@Composable
fun RuralLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
    isSecondary: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "rural_link_button"
) {
    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EmeraldGreen
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag(testTag)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                disabledContainerColor = EmeraldGreen.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag(testTag)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BrightLeaf,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

fun getStatusExplanation(status: String): String {
    return when (status.uppercase().replace(" ", "_")) {
        "PENDING", "SEARCHING" -> "Finding suitable transporters near you..."
        "TRANSPORTERS_RESPONDED", "RESPONSES_RECEIVED" -> "Transporters submitted competitive quotes"
        "MATCHED", "TRANSPORTER_SELECTED" -> "Transporter confirmed & scheduled for pickup"
        "COMPLETED" -> "Transport trip successfully completed"
        "CANCELLED" -> "Request cancelled"
        else -> "Finding suitable transporters..."
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val displayStatus = when (status) {
        "Pending", "SEARCHING" -> "FINDING MATCHES..."
        "Transporters Responded", "RESPONSES_RECEIVED" -> "RESPONSES RECEIVED"
        "Matched", "TRANSPORTER_SELECTED" -> "TRANSPORTER SELECTED"
        "PAYMENT_PENDING" -> "PAYMENT PENDING"
        "PAID" -> "PAID ✓"
        "IN_TRANSIT" -> "IN TRANSIT"
        "DELIVERED" -> "DELIVERED"
        "Completed", "COMPLETED" -> "COMPLETED"
        "Cancelled", "CANCELLED" -> "CANCELLED"
        else -> status.uppercase().replace("_", " ")
    }

    val normalized = when {
        displayStatus.startsWith("FINDING") || status == "SEARCHING" || status == "Pending" -> "SEARCHING"
        displayStatus == "RESPONSES RECEIVED" -> "RESPONSES_RECEIVED"
        displayStatus == "TRANSPORTER SELECTED" -> "TRANSPORTER_SELECTED"
        displayStatus == "PAYMENT PENDING" -> "PAYMENT_PENDING"
        displayStatus.startsWith("PAID") -> "PAID"
        displayStatus == "IN TRANSIT" -> "IN_TRANSIT"
        displayStatus == "DELIVERED" -> "DELIVERED"
        displayStatus == "COMPLETED" -> "COMPLETED"
        displayStatus == "CANCELLED" -> "CANCELLED"
        else -> "SEARCHING"
    }

    val (bgColor, fgColor, borderColor) = when (normalized) {
        "SEARCHING" -> Triple(EmeraldGreenContainer, EmeraldGreen, CardBorder)
        "RESPONSES_RECEIVED" -> Triple(Color(0xFFFFF9E6), Color(0xFF8D6E18), Color(0xFFFFE082))
        "TRANSPORTER_SELECTED", "PAYMENT_PENDING" -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), Color(0xFFC7D2FE)) // Indigo
        "PAID" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFFBBF7D0)) // Green
        "IN_TRANSIT" -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFFBFDBFE)) // Blue
        "COMPLETED", "DELIVERED" -> Triple(BrightLeafLight, Color(0xFF1B4021), BrightLeaf)
        "CANCELLED" -> Triple(Color(0xFFFFEBEE), ErrorRed, Color(0xFFFFCDD2))
        else -> Triple(EmeraldGreenContainer, EmeraldGreen, CardBorder)
    }

    val alpha = if (normalized == "SEARCHING") {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else {
        1f
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp).alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (normalized == "TRANSPORTER_SELECTED" || normalized == "COMPLETED") {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrightLeafDark)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayStatus,
                    color = fgColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            } else if (normalized == "SEARCHING") {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    color = fgColor,
                    strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayStatus,
                    color = fgColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            } else {
                Text(
                    text = displayStatus,
                    color = fgColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Premium elevated White Request Card with clean line art, produce badge,
 * subtle route connectors, and Bright Leaf status accents.
 */
@Composable
fun RequestCard(
    request: TransportRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusExplanation = getStatusExplanation(request.status)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("request_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Material + Produce Icon + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                request.materialType.contains("Onion", ignoreCase = true) -> Icons.Default.Agriculture
                                request.materialType.contains("Wheat", ignoreCase = true) -> Icons.Default.Grass
                                request.materialType.contains("Tomato", ignoreCase = true) -> Icons.Default.LocalFlorist
                                else -> Icons.Default.Inventory2
                            },
                            contentDescription = "Material Icon",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = request.materialType,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkText
                        )
                        Text(
                            text = "${request.weight} ${request.weightUnit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryText
                        )
                    }
                }
                StatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pickup to Dropoff Route with clean minimal path
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TripOrigin,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.pickupPoint,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDarkText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = request.dropPoint,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDarkText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date & Time + Request ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${request.requiredDate} • ${request.requiredTime}",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "#${request.id}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status explanation bar
            Surface(
                color = if (request.status == "TRANSPORTER_SELECTED" || request.status == "Matched") BrightLeafLight else EmeraldGreenContainer,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (request.status == "TRANSPORTER_SELECTED" || request.status == "Matched") BrightLeaf else CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusExplanation,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldGreen
                    )
                }
            }
        }
    }
}

/**
 * Clean white Notification Card with line art icon and Bright Leaf (#CDFF9B) new dot.
 */
@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!notification.read) BrightLeafDark.copy(alpha = 0.3f) else CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notification.read) 2.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!notification.read) BrightLeafContainer else AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.title.contains("Verified", ignoreCase = true)) Icons.Default.Verified else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (!notification.read) EmeraldGreen else SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkText
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    fontSize = 12.5.sp,
                    color = SecondaryText,
                    lineHeight = 18.sp
                )
            }
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BrightLeafDark)
                )
            }
        }
    }
}

@Composable
fun PhoneInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "phone_input"
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= 10 && input.all { it.isDigit() }) {
                onValueChange(input)
            }
        },
        label = { Text("Mobile Number", color = SecondaryText) },
        prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = PrimaryDarkText) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone", tint = EmeraldGreen)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldGreen,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedLabelColor = EmeraldGreen,
            unfocusedLabelColor = SecondaryText
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun OTPInput(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "otp_input"
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier.fillMaxWidth()
    ) {
        repeat(6) { index ->
            val char = otpValue.getOrNull(index)?.toString() ?: ""
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (char.isNotEmpty()) EmeraldGreen else CardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(if (char.isNotEmpty()) BrightLeafLight else CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Underlying TextField for keyboard entry
    OutlinedTextField(
        value = otpValue,
        onValueChange = { input ->
            if (input.length <= 6 && input.all { it.isDigit() }) {
                onOtpChange(input)
            }
        },
        label = { Text("Enter 6-Digit Verification Code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldGreen,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

/**
 * Visual badge indicating live Supabase connection status across all pages.
 */
@Composable
fun SupabaseStatusBadge(
    modifier: Modifier = Modifier,
    label: String = "Supabase Connected",
    showDot: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BrightLeafContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrightLeaf.copy(alpha = 0.6f)),
        modifier = modifier.testTag("supabase_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BrightLeafDark)
                )
            }
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen
            )
        }
    }
}

/**
 * Footer banner providing Supabase cloud sync status on screens.
 */
@Composable
fun SupabaseSyncFooter(
    modifier: Modifier = Modifier,
    statusText: String = "Live database sync active via Supabase PostgREST & Auth",
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = null,
            tint = if (onClick != null) EmeraldGreen else SecondaryText,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            fontSize = 11.5.sp,
            color = if (onClick != null) EmeraldGreen else SecondaryText,
            fontWeight = if (onClick != null) FontWeight.Bold else FontWeight.Medium
        )
        if (onClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = EmeraldGreenContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Inspector",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
