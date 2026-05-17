package com.leastcount.loosership.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leastcount.loosership.data.*
import com.leastcount.loosership.ui.components.*
import com.leastcount.loosership.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveGameScreen(
    gameId: Long,
    players: List<Player>,
    playerTotals: List<PlayerWithTotalScore>,
    rounds: List<Round>,
    onAddRound: (Map<Long, Int>, (Boolean, Long?) -> Unit) -> Unit,
    onUndoLastRound: () -> Unit,
    onDeleteGame: () -> Unit,
    onGameEnd: () -> Unit,
    onBack: () -> Unit
) {
    var showAddRoundDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var gameEnded by remember { mutableStateOf(false) }
    var loserInfo by remember { mutableStateOf<Pair<String, Int>?>(null) } // name, score

    // Sort players by total score ascending
    val sortedTotals = playerTotals.sortedBy { it.totalScore }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🎴 Active Game", color = SunnyYellow, style = MaterialTheme.typography.titleLarge)
                        Text("Round ${rounds.size}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (rounds.isNotEmpty()) {
                        IconButton(onClick = onUndoLastRound) {
                            Icon(Icons.Default.Undo, "Undo last round", tint = TangerineOrange)
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Delete game", tint = CoralPink.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            if (!gameEnded) {
                ExtendedFloatingActionButton(
                    onClick = { showAddRoundDialog = true },
                    containerColor = MintGreen,
                    contentColor = DarkBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Round", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Loser banner if game ended
            if (gameEnded && loserInfo != null) {
                item {
                    val (name, score) = loserInfo!!
                    val player = playerTotals.find { it.playerName == name }
                    LoserBanner(
                        playerName = name,
                        emoji = player?.emoji ?: "💀",
                        totalScore = score
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onGameEnd,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
                    ) {
                        Text("Back to Home", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Scoreboard header
            item {
                SectionHeader("📊 Scoreboard")
            }

            // Player standings
            itemsIndexed(sortedTotals) { index, playerTotal ->
                val isInDanger = playerTotal.totalScore >= 80
                val isWarning = playerTotal.totalScore in 60..79
                val bgColor = when {
                    isInDanger -> CoralPink.copy(alpha = 0.1f)
                    isWarning -> TangerineOrange.copy(alpha = 0.08f)
                    index == 0 -> MintGreen.copy(alpha = 0.08f)
                    else -> DarkCard
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank
                        Text(
                            text = "#${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = when (index) {
                                0 -> MintGreen
                                sortedTotals.lastIndex -> CoralPink
                                else -> Color.White.copy(alpha = 0.6f)
                            },
                            fontSize = 14.sp,
                            modifier = Modifier.width(32.dp)
                        )

                        PlayerAvatar(playerTotal.emoji, playerTotal.colorIndex, size = 38)
                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                playerTotal.playerName,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            if (isInDanger) {
                                Text("🔥 Danger zone!", fontSize = 11.sp, color = CoralPink)
                            } else if (isWarning) {
                                Text("⚠️ Getting warm...", fontSize = 11.sp, color = TangerineOrange)
                            } else if (index == 0 && playerTotal.totalScore > 0) {
                                Text("😎 Cruising", fontSize = 11.sp, color = MintGreen)
                            }
                        }

                        // Score with progress bar
                        Column(horizontalAlignment = Alignment.End) {
                            ScoreChip(
                                score = playerTotal.totalScore,
                                isWarning = isWarning,
                                isDanger = isInDanger
                            )
                            Spacer(Modifier.height(4.dp))
                            // Mini progress bar to 101
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                val progress = (playerTotal.totalScore / 101f).coerceIn(0f, 1f)
                                val barColor = when {
                                    isInDanger -> CoralPink
                                    isWarning -> TangerineOrange
                                    else -> MintGreen
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }
            }

            // Round history
            if (rounds.isNotEmpty()) {
                item {
                    SectionHeader("🔄 Rounds", modifier = Modifier.padding(top = 12.dp))
                }
                item {
                    Text(
                        "${rounds.size} round${if (rounds.size != 1) "s" else ""} played",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Add Round Dialog
    if (showAddRoundDialog) {
        AddRoundDialog(
            players = players,
            onDismiss = { showAddRoundDialog = false },
            onConfirm = { scores ->
                showAddRoundDialog = false
                onAddRound(scores) { ended, loserId ->
                    if (ended) {
                        gameEnded = true
                        val loser = playerTotals.find { it.playerId == loserId }
                            ?: sortedTotals.lastOrNull()
                        loserInfo = loser?.let { Pair(it.playerName, it.totalScore) }
                    }
                }
            }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Game?", color = CoralPink) },
            text = { Text("This will permanently delete this game and all its rounds.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteGame()
                    onBack()
                }) {
                    Text("Delete", color = CoralPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRoundDialog(
    players: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Int>) -> Unit
) {
    val scores = remember { mutableStateMapOf<Long, String>().apply {
        players.forEach { put(it.id, "") }
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🎴 Enter Round Scores", color = SunnyYellow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                players.forEach { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PlayerAvatar(player.emoji, player.colorIndex, size = 32)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            player.name,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp
                        )
                        OutlinedTextField(
                            value = scores[player.id] ?: "",
                            onValueChange = { value ->
                                if (value.all { it.isDigit() } && value.length <= 3) {
                                    scores[player.id] = value
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = SkyBlue
                            ),
                            placeholder = {
                                Text("0", textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White.copy(alpha = 0.3f))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedScores = scores.mapValues { (_, v) ->
                        v.toIntOrNull() ?: 0
                    }
                    onConfirm(parsedScores)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
            ) {
                Text("Save Round", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = DarkSurface
    )
}
