package com.leastcount.loosership.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leastcount.loosership.data.*
import com.leastcount.loosership.ui.components.*
import com.leastcount.loosership.ui.theme.*
import com.leastcount.loosership.viewmodel.GameViewModel
import com.leastcount.loosership.viewmodel.GlobalStats
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: GameViewModel,
    players: List<Player>,
    competitionLeaderboard: List<PlayerLossCount>,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var globalStats by remember { mutableStateOf<GlobalStats?>(null) }
    var playerStats by remember { mutableStateOf<Map<Long, PlayerStats>>(emptyMap()) }
    var expandedPlayerId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        globalStats = viewModel.getGlobalStats()
        val statsMap = mutableMapOf<Long, PlayerStats>()
        players.forEach { player ->
            viewModel.getPlayerStats(player.id)?.let { statsMap[player.id] = it }
        }
        playerStats = statsMap
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Statistics", color = SunnyYellow) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Global stats
            item {
                SectionHeader("🌍 Global Stats")
            }

            item {
                val gs = globalStats
                if (gs != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BigStatCard("🎮", "Total Games", gs.totalGamesPlayed.toString(), SkyBlue)
                                BigStatCard("🔥", "Highest Round", gs.highestSingleRoundScore.toString(), CoralPink)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BigStatCard("💀", "Most Losses", "${gs.mostLossesEmoji} ${gs.mostLosses}", TangerineOrange)
                                BigStatCard("🏆", "Biggest Loser", gs.mostLossesPlayer, LavenderPurple)
                            }
                        }
                    }
                }
            }

            // Fun facts
            item {
                val gs = globalStats
                if (gs != null && gs.totalGamesPlayed > 0) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.15f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("🎲 Fun Facts", style = MaterialTheme.typography.titleMedium, color = LavenderPurple)
                            Spacer(Modifier.height(8.dp))

                            val facts = buildList {
                                add("📈 ${gs.highestSingleRoundPlayer} holds the record for highest single round score (${gs.highestSingleRoundScore} pts)")
                                if (gs.mostLosses > 0) {
                                    add("👑 ${gs.mostLossesPlayer} has earned the crown of shame with ${gs.mostLosses} losses")
                                }
                                add("🃏 A total of ${gs.totalGamesPlayed} game${if (gs.totalGamesPlayed != 1) "s" else ""} have been played in this competition")

                                val topLoser = competitionLeaderboard.firstOrNull()
                                val secondLoser = competitionLeaderboard.getOrNull(1)
                                if (topLoser != null && secondLoser != null && topLoser.lossCount > secondLoser.lossCount) {
                                    val diff = topLoser.lossCount - secondLoser.lossCount
                                    add("📏 ${topLoser.playerName} leads the losership by $diff game${if (diff != 1) "s" else ""} over ${secondLoser.playerName}")
                                }
                            }

                            facts.forEach { fact ->
                                Text(
                                    fact,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Per-player stats
            item {
                SectionHeader("👤 Player Stats", modifier = Modifier.padding(top = 8.dp))
            }

            items(players) { player ->
                val stats = playerStats[player.id]
                val isExpanded = expandedPlayerId == player.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedPlayerId = if (isExpanded) null else player.id
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAvatar(player.emoji, player.colorIndex, size = 42)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    player.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                if (stats != null) {
                                    Text(
                                        getLoserTitle(stats.totalLosses),
                                        fontSize = 12.sp,
                                        color = PlayerColors.getOrElse(player.colorIndex) { SkyBlue }.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Expand",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        AnimatedVisibility(isExpanded) {
                            if (stats != null) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                    StatRow("🎮 Games Played", stats.totalGamesPlayed.toString())
                                    StatRow("💀 Total Losses", stats.totalLosses.toString(), CoralPink)
                                    StatRow("📊 Win Rate", if (stats.totalGamesPlayed > 0)
                                        "${((stats.totalGamesPlayed - stats.totalLosses) * 100 / stats.totalGamesPlayed)}%" else "N/A", MintGreen)
                                    StatRow("📈 Total Points", stats.totalPointsAccumulated.toString())
                                    StatRow("📉 Avg/Game", String.format("%.1f", stats.avgPointsPerGame))
                                    StatRow("🔥 Highest Round", stats.highestRoundScore.toString(), CoralPink)
                                    StatRow("❄️ Lowest Round", stats.lowestRoundScore.toString(), MintGreen)

                                    if (stats.currentLosingStreak > 0) {
                                        StatRow("🔥 Current Losing Streak", "${stats.currentLosingStreak} games", CoralPink)
                                    }
                                    if (stats.maxLosingStreak > 0) {
                                        StatRow("💀 Max Losing Streak", "${stats.maxLosingStreak} games", TangerineOrange)
                                    }
                                }
                            } else {
                                Text(
                                    "No stats yet — play some games!",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BigStatCard(emoji: String, label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(140.dp)
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = color,
            textAlign = TextAlign.Center
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
