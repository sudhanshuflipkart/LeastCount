package com.leastcount.loosership.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leastcount.loosership.data.Player
import com.leastcount.loosership.ui.components.*
import com.leastcount.loosership.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameScreen(
    players: List<Player>,
    onStartGame: (List<Long>) -> Unit,
    onBack: () -> Unit
) {
    var selectedPlayerIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎴 New Game", color = SunnyYellow) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "${selectedPlayerIds.size} players selected",
                        color = if (selectedPlayerIds.size >= 2) MintGreen else Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = { onStartGame(selectedPlayerIds.toList()) },
                        enabled = selectedPlayerIds.size >= 2,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPink,
                            disabledContainerColor = CoralPink.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            "🃏 Deal the Cards!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        if (players.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "👤",
                    title = "No players yet!",
                    subtitle = "Add some players in the Players screen first"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Pick who's playing today:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(players) { player ->
                    val isSelected = player.id in selectedPlayerIds
                    val borderColor = if (isSelected)
                        PlayerColors.getOrElse(player.colorIndex) { SkyBlue }
                    else Color.Transparent

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedPlayerIds = if (isSelected) {
                                    selectedPlayerIds - player.id
                                } else {
                                    selectedPlayerIds + player.id
                                }
                            }
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp, borderColor, RoundedCornerShape(14.dp)
                                ) else Modifier
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) DarkCard else DarkSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAvatar(player.emoji, player.colorIndex, size = 44)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                player.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedVisibility(isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "Selected",
                                    tint = MintGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
