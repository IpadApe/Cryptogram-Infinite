package dev.milan.cryptogram.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.engine.PuzzleStatus
import dev.milan.cryptogram.ui.rememberAppContainer
import dev.milan.cryptogram.ui.results.ResultArgs

@Composable
fun PlayScreen(
    difficulty: Difficulty,
    level: Int,
    onSolved: (ResultArgs) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    dailyDate: String? = null,
    viewModel: PlayViewModel = viewModel(
        factory = PlayViewModel.factory(difficulty, level, dailyDate),
        key = "play-${dailyDate ?: "level"}-$difficulty-$level",
    ),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val container = rememberAppContainer()
    val rewardedHint = remember { container.newRewardedHintAd() }
    val rewardedLoaded by rewardedHint.loaded.collectAsStateWithLifecycle()
    val soundEnabled by container.settingsStore.soundEnabled
        .collectAsStateWithLifecycle(initialValue = true)
    val hapticsEnabled by container.settingsStore.hapticsEnabled
        .collectAsStateWithLifecycle(initialValue = true)
    val feedback = rememberPlayFeedback(soundEnabled, hapticsEnabled)

    LaunchedEffect(Unit) { rewardedHint.load() }
    LaunchedEffect(rewardedLoaded) { viewModel.setAdHintLoaded(rewardedLoaded) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setResumed(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setResumed(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PlayEvent.NavigateToResults -> onSolved(event.args)
                PlayEvent.LoadFailed -> onExit()
            }
        }
    }

    val state = ui
    if (state == null) {
        Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val puzzle = state.puzzle

    val lastMistakes = remember { mutableIntStateOf(puzzle.mistakes) }
    LaunchedEffect(puzzle.mistakes) {
        if (puzzle.mistakes > lastMistakes.intValue) feedback.onWrong()
        lastMistakes.intValue = puzzle.mistakes
    }
    LaunchedEffect(puzzle.status) {
        if (puzzle.status == PuzzleStatus.SOLVED) feedback.onSolve()
    }

    val baseDensity = LocalDensity.current
    val clampedDensity = remember(baseDensity) {
        Density(baseDensity.density, baseDensity.fontScale.coerceAtMost(1.3f))
    }

    val topBar: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val band = state.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            Text(
                if (state.isDaily) "Daily · $band" else "$band $level",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(formatTime(puzzle.elapsedMs), style = MaterialTheme.typography.titleMedium)
            Text(
                "♥ ${puzzle.livesLeft}/${state.difficulty.lives}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Hints ${puzzle.hintsLeft}", style = MaterialTheme.typography.titleMedium)
        }
    }
    val grid: @Composable (Modifier) -> Unit = { m ->
        PuzzleGrid(state = puzzle, onCellClick = viewModel::select, modifier = m)
    }
    val keyboard: @Composable (Modifier) -> Unit = { m ->
        CipherKeyboard(
            usedLetters = puzzle.mapping.values.toSet(),
            canCheck = state.canCheck,
            hintLabel = when {
                state.canHint -> "Hint (${puzzle.hintsLeft})"
                state.canAdHint -> "Hint (watch ad)"
                else -> "Hint"
            },
            hintEnabled = state.canHint || state.canAdHint,
            onKey = { c -> feedback.onTap(); viewModel.enter(c) },
            onBackspace = viewModel::clearCell,
            onHint = {
                if (state.canHint) {
                    viewModel.hint(fromAd = false)
                } else if (state.canAdHint) {
                    (context as? Activity)?.let { activity ->
                        rewardedHint.show(activity) { viewModel.hint(fromAd = true) }
                    }
                }
            },
            onCheck = viewModel::check,
            modifier = m,
        )
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            if (maxWidth >= 600.dp) {
                Column(Modifier.fillMaxSize()) {
                    topBar()
                    Row(Modifier.fillMaxSize()) {
                        grid(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                        )
                        keyboard(Modifier.width(360.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    topBar()
                    grid(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                    keyboard(Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (state.showFailedDialog && puzzle.status == PuzzleStatus.FAILED) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Out of lives") },
            text = { Text("The puzzle restarts with a new cipher key.") },
            confirmButton = {
                TextButton(onClick = viewModel::tryAgain) { Text("Try again") }
            },
            dismissButton = {
                TextButton(onClick = onExit) { Text("Leave") }
            },
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
