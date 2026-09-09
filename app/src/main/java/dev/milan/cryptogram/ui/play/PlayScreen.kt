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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.engine.PuzzleStatus

@Composable
fun PlayScreen(
    difficulty: Difficulty,
    level: Int,
    onSolved: (ResultsRoute) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayViewModel = viewModel(
        factory = PlayViewModel.factory(difficulty, level),
        key = "play-$difficulty-$level",
    ),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

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
                is PlayEvent.NavigateToResults -> onSolved(event.route)
            }
        }
    }

    val state = ui
    if (state == null) {
        Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val puzzle = state.puzzle

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${state.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }} $level",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(formatTime(puzzle.elapsedMs), style = MaterialTheme.typography.titleMedium)
            Text(
                "♥ ${puzzle.livesLeft}/${state.difficulty.lives}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Hints ${puzzle.hintsLeft}", style = MaterialTheme.typography.titleMedium)
        }

        PuzzleGrid(
            state = puzzle,
            onCellClick = viewModel::select,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

        CipherKeyboard(
            usedLetters = puzzle.mapping.values.toSet(),
            canCheck = state.canCheck,
            hintLabel = when {
                state.canHint -> "Hint (${puzzle.hintsLeft})"
                state.canAdHint -> "Hint (watch ad)"
                else -> "Hint"
            },
            hintEnabled = state.canHint, // ad path enabled in Brief 5
            onKey = viewModel::enter,
            onBackspace = viewModel::clearCell,
            onHint = { viewModel.hint(fromAd = false) },
            onCheck = viewModel::check,
        )
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
