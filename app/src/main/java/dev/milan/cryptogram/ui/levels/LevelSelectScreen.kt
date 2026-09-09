package dev.milan.cryptogram.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.ads.BannerAd
import dev.milan.cryptogram.engine.Difficulty

@Composable
fun LevelSelectScreen(
    difficulty: Difficulty,
    onOpenLevel: (Difficulty, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModel.factory(difficulty),
        key = "levels-$difficulty",
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { BannerAd() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Continue · ${state.continueLevel}") },
                icon = {},
                onClick = { onOpenLevel(difficulty, state.continueLevel) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp)) {
            Text(
                difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (state.totalPages > 1) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = { viewModel.setPage(state.page - 1) },
                        enabled = state.page > 0,
                    ) { Text("Prev") }
                    Text(
                        "Page ${state.page + 1} / ${state.totalPages}",
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    TextButton(
                        onClick = { viewModel.setPage(state.page + 1) },
                        enabled = state.page < state.totalPages - 1,
                    ) { Text("Next") }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.levels, key = { it.level }) { cell ->
                    LevelChip(cell, onClick = { onOpenLevel(difficulty, cell.level) })
                }
            }
        }
    }
}

@Composable
private fun LevelChip(cell: LevelCell, onClick: () -> Unit) {
    val label = when (cell.status) {
        LevelStatus.SOLVED -> "${cell.level}\n${"★".repeat(cell.stars)}"
        LevelStatus.IN_PROGRESS -> "${cell.level}\n•"
        LevelStatus.UNSOLVED -> "${cell.level}"
    }
    val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
    when (cell.status) {
        LevelStatus.SOLVED -> Card(
            onClick = onClick,
            modifier = Modifier.aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) { content() }

        else -> OutlinedCard(
            onClick = onClick,
            modifier = Modifier.aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
        ) { content() }
    }
}
