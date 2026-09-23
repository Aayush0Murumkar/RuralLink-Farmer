package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Farmer
import com.example.services.AuthService
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onContinueAsFarmer: (Farmer) -> Unit,
    onNavigateToRegister: () -> Unit = {}
) {
    val demoFarmers = AuthService.DEMO_FARMERS
    var selectedFarmer by remember { mutableStateOf(demoFarmers.first()) }
    var isCustomProfileMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customMobile by remember { mutableStateOf("") }
    var customLocation by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .testTag("welcome_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = "Logo",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RuralLink",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Graphic (Animated)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryGreenContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PlantingAnimation()
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Taglines (Advert style)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "SEED TO MARKET",
                    color = AccentEarthy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "High-speed farmgate produce & transport distribution",
                    color = PrimaryDarkText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connecting farmers directly with verified local transport at fair mandi rates.",
                    color = SecondaryText,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mock Authentication Selector Card (Before Continue as Farmer)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mock_auth_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mock Authentication",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryDarkText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryGreenContainer
                        ) {
                            Text(
                                text = "Instant Access",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Choose a demo farmer profile to test booking and matching without OTP restrictions:",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Profile options
                    demoFarmers.forEach { farmer ->
                        val isSelected = !isCustomProfileMode && selectedFarmer.id == farmer.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldGreen.copy(alpha = 0.08f) else AppBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EmeraldGreen else Color(0xFFE0E0E0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    isCustomProfileMode = false
                                    selectedFarmer = farmer
                                }
                                .testTag("mock_farmer_${farmer.name.replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) EmeraldGreen else Color(0xFFCFD8DC),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = farmer.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = farmer.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) EmeraldGreenDark else PrimaryDarkText
                                        )
                                        Text(
                                            text = "${farmer.address} • +91 ${farmer.mobileNumber}",
                                            fontSize = 11.5.sp,
                                            color = SecondaryText
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        isCustomProfileMode = false
                                        selectedFarmer = farmer
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EmeraldGreen,
                                        unselectedColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Profile toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCustomProfileMode) EmeraldGreen.copy(alpha = 0.08f) else AppBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCustomProfileMode) 1.5.dp else 1.dp,
                            color = if (isCustomProfileMode) EmeraldGreen else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { isCustomProfileMode = true }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (isCustomProfileMode) EmeraldGreen else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Custom Farmer Details (Instant)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isCustomProfileMode) EmeraldGreenDark else PrimaryDarkText
                                    )
                                }
                                RadioButton(
                                    selected = isCustomProfileMode,
                                    onClick = { isCustomProfileMode = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                                )
                            }

                            AnimatedVisibility(visible = isCustomProfileMode) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = customName,
                                        onValueChange = { customName = it },
                                        label = { Text("Farmer Name (e.g. Balasaheb Thorat)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = customMobile,
                                        onValueChange = { customMobile = it },
                                        label = { Text("Mobile Number (e.g. 9811223344)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = customLocation,
                                        onValueChange = { customLocation = it },
                                        label = { Text("Market / Location (e.g. Sangamner Mandi)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val finalFarmer = if (isCustomProfileMode && customName.isNotBlank()) {
                            Farmer(
                                id = "custom_${System.currentTimeMillis()}",
                                name = customName.trim(),
                                address = customLocation.trim().ifBlank { "Maharashtra Agri Market" },
                                mobileNumber = customMobile.filter { it.isDigit() }.ifBlank { "9800000000" },
                                email = "${customName.trim().lowercase().replace(" ", "")}@gmail.com",
                                aadhaarMasked = "XXXX XXXX 5566",
                                mobileVerified = true
                            )
                        } else {
                            selectedFarmer
                        }
                        onContinueAsFarmer(finalFarmer)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("continue_as_farmer_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCustomProfileMode && customName.isNotBlank()) {
                                    "Continue as ${customName.trim().substringBefore(" ")}"
                                } else {
                                    "Continue as ${selectedFarmer.name.substringBefore(" ")}"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onNavigateToRegister,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manual Supabase Sign Up / Sign In", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PlantingAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "planting_transition")
    
    // Total animation duration 4000ms
    val seedDropY by transition.animateFloat(
        initialValue = -120f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                -120f at 0 // Start high
                0f at 600 using FastOutSlowInEasing // Hit ground at 600ms
                0f at 4000 // Stay on ground
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "seed_drop"
    )

    val seedAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                1f at 200 // Fade in while dropping
                1f at 500
                0f at 800 // Fade out after hitting ground
                0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "seed_alpha"
    )

    val plantScale by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 600 // Start growing when seed hits
                1.1f at 1400 using FastOutSlowInEasing // Overshoot slightly
                1f at 1700 using FastOutLinearInEasing // Settle
                1f at 3400 // Hold
                0f at 3800 // Shrink back down to restart
                0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "plant_scale"
    )
    
    // Cloud floating horizontally
    val cloudOffsetX by transition.animateFloat(
        initialValue = -150f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_pan"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements (sun/cloud)
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = AccentEarthyLight,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(48.dp)
            )
            
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = cloudOffsetX.dp, y = 40.dp)
                    .size(64.dp)
            )
        }
        
        // Soil base and plant
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Seed
            Box(
                modifier = Modifier
                    .offset(y = seedDropY.dp - 14.dp) // Offset above ground
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AccentEarthyDark.copy(alpha = seedAlpha.coerceIn(0f, 1f)))
            )

            // Plant
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = "Growing Plant",
                tint = PrimaryGreen,
                modifier = Modifier
                    .offset(y = (-6).dp) // Adjust to sit properly in soil
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = plantScale.coerceAtLeast(0f)
                        scaleY = plantScale.coerceAtLeast(0f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            )
            
            // Soil
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentEarthyDark.copy(alpha = 0.9f))
            )
        }
    }
}
