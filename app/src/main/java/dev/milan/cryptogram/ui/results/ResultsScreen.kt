package dev.milan.cryptogram.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.milan.cryptogram.engine.Difficulty

/** Brief 2 stub. Full Results (quote, author, source, share) arrives in Brief 3. */
@Composable
fun ResultsScreen(
    difficulty: Difficulty,
    level: Int,
    timeMs: Long,
    mistakes: Int,
    hintsUsed: Int,
    stars: Int,
    onNextLevel: (Difficulty, Int) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Solved!", style = MaterialTheme.typography.headlineMedium)
        Text("★".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.headlineSmall)
        Text("Time ${timeMs / 1000}s")
        Text("Mistakes $mistakes")
        Text("Hints used $hintsUsed")
        Button(onClick = { onNextLevel(difficulty, level + 1) }) { Text("Next level") }
        Button(onClick = onHome) { Text("Home") }
    }
}
