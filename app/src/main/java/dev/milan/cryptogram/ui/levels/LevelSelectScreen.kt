package dev.milan.cryptogram.ui.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.ads.BannerAd
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.components.AccentButton
import dev.milan.cryptogram.ui.components.BackHeader
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

@Composable
fun LevelSelectScreen(
    difficulty: Difficulty,
    onOpenLevel: (Difficulty, Int) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModel.factory(difficulty),
        key = "levels-$difficulty",
    ),
) {
    val c = CryptoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .padding(top = 56.dp, start = 20.dp, end = 20.dp),
    ) {
        BackHeader(difficulty.name, onBack)
        Spacer(Modifier.height(16.dp))

        if (state.totalPages > 1) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PageBtn("PREV", state.page > 0) { viewModel.setPage(state.page - 1) }
                Spacer(Modifier.weight(1f))
                Text(
                    "PAGE ${state.page + 1} / ${state.totalPages}",
                    fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.14.em, color = c.muted,
                )
                Spacer(Modifier.weight(1f))
                PageBtn("NEXT", state.page < state.totalPages - 1) { viewModel.setPage(state.page + 1) }
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(state.levels, key = { it.level }) { cell ->
                LevelChip(cell) { onOpenLevel(difficulty, cell.level) }
            }
        }

        AccentButton("Continue · ${state.continueLevel}", onClick = {
            onOpenLevel(difficulty, state.continueLevel)
        }, modifier = Modifier.padding(vertical = 10.dp))
        BannerAd()
    }
}

@Composable
private fun PageBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = CryptoTheme.colors
    Text(
        label,
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
        fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.14.em,
        color = if (enabled) c.accent else c.muted.copy(alpha = 0.4f),
    )
}

@Composable
private fun LevelChip(cell: LevelCell, onClick: () -> Unit) {
    val c = CryptoTheme.colors
    val solved = cell.status == LevelStatus.SOLVED
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (solved) c.accent.copy(alpha = 0.14f) else c.card)
            .border(1.dp, if (solved) c.accent.copy(alpha = 0.4f) else c.divider, shape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            cell.level.toString(),
            fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 16.sp,
            color = if (solved) c.accent else c.ink,
        )
        when (cell.status) {
            LevelStatus.SOLVED -> Text(
                "★".repeat(cell.stars.coerceIn(0, 3)),
                fontSize = 7.sp, color = c.accent, textAlign = TextAlign.Center,
            )
            LevelStatus.IN_PROGRESS -> Text("·", fontFamily = Mono, fontSize = 10.sp, color = c.muted)
            LevelStatus.UNSOLVED -> {}
        }
    }
}
