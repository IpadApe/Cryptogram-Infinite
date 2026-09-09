package dev.milan.cryptogram.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.engine.Difficulty

@Composable
fun HomeScreen(
    onOpenBand: (Difficulty) -> Unit,
    onOpenDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }

        is HomeUiState.Ready -> Column(
            modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cryptogram Infinite", style = MaterialTheme.typography.headlineSmall)
                Row {
                    TextButton(onClick = onOpenStats) { Text("Stats") }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.bands, key = { it.difficulty }) { band ->
                    BandCard(band, onClick = { onOpenBand(band.difficulty) })
                }
                item {
                    Card(onClick = onOpenDaily, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Daily", style = MaterialTheme.typography.titleMedium)
                            Text(
                                s.daily?.date ?: "Tap to play today's puzzles",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            s.daily?.let { d ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Difficulty.entries.forEach { band ->
                                        val done = band in d.solvedBands
                                        Text(
                                            (if (done) "● " else "○ ") + band.name.first(),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                            Text("Streak: ${s.streak}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (!s.removeAdsOwned) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Remove ads")
                }
            }
        }
    }
}

@Composable
private fun BandCard(band: BandSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                band.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Level ${band.nextLevel}", style = MaterialTheme.typography.bodyLarge)
            Text("${band.solvedCount} solved", style = MaterialTheme.typography.bodySmall)
        }
    }
}
