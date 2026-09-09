package dev.milan.cryptogram.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleSessionTest {

    private val alphabetPlain = "ABCDEFGHIJKLMNOP" // 16 distinct letters, no repeats

    private fun PuzzleSession.correctFor(cipherChar: Char): Char {
        val inv = Cipher.invert(state.value.key)
        return inv[cipherChar - 'A']
    }

    /** A cipher letter present in the ciphertext that is not pre-revealed/locked. */
    private fun PuzzleSession.firstUnlocked(): Char {
        val s = state.value
        return s.cipher.first { it in 'A'..'Z' && correctFor(it) !in s.revealed }
    }

    private fun PuzzleSession.solveAllCorrect(except: Char? = null) {
        val s = state.value
        for (c in s.cipher.toSet().filter { it in 'A'..'Z' }) {
            if (c == except) continue
            if (correctFor(c) in s.revealed) continue
            select(c)
            enter(correctFor(c))
        }
    }

    @Test
    fun `IMMEDIATE loses a life on a wrong entry and not on a correct one`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EASY, seed = 1L)
        val start = session.state.value.livesLeft

        val target = session.firstUnlocked()
        val wrong = ('A'..'Z').first { it != session.correctFor(target) && it !in alphabetPlain }
        session.select(target)
        session.enter(wrong)

        assertEquals(start - 1, session.state.value.livesLeft)
        assertEquals(1, session.state.value.mistakes)
        assertTrue(target in session.state.value.wrongCipherChars)

        session.select(target)
        session.enter(session.correctFor(target))
        assertEquals(start - 1, session.state.value.livesLeft) // unchanged
        assertTrue(target !in session.state.value.wrongCipherChars)
    }

    @Test
    fun `ON_COMPLETE loses a life on a wrong full grid without exposing wrong chars`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EXTREME, seed = 2L)
        val start = session.state.value.livesLeft
        val last = session.state.value.cipher.last { it in 'A'..'Z' }

        session.solveAllCorrect(except = last)
        // Fill the final cell with a letter that appears nowhere else, so the grid completes.
        val bogus = ('A'..'Z').first { it !in alphabetPlain }
        session.select(last)
        session.enter(bogus)

        val s = session.state.value
        assertEquals(PuzzleStatus.IN_PROGRESS, s.status)
        assertEquals(start - 1, s.livesLeft)
        assertEquals(1, s.mistakes)
        assertEquals("wrong chars must stay hidden", emptySet<Char>(), s.wrongCipherChars)
    }

    @Test
    fun `ON_CHECK loses a life only when check finds a wrong letter`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.HARD, seed = 3L)
        val start = session.state.value.livesLeft
        val last = session.state.value.cipher.last { it in 'A'..'Z' }

        session.solveAllCorrect(except = last)
        val bogus = ('A'..'Z').first { it !in alphabetPlain }
        session.select(last)
        session.enter(bogus)

        // No evaluation on entry for ON_CHECK.
        assertEquals(start, session.state.value.livesLeft)
        assertEquals(0, session.state.value.mistakes)

        session.check()
        assertEquals(start - 1, session.state.value.livesLeft)
        assertEquals(1, session.state.value.mistakes)
        assertTrue(last in session.state.value.wrongCipherChars)

        // Fix it, then check() completes the puzzle.
        session.select(last)
        session.enter(session.correctFor(last))
        session.check()
        assertEquals(PuzzleStatus.SOLVED, session.state.value.status)
        assertEquals(start - 1, session.state.value.livesLeft) // no extra penalty
    }

    @Test
    fun `hint on the last unmapped letter solves the puzzle`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.MEDIUM, seed = 4L)
        val last = session.state.value.cipher.last { it in 'A'..'Z' }

        session.solveAllCorrect(except = last)
        assertEquals(PuzzleStatus.IN_PROGRESS, session.state.value.status)

        session.select(last)
        session.hint(fromAd = false)

        assertEquals(PuzzleStatus.SOLVED, session.state.value.status)
    }

    @Test
    fun `zero lives moves the puzzle to FAILED`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EASY, seed = 5L)
        val lives = session.state.value.livesLeft
        val unlocked = session.state.value.cipher
            .filter { it in 'A'..'Z' && session.correctFor(it) !in session.state.value.revealed }
            .toSet()
            .toList()

        var wrongEntries = 0
        outer@ for (round in 0..5) {
            for (c in unlocked) {
                val wrong = ('A'..'Z').first {
                    it != session.correctFor(c) && it !in alphabetPlain
                }
                session.select(c)
                session.enter(wrong)
                if (++wrongEntries == lives) break@outer
            }
        }

        assertEquals(PuzzleStatus.FAILED, session.state.value.status)
        assertEquals(0, session.state.value.livesLeft)
    }

    @Test
    fun `autosave round-trips through json`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.HARD, seed = 6L)
        val target = session.firstUnlocked()
        session.select(target)
        session.enter(session.correctFor(target))

        val restored = PuzzleSession.fromJson(session.toJson())
        assertEquals(session.state.value, restored.state.value)

        // Restored session still enforces ON_CHECK feedback rules.
        val before = restored.state.value.livesLeft
        val other = restored.state.value.cipher.first {
            it in 'A'..'Z' && it !in restored.state.value.mapping
        }
        restored.select(other)
        restored.enter(('A'..'Z').first { it !in alphabetPlain })
        assertEquals(before, restored.state.value.livesLeft) // no immediate penalty on HARD
        assertNotEquals(PuzzleStatus.SOLVED, restored.state.value.status)
    }
}
