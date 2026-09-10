package dev.milan.cryptogram.engine

import kotlin.math.floor

/**
 * Chooses which distinct plaintext letters are pre-revealed for a puzzle
 * (design doc section 4.3). Count is `floor(distinctLetters * revealRatio)`, but
 * never leaves more than `difficulty.maxHidden` distinct letters blank, so short
 * quotes stay solvable. Deterministic for the puzzle seed; revealed letters are
 * pre-filled and locked.
 */
object RevealPolicy {

    fun revealCount(distinct: Int, difficulty: Difficulty): Int =
        maxOf(
            floor(distinct * difficulty.revealRatio).toInt(),
            distinct - difficulty.maxHidden,
        ).coerceIn(0, distinct)

    fun revealedLetters(plain: String, difficulty: Difficulty, seed: Long): Set<Char> {
        val distinct = plain.uppercase().filter { it in 'A'..'Z' }.toSortedSet().toList()
        val n = revealCount(distinct.size, difficulty)
        if (n <= 0) return emptySet()
        val rng = kotlin.random.Random(seed xor 0x5EEDL)
        return distinct.shuffled(rng).take(n).toSet()
    }
}
