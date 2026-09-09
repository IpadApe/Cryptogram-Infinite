package dev.milan.cryptogram.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.data.daily.DailyRepository
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.data.progress.ProgressRepository
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BandStatRow(
    val difficulty: Difficulty,
    val solved: Int,
    val bestTimeMs: Long?,
    val avgTimeMs: Long?,
    val stars: Int,
)

data class StatsUiState(
    val bands: List<BandStatRow> = emptyList(),
    val dailyStreak: Int = 0,
    val bestStreak: Int = 0,
    val dailiesSolved: Int = 0,
    val loading: Boolean = true,
)

class StatsViewModel(
    private val progress: ProgressRepository,
    private val daily: DailyRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rows = Difficulty.entries.map { d ->
                val s = progress.statsForBand(d)
                BandStatRow(
                    difficulty = d,
                    solved = s.count,
                    bestTimeMs = s.minBestTimeMs,
                    avgTimeMs = s.avgBestTimeMs?.toLong(),
                    stars = s.sumStars,
                )
            }
            val today = LocalDate.now()
            val streak = DailyRepository.currentStreak(daily.solvedDatesAllFour().first(), today)
            val best = maxOf(settings.bestStreak.first(), streak)
            if (best > settings.bestStreak.first()) settings.setBestStreak(best)
            _state.value = StatsUiState(
                bands = rows,
                dailyStreak = streak,
                bestStreak = best,
                dailiesSolved = daily.totalDailiesSolved().first(),
                loading = false,
            )
        }
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val c = appContainer
                StatsViewModel(c.progressRepository, c.dailyRepository, c.settingsStore)
            }
        }
    }
}
