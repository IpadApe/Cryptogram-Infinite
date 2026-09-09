package dev.milan.cryptogram.ui.results

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ResultsScreen(
    args: ResultArgs,
    onNextLevel: (ResultArgs) -> Unit,
    onBackToDaily: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsViewModel = viewModel(
        factory = ResultsViewModel.factory(args),
        key = "results-${args.quoteId}-${args.timeMs}",
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Solved!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "★".repeat(args.stars) + "☆".repeat(3 - args.stars),
            style = MaterialTheme.typography.headlineSmall,
        )

        if (state.quoteText.isNotEmpty()) {
            Text(
                "“${state.quoteText}”",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                buildString {
                    append("— ${state.author}")
                    if (state.source.isNotBlank()) append(", ${state.source}")
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = if (state.sourceUrl.isNotBlank()) {
                    Modifier.clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, state.sourceUrl.toUri()))
                        }
                    }
                } else {
                    Modifier
                },
            )
        }

        Text("Time ${formatTime(args.timeMs)}")
        Text("Mistakes ${args.mistakes}")
        Text("Hints used ${args.hintsUsed}")

        val bandLabel = args.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
        val levelLabel = args.level?.toString() ?: args.date.orEmpty()

        if (args.kind == PlayKind.LEVEL && args.level != null) {
            Button(onClick = { onNextLevel(args) }) { Text("Next level") }
        } else {
            Button(onClick = onBackToDaily) { Text("Back to Daily") }
        }
        OutlinedButton(onClick = {
            val share = "Cryptogram Infinite · $bandLabel $levelLabel · " +
                "${formatTime(args.timeMs)} · ${"★".repeat(args.stars)}"
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, share)
                    },
                    null,
                ),
            )
        }) { Text("Share") }
        OutlinedButton(onClick = onHome) { Text("Home") }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
