package dev.milan.cryptogram.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import dev.milan.cryptogram.data.corpus.CorpusLoader
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.data.progress.ProgressRepository
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class BandSummary(
    val difficulty: Difficulty,
    val nextLevel: Int,
    val solvedCount: Int,
)

/** Daily summary is populated from Brief 4 onwards. */
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
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            corpusLoader.load()
            readyStream().collect { _state.value = it }
        }
    }

    private fun readyStream() = combine(
        combine(
            Difficulty.entries.map { d ->
                combine(progress.nextLevel(d), progress.solvedCount(d)) { next, solved ->
                    BandSummary(d, next, solved)
                }
            },
        ) { it.toList() },
        settings.removeAdsOwned,
    ) { bands, ownsAds ->
        HomeUiState.Ready(bands = bands, daily = null, streak = 0, removeAdsOwned = ownsAds)
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val c = appContainer
                HomeViewModel(c.corpusLoader, c.progressRepository, c.settingsStore)
            }
        }
    }
}
