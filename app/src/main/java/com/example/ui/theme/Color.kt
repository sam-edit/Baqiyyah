package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Islamic Emerald & Teal Palette (Geometric Balance)
val EmeraldPrimary: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF2DD4A2) else Color(0xFF135A46)

val EmeraldDark: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF1B8A6A) else Color(0xFF0B3A2C)

val EmeraldLight: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF4EE2B7) else Color(0xFF1D775E)

val EmeraldContainer: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF134235) else Color(0xFFD9EFE6)

val OnEmeraldContainer: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFC7F9E9) else Color(0xFF052A20)

// Geometric Balance Neutral Palette
val MintBackground: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF0F1715) else Color(0xFFF7F9F8)

val MintSurface: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF172420) else Color(0xFFFFFFFF)

val MintSurfaceVariant: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF1F302B) else Color(0xFFEFF3F1)

val MintBorder: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF2E453E) else Color(0xFFDEE5E1)

// Gold & Warm Geometric Accents
val GoldAccent: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFFACC15) else Color(0xFFC59419)

val GoldLight: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF3E310C) else Color(0xFFFFF7E3)

val AmberHighlight: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFFBBF24) else Color(0xFFD97706)

// Text Colors (Geometric Balance)
val TextPrimary: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFF0F6F3) else Color(0xFF2D312E)

val TextSecondary: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFA0B5AC) else Color(0xFF5F6964)

val TextTertiary: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF768E85) else Color(0xFF8E9A95)

val TextArabic: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFF0F6F3) else Color(0xFF1A332A)

// Functional & Status Colors
val GreenActive: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF34D399) else Color(0xFF21825B)

val RedForbidden: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFFF87171) else Color(0xFFC24134)

val RedForbiddenLight: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF381A1A) else Color(0xFFFDF4F3)

val BluePrayer: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF38BDF8) else Color(0xFF2575A5)

val BluePrayerLight: Color
    @Composable get() = if (LocalAppIsDark.current) Color(0xFF0F2D3D) else Color(0xFFEEF5FA)

// Gradients (Geometric Balance)
val DashboardGradient: Brush
    @Composable get() = if (LocalAppIsDark.current) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF123D32),
                Color(0xFF1B5244)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F503E),
                Color(0xFF176550)
            )
        )
    }

val CardSoftGradient: Brush
    @Composable get() = if (LocalAppIsDark.current) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A2824),
                Color(0xFF15211E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF7F9F8)
            )
        )
    }

val ActivePrayerGradient: Brush
    @Composable get() = if (LocalAppIsDark.current) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF183B30),
                Color(0xFF1B4538)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFE8F4EF),
                Color(0xFFDCEFE7)
            )
        )
    }

val GoldSubtleGradient: Brush
    @Composable get() = if (LocalAppIsDark.current) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF3B321B),
                Color(0xFF2E2612)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFF9EE),
                Color(0xFFFFF2D6)
            )
        )
    }

val ForbiddenCardGradient: Brush
    @Composable get() = if (LocalAppIsDark.current) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E1C1B),
                Color(0xFF241514)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFBFB),
                Color(0xFFFDF4F3)
            )
        )
    }


