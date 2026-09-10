package dev.milan.cryptogram.engine

import kotlinx.serialization.Serializable

@Serializable
enum class PuzzleStatus { IN_PROGRESS, SOLVED, FAILED }

/**
 * Immutable snapshot of a puzzle in play (design doc section 4.5).
 *
 * The ciphertext is a number-substitution of [plain]: [key] maps plaintext index
 * -> cipher number (1..26). [mapping] holds cipher number -> the plaintext letter
 * the player (or a reveal/hint) has assigned to it.
 */
@Serializable
data class PuzzleState(
    val plain: String,
    val difficulty: Difficulty,
    val key: IntArray,
    val mapping: Map<Int, Char>,
    val revealed: Set<Char>,
    val livesLeft: Int,
    val hintsLeft: Int,
    val adHintsUsed: Int,
    val mistakes: Int,
    val wrongCipherNums: Set<Int>,
    val selectedCipherNum: Int?,
    val elapsedMs: Long,
    val status: PuzzleStatus,
    /** A cipher number whose last IMMEDIATE guess was wrong: flash red + shake, then it stays empty. */
    val lastWrongNum: Int? = null,
) {
    /** The ciphertext, position by position. */
    fun tokens(): List<CipherToken> = Cipher.encrypt(plain, key)

    /** Distinct cipher numbers that appear in the ciphertext and must be solved. */
    val solvableCipherNums: Set<Int>
        get() = plain.uppercase().filter { it in 'A'..'Z' }.map { key[it - 'A'] }.toSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PuzzleState) return false
        return plain == other.plain &&
            difficulty == other.difficulty &&
            key.contentEquals(other.key) &&
            mapping == other.mapping &&
            revealed == other.revealed &&
            livesLeft == other.livesLeft &&
            hintsLeft == other.hintsLeft &&
            adHintsUsed == other.adHintsUsed &&
            mistakes == other.mistakes &&
            wrongCipherNums == other.wrongCipherNums &&
            selectedCipherNum == other.selectedCipherNum &&
            elapsedMs == other.elapsedMs &&
            status == other.status &&
            lastWrongNum == other.lastWrongNum
    }

    override fun hashCode(): Int {
        var result = plain.hashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + mapping.hashCode()
        result = 31 * result + revealed.hashCode()
        result = 31 * result + livesLeft
        result = 31 * result + hintsLeft
        result = 31 * result + adHintsUsed
        result = 31 * result + mistakes
        result = 31 * result + wrongCipherNums.hashCode()
        result = 31 * result + (selectedCipherNum ?: 0)
        result = 31 * result + elapsedMs.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (lastWrongNum ?: 0)
        return result
    }
}

/** Stars shown on Results (design doc section 4.5). */
fun starRating(mistakes: Int, adHintsUsed: Int): Int = when {
    mistakes == 0 && adHintsUsed == 0 -> 3
    mistakes <= 1 && adHintsUsed <= 1 -> 2
    else -> 1
}
