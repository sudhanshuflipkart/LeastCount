package com.leastcount.loosership.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert
    suspend fun insert(player: Player): Long

    @Update
    suspend fun update(player: Player)

    @Query("SELECT * FROM players WHERE isActive = 1 ORDER BY name ASC")
    fun getActivePlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: Long): Player?

    @Query("UPDATE players SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: Game): Long

    @Update
    suspend fun update(game: Game)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): Game?

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE date = :date ORDER BY createdAt DESC")
    fun getGamesByDate(date: String): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE isCompleted = 0 LIMIT 1")
    suspend fun getActiveGame(): Game?

    @Query("""
        SELECT g.id as gameId, g.date, g.isCompleted, g.loserId,
               p.name as loserName, p.emoji as loserEmoji, g.createdAt,
               (SELECT COUNT(*) FROM game_players WHERE gameId = g.id) as playerCount
        FROM games g
        LEFT JOIN players p ON g.loserId = p.id
        ORDER BY g.createdAt DESC
    """)
    fun getGamesWithDetails(): Flow<List<GameWithDetails>>

    @Query("""
        SELECT g.id as gameId, g.date, g.isCompleted, g.loserId,
               p.name as loserName, p.emoji as loserEmoji, g.createdAt,
               (SELECT COUNT(*) FROM game_players WHERE gameId = g.id) as playerCount
        FROM games g
        LEFT JOIN players p ON g.loserId = p.id
        WHERE g.date = :date
        ORDER BY g.createdAt DESC
    """)
    fun getGamesWithDetailsByDate(date: String): Flow<List<GameWithDetails>>

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GamePlayerDao {
    @Insert
    suspend fun insert(gamePlayer: GamePlayer)

    @Insert
    suspend fun insertAll(gamePlayers: List<GamePlayer>)

    @Query("SELECT p.* FROM players p INNER JOIN game_players gp ON p.id = gp.playerId WHERE gp.gameId = :gameId")
    suspend fun getPlayersForGame(gameId: Long): List<Player>

    @Query("SELECT p.* FROM players p INNER JOIN game_players gp ON p.id = gp.playerId WHERE gp.gameId = :gameId")
    fun getPlayersForGameFlow(gameId: Long): Flow<List<Player>>
}

@Dao
interface RoundDao {
    @Insert
    suspend fun insert(round: Round): Long

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getRoundsForGame(gameId: Long): List<Round>

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    fun getRoundsForGameFlow(gameId: Long): Flow<List<Round>>

    @Query("SELECT COALESCE(MAX(roundNumber), 0) FROM rounds WHERE gameId = :gameId")
    suspend fun getMaxRoundNumber(gameId: Long): Int

    @Query("DELETE FROM rounds WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RoundScoreDao {
    @Insert
    suspend fun insert(roundScore: RoundScore)

    @Insert
    suspend fun insertAll(scores: List<RoundScore>)

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    suspend fun getScoresForRound(roundId: Long): List<RoundScore>

    @Query("""
        SELECT rs.playerId, p.name as playerName, p.emoji, p.colorIndex, rs.score
        FROM round_scores rs
        INNER JOIN players p ON rs.playerId = p.id
        WHERE rs.roundId = :roundId
        ORDER BY p.name ASC
    """)
    suspend fun getScoreInfoForRound(roundId: Long): List<RoundScoreInfo>

    @Query("""
        SELECT gp.playerId, p.name as playerName, p.emoji, p.colorIndex,
               COALESCE(SUM(rs.score), 0) as totalScore
        FROM game_players gp
        INNER JOIN players p ON gp.playerId = p.id
        LEFT JOIN rounds r ON r.gameId = gp.gameId
        LEFT JOIN round_scores rs ON rs.roundId = r.id AND rs.playerId = gp.playerId
        WHERE gp.gameId = :gameId
        GROUP BY gp.playerId
        ORDER BY totalScore ASC
    """)
    fun getPlayerTotalsForGame(gameId: Long): Flow<List<PlayerWithTotalScore>>

    @Query("""
        SELECT gp.playerId, p.name as playerName, p.emoji, p.colorIndex,
               COALESCE(SUM(rs.score), 0) as totalScore
        FROM game_players gp
        INNER JOIN players p ON gp.playerId = p.id
        LEFT JOIN rounds r ON r.gameId = gp.gameId
        LEFT JOIN round_scores rs ON rs.roundId = r.id AND rs.playerId = gp.playerId
        WHERE gp.gameId = :gameId
        GROUP BY gp.playerId
        ORDER BY totalScore ASC
    """)
    suspend fun getPlayerTotalsForGameOnce(gameId: Long): List<PlayerWithTotalScore>

    // Competition stats: total losses per player across all games
    @Query("""
        SELECT p.id as playerId, p.name as playerName, p.emoji, p.colorIndex,
               COUNT(g.id) as lossCount
        FROM players p
        LEFT JOIN games g ON g.loserId = p.id AND g.isCompleted = 1
        WHERE p.isActive = 1
        GROUP BY p.id
        ORDER BY lossCount DESC
    """)
    fun getPlayerLossCounts(): Flow<List<PlayerLossCount>>

    // Highest single round score ever
    @Query("SELECT MAX(score) FROM round_scores")
    suspend fun getHighestRoundScore(): Int?

    // Player with highest single round score
    @Query("""
        SELECT p.name FROM round_scores rs
        INNER JOIN players p ON rs.playerId = p.id
        ORDER BY rs.score DESC LIMIT 1
    """)
    suspend fun getPlayerWithHighestRoundScore(): String?

    // Total points accumulated by a player across all games
    @Query("""
        SELECT COALESCE(SUM(rs.score), 0) FROM round_scores rs
        WHERE rs.playerId = :playerId
    """)
    suspend fun getTotalPointsForPlayer(playerId: Long): Int

    // Average points per game for a player
    @Query("""
        SELECT COALESCE(AVG(game_total), 0) FROM (
            SELECT SUM(rs.score) as game_total
            FROM round_scores rs
            INNER JOIN rounds r ON rs.roundId = r.id
            INNER JOIN game_players gp ON gp.gameId = r.gameId AND gp.playerId = rs.playerId
            WHERE rs.playerId = :playerId
            GROUP BY r.gameId
        )
    """)
    suspend fun getAvgPointsPerGame(playerId: Long): Double

    // Total games played by a player
    @Query("SELECT COUNT(*) FROM game_players WHERE playerId = :playerId")
    suspend fun getTotalGamesPlayed(playerId: Long): Int

    // Highest round score for a player
    @Query("SELECT COALESCE(MAX(score), 0) FROM round_scores WHERE playerId = :playerId")
    suspend fun getHighestRoundScoreForPlayer(playerId: Long): Int

    // Lowest round score for a player (excluding 0)
    @Query("SELECT COALESCE(MIN(score), 0) FROM round_scores WHERE playerId = :playerId AND score > 0")
    suspend fun getLowestRoundScoreForPlayer(playerId: Long): Int

    // Total completed games count
    @Query("SELECT COUNT(*) FROM games WHERE isCompleted = 1")
    suspend fun getTotalCompletedGames(): Int
}
