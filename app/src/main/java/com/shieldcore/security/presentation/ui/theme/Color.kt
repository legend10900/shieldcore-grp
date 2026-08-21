package com.shieldcore.security.presentation.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Backgrounds & Base Surfaces (Deep Space / Cyber Midnight)
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF0F172A)
val DarkSurfaceElevated = Color(0xFF162036)
val DarkCardSurface = Color(0xFF1B2742)
val DarkCardBorder = Color(0xFF2E3D60)
val DarkGlassSurface = Color(0xCC131D33)
val DarkGlassBorder = Color(0x4038BDF8)

// Neon & Vivid Accent Colors
val NeonCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF0284C7)
val SkyBlue = Color(0xFF38BDF8)
val DeepCyan = Color(0xFF0891B2)

val MatrixGreen = Color(0xFF10B981)
val NeonLime = Color(0xFF00F59B)
val EmeraldLight = Color(0xFF34D399)
val DarkEmerald = Color(0xFF065F46)

val ElectricViolet = Color(0xFF8B5CF6)
val NeonIndigo = Color(0xFF6366F1)
val PurpleGlow = Color(0xFFA855F7)
val DeepViolet = Color(0xFF4C1D95)

val RadiantAmber = Color(0xFFF59E0B)
val NeonOrange = Color(0xFFFF6B00)
val SunsetGold = Color(0xFFFBBF24)
val DarkAmber = Color(0xFF78350F)

val LaserRed = Color(0xFFEF4444)
val CrimsonGlow = Color(0xFFFF3366)
val RoseRed = Color(0xFFF43F5E)
val DarkCrimson = Color(0xFF7F1D1D)

val CoralTeal = Color(0xFF14B8A6)
val MagentaGlow = Color(0xFFD946EF)

// Text & Neutral Tints
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val BorderSubtle = Color(0xFF1E293B)
val BorderHighlight = Color(0xFF334155)

// Linear & Sweep Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, NeonIndigo)
)

val ShieldCyanGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF0284C7))
)

val SafeGreenGradient = Brush.horizontalGradient(
    colors = listOf(NeonLime, MatrixGreen)
)

val DangerRedGradient = Brush.horizontalGradient(
    colors = listOf(CrimsonGlow, LaserRed)
)

val WarningAmberGradient = Brush.horizontalGradient(
    colors = listOf(SunsetGold, NeonOrange)
)

val VioletPurpleGradient = Brush.horizontalGradient(
    colors = listOf(PurpleGlow, ElectricViolet)
)

val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E2C4A), Color(0xFF111A2E))
)

val GlassBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0x8038BDF8),
        Color(0x20818CF8),
        Color(0x4000E5FF)
    )
)

val HeroRadarGradient = Brush.radialGradient(
    colors = listOf(
        Color(0x3000E5FF),
        Color(0x1000E5FF),
        Color.Transparent
    )
)
