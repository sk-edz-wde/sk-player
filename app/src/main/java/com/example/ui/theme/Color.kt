package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cyberpunk Neon Palette for SK Edz Player
val CyberBackground = Color(0xFF04050A)
val CyberSurfaceDark = Color(0xFF0B0F19)
val CyberSurfaceCard = Color(0xFF111827)
val CyberCardBg = Color(0xFF111827)
val CyberSurfaceElevated = Color(0xFF1E293B)
val CyberGlassBorder = Color(0x3306B6D4)
val CyberGlassFill = Color(0x0DFFFFFF)

// Neon Primary & Accents
val NeonCyan = Color(0xFF06B6D4)
val NeonCyanBright = Color(0xFF22D3EE)
val NeonIndigo = Color(0xFF6366F1)
val NeonIndigoBright = Color(0xFF818CF8)
val NeonFuchsia = Color(0xFFD946EF)
val NeonPink = Color(0xFFEC4899)
val NeonRose = Color(0xFFF43F5E)

// VIP Gold & Accents
val VipGold = Color(0xFFF59E0B)
val GoldPrimary = Color(0xFFF59E0B)
val VipGoldBright = Color(0xFFFBBF24)
val SuccessGreen = Color(0xFF10B981)

// Neutral Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val CyberGradient = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonIndigo, NeonFuchsia)
)

val HeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))
)

val VinylGrooveGradient = Brush.radialGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0A0F1D), Color(0xFF04050A))
)

val VipGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFDE68A))
)
