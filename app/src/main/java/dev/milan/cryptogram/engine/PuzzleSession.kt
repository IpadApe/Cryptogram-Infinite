package dev.milan.cryptogram.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Pure-Kotlin puzzle logic (design doc section 4.5). No Android dependencies.
 *
 * Construct with the plaintext, its [Difficulty] and a seed; the cipher key, the
 * ciphertext and the pre-revealed letters are all derived deterministically.
 */
class PuzzleSession private constructor(initial: PuzzleState) {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    constructor(plain: String, difficulty: Difficulty, seed: Long) : this(
        buildInitial(plain.uppercase(), difficulty, seed)
    )

    private val difficulty: Difficulty = initial.difficulty

    private val correctPlainOf: Map<Char, Char> = run {
        val inv = Cipher.invert(initial.key)
        ('A'..'Z').associateWith { c -> inv[c - 'A'] }
    }

    private fun lockedCipherChars(s: PuzzleState): Set<Char> =
        s.solvableCipherChars.filter { correctPlainOf.getValue(it) in s.revealed }.toSet()

    // --- public API --------------------------------------------------------

    fun select(cipherChar: Char) {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (cipherChar !in s.solvableCipherChars) return
        _state.value = s.copy(selectedCipherChar = cipherChar)
    }

    fun enter(plainGuess: Char) {
        val guess = plainGuess.uppercaseChar()
        var s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        val target = s.selectedCipherChar ?: return
        if (guess !in 'A'..'Z') return
        if (target in lockedCipherChars(s)) return

        val locked = lockedCipherChars(s)
        val mapping = s.mapping.toMutableMap()
        // One plaintext letter may back only one cipher letter.
        mapping.entries
            .filter { it.value == guess && it.key != target && it.key !in locked }
            .map { it.key }
            .forEach { mapping.remove(it) }
        mapping[target] = guess

        val wrong = s.wrongCipherChars.toMutableSet()
        var mistakes = s.mistakes
        var lives = s.livesLeft

        if (difficulty.feedback == FeedbackMode.IMMEDIATE) {
            if (correctPlainOf.getValue(target) != guess) {
                wrong += target
                mistakes++
                lives--
            } else {
                wrong -= target
            }
        }

        s = s.copy(
            mapping = mapping,
            wrongCipherChars = wrong,
            mistakes = mistakes,
            livesLeft = lives,
        )
        s = evaluateIfComplete(s)
        s = advanceSelection(s)
        _state.value = finalize(s)
    }

    fun clear() {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        val target = s.selectedCipherChar ?: return
        if (target in lockedCipherChars(s)) return
        _state.value = s.copy(
            mapping = s.mapping - target,
            wrongCipherChars = s.wrongCipherChars - target,
        )
    }

    /** ON_CHECK feedback only. */
    fun check() {
        var s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (difficulty.feedback != FeedbackMode.ON_CHECK) return

        val wrong = s.mapping.filter { (c, g) -> correctPlainOf.getValue(c) != g }.keys
        if (wrong.isNotEmpty()) {
            s = s.copy(
                wrongCipherChars = wrong,
                mistakes = s.mistakes + 1,
                livesLeft = s.livesLeft - 1,
            )
        } else if (isGridComplete(s)) {
            s = s.copy(status = PuzzleStatus.SOLVED, wrongCipherChars = emptySet())
        }
        _state.value = finalize(s)
    }

    fun hint(fromAd: Boolean) {
        var s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (!fromAd && s.hintsLeft <= 0) return
        if (fromAd && s.adHintsUsed >= MAX_AD_HINTS_PER_PUZZLE) return

        val target = s.selectedCipherChar?.takeIf { it !in lockedCipherChars(s) }
            ?: firstUnmapped(s)
            ?: return
        val answer = correctPlainOf.getValue(target)

        val mapping = s.mapping.toMutableMap()
        mapping.entries.filter { it.value == answer && it.key != target }.map { it.key }
            .forEach { mapping.remove(it) }
        mapping[target] = answer

        s = s.copy(
            mapping = mapping,
            revealed = s.revealed + answer,
            wrongCipherChars = s.wrongCipherChars - target,
            hintsLeft = if (fromAd) s.hintsLeft else s.hintsLeft - 1,
            adHintsUsed = if (fromAd) s.adHintsUsed + 1 else s.adHintsUsed,
        )
        s = evaluateIfComplete(s)
        s = advanceSelection(s)
        _state.value = finalize(s)
    }

    fun tick(deltaMs: Long) {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        _state.value = s.copy(elapsedMs = s.elapsedMs + deltaMs)
    }

    fun toJson(): String = json.encodeToString(PuzzleState.serializer(), _state.value)

    // --- internals -------------------------------------------------------

    private fun isGridComplete(s: PuzzleState): Boolean =
        s.solvableCipherChars.all { it in s.mapping }

    private fun isGridCorrect(s: PuzzleState): Boolean =
        s.solvableCipherChars.all { s.mapping[it] == correctPlainOf.getValue(it) }

    private fun firstUnmapped(s: PuzzleState): Char? =
        s.cipher.firstOrNull { it in 'A'..'Z' && it !in s.mapping }

    private fun advanceSelection(s: PuzzleState): PuzzleState {
        if (s.status != PuzzleStatus.IN_PROGRESS) return s
        val next = firstUnmapped(s) ?: return s
        return s.copy(selectedCipherChar = next)
    }

    /** Full-grid evaluation once every cipher letter is mapped. */
    private fun evaluateIfComplete(s: PuzzleState): PuzzleState {
        if (!isGridComplete(s)) return s
        if (isGridCorrect(s)) {
            return s.copy(status = PuzzleStatus.SOLVED, wrongCipherChars = emptySet())
        }
        if (difficulty.feedback == FeedbackMode.ON_COMPLETE) {
            // Lose a life, but do not expose which letters are wrong.
            return s.copy(mistakes = s.mistakes + 1, livesLeft = s.livesLeft - 1)
        }
        return s
    }

    private fun finalize(s: PuzzleState): PuzzleState =
        if (s.status == PuzzleStatus.IN_PROGRESS && s.livesLeft <= 0) {
            s.copy(status = PuzzleStatus.FAILED)
        } else {
            s
        }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): PuzzleSession =
            PuzzleSession(json.decodeFromString(PuzzleState.serializer(), text))

        private fun buildInitial(plain: String, difficulty: Difficulty, seed: Long): PuzzleState {
            val key = Cipher.key(seed)
            val cipher = Cipher.encrypt(plain, key)
            val revealed = RevealPolicy.revealedLetters(plain, difficulty, seed)

            val inv = Cipher.invert(key)
            val solvable = cipher.filter { it in 'A'..'Z' }.toSet()
            val mapping = solvable
                .filter { inv[it - 'A'] in revealed }
                .associateWith { inv[it - 'A'] }

            val firstUnmapped = cipher.firstOrNull { it in 'A'..'Z' && it !in mapping }

            return PuzzleState(
                plain = plain,
                cipher = cipher,
                difficulty = difficulty,
                key = key,
                mapping = mapping,
                revealed = revealed,
                livesLeft = difficulty.lives,
                hintsLeft = difficulty.freeHints,
                adHintsUsed = 0,
                mistakes = 0,
                wrongCipherChars = emptySet(),
                selectedCipherChar = firstUnmapped,
                elapsedMs = 0L,
                status = PuzzleStatus.IN_PROGRESS,
            )
        }

    }
}
