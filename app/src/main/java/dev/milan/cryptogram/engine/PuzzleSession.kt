package dev.milan.cryptogram.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Pure-Kotlin puzzle logic (design doc section 4.5). No Android dependencies.
 *
 * Cipher symbols are numbers 1..26; the player assigns a plaintext letter to each.
 * With [PuzzleState.autofill] true, assigning a number fills every tile of that
 * number at once; false, the player fills one tile at a time.
 */
class PuzzleSession private constructor(initial: PuzzleState) {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    constructor(plain: String, difficulty: Difficulty, seed: Long, autofill: Boolean = true) : this(
        buildInitial(plain.uppercase(), difficulty, seed, autofill)
    )

    private val difficulty: Difficulty = initial.difficulty

    /** cipher number -> the correct plaintext letter. */
    private val correctPlainOf: Map<Int, Char> = run {
        val inv = Cipher.invert(initial.key)
        (1..26).associateWith { inv[it] }
    }

    private fun lockedCipherNums(s: PuzzleState): Set<Int> =
        s.solvableCipherNums.filter { correctPlainOf.getValue(it) in s.revealed }.toSet()

    private fun positionsOf(s: PuzzleState, num: Int): List<Int> =
        s.letterNums.withIndex().filter { it.value == num }.map { it.index }

    private fun firstUnfilledPosition(s: PuzzleState): Int? {
        val locked = lockedCipherNums(s)
        return s.letterNums.indices.firstOrNull {
            !s.isPositionFilled(it) && s.letterNums[it] !in locked
        }
    }

    /** Next unfilled, unlocked position strictly after [from]. */
    private fun nextUnfilledPositionAfter(s: PuzzleState, from: Int): Int? {
        val locked = lockedCipherNums(s)
        return ((from + 1) until s.letterNums.size).firstOrNull {
            !s.isPositionFilled(it) && s.letterNums[it] !in locked
        }
    }

    private fun firstUnmappedNumber(s: PuzzleState): Int? =
        s.tokens().firstNotNullOfOrNull { t ->
            (t as? CipherToken.Num)?.n?.takeIf { it !in s.mapping }
        }

    // --- public API --------------------------------------------------------

    fun select(cipherNum: Int) {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        if (cipherNum !in s.solvableCipherNums) return
        val pos = positionsOf(s, cipherNum).firstOrNull { !s.isPositionFilled(it) }
            ?: positionsOf(s, cipherNum).firstOrNull()
        _state.value = s.copy(selectedCipherNum = cipherNum, selectedPosition = pos, lastWrongNum = null)
    }

    fun selectAt(position: Int) {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        val num = s.letterNums.getOrNull(position) ?: return
        _state.value = s.copy(selectedCipherNum = num, selectedPosition = position, lastWrongNum = null)
    }

    fun enter(plainGuess: Char) {
        val guess = plainGuess.uppercaseChar()
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        val target = s.selectedCipherNum ?: return
        if (guess !in 'A'..'Z') return
        if (target in lockedCipherNums(s)) return

        // IMMEDIATE + wrong: don't place it, don't disturb other tiles. Lose a
        // life, flash the tile red + shake, leave it empty, keep the selection.
        if (difficulty.feedback == FeedbackMode.IMMEDIATE &&
            correctPlainOf.getValue(target) != guess
        ) {
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
        var filled = s.filledPositions.toMutableSet()

        // One plaintext letter may back only one cipher number.
        mapping.entries
            .filter { it.value == guess && it.key != target && it.key !in locked }
            .map { it.key }
            .forEach { dup ->
                mapping.remove(dup)
                filled.removeAll(positionsOf(s, dup).toSet())
            }
        mapping[target] = guess

        if (s.autofill) {
            filled.addAll(positionsOf(s, target))
        } else {
            val pos = s.selectedPosition?.takeIf { s.letterNums.getOrNull(it) == target }
                ?: positionsOf(s, target).firstOrNull { it !in filled }
                ?: positionsOf(s, target).firstOrNull()
            if (pos != null) filled.add(pos)
        }

        var next = s.copy(
            mapping = mapping,
            filledPositions = filled,
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

        val filled = s.filledPositions.toMutableSet()
        val mapping = s.mapping.toMutableMap()
        if (s.autofill) {
            filled.removeAll(positionsOf(s, target).toSet())
            mapping.remove(target)
        } else {
            s.selectedPosition?.let { filled.remove(it) }
            if (positionsOf(s, target).none { it in filled }) mapping.remove(target)
        }
        _state.value = s.copy(
            mapping = mapping,
            filledPositions = filled,
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
            // Check marks the wrong tiles so the player can finish; it costs a
            // star (mistakes++) but not a life.
            s = s.copy(wrongCipherNums = wrong, mistakes = s.mistakes + 1)
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
            ?: firstUnfilledPosition(s)?.let { s.letterNums[it] }
            ?: firstUnmappedNumber(s)
            ?: return
        val answer = correctPlainOf.getValue(target)

        val mapping = s.mapping.toMutableMap()
        val filled = s.filledPositions.toMutableSet()
        mapping.entries.filter { it.value == answer && it.key != target }.map { it.key }
            .forEach { dup ->
                mapping.remove(dup)
                filled.removeAll(positionsOf(s, dup).toSet())
            }
        mapping[target] = answer
        filled.addAll(positionsOf(s, target)) // a reveal fills every tile of the number

        s = s.copy(
            mapping = mapping,
            filledPositions = filled,
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

    /** Switch fill mode, carrying the player's progress across. */
    fun setAutofill(enabled: Boolean) {
        val s = _state.value
        if (s.autofill == enabled) return
        // Manual mode needs an explicit filled-position set; derive it from the
        // current mapping so nothing the player already placed disappears.
        val filled = if (enabled) s.filledPositions
        else s.letterNums.indices.filter { s.letterNums[it] in s.mapping }.toSet()
        _state.value = s.copy(autofill = enabled, filledPositions = filled)
    }

    /** Advance to the next unfilled tile / number (design: "NEXT NUMBER"). */
    fun selectNext() {
        val s = _state.value
        if (s.status != PuzzleStatus.IN_PROGRESS) return
        _state.value = advanceSelection(s).copy(lastWrongNum = null)
    }

    fun toJson(): String = json.encodeToString(PuzzleState.serializer(), _state.value)

    // --- internals -------------------------------------------------------

    private fun isGridComplete(s: PuzzleState): Boolean =
        if (s.autofill) s.solvableCipherNums.all { it in s.mapping }
        else s.letterNums.indices.all { it in s.filledPositions }

    private fun isGridCorrect(s: PuzzleState): Boolean =
        s.solvableCipherNums.all { s.mapping[it] == correctPlainOf.getValue(it) }

    /**
     * Move on from the just-filled tile: the next empty tile *after* it in
     * reading order, so filling in sequence keeps moving forward instead of
     * jumping back to an earlier gap left by filling out of order. Only
     * wraps to the earliest remaining gap once nothing is left ahead.
     */
    private fun advanceSelection(s: PuzzleState): PuzzleState {
        if (s.status != PuzzleStatus.IN_PROGRESS) return s
        val pos = s.selectedPosition?.let { nextUnfilledPositionAfter(s, it) }
            ?: firstUnfilledPosition(s)
            ?: return s
        return s.copy(selectedPosition = pos, selectedCipherNum = s.letterNums[pos])
    }

    private fun evaluateIfComplete(s: PuzzleState): PuzzleState {
        if (!isGridComplete(s)) return s
        if (isGridCorrect(s)) {
            return s.copy(status = PuzzleStatus.SOLVED, wrongCipherNums = emptySet())
        }
        return when (difficulty.feedback) {
            FeedbackMode.ON_COMPLETE ->
                s.copy(mistakes = s.mistakes + 1, livesLeft = s.livesLeft - 1)
            FeedbackMode.ON_CHECK ->
                // Grid is full but wrong: surface the wrong tiles now so the
                // player isn't stuck hunting for CHECK.
                s.copy(wrongCipherNums = s.mapping.filter { (n, g) ->
                    correctPlainOf.getValue(n) != g
                }.keys)
            FeedbackMode.IMMEDIATE -> s
        }
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

        private fun buildInitial(
            plain: String,
            difficulty: Difficulty,
            seed: Long,
            autofill: Boolean,
        ): PuzzleState {
            val key = Cipher.key(seed)
            val revealed = RevealPolicy.revealedLetters(plain, difficulty, seed)
            val inv = Cipher.invert(key)

            val letterNums = plain.filter { it in 'A'..'Z' }.map { key[it - 'A'] }
            val solvable = letterNums.toSet()
            val mapping = solvable.filter { inv[it] in revealed }.associateWith { inv[it] }
            val filled = letterNums.indices.filter { letterNums[it] in mapping }.toSet()
            val firstOpen = letterNums.indices.firstOrNull { it !in filled }

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
                selectedCipherNum = firstOpen?.let { letterNums[it] },
                elapsedMs = 0L,
                status = PuzzleStatus.IN_PROGRESS,
                autofill = autofill,
                filledPositions = filled,
                selectedPosition = firstOpen,
            )
        }
    }
}
