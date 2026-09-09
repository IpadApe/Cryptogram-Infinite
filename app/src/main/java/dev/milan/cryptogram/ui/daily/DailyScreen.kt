package dev.milan.cryptogram.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.engine.Difficulty

@Composable
fun DailyScreen(
    onPlay: (date: String, difficulty: Difficulty) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = viewModel(factory = DailyViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Daily · ${state.date}", style = MaterialTheme.typography.headlineSmall)
        Text("Streak: ${state.streak}", style = MaterialTheme.typography.bodyLarge)

        state.cards.forEach { card ->
            Card(
                onClick = { onPlay(state.date, card.difficulty) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        card.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (card.solved) {
                        Text("Solved · ${"★".repeat(card.stars)} · ${format(card.timeMs)}")
                    } else {
                        Text("Not solved")
                    }
                }
            }
        }

        HorizontalDivider()
        Text("Previous days", style = MaterialTheme.typography.titleMedium)
        state.previous.forEach { day ->
            Card(
                onClick = { onPlay(day.date, Difficulty.EASY) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(day.date)
                    Text(if (day.allSolved) "All four solved" else "Incomplete")
                }
            }
        }
    }
}

private fun format(ms: Long?): String {
    if (ms == null) return "—"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
