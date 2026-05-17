package com.leastcount.loosership

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.leastcount.loosership.ui.screens.*
import com.leastcount.loosership.ui.theme.LeastCountLoosership
import com.leastcount.loosership.viewmodel.GameViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewGame : Screen("new_game")
    object ActiveGame : Screen("active_game/{gameId}") {
        fun createRoute(gameId: Long) = "active_game/$gameId"
    }
    object GameDetail : Screen("game_detail/{gameId}") {
        fun createRoute(gameId: Long) = "game_detail/$gameId"
    }
    object Stats : Screen("stats")
    object Players : Screen("players")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeastCountLoosership {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: GameViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Collect state
    val activePlayers by viewModel.activePlayers.collectAsState()
    val allPlayers by viewModel.allPlayers.collectAsState()
    val allGames by viewModel.allGamesWithDetails.collectAsState()
    val competitionLeaderboard by viewModel.competitionLeaderboard.collectAsState()
    val activeGameId by viewModel.activeGameId.collectAsState()
    val activeGamePlayers by viewModel.activeGamePlayers.collectAsState()
    val activeGamePlayerTotals by viewModel.activeGamePlayerTotals.collectAsState()
    val activeGameRounds by viewModel.activeGameRounds.collectAsState()

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    competitionLeaderboard = competitionLeaderboard,
                    recentGames = allGames,
                    activeGameId = activeGameId,
                    onNewGame = { navController.navigate(Screen.NewGame.route) },
                    onContinueGame = { gameId ->
                        navController.navigate(Screen.ActiveGame.createRoute(gameId))
                    },
                    onGameDetail = { gameId ->
                        navController.navigate(Screen.GameDetail.createRoute(gameId))
                    },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToPlayers = { navController.navigate(Screen.Players.route) }
                )
            }

            composable(Screen.NewGame.route) {
                NewGameScreen(
                    players = activePlayers,
                    onStartGame = { playerIds ->
                        viewModel.startNewGame(playerIds) { gameId ->
                            navController.navigate(Screen.ActiveGame.createRoute(gameId)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ActiveGame.route,
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong("gameId") ?: return@composable

                ActiveGameScreen(
                    gameId = gameId,
                    players = activeGamePlayers,
                    playerTotals = activeGamePlayerTotals,
                    rounds = activeGameRounds,
                    onAddRound = { scores, onComplete ->
                        viewModel.addRound(gameId, scores, onComplete)
                    },
                    onUndoLastRound = { viewModel.deleteLastRound(gameId) },
                    onDeleteGame = { viewModel.deleteGame(gameId) },
                    onGameEnd = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.GameDetail.route,
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong("gameId") ?: return@composable

                GameDetailScreen(
                    gameId = gameId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    viewModel = viewModel,
                    players = activePlayers,
                    competitionLeaderboard = competitionLeaderboard,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Players.route) {
                PlayersScreen(
                    players = allPlayers,
                    onAddPlayer = { name, emoji, colorIndex ->
                        viewModel.addPlayer(name, emoji, colorIndex)
                    },
                    onUpdatePlayer = { player -> viewModel.updatePlayer(player) },
                    onToggleActive = { id, active -> viewModel.togglePlayerActive(id, active) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
