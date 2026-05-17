package com.leastcount.loosership.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leastcount.loosership.ui.theme.*

@Composable
fun PlayerAvatar(
    emoji: String,
    colorIndex: Int,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    val color = PlayerColors.getOrElse(colorIndex) { PlayerColors[0] }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.4f))
                )
            )
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size / 2).sp
        )
    }
}

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    glowColor: Color = CoralPink,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = glowColor.copy(alpha = 0.3f),
                spotColor = glowColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    emoji: String = "",
    modifier: Modifier = Modifier
) {
    Text(
        text = if (emoji.isNotEmpty()) "$emoji $title" else title,
        style = MaterialTheme.typography.headlineMedium,
        color = SunnyYellow,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val (bgColor, text) = when (rank) {
        1 -> Pair(Color(0xFFFFD700), "💀 1st")  // Gold - biggest loser!
        2 -> Pair(Color(0xFFC0C0C0), "😰 2nd")
        3 -> Pair(Color(0xFFCD7F32), "😅 3rd")
        else -> Pair(Color(0xFF555555), "#$rank")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = bgColor
        )
    }
}

@Composable
fun ScoreChip(
    score: Int,
    isWarning: Boolean = false,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = when {
        isDanger -> CoralPink
        isWarning -> TangerineOrange
        score == 0 -> MintGreen
        else -> SkyBlue
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = score.toString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 16.sp
        )
    }
}

@Composable
fun PulsingDot(
    color: Color = MintGreen,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoserBanner(
    playerName: String,
    emoji: String,
    totalScore: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loserBanner")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loserScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoralPink.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💀 LOSER ALERT 💀", fontSize = 14.sp, color = CoralPink, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "$emoji $playerName",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "busted with $totalScore points!",
                fontSize = 16.sp,
                color = CoralPink.copy(alpha = 0.8f)
            )
        }
    }
}
