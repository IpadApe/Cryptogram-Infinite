package dev.milan.cryptogram.engine

import kotlin.math.floor

/**
 * Chooses which distinct plaintext letters are pre-revealed for a puzzle
 * (design doc section 4.3). Count is `floor(distinctLetters * revealRatio)`,
 * deterministic for the puzzle seed. Revealed letters are pre-filled and locked.
 */
object RevealPolicy {

    fun revealedLetters(plain: String, difficulty: Difficulty, seed: Long): Set<Char> {
        val distinct = plain.uppercase().filter { it in 'A'..'Z' }.toSortedSet().toList()
        val n = floor(distinct.size * difficulty.revealRatio).toInt()
        if (n <= 0) return emptySet()
        val rng = kotlin.random.Random(seed xor 0x5EEDL)
        return distinct.shuffled(rng).take(n).toSet()
    }
}
