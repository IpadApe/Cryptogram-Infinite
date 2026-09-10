package dev.milan.cryptogram.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.ui.components.BackHeader
import dev.milan.cryptogram.ui.components.PaperDivider
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

@Composable
fun StatsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.factory()),
) {
    val c = CryptoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val totalSolved = state.bands.sumOf { it.solved }
    val bestOverall = state.bands.mapNotNull { it.bestTimeMs }.minOrNull()
    val avgOverall = state.bands.mapNotNull { it.avgTimeMs }.let { if (it.isEmpty()) null else it.average().toLong() }

    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, start = 26.dp, end = 26.dp, bottom = 40.dp),
    ) {
        BackHeader("Statistics", onBack)
        Spacer(Modifier.height(30.dp))

        Row(Modifier.fillMaxWidth()) {
            BigStat("Solved", totalSolved.toString(), c.ink)
            BigStat("Current streak", state.dailyStreak.toString(), c.accent)
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth()) {
            BigStat("Average", avgOverall?.let(::fmt) ?: "—", c.ink)
            BigStat("Fastest", bestOverall?.let(::fmt) ?: "—", c.ink)
        }

        Spacer(Modifier.height(32.dp))
        PaperDivider()
        Spacer(Modifier.height(18.dp))
        Text("BY DIFFICULTY", fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.18.em, color = c.muted)
        Spacer(Modifier.height(14.dp))
        val maxSolved = (state.bands.maxOfOrNull { it.solved } ?: 1).coerceAtLeast(1)
        state.bands.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.difficulty.name.take(4).uppercase(),
                    fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.12.em, color = c.muted,
                    modifier = Modifier.width(56.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.ink.copy(alpha = 0.08f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(row.solved.toFloat() / maxSolved)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.accent),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${row.solved} · ${row.bestTimeMs?.let(::fmt) ?: "—"}",
                    fontFamily = Mono, fontSize = 9.sp, color = c.muted,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PaperDivider()
        Spacer(Modifier.height(18.dp))
        Text("HABIT", fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.18.em, color = c.muted)
        Spacer(Modifier.height(12.dp))
        Text(
            "Best daily streak so far is ${state.bestStreak}. You have solved ${state.dailiesSolved} dailies.",
            fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 18.sp, lineHeight = 26.sp, color = c.ink,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BigStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    val c = CryptoTheme.colors
    Column(Modifier.weight(1f)) {
        Text(value, fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 42.sp, color = valueColor)
        Spacer(Modifier.height(9.dp))
        Text(label.uppercase(), fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.16.em, color = c.muted)
    }
}

private fun fmt(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
