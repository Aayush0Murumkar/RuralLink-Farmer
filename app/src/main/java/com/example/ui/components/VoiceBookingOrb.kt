package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A highly aesthetic, Siri-inspired glowing AI Voice Orb with multi-layered 
 * vibrant gradients (cyan, purple, pink/magenta, glowing white core) inside a circular dark orb.
 */
@Composable
fun VoiceBookingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isListening: Boolean = false,
    showMicIcon: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbAnimation")
    
    // Rotation animation for fluid multi-color waves inside the orb
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation for listening state
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow alpha animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .shadow(
                elevation = if (isListening) 16.dp else 8.dp,
                shape = CircleShape,
                ambientColor = Color(0xFFFF1E6C),
                spotColor = Color(0xFF00E5FF)
            )
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val center = Offset(width / 2f, height / 2f)
            val radius = width / 2f

            // 1. Dark Base Sphere Background (Deep Midnight Black / Purple)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF180A2E),
                        Color(0xFF0B0316),
                        Color(0xFF040108)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 2. Cyan / Turquoise Wave Layer (Top Left)
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00F2FE).copy(alpha = 0.85f),
                            Color(0xFF4FACFE).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                        radius = radius * 0.75f
                    ),
                    radius = radius * 0.75f,
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f)
                )

                // 3. Pink / Magenta Wave Layer (Bottom Left to Center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF0844).copy(alpha = 0.9f),
                            Color(0xFFFF4E50).copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = Offset(center.x - radius * 0.25f, center.y + radius * 0.35f),
                        radius = radius * 0.8f
                    ),
                    radius = radius * 0.8f,
                    center = Offset(center.x - radius * 0.25f, center.y + radius * 0.35f)
                )

                // 4. Purple / Violet Blob (Top Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA855F7).copy(alpha = 0.85f),
                            Color(0xFF7C3AED).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(center.x + radius * 0.35f, center.y - radius * 0.25f),
                        radius = radius * 0.75f
                    ),
                    radius = radius * 0.75f,
                    center = Offset(center.x + radius * 0.35f, center.y - radius * 0.25f)
                )

                // 5. Electric Blue Blob (Bottom Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.8f),
                            Color(0xFF00C6FF).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(center.x + radius * 0.3f, center.y + radius * 0.3f),
                        radius = radius * 0.7f
                    ),
                    radius = radius * 0.7f,
                    center = Offset(center.x + radius * 0.3f, center.y + radius * 0.3f)
                )
            }

            // 6. Luminous Center Core Flare (Bright White Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color(0xFFE0F7FA).copy(alpha = 0.8f),
                        Color(0xFFF48FB1).copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.45f
                ),
                radius = radius * 0.45f,
                center = center
            )

            // 7. Outer Glassy Rim Stroke with Gradient
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = glowAlpha),
                        Color(0xFFFF4081).copy(alpha = glowAlpha),
                        Color(0xFFA855F7).copy(alpha = glowAlpha),
                        Color(0xFF00E5FF).copy(alpha = glowAlpha)
                    ),
                    center = center
                ),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Overlay Mic Icon if requested
        if (showMicIcon) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Assistant",
                tint = Color.White,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}
