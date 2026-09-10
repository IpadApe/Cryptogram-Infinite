package dev.milan.cryptogram.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import dev.milan.cryptogram.ads.BannerAd
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.components.PaperCard
import dev.milan.cryptogram.ui.components.SectionLabel
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

@Composable
fun HomeScreen(
    onOpenBand: (Difficulty) -> Unit,
    onOpenDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
) {
    val c = CryptoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .padding(top = 56.dp, start = 26.dp, end = 26.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CRYPTOGRAM",
                fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                letterSpacing = 0.24.em, color = c.ink,
            )
            Spacer(Modifier.weight(1f))
            val streak = (state as? HomeUiState.Ready)?.streak ?: 0
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.accent.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "$streak DAY STREAK",
                        fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.5.sp,
                        letterSpacing = 0.08.em, color = c.accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        when (val s = state) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }

            is HomeUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.bands, key = { it.difficulty }) { band ->
                        PaperCard(onClick = { onOpenBand(band.difficulty) }) {
                            SectionLabel(band.difficulty.name)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Level ${band.nextLevel}",
                                fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 26.sp,
                                color = c.ink,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${band.solvedCount} SOLVED",
                                fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.14.em,
                                color = c.muted,
                            )
                        }
                    }
                    item {
                        PaperCard(onClick = onOpenDaily) {
                            SectionLabel("Daily")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                s.daily?.date ?: "Today's four puzzles",
                                fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 20.sp,
                                color = c.ink,
                            )
                            s.daily?.let { d ->
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Difficulty.entries.forEach { band ->
                                        val done = band in d.solvedBands
                                        Text(
                                            (if (done) "● " else "○ ") + band.name.first(),
                                            fontFamily = Mono, fontSize = 9.5.sp,
                                            color = if (done) c.accent else c.muted,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "STREAK ${s.streak}",
                                fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.14.em,
                                color = c.muted,
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    TabItem("Stats", onOpenStats)
                    TabItem("Settings", onOpenSettings)
                    if (!s.removeAdsOwned) TabItem("Remove ads", onOpenSettings)
                }

                BannerAd()
            }
        }
    }
}

@Composable
private fun TabItem(label: String, onClick: () -> Unit) {
    Text(
        label.uppercase(),
        modifier = Modifier.clickable(onClick = onClick),
        fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.14.em,
        color = CryptoTheme.colors.muted,
    )
}
