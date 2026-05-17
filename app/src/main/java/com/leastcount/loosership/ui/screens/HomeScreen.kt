package com.leastcount.loosership.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leastcount.loosership.data.*
import com.leastcount.loosership.ui.components.*
import com.leastcount.loosership.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    competitionLeaderboard: List<PlayerLossCount>,
    recentGames: List<GameWithDetails>,
    activeGameId: Long?,
    onNewGame: () -> Unit,
    onContinueGame: (Long) -> Unit,
    onGameDetail: (Long) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToPlayers: () -> Unit
) {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val todayGames = recentGames.filter { it.date == today }
    val todayLosses = todayGames.filter { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "🃏 Least Count",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SunnyYellow
                        )
                        Text(
                            "Loosership",
                            style = MaterialTheme.typography.labelMedium,
                            color = CoralPink,
                            letterSpacing = 3.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, "Stats", tint = SunnyYellow)
                    }
                    IconButton(onClick = onNavigateToPlayers) {
                        Icon(Icons.Default.People, "Players", tint = SunnyYellow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            if (activeGameId == null) {
                ExtendedFloatingActionButton(
                    onClick = onNewGame,
                    containerColor = CoralPink,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Game", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active game banner
            if (activeGameId != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContinueGame(activeGameId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PulsingDot(MintGreen)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Game in Progress!", fontWeight = FontWeight.Bold, color = MintGreen)
                                Text("Tap to continue", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ArrowForward, null, tint = MintGreen)
                        }
                    }
                }
            }

            // Today's summary
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("📅 Today", style = MaterialTheme.typography.titleLarge, color = SunnyYellow)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBubble("Games", todayGames.size.toString(), SkyBlue)
                            StatBubble("Completed", todayLosses.size.toString(), MintGreen)
                            val todayLoser = todayLosses.groupBy { it.loserId }
                                .maxByOrNull { it.value.size }
                            StatBubble(
                                "Top Loser",
                                todayLoser?.value?.firstOrNull()?.loserEmoji ?: "—",
                                CoralPink
                            )
                        }
                    }
                }
            }

            // Competition Loser Board
            item {
                SectionHeader("🏆 Loser Board", modifier = Modifier.padding(top = 8.dp))
            }

            if (competitionLeaderboard.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🎴",
                        title = "No losers yet!",
                        subtitle = "Start a game to crown the first loser"
                    )
                }
            } else {
                itemsIndexed(competitionLeaderboard) { index, entry ->
                    LeaderboardRow(
                        rank = index + 1,
                        entry = entry,
                        isTopLoser = index == 0 && entry.lossCount > 0
                    )
                }
            }

            // Recent games
            item {
                SectionHeader("🕹️ Recent Games", modifier = Modifier.padding(top = 8.dp))
            }

            if (recentGames.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🃏",
                        title = "No games yet",
                        subtitle = "Hit 'New Game' to start playing!"
                    )
                }
            } else {
                items(recentGames.take(10)) { game ->
                    GameHistoryRow(
                        game = game,
                        onClick = { onGameDetail(game.gameId) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StatBubble(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = if (value.length <= 2) 28.sp else 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: PlayerLossCount,
    isTopLoser: Boolean
) {
    val bgColor = if (isTopLoser) CoralPink.copy(alpha = 0.1f) else DarkCard
    val borderColor = if (isTopLoser) CoralPink.copy(alpha = 0.3f) else Color.Transparent

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isTopLoser) androidx.compose.foundation.BorderStroke(
            2.dp, Brush.horizontalGradient(listOf(CoralPink, TangerineOrange))
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank)
            Spacer(Modifier.width(12.dp))
            PlayerAvatar(entry.emoji, entry.colorIndex, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.playerName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                if (isTopLoser && entry.lossCount > 0) {
                    Text(
                        getLoserTitle(entry.lossCount),
                        fontSize = 12.sp,
                        color = CoralPink.copy(alpha = 0.8f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${entry.lossCount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = if (entry.lossCount > 0) CoralPink else MintGreen
                )
                Text("losses", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun GameHistoryRow(
    game: GameWithDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SkyBlue.copy(alpha = 0.15f)
            ) {
                Text(
                    text = formatDate(game.date),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = SkyBlue,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (game.isCompleted) {
                    Text(
                        "${game.loserEmoji ?: "💀"} ${game.loserName ?: "Unknown"} lost!",
                        fontWeight = FontWeight.SemiBold,
                        color = CoralPink,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(MintGreen, Modifier.padding(end = 6.dp))
                        Text("In Progress", color = MintGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                Text(
                    "${game.playerCount} players",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (e: Exception) {
        dateStr
    }
}
