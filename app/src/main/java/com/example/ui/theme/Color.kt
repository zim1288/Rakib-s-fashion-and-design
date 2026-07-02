package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// "Natural Tones" Design Palette
val SlateDark = Color(0xFF2D2926)         // Cozy charcoal/slate black for high contrast
val EspressoDark = Color(0xFF4A3728)      // Rich dark cocoa/espresso
val TobaccoSaddle = Color(0xFF704214)     // Earthy saddle brown (Primary Accent)
val WarmBeige = Color(0xFF8C7E6D)         // Khaki beige/timber wolf (Secondary Accent)
val SoftEggshell = Color(0xFFFAF9F6)      // Extremely light cream background
val AntiqueCream = Color(0xFFEAE3D9)      // Soft clay border tone
val SageGreen = Color(0xFFD4E0D2)         // Pale sage green (Purchase highlights)
val TerracottaPink = Color(0xFFF8E1D8)    // Muted terracotta/peach clay (Sell highlights)

// Remap existing reference tokens to maintain complete compilation logic seamlessly
val RoyalCrimson = TobaccoSaddle          // The primary warm accent
val GoldAccent = WarmBeige                // The secondary natural tone
val WarmGold = AntiqueCream               // Light clay beige
val CardinalRed = Color(0xFFC87050)       // Earthy natural terracotta highlight red

// Light Scheme Colors
val PrimaryLight = TobaccoSaddle
val SecondaryLight = WarmBeige
val TertiaryLight = SageGreen
val BackgroundLight = SoftEggshell
val SurfaceLight = Color.White
val OnPrimaryLight = Color.White
val OnSecondaryLight = SlateDark

// Dark Scheme Colors
val PrimaryDark = AntiqueCream
val SecondaryDark = WarmBeige
val TertiaryDark = TerracottaPink
val BackgroundDark = SlateDark
val SurfaceDark = Color(0xFF383330)
val OnPrimaryDark = SlateDark
val OnSecondaryDark = SlateDark
