package dev.milan.cryptogram.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.data.db.dao.InProgressDao
import dev.milan.cryptogram.data.db.entities.InProgressEntity
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.data.progress.ProgressRepository
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.engine.FeedbackMode
import dev.milan.cryptogram.engine.LevelIndex
import dev.milan.cryptogram.engine.MAX_AD_HINTS_PER_PUZZLE
import dev.milan.cryptogram.engine.PuzzleSession
import dev.milan.cryptogram.engine.PuzzleState
import dev.milan.cryptogram.engine.PuzzleStatus
import dev.milan.cryptogram.engine.starRating
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val KIND_LEVEL = "LEVEL"

data class PlayUiState(
    val puzzle: PuzzleState,
    val quoteAuthor: String,
    val difficulty: Difficulty,
    val level: Int,
    val canCheck: Boolean,
    val canHint: Boolean,
    val canAdHint: Boolean,
    val adHintLoaded: Boolean,
    val showFailedDialog: Boolean,
)

data class ResultsRoute(
    val difficulty: Difficulty,
    val level: Int,
    val quoteId: Int,
    val timeMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val stars: Int,
)

sealed interface PlayEvent {
    data class NavigateToResults(val route: ResultsRoute) : PlayEvent
}

class PlayViewModel(
    private val difficulty: Difficulty,
    private val level: Int,
    private val quotes: QuoteRepository,
    private val progress: ProgressRepository,
    private val inProgressDao: InProgressDao,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayUiState?>(null)
    val state: StateFlow<PlayUiState?> = _state.asStateFlow()

    private val events = Channel<PlayEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val resumed = MutableStateFlow(false)
    private val failedDialog = MutableStateFlow(false)

    private lateinit var levelIndex: LevelIndex
    private var quoteId = -1
    private var author = ""
    private var restartCount = 0
    private var solveHandled = false

    private var session: PuzzleSession? = null
    private val bindingJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            levelIndex = quotes.levelIndex()
            quoteId = levelIndex.quoteIdFor(difficulty, level)
            val quote = quotes.byId(quoteId)
            author = quote?.author.orEmpty()

            val slot = inProgressDao.get(KIND_LEVEL, difficulty.name)
            val restored = slot
                ?.takeIf { it.level == level }
                ?.let { runCatching { PuzzleSession.fromJson(it.stateJson) }.getOrNull() }
            startSession(restored ?: freshSession(quote?.text.orEmpty(), cycleOffset = 0))
        }
    }

    private fun freshSession(plain: String, cycleOffset: Int): PuzzleSession {
        val cycle = levelIndex.cycleFor(difficulty, level) + cycleOffset
        val seed = difficulty.ordinal * 1_000_000_000L + level * 1_000L + cycle
        return PuzzleSession(plain, difficulty, seed)
    }

    private fun startSession(newSession: PuzzleSession) {
        bindingJobs.forEach(Job::cancel)
        bindingJobs.clear()
        session = newSession
        solveHandled = false

        bindingJobs += viewModelScope.launch {
            combine(newSession.state, failedDialog) { s, failed -> uiState(s, failed) }
                .collect { _state.value = it }
        }
        bindingJobs += viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (resumed.value && newSession.state.value.status == PuzzleStatus.IN_PROGRESS) {
                    newSession.tick(1_000)
                }
            }
        }
        bindingJobs += viewModelScope.launch {
            newSession.state.drop(1).debounce(300).collect { s ->
                if (s.status == PuzzleStatus.IN_PROGRESS) {
                    inProgressDao.upsert(
                        InProgressEntity(
                            kind = KIND_LEVEL,
                            difficulty = difficulty.name,
                            level = level,
                            date = null,
                            stateJson = newSession.toJson(),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
        bindingJobs += viewModelScope.launch {
            newSession.state.collect { s ->
                when (s.status) {
                    PuzzleStatus.SOLVED -> onSolved(s)
                    PuzzleStatus.FAILED -> failedDialog.value = true
                    PuzzleStatus.IN_PROGRESS -> Unit
                }
            }
        }
    }

    private fun uiState(s: PuzzleState, failed: Boolean): PlayUiState {
        val running = s.status == PuzzleStatus.IN_PROGRESS
        return PlayUiState(
            puzzle = s,
            quoteAuthor = author,
            difficulty = difficulty,
            level = level,
            canCheck = running && difficulty.feedback == FeedbackMode.ON_CHECK,
            canHint = running && s.hintsLeft > 0,
            canAdHint = running && s.hintsLeft == 0 && s.adHintsUsed < MAX_AD_HINTS_PER_PUZZLE,
            adHintLoaded = false, // wired in Brief 5
            showFailedDialog = failed,
        )
    }

    private suspend fun onSolved(s: PuzzleState) {
        if (solveHandled) return
        solveHandled = true
        val stars = starRating(s.mistakes, s.adHintsUsed)
        val hintsUsed = (difficulty.freeHints - s.hintsLeft) + s.adHintsUsed
        progress.recordSolve(
            difficulty = difficulty,
            level = level,
            timeMs = s.elapsedMs,
            mistakes = s.mistakes,
            stars = stars,
            hintsUsed = hintsUsed,
            solvedAt = System.currentTimeMillis(),
        )
        inProgressDao.delete(KIND_LEVEL, difficulty.name)
        val current = settings.currentLevel(difficulty).first()
        settings.setCurrentLevel(difficulty, maxOf(current, level + 1))
        events.send(
            PlayEvent.NavigateToResults(
                ResultsRoute(difficulty, level, quoteId, s.elapsedMs, s.mistakes, hintsUsed, stars),
            ),
        )
    }

    fun setResumed(value: Boolean) { resumed.value = value }
    fun select(cipherChar: Char) { session?.select(cipherChar) }
    fun enter(plainChar: Char) { session?.enter(plainChar) }
    fun clearCell() { session?.clear() }
    fun check() { session?.check() }
    fun hint(fromAd: Boolean) { session?.hint(fromAd) }

    fun tryAgain() {
        val plain = session?.state?.value?.plain ?: return
        restartCount++
        failedDialog.value = false
        startSession(freshSession(plain, cycleOffset = 1 + restartCount))
    }

    companion object {
        fun factory(difficulty: Difficulty, level: Int) = viewModelFactory {
            initializer {
                val c = appContainer
                PlayViewModel(
                    difficulty, level,
                    c.quoteRepository, c.progressRepository,
                    c.database.inProgressDao(), c.settingsStore,
                )
            }
        }
    }
}
