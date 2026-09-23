package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Minimal, Aesthetically Pleasing Agriculture Palette
val PrimaryGreen = Color(0xFF2E5C43) // Deep Forest Green
val PrimaryGreenLight = Color(0xFF437A59) // Moss Green
val PrimaryGreenDark = Color(0xFF1E3F2D) // Dark Pine
val PrimaryGreenContainer = Color(0xFFE6F0EA) // Soft Sage
val OnPrimaryGreenContainer = Color(0xFF0F261A) // Deepest Green

val AccentEarthy = Color(0xFFD97706) // Harvest Amber / Orange
val AccentEarthyLight = Color(0xFFFBBF24) // Wheat Yellow
val AccentEarthyDark = Color(0xFFB45309) // Soil Brown
val AccentEarthyContainer = Color(0xFFFEF3C7) // Pale Corn
val OnAccentEarthy = Color(0xFF78350F) // Dark Earth

// Surfaces & Backgrounds
val AppBackground = Color(0xFFF9F9F6) // Warm Off-White / Cream
val AppSurface = Color(0xFFFFFFFF) // White
val CardBackground = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFE5E8E5) // Light Gray-Green
val CardBorderSubtle = Color(0xFFF2F4F2) // Very Light Gray-Green
val CardShadow = Color(0x0A000000)

// Typography & Text
val PrimaryDarkText = Color(0xFF1A2620) // Almost Black Green
val SecondaryText = Color(0xFF55695E) // Muted Sage Gray
val MutedText = Color(0xFF94A39B) // Light Sage Gray
val LightText = Color(0xFFFFFFFF)

// Semantic Status Colors
val SuccessGreen = Color(0xFF10B981) // Emerald 500
val SuccessGreenBg = Color(0xFFD1FAE5) // Emerald 100
val ErrorRed = Color(0xFFEF4444) // Red 500
val WarningOrange = Color(0xFFF59E0B) // Amber 500
val InfoBlue = Color(0xFF3B82F6) // Blue 500

// Status Card Badges
val StatusPendingBg = Color(0xFFF1F5F9) // Slate 100
val StatusPendingFg = Color(0xFF475569) // Slate 600
val StatusRespondedBg = AccentEarthyContainer
val StatusRespondedFg = OnAccentEarthy
val StatusMatchedBg = SuccessGreenBg
val StatusMatchedFg = Color(0xFF065F46) // Emerald 800

// Legacy Aliases mapped to new green style
val EmeraldGreen = PrimaryGreen
val EmeraldGreenLight = PrimaryGreenLight
val EmeraldGreenDark = PrimaryGreenDark
val EmeraldGreenContainer = PrimaryGreenContainer
val OnEmeraldGreenContainer = OnPrimaryGreenContainer

val BrightLeaf = AccentEarthy
val BrightLeafLight = AccentEarthyLight
val BrightLeafDark = AccentEarthyDark
val BrightLeafContainer = AccentEarthyContainer
val OnBrightLeaf = OnAccentEarthy

val PrimaryDeepTeal = PrimaryGreen
val PrimaryEmerald = PrimaryGreenLight
val SecondaryTeal = PrimaryGreenLight
val LightTeal = PrimaryGreenContainer
val VeryLightTeal = Color(0xFFF9F9F6)
val DarkTealText = PrimaryGreenDark
val GoldAccent = AccentEarthy
val LightGold = AccentEarthyLight
val AgroGreenPrimary = PrimaryGreen
val AgroGreenSecondary = PrimaryGreenLight
val AgroGreenContainer = PrimaryGreenContainer
val OnAgroGreenContainer = OnPrimaryGreenContainer
val HarvestAmberPrimary = AccentEarthy
val HarvestAmberContainer = AccentEarthyContainer
val OnHarvestAmberContainer = OnAccentEarthy

val AppPrimaryWhite = Color(0xFFFFFFFF)
val AppOnPrimaryDark = PrimaryDarkText
val AppPrimaryContainer = PrimaryGreenContainer
val AppOnPrimaryContainer = OnPrimaryGreenContainer
val AppSecondaryBrown = PrimaryGreen
val AppOnSecondaryWhite = Color(0xFFFFFFFF)
val AppSecondaryContainer = PrimaryGreenContainer
val AppOnSecondaryContainer = OnPrimaryGreenContainer
val AppTertiaryGreen = SuccessGreen
val AppOnTertiaryWhite = Color(0xFFFFFFFF)
val AppTertiaryContainer = SuccessGreenBg
val AppOnTertiaryContainer = Color(0xFF065F46)

val EarthySurface = AppBackground
val EarthyCardSurface = CardBackground
val EarthyBorder = CardBorder
