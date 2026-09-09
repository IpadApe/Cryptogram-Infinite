package dev.milan.cryptogram.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.data.corpus.CorpusLoader
import dev.milan.cryptogram.data.daily.DailyRepository
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.data.progress.ProgressRepository
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BandSummary(
    val difficulty: Difficulty,
    val nextLevel: Int,
    val solvedCount: Int,
)

data class DailySummary(
    val date: String,
    val solvedBands: Set<Difficulty>,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val bands: List<BandSummary>,
        val daily: DailySummary?,
        val streak: Int,
        val removeAdsOwned: Boolean,
    ) : HomeUiState
}

class HomeViewModel(
    private val corpusLoader: CorpusLoader,
    private val progress: ProgressRepository,
    private val daily: DailyRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            corpusLoader.load()
            val today = LocalDate.now()
            runCatching { daily.getToday(today) } // warm the cache; ignore failures
            readyStream(today).collect { ready ->
                _state.value = ready
                if (ready.streak > settings.bestStreak.first()) {
                    settings.setBestStreak(ready.streak)
                }
            }
        }
    }

    private fun readyStream(today: LocalDate) = combine(
        combine(
            Difficulty.entries.map { d ->
                combine(progress.nextLevel(d), progress.solvedCount(d)) { next, solved ->
                    BandSummary(d, next, solved)
                }
            },
        ) { it.toList() },
        settings.removeAdsOwned,
        daily.resultsForDate(today.toString()),
        daily.solvedDatesAllFour(),
    ) { bands, ownsAds, todayResults, allFour ->
        val solvedBands = todayResults
            .mapNotNull { runCatching { Difficulty.valueOf(it.difficulty) }.getOrNull() }
            .toSet()
        HomeUiState.Ready(
            bands = bands,
            daily = DailySummary(today.toString(), solvedBands),
            streak = DailyRepository.currentStreak(allFour, today),
            removeAdsOwned = ownsAds,
        )
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val c = appContainer
                HomeViewModel(c.corpusLoader, c.progressRepository, c.dailyRepository, c.settingsStore)
            }
        }
    }
}
