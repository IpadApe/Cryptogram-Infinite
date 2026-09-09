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

/** Route strings and argument keys for the whole app (design doc section 8). */
object Routes {
    const val HOME = "home"
    const val LEVELS = "levels/{difficulty}"
    const val PLAY_LEVEL = "play/level/{difficulty}/{level}"
    const val PLAY_DAILY = "play/daily/{date}/{difficulty}"
    const val RESULTS = "results"
    const val DAILY = "daily"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SOURCES = "sources"

    fun levels(difficulty: String) = "levels/$difficulty"
    fun playLevel(difficulty: String, level: Int) = "play/level/$difficulty/$level"
    fun playDaily(date: String, difficulty: String) = "play/daily/$date/$difficulty"
}

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
        composable(Routes.HOME) { Placeholder("Home") }

        composable(
            Routes.LEVELS,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) { Placeholder("LevelSelect") }

        composable(
            Routes.PLAY_LEVEL,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("level") { type = NavType.IntType },
            ),
        ) { Placeholder("Play (level)") }

        composable(
            Routes.PLAY_DAILY,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
            ),
        ) { Placeholder("Play (daily)") }

        composable(Routes.RESULTS) { Placeholder("Results") }
        composable(Routes.DAILY) { Placeholder("Daily") }
        composable(Routes.STATS) { Placeholder("Stats") }
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
