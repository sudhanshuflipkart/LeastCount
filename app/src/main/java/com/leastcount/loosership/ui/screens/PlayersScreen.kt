package com.leastcount.loosership.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leastcount.loosership.data.Player
import com.leastcount.loosership.ui.components.*
import com.leastcount.loosership.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    players: List<Player>,
    onAddPlayer: (String, String, Int) -> Unit,
    onUpdatePlayer: (Player) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👥 Players", color = SunnyYellow) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MintGreen,
                contentColor = DarkBackground,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Player", fontWeight = FontWeight.Bold)
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
                    subtitle = "Add your friends to start playing"
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
                        "${players.count { it.isActive }} active • ${players.count { !it.isActive }} inactive",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(players) { player ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (player.isActive) DarkCard else DarkSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAvatar(
                                player.emoji,
                                player.colorIndex,
                                size = 44
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    player.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (player.isActive) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    if (player.isActive) "Active" else "Inactive",
                                    fontSize = 12.sp,
                                    color = if (player.isActive) MintGreen.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
                                )
                            }
                            // Edit button
                            IconButton(onClick = { editingPlayer = player }) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Edit",
                                    tint = SkyBlue.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Active toggle
                            Switch(
                                checked = player.isActive,
                                onCheckedChange = { onToggleActive(player.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MintGreen,
                                    checkedTrackColor = MintGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Add player dialog
    if (showAddDialog) {
        PlayerDialog(
            title = "Add Player",
            initialName = "",
            initialEmoji = PlayerEmojis[0],
            initialColorIndex = 0,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, emoji, colorIndex ->
                onAddPlayer(name, emoji, colorIndex)
                showAddDialog = false
            }
        )
    }

    // Edit player dialog
    editingPlayer?.let { player ->
        PlayerDialog(
            title = "Edit Player",
            initialName = player.name,
            initialEmoji = player.emoji,
            initialColorIndex = player.colorIndex,
            onDismiss = { editingPlayer = null },
            onConfirm = { name, emoji, colorIndex ->
                onUpdatePlayer(player.copy(name = name, emoji = emoji, colorIndex = colorIndex))
                editingPlayer = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    initialColorIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedEmoji by remember { mutableStateOf(initialEmoji) }
    var selectedColorIndex by remember { mutableStateOf(initialColorIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = SunnyYellow, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("Name", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = SkyBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Emoji picker
                Text("Pick an avatar:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PlayerEmojis) { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) SkyBlue.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.dp, SkyBlue, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }

                // Color picker
                Text("Pick a color:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayerColors.forEachIndexed { index, color ->
                        val isSelected = index == selectedColorIndex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }

                // Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Preview: ", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    PlayerAvatar(selectedEmoji, selectedColorIndex, size = 36)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        name.ifEmpty { "Player Name" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedEmoji, selectedColorIndex)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
            ) {
                Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
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
