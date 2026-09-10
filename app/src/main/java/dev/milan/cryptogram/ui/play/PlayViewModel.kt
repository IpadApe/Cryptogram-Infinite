package dev.milan.cryptogram.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.data.daily.DailyRepository
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
import dev.milan.cryptogram.ui.results.PlayKind
import dev.milan.cryptogram.ui.results.ResultArgs
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
import java.time.LocalDate

const val KIND_LEVEL = "LEVEL"
const val KIND_DAILY = "DAILY"

data class PlayUiState(
    val puzzle: PuzzleState,
    val quoteAuthor: String,
    val difficulty: Difficulty,
    val level: Int,
    val isDaily: Boolean,
    val canCheck: Boolean,
    val canHint: Boolean,
    val canAdHint: Boolean,
    val adHintLoaded: Boolean,
    val showFailedDialog: Boolean,
)

sealed interface PlayEvent {
    data class NavigateToResults(val args: ResultArgs) : PlayEvent
    data object LoadFailed : PlayEvent
}

class PlayViewModel(
    private val difficulty: Difficulty,
    private val level: Int,
    private val dailyDate: String?,
    private val quotes: QuoteRepository,
    private val progress: ProgressRepository,
    private val daily: DailyRepository,
    private val inProgressDao: InProgressDao,
    private val settings: SettingsStore,
) : ViewModel() {

    private val isDaily = dailyDate != null
    private val kind = if (isDaily) KIND_DAILY else KIND_LEVEL

    private val _state = MutableStateFlow<PlayUiState?>(null)
    val state: StateFlow<PlayUiState?> = _state.asStateFlow()

    private val events = Channel<PlayEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val resumed = MutableStateFlow(false)
    private val failedDialog = MutableStateFlow(false)
    private val adHintLoaded = MutableStateFlow(false)

    private lateinit var levelIndex: LevelIndex
    private var quoteId = -1
    private var dailySeed = 0L
    private var author = ""
    private var restartCount = 0
    private var solveHandled = false

    private var session: PuzzleSession? = null
    private val bindingJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            val ok = runCatching {
                levelIndex = quotes.levelIndex()
                if (isDaily) {
                    val pick = daily.getToday(LocalDate.parse(dailyDate)).picks.getValue(difficulty)
                    quoteId = pick.quoteId
                    dailySeed = pick.seed
                } else {
                    quoteId = levelIndex.quoteIdFor(difficulty, level)
                }
                val quote = quotes.byId(quoteId)
                author = quote?.author.orEmpty()

                val slot = inProgressDao.get(kind, difficulty.name)
                val matches = if (isDaily) slot?.date == dailyDate else slot?.level == level
                val restored = slot
                    ?.takeIf { matches }
                    ?.let { runCatching { PuzzleSession.fromJson(it.stateJson) }.getOrNull() }
                startSession(restored ?: freshSession(quote?.text.orEmpty(), cycleOffset = 0))
            }.isSuccess
            if (!ok) events.send(PlayEvent.LoadFailed)
        }
    }

    private fun freshSession(plain: String, cycleOffset: Int): PuzzleSession {
        val seed = if (isDaily) {
            dailySeed + cycleOffset
        } else {
            val cycle = levelIndex.cycleFor(difficulty, level) + cycleOffset
            difficulty.ordinal * 1_000_000_000L + level * 1_000L + cycle
        }
        return PuzzleSession(plain, difficulty, seed)
    }

    private fun startSession(newSession: PuzzleSession) {
        bindingJobs.forEach(Job::cancel)
        bindingJobs.clear()
        session = newSession
        solveHandled = false

        bindingJobs += viewModelScope.launch {
            combine(newSession.state, failedDialog, adHintLoaded) { s, failed, adLoaded ->
                uiState(s, failed, adLoaded)
            }.collect { _state.value = it }
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
                            kind = kind,
                            difficulty = difficulty.name,
                            level = level,
                            date = dailyDate,
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

    private fun uiState(s: PuzzleState, failed: Boolean, adLoaded: Boolean): PlayUiState {
        val running = s.status == PuzzleStatus.IN_PROGRESS
        return PlayUiState(
            puzzle = s,
            quoteAuthor = author,
            difficulty = difficulty,
            level = level,
            isDaily = isDaily,
            canCheck = running && difficulty.feedback == FeedbackMode.ON_CHECK,
            canHint = running && s.hintsLeft > 0,
            canAdHint = running && s.hintsLeft == 0 &&
                s.adHintsUsed < MAX_AD_HINTS_PER_PUZZLE && adLoaded,
            adHintLoaded = adLoaded,
            showFailedDialog = failed,
        )
    }

    fun setAdHintLoaded(value: Boolean) { adHintLoaded.value = value }

    private suspend fun onSolved(s: PuzzleState) {
        if (solveHandled) return
        solveHandled = true
        val stars = starRating(s.mistakes, s.adHintsUsed)
        val hintsUsed = (difficulty.freeHints - s.hintsLeft) + s.adHintsUsed

        if (isDaily) {
            daily.recordSolve(dailyDate!!, difficulty, s.elapsedMs, s.mistakes, stars)
        } else {
            progress.recordSolve(
                difficulty = difficulty,
                level = level,
                timeMs = s.elapsedMs,
                mistakes = s.mistakes,
                stars = stars,
                hintsUsed = hintsUsed,
                solvedAt = System.currentTimeMillis(),
            )
            val current = settings.currentLevel(difficulty).first()
            settings.setCurrentLevel(difficulty, maxOf(current, level + 1))
        }
        inProgressDao.delete(kind, difficulty.name)

        events.send(
            PlayEvent.NavigateToResults(
                ResultArgs(
                    kind = if (isDaily) PlayKind.DAILY else PlayKind.LEVEL,
                    difficulty = difficulty,
                    level = if (isDaily) null else level,
                    date = dailyDate,
                    quoteId = quoteId,
                    timeMs = s.elapsedMs,
                    mistakes = s.mistakes,
                    hintsUsed = hintsUsed,
                    stars = stars,
                ),
            ),
        )
    }

    fun setResumed(value: Boolean) { resumed.value = value }
    fun select(cipherNum: Int) { session?.select(cipherNum) }
    fun enter(plainChar: Char) { session?.enter(plainChar) }
    fun clearCell() { session?.clear() }
    fun nextNumber() { session?.selectNext() }
    fun check() { session?.check() }
    fun hint(fromAd: Boolean) { session?.hint(fromAd) }

    fun tryAgain() {
        val plain = session?.state?.value?.plain ?: return
        restartCount++
        failedDialog.value = false
        startSession(freshSession(plain, cycleOffset = 1 + restartCount))
    }

    companion object {
        fun factory(difficulty: Difficulty, level: Int, dailyDate: String? = null) = viewModelFactory {
            initializer {
                val c = appContainer
                PlayViewModel(
                    difficulty = difficulty,
                    level = level,
                    dailyDate = dailyDate,
                    quotes = c.quoteRepository,
                    progress = c.progressRepository,
                    daily = c.dailyRepository,
                    inProgressDao = c.inProgressDao,
                    settings = c.settingsStore,
                )
            }
        }
    }
}
