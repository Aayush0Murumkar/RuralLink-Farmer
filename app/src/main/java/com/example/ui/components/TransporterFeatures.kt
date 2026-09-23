package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ClickableOnlineOfflineButton(
    isOnline: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
        label = "bg_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFF10B981) else Color(0xFFCBD5E1),
        label = "border_color"
    )
    val textColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFF047857) else Color(0xFF64748B),
        label = "text_color"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
        label = "dot_color"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle(!isOnline) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Animated Status Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            // Lightning Bolt Icon
            Icon(
                imageVector = if (isOnline) Icons.Default.Bolt else Icons.Default.PowerSettingsNew,
                contentDescription = if (isOnline) "Online" else "Offline",
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )

            // Status Label
            Text(
                text = if (isOnline) "Online" else "Offline",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
fun OrderBackoutConfirmationDialog(
    orderId: String,
    onDismiss: () -> Unit,
    onConfirmBackout: (reason: String) -> Unit
) {
    val backoutReasons = listOf(
        "Emergency Vehicle Maintenance / Breakdown",
        "Overload or Illegal Cargo Safety Hazard",
        "Severe Highway Jam / Route Inaccessible",
        "Consignor Unresponsive / Excessive Loading Delay",
        "Driver Shift Emergency"
    )
    var selectedReason by remember { mutableStateOf(backoutReasons.first()) }
    var additionalNotes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Backout / Decline",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                // Subtitle
                Text(
                    text = "Are you sure you want to release Order #$orderId? This shipment will be instantly placed back in the live dispatch pool.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                // Selectable Reasons
                Text(
                    text = "SELECT BACKOUT REASON:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF94A3B8)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    backoutReasons.forEach { reason ->
                        val isSelected = reason == selectedReason
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFEF2F2) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFEF4444) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedReason = reason }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFDC2626) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = reason,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF991B1B) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }

                // Optional Notes
                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = { additionalNotes = it },
                    label = { Text("Additional Remarks (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Keep", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }

                    Button(
                        onClick = {
                            val finalReason = if (additionalNotes.isNotBlank()) "$selectedReason ($additionalNotes)" else selectedReason
                            onConfirmBackout(finalReason)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1.3f).height(44.dp)
                    ) {
                        Text("Confirm Backout", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
