package dev.milan.cryptogram.ui.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.data.db.dao.InProgressDao
import dev.milan.cryptogram.data.progress.ProgressRepository
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

enum class LevelStatus { UNSOLVED, IN_PROGRESS, SOLVED }

data class LevelCell(val level: Int, val status: LevelStatus, val stars: Int)

data class LevelSelectUiState(
    val difficulty: Difficulty,
    val page: Int,
    val totalPages: Int,
    val continueLevel: Int,
    val levels: List<LevelCell>,
)

private const val PAGE_SIZE = 100

class LevelSelectViewModel(
    private val difficulty: Difficulty,
    quotes: QuoteRepository,
    progress: ProgressRepository,
    inProgressDao: InProgressDao,
) : ViewModel() {

    private val page = MutableStateFlow(0)
    private val bandSize = MutableStateFlow(0)

    init {
        viewModelScope.launch { bandSize.value = quotes.levelIndex().bandSize(difficulty) }
    }

    val state: StateFlow<LevelSelectUiState> = combine(
        page,
        bandSize,
        progress.bandProgress(difficulty),
        progress.highestSolved(difficulty),
        inProgressDao.observe("LEVEL", difficulty.name),
    ) { page, size, solved, highest, slot ->
        val starsByLevel = solved.associate { it.level to it.stars }
        val inProgressLevel = slot?.level
        val totalPages = max(1, ceil(max(size, highest + PAGE_SIZE) / PAGE_SIZE.toDouble()).toInt())
        val start = page * PAGE_SIZE + 1
        val cells = (start until start + PAGE_SIZE).map { level ->
            val status = when {
                starsByLevel.containsKey(level) -> LevelStatus.SOLVED
                level == inProgressLevel -> LevelStatus.IN_PROGRESS
                else -> LevelStatus.UNSOLVED
            }
            LevelCell(level, status, starsByLevel[level] ?: 0)
        }
        LevelSelectUiState(difficulty, page, totalPages, highest + 1, cells)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LevelSelectUiState(difficulty, 0, 1, 1, emptyList()),
    )

    fun setPage(p: Int) {
        page.value = p.coerceAtLeast(0)
    }

    companion object {
        fun factory(difficulty: Difficulty) = viewModelFactory {
            initializer {
                val c = appContainer
                LevelSelectViewModel(
                    difficulty,
                    c.quoteRepository,
                    c.progressRepository,
                    c.database.inProgressDao(),
                )
            }
        }
    }
}
