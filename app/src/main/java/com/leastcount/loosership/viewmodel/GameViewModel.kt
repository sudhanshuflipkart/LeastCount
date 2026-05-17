package com.leastcount.loosership.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leastcount.loosership.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val playerDao = db.playerDao()
    private val gameDao = db.gameDao()
    private val gamePlayerDao = db.gamePlayerDao()
    private val roundDao = db.roundDao()
    private val roundScoreDao = db.roundScoreDao()

    // --- State flows ---
    val activePlayers: StateFlow<List<Player>> = playerDao.getActivePlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlayers: StateFlow<List<Player>> = playerDao.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGamesWithDetails: StateFlow<List<GameWithDetails>> = gameDao.getGamesWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val competitionLeaderboard: StateFlow<List<PlayerLossCount>> = roundScoreDao.getPlayerLossCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active game state
    private val _activeGameId = MutableStateFlow<Long?>(null)
    val activeGameId: StateFlow<Long?> = _activeGameId.asStateFlow()

    private val _activeGamePlayers = MutableStateFlow<List<Player>>(emptyList())
    val activeGamePlayers: StateFlow<List<Player>> = _activeGamePlayers.asStateFlow()

    val activeGamePlayerTotals: StateFlow<List<PlayerWithTotalScore>> = _activeGameId
        .flatMapLatest { gameId ->
            if (gameId != null) roundScoreDao.getPlayerTotalsForGame(gameId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGameRounds: StateFlow<List<Round>> = _activeGameId
        .flatMapLatest { gameId ->
            if (gameId != null) roundDao.getRoundsForGameFlow(gameId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        // Check if there's an active game on start
        viewModelScope.launch {
            val activeGame = gameDao.getActiveGame()
            if (activeGame != null) {
                _activeGameId.value = activeGame.id
                _activeGamePlayers.value = gamePlayerDao.getPlayersForGame(activeGame.id)
            }
        }
    }

    // --- Player management ---

    fun addPlayer(name: String, emoji: String, colorIndex: Int) {
        viewModelScope.launch {
            playerDao.insert(Player(name = name.trim(), emoji = emoji, colorIndex = colorIndex))
            _snackbarMessage.emit("${emoji} ${name.trim()} joined the party!")
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            playerDao.update(player)
        }
    }

    fun togglePlayerActive(playerId: Long, isActive: Boolean) {
        viewModelScope.launch {
            playerDao.setActive(playerId, isActive)
        }
    }

    // --- Game management ---

    fun startNewGame(playerIds: List<Long>, onGameStarted: (Long) -> Unit) {
        viewModelScope.launch {
            // Check for existing active game
            val activeGame = gameDao.getActiveGame()
            if (activeGame != null) {
                _snackbarMessage.emit("There's already an active game! Finish or delete it first.")
                return@launch
            }

            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val gameId = gameDao.insert(Game(date = today))

            val gamePlayers = playerIds.map { GamePlayer(gameId = gameId, playerId = it) }
            gamePlayerDao.insertAll(gamePlayers)

            _activeGameId.value = gameId
            _activeGamePlayers.value = gamePlayerDao.getPlayersForGame(gameId)

            _snackbarMessage.emit("🃏 Game on! May the worst hand win!")
            onGameStarted(gameId)
        }
    }

    fun addRound(gameId: Long, scores: Map<Long, Int>, onComplete: (Boolean, Long?) -> Unit) {
        viewModelScope.launch {
            val roundNumber = roundDao.getMaxRoundNumber(gameId) + 1
            val roundId = roundDao.insert(Round(gameId = gameId, roundNumber = roundNumber))

            val roundScores = scores.map { (playerId, score) ->
                RoundScore(roundId = roundId, playerId = playerId, score = score)
            }
            roundScoreDao.insertAll(roundScores)

            // Check if anyone crossed 101
            val totals = roundScoreDao.getPlayerTotalsForGameOnce(gameId)
            val loser = totals.find { it.totalScore >= 101 }

            if (loser != null) {
                gameDao.update(
                    gameDao.getById(gameId)!!.copy(
                        isCompleted = true,
                        loserId = loser.playerId
                    )
                )
                _activeGameId.value = null
                _activeGamePlayers.value = emptyList()
                _snackbarMessage.emit("💀 ${loser.emoji} ${loser.playerName} LOST with ${loser.totalScore} points!")
                onComplete(true, loser.playerId)
            } else {
                _snackbarMessage.emit("Round $roundNumber recorded! 🎴")
                onComplete(false, null)
            }
        }
    }

    fun deleteLastRound(gameId: Long) {
        viewModelScope.launch {
            val rounds = roundDao.getRoundsForGame(gameId)
            if (rounds.isNotEmpty()) {
                roundDao.delete(rounds.last().id)
                _snackbarMessage.emit("Last round undone! ↩️")
            }
        }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            if (_activeGameId.value == gameId) {
                _activeGameId.value = null
                _activeGamePlayers.value = emptyList()
            }
            gameDao.delete(gameId)
            _snackbarMessage.emit("Game deleted 🗑️")
        }
    }

    // --- Data fetchers for detail screens ---

    suspend fun getGameById(gameId: Long): Game? = gameDao.getById(gameId)

    suspend fun getPlayersForGame(gameId: Long): List<Player> =
        gamePlayerDao.getPlayersForGame(gameId)

    suspend fun getPlayerTotalsForGame(gameId: Long): List<PlayerWithTotalScore> =
        roundScoreDao.getPlayerTotalsForGameOnce(gameId)

    suspend fun getRoundsWithScores(gameId: Long): List<RoundWithScores> {
        val rounds = roundDao.getRoundsForGame(gameId)
        return rounds.map { round ->
            val scores = roundScoreDao.getScoreInfoForRound(round.id)
            RoundWithScores(
                roundId = round.id,
                roundNumber = round.roundNumber,
                scores = scores
            )
        }
    }

    fun getPlayerTotalsFlow(gameId: Long): Flow<List<PlayerWithTotalScore>> =
        roundScoreDao.getPlayerTotalsForGame(gameId)

    // --- Statistics ---

    suspend fun getPlayerStats(playerId: Long): PlayerStats? {
        val player = playerDao.getById(playerId) ?: return null
        val totalGames = roundScoreDao.getTotalGamesPlayed(playerId)
        val totalPoints = roundScoreDao.getTotalPointsForPlayer(playerId)
        val avgPoints = roundScoreDao.getAvgPointsPerGame(playerId)
        val highestRound = roundScoreDao.getHighestRoundScoreForPlayer(playerId)
        val lowestRound = roundScoreDao.getLowestRoundScoreForPlayer(playerId)

        // Calculate loss count and streaks
        val allGames = allGamesWithDetails.value
            .filter { it.isCompleted }
            .sortedBy { it.createdAt }

        var totalLosses = 0
        var currentStreak = 0
        var maxStreak = 0
        var lastWasLoss = false

        for (game in allGames) {
            // Check if this player was in the game
            val gamePlayers = gamePlayerDao.getPlayersForGame(game.gameId)
            if (gamePlayers.any { it.id == playerId }) {
                if (game.loserId == playerId) {
                    totalLosses++
                    currentStreak++
                    maxStreak = maxOf(maxStreak, currentStreak)
                    lastWasLoss = true
                } else {
                    currentStreak = 0
                    lastWasLoss = false
                }
            }
        }

        if (!lastWasLoss) currentStreak = 0

        return PlayerStats(
            playerId = playerId,
            playerName = player.name,
            emoji = player.emoji,
            colorIndex = player.colorIndex,
            totalGamesPlayed = totalGames,
            totalLosses = totalLosses,
            totalPointsAccumulated = totalPoints,
            avgPointsPerGame = avgPoints,
            highestRoundScore = highestRound,
            lowestRoundScore = lowestRound,
            currentLosingStreak = currentStreak,
            maxLosingStreak = maxStreak
        )
    }

    suspend fun getGlobalStats(): GlobalStats {
        val totalGames = roundScoreDao.getTotalCompletedGames()
        val highestRound = roundScoreDao.getHighestRoundScore() ?: 0
        val highestRoundPlayer = roundScoreDao.getPlayerWithHighestRoundScore() ?: "N/A"
        val leaderboard = competitionLeaderboard.value

        return GlobalStats(
            totalGamesPlayed = totalGames,
            highestSingleRoundScore = highestRound,
            highestSingleRoundPlayer = highestRoundPlayer,
            mostLosses = leaderboard.firstOrNull()?.lossCount ?: 0,
            mostLossesPlayer = leaderboard.firstOrNull()?.playerName ?: "N/A",
            mostLossesEmoji = leaderboard.firstOrNull()?.emoji ?: "🃏"
        )
    }

    // For share image
    suspend fun getGameShareData(gameId: Long): GameShareData? {
        val game = gameDao.getById(gameId) ?: return null
        val players = getPlayerTotalsForGame(gameId)
        val rounds = getRoundsWithScores(gameId)
        val loser = players.find { it.playerId == game.loserId }

        return GameShareData(
            date = game.date,
            playerTotals = players,
            rounds = rounds,
            loserName = loser?.playerName ?: "",
            loserEmoji = loser?.emoji ?: "",
            totalRounds = rounds.size
        )
    }
}

data class GlobalStats(
    val totalGamesPlayed: Int,
    val highestSingleRoundScore: Int,
    val highestSingleRoundPlayer: String,
    val mostLosses: Int,
    val mostLossesPlayer: String,
    val mostLossesEmoji: String
)

data class GameShareData(
    val date: String,
    val playerTotals: List<PlayerWithTotalScore>,
    val rounds: List<RoundWithScores>,
    val loserName: String,
    val loserEmoji: String,
    val totalRounds: Int
)
