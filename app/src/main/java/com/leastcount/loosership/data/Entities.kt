package com.leastcount.loosership.data

import androidx.room.*

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🃏",
    val colorIndex: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val isCompleted: Boolean = false,
    val loserId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "game_players",
    foreignKeys = [
        ForeignKey(entity = Game::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Player::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("gameId"), Index("playerId")]
)
data class GamePlayer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val playerId: Long
)

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(entity = Game::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("gameId")]
)
data class Round(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val roundNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "round_scores",
    foreignKeys = [
        ForeignKey(entity = Round::class, parentColumns = ["id"], childColumns = ["roundId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Player::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("roundId"), Index("playerId")]
)
data class RoundScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val playerId: Long,
    val score: Int
)

// --- Query result classes ---

data class PlayerWithTotalScore(
    val playerId: Long,
    val playerName: String,
    val emoji: String,
    val colorIndex: Int,
    val totalScore: Int
)

data class GameWithDetails(
    val gameId: Long,
    val date: String,
    val isCompleted: Boolean,
    val loserId: Long?,
    val loserName: String?,
    val loserEmoji: String?,
    val createdAt: Long,
    val playerCount: Int
)

data class RoundWithScores(
    val roundId: Long,
    val roundNumber: Int,
    val scores: List<RoundScoreInfo>
)

data class RoundScoreInfo(
    val playerId: Long,
    val playerName: String,
    val emoji: String,
    val colorIndex: Int,
    val score: Int
)

data class PlayerLossCount(
    val playerId: Long,
    val playerName: String,
    val emoji: String,
    val colorIndex: Int,
    val lossCount: Int
)

data class PlayerStats(
    val playerId: Long,
    val playerName: String,
    val emoji: String,
    val colorIndex: Int,
    val totalGamesPlayed: Int,
    val totalLosses: Int,
    val totalPointsAccumulated: Int,
    val avgPointsPerGame: Double,
    val highestRoundScore: Int,
    val lowestRoundScore: Int,
    val currentLosingStreak: Int,
    val maxLosingStreak: Int
)
