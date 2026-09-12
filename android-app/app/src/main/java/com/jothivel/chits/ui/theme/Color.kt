package com.jothivel.chits.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ── Core Maroon Palette ──────────────────────────────────────────────────────
val MaroonPrimary = Color(0xFF761F29)
val MaroonDark = Color(0xFF51131B)
val MaroonLight = Color(0xFF8E2D37)
val MaroonBackground = Color(0xFFF8F6F3)
val MaroonSurfaceLight = Color(0xFFF8EDEF)
val MaroonDeep = Color(0xFF3A0E0E)

// ── Gradient Brushes ─────────────────────────────────────────────────────────
val MaroonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B2E2E), Color(0xFF6B1E1E))
)
val LightMaroonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFDF6F6), Color(0xFFF5E4E4))
)
val DeepMaroonGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF6B1E1E), Color(0xFF4A1414), Color(0xFF3A0E0E))
)
val LoginGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF7B2525),
        Color(0xFF6B1E1E),
        Color(0xFF4A1414),
        Color(0xFF3A0E0E)
    )
)
val GoldShimmerGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFD4AF37),
        Color(0xFFE8C84A),
        Color(0xFFF0D860),
        Color(0xFFE8C84A),
        Color(0xFFD4AF37)
    )
)

// ── Accent Colors ────────────────────────────────────────────────────────────
val AccentGold = Color(0xFFB78618)
val AccentGoldLight = Color(0xFFE8C84A)
val AccentGreen = Color(0xFF237A45)
val AccentGreenLight = Color(0xFFE8F5E9)
val AccentRed = Color(0xFFBE3341)
val AccentRedLight = Color(0xFFFFEBEE)
val AccentOrange = Color(0xFFF57C00)

// ── Neutral Colors ───────────────────────────────────────────────────────────
val OffWhite = Color(0xFFF8F9FA)
val TextGray = Color(0xFF6F6A68)
val TextLightGray = Color(0xFFBDBDBD)
val DividerGray = Color(0xFFE7E1DE)
val CardWhite = Color(0xFFFFFFFF)
val SurfaceElevated = Color(0xFFF5F5F5)

// ── Glassmorphism ────────────────────────────────────────────────────────────
val GlassWhite = Color(0x33FFFFFF)
val GlassWhiteStrong = Color(0x66FFFFFF)
val GlassDark = Color(0x1A000000)

// ── PIN Dot Colors ───────────────────────────────────────────────────────────
val PinDotEmpty = Color(0x44FFFFFF)
val PinDotFilled = Color(0xFFD4AF37)
val PinDotError = Color(0xFFFF6B6B)
