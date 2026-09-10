package dev.milan.cryptogram.ui.results

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.ads.BannerAd
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

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
    val c = CryptoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onAccent = c.onAccent
    val faint = onAccent.copy(alpha = 0.6f)

    Column(modifier.fillMaxSize().background(c.paper)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(c.accent)
                .padding(horizontal = 26.dp)
                .padding(top = 72.dp, bottom = 32.dp),
        ) {
            Box(
                Modifier
                    .width(38.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(onAccent.copy(alpha = 0.35f))
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))

            SolvedLabel(faint)
            Spacer(Modifier.height(14.dp))

            if (state.quoteText.isNotEmpty()) {
                Text(
                    "“${state.quoteText}”",
                    fontFamily = Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light,
                    fontSize = 20.sp, lineHeight = 30.sp, color = onAccent,
                )
                Spacer(Modifier.height(14.dp))
            }
            Text(
                buildString {
                    append(state.author.ifBlank { "Unknown" })
                    if (state.source.isNotBlank()) append(", ${state.source}")
                },
                fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = faint,
                modifier = if (state.sourceUrl.isNotBlank()) Modifier.clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, state.sourceUrl.toUri())) }
                } else Modifier,
            )

            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(onAccent.copy(alpha = 0.28f)))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("TIME", formatTime(args.timeMs), onAccent, faint)
                Stat("REVEALS", args.hintsUsed.toString(), onAccent, faint)
                Stat("STARS", "★".repeat(args.stars.coerceIn(0, 3)), onAccent, faint)
            }

            Spacer(Modifier.height(24.dp))
            val primaryLabel = if (args.kind == PlayKind.LEVEL && args.level != null) "Next level" else "Back to Daily"
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                SheetButton(primaryLabel, filled = true, modifier = Modifier.weight(1f)) {
                    if (args.kind == PlayKind.LEVEL && args.level != null) onNextLevel(args) else onBackToDaily()
                }
                SheetButton("Share", filled = false, modifier = Modifier.weight(1f)) {
                    val band = args.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
                    val lvl = args.level?.toString() ?: args.date.orEmpty()
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Cryptogram Infinite · $band $lvl · ${formatTime(args.timeMs)} · ${"★".repeat(args.stars)}",
                                )
                            },
                            null,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            SheetButton("Done", filled = false, modifier = Modifier.fillMaxWidth(), onClick = onHome)
        }
        BannerAd()
    }
}

@Composable
private fun SolvedLabel(faint: androidx.compose.ui.graphics.Color) {
    Text(
        "SOLVED",
        fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.sp,
        letterSpacing = 0.2.em, color = faint,
    )
}


@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(
    label: String,
    value: String,
    strong: androidx.compose.ui.graphics.Color,
    faint: androidx.compose.ui.graphics.Color,
) {
    Column(Modifier.weight(1f)) {
        Text(value, fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 25.sp, color = strong)
        Spacer(Modifier.height(7.dp))
        Text(label, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.16.em, color = faint)
    }
}

@Composable
private fun SheetButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = CryptoTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(50.dp)
            .clip(shape)
            .then(
                if (filled) Modifier.background(c.onAccent)
                else Modifier.border(1.dp, c.onAccent.copy(alpha = 0.45f), shape),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.16.em,
            color = if (filled) c.accent else c.onAccent.copy(alpha = 0.9f),
        )
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
