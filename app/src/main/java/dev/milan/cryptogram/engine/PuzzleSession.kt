package dev.milan.cryptogram.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Pure-Kotlin puzzle logic (design doc section 4.5). No Android dependencies.
 *
 * The cipher symbols are numbers 1..26; the player assigns a plaintext letter to
 * each number. Construct with the plaintext, its [Difficulty] and a seed.
 */
class PuzzleSession private constructor(initial: PuzzleState) {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    constructor(plain: String, difficulty: Difficulty, seed: Long) : this(
        buildInitial(plain.uppercase(), difficulty, seed)
    )

    private val difficulty: Difficulty = initial.difficulty

    /** cipher number -> the correct plaintext letter. */
    private val correctPlainOf: Map<Int, Char> = run {
        val inv = Cipher.invert(initial.key)
        (1..26).associateWith { inv[it] }
    }

    private fun lockedCipherNums(s: PuzzleState): Set<Int> =
        s.solvableCipherNums.filter { correctPlainOf.getValue(it) in s.revealed }.toSet()

    // --- public API --------------------------------------------------------

    fun select(cipherNum: Int) {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (cipherNum !in s.solvableCipherNums) return
        _state.value = s.copy(selectedCipherNum = cipherNum, lastWrongNum = null)
    }

    fun enter(plainGuess: Char) {
        val guess = plainGuess.uppercaseChar()
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        val target = s.selectedCipherNum ?: return
        if (guess !in 'A'..'Z') return
        if (target in lockedCipherNums(s)) return

        // IMMEDIATE + wrong: don't place it, don't disturb other cells. Lose a
        // life, flash the cell red + shake, and leave it empty.
        if (difficulty.feedback == FeedbackMode.IMMEDIATE &&
            correctPlainOf.getValue(target) != guess
        ) {
            // Keep the selection on this cell so the player can retry it.
            _state.value = finalize(
                s.copy(
                    mistakes = s.mistakes + 1,
                    livesLeft = s.livesLeft - 1,
                    wrongCipherNums = s.wrongCipherNums - target,
                    lastWrongNum = target,
                ),
            )
            return
        }

        val locked = lockedCipherNums(s)
        val mapping = s.mapping.toMutableMap()
        // One plaintext letter may back only one cipher number.
        mapping.entries
            .filter { it.value == guess && it.key != target && it.key !in locked }
            .map { it.key }
            .forEach { mapping.remove(it) }
        mapping[target] = guess

        var next = s.copy(
            mapping = mapping,
            wrongCipherNums = s.wrongCipherNums - target,
            lastWrongNum = null,
        )
        next = evaluateIfComplete(next)
        next = advanceSelection(next)
        _state.value = finalize(next)
    }

    fun clear() {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        val target = s.selectedCipherNum ?: return
        if (target in lockedCipherNums(s)) return
        _state.value = s.copy(
            mapping = s.mapping - target,
            wrongCipherNums = s.wrongCipherNums - target,
            lastWrongNum = null,
        )
    }

    /** ON_CHECK feedback only. */
    fun check() {
        var s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (difficulty.feedback != FeedbackMode.ON_CHECK) return

        val wrong = s.mapping.filter { (n, g) -> correctPlainOf.getValue(n) != g }.keys
        if (wrong.isNotEmpty()) {
            s = s.copy(
                wrongCipherNums = wrong,
                mistakes = s.mistakes + 1,
                livesLeft = s.livesLeft - 1,
            )
        } else if (isGridComplete(s)) {
            s = s.copy(status = PuzzleStatus.SOLVED, wrongCipherNums = emptySet())
        }
        _state.value = finalize(s)
    }

    fun hint(fromAd: Boolean) {
        var s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (!fromAd && s.hintsLeft <= 0) return
        if (fromAd && s.adHintsUsed >= MAX_AD_HINTS_PER_PUZZLE) return

        val target = s.selectedCipherNum?.takeIf { it !in lockedCipherNums(s) }
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
            wrongCipherNums = s.wrongCipherNums - target,
            lastWrongNum = null,
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
        s.solvableCipherNums.all { it in s.mapping }

    private fun isGridCorrect(s: PuzzleState): Boolean =
        s.solvableCipherNums.all { s.mapping[it] == correctPlainOf.getValue(it) }

    private fun firstUnmapped(s: PuzzleState): Int? =
        s.tokens().firstNotNullOfOrNull { t ->
            (t as? CipherToken.Num)?.n?.takeIf { it !in s.mapping }
        }

    private fun advanceSelection(s: PuzzleState): PuzzleState {
        if (s.status != PuzzleStatus.IN_PROGRESS) return s
        val next = firstUnmapped(s) ?: return s
        return s.copy(selectedCipherNum = next)
    }

    private fun evaluateIfComplete(s: PuzzleState): PuzzleState {
        if (!isGridComplete(s)) return s
        if (isGridCorrect(s)) {
            return s.copy(status = PuzzleStatus.SOLVED, wrongCipherNums = emptySet())
        }
        if (difficulty.feedback == FeedbackMode.ON_COMPLETE) {
            // Lose a life, but do not expose which numbers are wrong.
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
            val revealed = RevealPolicy.revealedLetters(plain, difficulty, seed)
            val inv = Cipher.invert(key)

            val solvable = plain.filter { it in 'A'..'Z' }.map { key[it - 'A'] }.toSet()
            val mapping = solvable
                .filter { inv[it] in revealed }
                .associateWith { inv[it] }

            val firstUnmapped = plain.firstNotNullOfOrNull { c ->
                if (c in 'A'..'Z') key[c - 'A'].takeIf { it !in mapping } else null
            }

            return PuzzleState(
                plain = plain,
                difficulty = difficulty,
                key = key,
                mapping = mapping,
                revealed = revealed,
                livesLeft = difficulty.lives,
                hintsLeft = difficulty.freeHints,
                adHintsUsed = 0,
                mistakes = 0,
                wrongCipherNums = emptySet(),
                selectedCipherNum = firstUnmapped,
                elapsedMs = 0L,
                status = PuzzleStatus.IN_PROGRESS,
            )
        }
    }
}
