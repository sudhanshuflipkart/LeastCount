package com.leastcount.loosership.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// === Playful Color Palette ===
val CoralPink = Color(0xFFFF6B6B)
val SunnyYellow = Color(0xFFFFD93D)
val MintGreen = Color(0xFF6BCB77)
val SkyBlue = Color(0xFF4D96FF)
val LavenderPurple = Color(0xFFBB6BD9)
val TangerineOrange = Color(0xFFFF8C42)
val HotPink = Color(0xFFFF4081)
val DeepPurple = Color(0xFF7C3AED)

val DarkBackground = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF16213E)
val DarkCard = Color(0xFF0F3460)
val LightGold = Color(0xFFFFE66D)

// Player colors (vibrant)
val PlayerColors = listOf(
    Color(0xFFFF6B6B), // Coral
    Color(0xFF4D96FF), // Blue
    Color(0xFF6BCB77), // Green
    Color(0xFFFFD93D), // Yellow
    Color(0xFFBB6BD9), // Purple
    Color(0xFFFF8C42), // Orange
)

// Player emojis
val PlayerEmojis = listOf("🃏", "🎴", "🂡", "🎯", "🎲", "🎪", "🎭", "🎨", "🦊", "🐼", "🦁", "🐸")

// Fun titles for the biggest loser
val LoserTitles = listOf(
    "👑 Supreme Loser",
    "🏆 Loser Champion",
    "💀 Card Catastrophe",
    "🤡 Joker of the Pack",
    "🗑️ Trash Hand King",
    "😭 Weeping Winner... of Losses",
)

fun getLoserTitle(lossCount: Int): String {
    return when {
        lossCount >= 50 -> "🐐 GOAT of Losing"
        lossCount >= 30 -> "👑 Supreme Loser"
        lossCount >= 20 -> "🏆 Loser Champion"
        lossCount >= 10 -> "💀 Card Catastrophe"
        lossCount >= 5 -> "🤡 Joker of the Pack"
        lossCount >= 1 -> "😢 First-time Loser"
        else -> "😎 Untouched"
    }
}

// Dark color scheme (primary theme)
private val DarkColorScheme = darkColorScheme(
    primary = CoralPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF93000A),
    secondary = SunnyYellow,
    onSecondary = Color.Black,
    tertiary = MintGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFFF5252)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun LeastCountLoosership(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
