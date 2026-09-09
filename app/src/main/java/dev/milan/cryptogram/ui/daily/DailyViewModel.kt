package dev.milan.cryptogram.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.data.daily.DailyRepository
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyCard(
    val difficulty: Difficulty,
    val solved: Boolean,
    val stars: Int,
    val timeMs: Long?,
)

data class PreviousDay(val date: String, val allSolved: Boolean)

data class DailyUiState(
    val date: String,
    val cards: List<DailyCard>,
    val streak: Int,
    val previous: List<PreviousDay>,
    val loading: Boolean,
)

class DailyViewModel(
    private val daily: DailyRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val todayStr = today.toString()
    private val ready = MutableStateFlow(false)

    val state: StateFlow<DailyUiState> = combine(
        ready,
        daily.resultsForDate(todayStr),
        daily.solvedDatesAllFour(),
    ) { isReady, todayResults, allFour ->
        val byBand = todayResults.associateBy { it.difficulty }
        val cards = Difficulty.entries.map { d ->
            val r = byBand[d.name]
            DailyCard(d, r != null, r?.stars ?: 0, r?.timeMs)
        }
        val streak = DailyRepository.currentStreak(allFour, today)
        val previous = (1..6).map { offset ->
            val date = today.minusDays(offset.toLong()).toString()
            PreviousDay(date, allFour.contains(date))
        }
        DailyUiState(todayStr, cards, streak, previous, loading = !isReady)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DailyUiState(todayStr, emptyList(), 0, emptyList(), loading = true),
    )

    init {
        viewModelScope.launch {
            runCatching { daily.getToday(today) }
            ready.value = true
            // keep bestStreak up to date
            val streak = DailyRepository.currentStreak(daily.solvedDatesAllFour().first(), today)
            if (streak > settings.bestStreak.first()) settings.setBestStreak(streak)
        }
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val c = appContainer
                DailyViewModel(c.dailyRepository, c.settingsStore)
            }
        }
    }
}
