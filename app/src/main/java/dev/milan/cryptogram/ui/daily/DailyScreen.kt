package dev.milan.cryptogram.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.components.BackHeader
import dev.milan.cryptogram.ui.components.PaperCard
import dev.milan.cryptogram.ui.components.PaperDivider
import dev.milan.cryptogram.ui.components.SectionLabel
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

@Composable
fun DailyScreen(
    onPlay: (date: String, difficulty: Difficulty) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = viewModel(factory = DailyViewModel.factory()),
) {
    val c = CryptoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, start = 26.dp, end = 26.dp, bottom = 40.dp),
    ) {
        BackHeader("Daily", onBack)
        Spacer(Modifier.height(18.dp))
        Text(state.date, fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 32.sp, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "STREAK ${state.streak}",
            fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.14.em, color = c.muted,
        )
        Spacer(Modifier.height(22.dp))

        state.cards.forEach { card ->
            PaperCard(
                onClick = { onPlay(state.date, card.difficulty) },
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                SectionLabel(card.difficulty.name)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (card.solved)
                        "Solved · ${"★".repeat(card.stars.coerceIn(0, 3))} · ${format(card.timeMs)}"
                    else "Not solved",
                    fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 17.sp, color = c.ink,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        PaperDivider()
        Spacer(Modifier.height(16.dp))
        Text("PREVIOUS DAYS", fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.18.em, color = c.muted)
        Spacer(Modifier.height(10.dp))
        state.previous.forEach { day ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
            ) {
                Text(day.date, fontFamily = Mono, fontSize = 11.sp, color = c.ink)
                Spacer(Modifier.weight(1f))
                Text(
                    if (day.allSolved) "ALL FOUR" else "INCOMPLETE",
                    fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.12.em,
                    color = if (day.allSolved) c.accent else c.muted,
                )
            }
        }
    }
}

private fun format(ms: Long?): String {
    if (ms == null) return "—"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
