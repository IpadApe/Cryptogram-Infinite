package dev.milan.cryptogram.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

class RevealPolicyTest {

    private fun distinctCount(s: String) =
        s.uppercase().filter { it in 'A'..'Z' }.toSet().size

    @Test
    fun `reveal count is floor of distinct letters times ratio`() {
        val samples = listOf(
            "Imagination is more important than knowledge" to Difficulty.MEDIUM,
            "Be yourself everyone else is taken" to Difficulty.EASY,
            "The only thing we have to fear is fear itself and nothing more" to Difficulty.HARD,
            "A room without books is like a body without a soul it simply cannot breathe" to Difficulty.EXTREME,
        )
        for ((text, difficulty) in samples) {
            val n = distinctCount(text)
            val expected = floor(n * difficulty.revealRatio).toInt()
            val revealed = RevealPolicy.revealedLetters(text, difficulty, seed = 12345L)
            assertEquals("text='$text'", expected, revealed.size)
            assertTrue(revealed.all { it in 'A'..'Z' })
        }
    }

    @Test
    fun `revealed set is deterministic for a seed`() {
        val text = "Imagination is more important than knowledge"
        assertEquals(
            RevealPolicy.revealedLetters(text, Difficulty.MEDIUM, 99L),
            RevealPolicy.revealedLetters(text, Difficulty.MEDIUM, 99L),
        )
    }

    @Test
    fun `extreme reveal count follows the band ratio`() {
        val text = "Short amount of distinct letters here now"
        val revealed = RevealPolicy.revealedLetters(text, Difficulty.EXTREME, 1L)
        assertEquals(
            floor(distinctCount(text) * Difficulty.EXTREME.revealRatio).toInt(),
            revealed.size,
        )
    }
}
