package dev.milan.cryptogram.engine

import kotlinx.serialization.Serializable

@Serializable
enum class PuzzleStatus { IN_PROGRESS, SOLVED, FAILED }

/**
 * Immutable snapshot of a puzzle in play (design doc section 4.5).
 *
 * [key] maps plaintext index -> cipher char. [mapping] holds cipher char -> the
 * plaintext char the player (or a reveal/hint) has assigned to it.
 */
@Serializable
data class PuzzleState(
    val plain: String,
    val cipher: String,
    val difficulty: Difficulty,
    val key: CharArray,
    val mapping: Map<Char, Char>,
    val revealed: Set<Char>,
    val livesLeft: Int,
    val hintsLeft: Int,
    val adHintsUsed: Int,
    val mistakes: Int,
    val wrongCipherChars: Set<Char>,
    val selectedCipherChar: Char?,
    val elapsedMs: Long,
    val status: PuzzleStatus,
) {
    /** Distinct cipher letters that appear in the ciphertext and must be solved. */
    val solvableCipherChars: Set<Char>
        get() = cipher.filter { it in 'A'..'Z' }.toSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PuzzleState) return false
        return plain == other.plain &&
            cipher == other.cipher &&
            difficulty == other.difficulty &&
            key.contentEquals(other.key) &&
            mapping == other.mapping &&
            revealed == other.revealed &&
            livesLeft == other.livesLeft &&
            hintsLeft == other.hintsLeft &&
            adHintsUsed == other.adHintsUsed &&
            mistakes == other.mistakes &&
            wrongCipherChars == other.wrongCipherChars &&
            selectedCipherChar == other.selectedCipherChar &&
            elapsedMs == other.elapsedMs &&
            status == other.status
    }

    override fun hashCode(): Int {
        var result = plain.hashCode()
        result = 31 * result + cipher.hashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + mapping.hashCode()
        result = 31 * result + revealed.hashCode()
        result = 31 * result + livesLeft
        result = 31 * result + hintsLeft
        result = 31 * result + adHintsUsed
        result = 31 * result + mistakes
        result = 31 * result + wrongCipherChars.hashCode()
        result = 31 * result + (selectedCipherChar?.hashCode() ?: 0)
        result = 31 * result + elapsedMs.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}

/** Stars shown on Results (design doc section 4.5). */
fun starRating(mistakes: Int, adHintsUsed: Int): Int = when {
    mistakes == 0 && adHintsUsed == 0 -> 3
    mistakes <= 1 && adHintsUsed <= 1 -> 2
    else -> 1
}
