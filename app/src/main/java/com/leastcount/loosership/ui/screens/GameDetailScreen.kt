package com.leastcount.loosership.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.leastcount.loosership.data.*
import com.leastcount.loosership.ui.components.*
import com.leastcount.loosership.ui.theme.*
import com.leastcount.loosership.viewmodel.GameShareData
import com.leastcount.loosership.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: Long,
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var game by remember { mutableStateOf<com.leastcount.loosership.data.Game?>(null) }
    var playerTotals by remember { mutableStateOf<List<PlayerWithTotalScore>>(emptyList()) }
    var roundsWithScores by remember { mutableStateOf<List<RoundWithScores>>(emptyList()) }
    var expandedRounds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        game = viewModel.getGameById(gameId)
        playerTotals = viewModel.getPlayerTotalsForGame(gameId)
        roundsWithScores = viewModel.getRoundsWithScores(gameId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🎴 Game Details",
                        color = SunnyYellow,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Share button
                    IconButton(onClick = {
                        scope.launch {
                            val shareData = viewModel.getGameShareData(gameId)
                            if (shareData != null) {
                                shareGameAsImage(context, shareData)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = SkyBlue)
                    }
                    // Delete button
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = CoralPink.copy(alpha = 0.7f))
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Game info
            item {
                val g = game
                if (g != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📅 ${g.date}", color = SkyBlue, fontWeight = FontWeight.Medium)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (g.isCompleted) CoralPink.copy(alpha = 0.15f) else MintGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        if (g.isCompleted) "Completed" else "In Progress",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = if (g.isCompleted) CoralPink else MintGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${roundsWithScores.size} rounds • ${playerTotals.size} players",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Loser banner for completed games
            val g = game
            if (g?.isCompleted == true) {
                val loser = playerTotals.find { it.playerId == g.loserId }
                if (loser != null) {
                    item {
                        LoserBanner(
                            playerName = loser.playerName,
                            emoji = loser.emoji,
                            totalScore = loser.totalScore
                        )
                    }
                }
            }

            // Final standings
            item {
                SectionHeader("📊 Final Standings")
            }

            val sortedTotals = playerTotals.sortedBy { it.totalScore }
            itemsIndexed(sortedTotals) { index, pt ->
                val isLoser = pt.playerId == game?.loserId
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLoser) CoralPink.copy(alpha = 0.1f) else DarkSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RankBadge(index + 1)
                        Spacer(Modifier.width(10.dp))
                        PlayerAvatar(pt.emoji, pt.colorIndex, size = 36)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            pt.playerName,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (isLoser) {
                            Text("💀 ", fontSize = 16.sp)
                        }
                        ScoreChip(
                            score = pt.totalScore,
                            isDanger = isLoser
                        )
                    }
                }
            }

            // Rounds (expandable)
            item {
                SectionHeader("🔄 Round Details", modifier = Modifier.padding(top = 8.dp))
            }

            items(roundsWithScores) { round ->
                val isExpanded = round.roundId in expandedRounds

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedRounds = if (isExpanded)
                                expandedRounds - round.roundId
                            else
                                expandedRounds + round.roundId
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Round ${round.roundNumber}",
                                fontWeight = FontWeight.Bold,
                                color = SkyBlue,
                                modifier = Modifier.weight(1f)
                            )
                            // Show mini scores inline
                            round.scores.forEach { scoreInfo ->
                                Text(
                                    "${scoreInfo.emoji}${scoreInfo.score}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Expand",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(isExpanded) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                round.scores.sortedByDescending { it.score }.forEach { scoreInfo ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlayerAvatar(scoreInfo.emoji, scoreInfo.colorIndex, size = 28)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            scoreInfo.playerName,
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.weight(1f),
                                            fontSize = 14.sp
                                        )
                                        ScoreChip(
                                            score = scoreInfo.score,
                                            isDanger = scoreInfo.score >= 30,
                                            isWarning = scoreInfo.score in 15..29
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Game?", color = CoralPink) },
            text = { Text("This will permanently delete this game.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteGame(gameId)
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

// --- Share as Image ---
private suspend fun shareGameAsImage(context: Context, data: GameShareData) {
    withContext(Dispatchers.IO) {
        val width = 720
        val padding = 40f
        val rowHeight = 60f
        val headerHeight = 180f
        val roundHeaderHeight = 50f
        val roundRowHeight = 40f

        val roundsHeight = data.rounds.size * (roundHeaderHeight + data.playerTotals.size * roundRowHeight + 20f)
        val height = (headerHeight + data.playerTotals.size * rowHeight + 100f + roundsHeight + 80f).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = 0xFF1A1A2E.toInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title
        val titlePaint = Paint().apply {
            color = 0xFFFFD93D.toInt()
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("🃏 Least Count Loosership", padding, 50f, titlePaint)

        // Date
        val subtitlePaint = Paint().apply {
            color = 0xFF4D96FF.toInt()
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("📅 ${data.date} • ${data.totalRounds} rounds", padding, 85f, subtitlePaint)

        // Loser banner
        if (data.loserName.isNotEmpty()) {
            val bannerPaint = Paint().apply {
                color = 0x30FF6B6B.toInt()
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(padding, 100f, width - padding, 165f), 16f, 16f, bannerPaint)

            val loserPaint = Paint().apply {
                color = 0xFFFF6B6B.toInt()
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText("💀 ${data.loserEmoji} ${data.loserName} LOST!", padding + 16f, 142f, loserPaint)
        }

        // Standings
        var y = headerHeight
        val headerPaint = Paint().apply {
            color = 0xFFFFD93D.toInt()
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("📊 Final Standings", padding, y, headerPaint)
        y += 10f

        val sortedTotals = data.playerTotals.sortedBy { it.totalScore }
        sortedTotals.forEachIndexed { index, pt ->
            y += rowHeight
            val rowPaint = Paint().apply {
                color = if (pt.playerId == data.playerTotals.find { it.playerName == data.loserName }?.playerId)
                    0x20FF6B6B.toInt() else 0xFF16213E.toInt()
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(padding, y - 35f, width - padding, y + 15f), 12f, 12f, rowPaint)

            val namePaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 22f
                isAntiAlias = true
            }
            canvas.drawText("#${index + 1}  ${pt.emoji} ${pt.playerName}", padding + 16f, y, namePaint)

            val scorePaint = Paint().apply {
                color = if (pt.totalScore >= 101) 0xFFFF6B6B.toInt() else 0xFF4D96FF.toInt()
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("${pt.totalScore}", width - padding - 16f, y, scorePaint)
        }

        y += 40f
        canvas.drawText("🔄 Round Details", padding, y, headerPaint)
        y += 10f

        // Rounds
        data.rounds.forEach { round ->
            y += roundHeaderHeight
            val roundTitlePaint = Paint().apply {
                color = 0xFF4D96FF.toInt()
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText("Round ${round.roundNumber}", padding + 8f, y, roundTitlePaint)

            round.scores.sortedByDescending { it.score }.forEach { scoreInfo ->
                y += roundRowHeight
                val rNamePaint = Paint().apply {
                    color = 0xCCFFFFFF.toInt()
                    textSize = 17f
                    isAntiAlias = true
                }
                canvas.drawText("  ${scoreInfo.emoji} ${scoreInfo.playerName}", padding + 20f, y, rNamePaint)

                val rScorePaint = Paint().apply {
                    color = 0xFF4D96FF.toInt()
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("${scoreInfo.score}", width - padding - 16f, y, rScorePaint)
            }
        }

        // Watermark
        y += 50f
        val watermarkPaint = Paint().apply {
            color = 0x66FFFFFF.toInt()
            textSize = 14f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Made with Least Count Loosership 🃏", width / 2f, y, watermarkPaint)

        // Save and share
        val file = File(context.cacheDir, "game_result.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "🃏 Least Count result: ${data.loserEmoji} ${data.loserName} lost with ${data.playerTotals.find { it.playerName == data.loserName }?.totalScore ?: "??"} points!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Game Result"))
    }
}
