package dev.milan.cryptogram.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.home.HomeScreen
import dev.milan.cryptogram.ui.levels.LevelSelectScreen
import dev.milan.cryptogram.ui.play.PlayScreen
import dev.milan.cryptogram.ui.results.ResultArgs
import dev.milan.cryptogram.ui.results.ResultsScreen
import dev.milan.cryptogram.ui.stats.StatsScreen

/** Route strings and argument keys for the whole app (design doc section 8). */
object Routes {
    const val HOME = "home"
    const val LEVELS = "levels/{difficulty}"
    const val PLAY_LEVEL = "play/level/{difficulty}/{level}"
    const val PLAY_DAILY = "play/daily/{date}/{difficulty}"
    const val RESULTS = "results/{args}"
    const val DAILY = "daily"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SOURCES = "sources"

    fun levels(difficulty: Difficulty) = "levels/${difficulty.name}"
    fun playLevel(difficulty: Difficulty, level: Int) = "play/level/${difficulty.name}/$level"
    fun playDaily(date: String, difficulty: Difficulty) = "play/daily/$date/${difficulty.name}"
    fun results(args: ResultArgs) = "results/${args.encode()}"
}

private fun diffArg(name: String?): Difficulty =
    runCatching { Difficulty.valueOf(name ?: "") }.getOrDefault(Difficulty.EASY)

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenBand = { navController.navigate(Routes.levels(it)) },
                onOpenDaily = { navController.navigate(Routes.DAILY) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.LEVELS,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) { entry ->
            LevelSelectScreen(
                difficulty = diffArg(entry.arguments?.getString("difficulty")),
                onOpenLevel = { d, level -> navController.navigate(Routes.playLevel(d, level)) },
            )
        }

        composable(
            Routes.PLAY_LEVEL,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("level") { type = NavType.IntType },
            ),
        ) { entry ->
            PlayScreen(
                difficulty = diffArg(entry.arguments?.getString("difficulty")),
                level = entry.arguments?.getInt("level") ?: 1,
                onSolved = { args ->
                    navController.navigate(Routes.results(args)) {
                        popUpTo(Routes.PLAY_LEVEL) { inclusive = true }
                    }
                },
                onExit = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        composable(
            Routes.RESULTS,
            arguments = listOf(navArgument("args") { type = NavType.StringType }),
        ) { entry ->
            val args = ResultArgs.decode(entry.arguments?.getString("args").orEmpty())
            ResultsScreen(
                args = args,
                onNextLevel = { a ->
                    navController.navigate(Routes.playLevel(a.difficulty, (a.level ?: 0) + 1)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBackToDaily = {
                    navController.navigate(Routes.DAILY) { popUpTo(Routes.HOME) }
                },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        composable(Routes.STATS) { StatsScreen() }

        composable(Routes.PLAY_DAILY) { Placeholder("Play (daily)") }
        composable(Routes.DAILY) { Placeholder("Daily") }
        composable(Routes.SETTINGS) { Placeholder("Settings") }
        composable(Routes.SOURCES) { Placeholder("Sources") }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = MaterialTheme.typography.headlineMedium)
    }
}
