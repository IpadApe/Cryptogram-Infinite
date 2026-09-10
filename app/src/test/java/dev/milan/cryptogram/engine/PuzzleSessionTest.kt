package dev.milan.cryptogram.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleSessionTest {

    private val alphabetPlain = "ABCDEFGHIJKLMNOP" // 16 distinct letters, no repeats

    private fun PuzzleSession.correctFor(cipherNum: Int): Char {
        val inv = Cipher.invert(state.value.key)
        return inv[cipherNum]
    }

    private fun PuzzleSession.numbers(): List<Int> =
        state.value.tokens().filterIsInstance<CipherToken.Num>().map { it.n }

    private fun PuzzleSession.firstUnlocked(): Int {
        val s = state.value
        return numbers().first { correctFor(it) !in s.revealed }
    }

    private fun PuzzleSession.solveAllCorrect(except: Int? = null) {
        val s = state.value
        for (n in s.solvableCipherNums) {
            if (n == except) continue
            if (correctFor(n) in s.revealed) continue
            select(n)
            enter(correctFor(n))
        }
    }

    @Test
    fun `IMMEDIATE wrong entry loses a life, clears the guess, and flags the cell`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EASY, seed = 1L)
        val start = session.state.value.livesLeft

        val target = session.firstUnlocked()
        val wrong = ('A'..'Z').first { it != session.correctFor(target) && it !in alphabetPlain }
        session.select(target)
        session.enter(wrong)

        assertEquals(start - 1, session.state.value.livesLeft)
        assertEquals(1, session.state.value.mistakes)
        assertEquals(target, session.state.value.lastWrongNum)
        assertNull("wrong guess must not stick", session.state.value.mapping[target])

        // Selecting the cell again clears the red flag; a correct entry sticks.
        session.select(target)
        assertNull(session.state.value.lastWrongNum)
        session.enter(session.correctFor(target))
        assertEquals(start - 1, session.state.value.livesLeft)
        assertEquals(session.correctFor(target), session.state.value.mapping[target])
        assertNull(session.state.value.lastWrongNum)
    }

    @Test
    fun `ON_COMPLETE loses a life on a wrong full grid without exposing wrong numbers`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EXTREME, seed = 2L)
        val start = session.state.value.livesLeft
        val last = session.numbers().last { it in session.state.value.solvableCipherNums }

        session.solveAllCorrect(except = last)
        val bogus = ('A'..'Z').first { it !in alphabetPlain }
        session.select(last)
        session.enter(bogus)

        val s = session.state.value
        assertEquals(PuzzleStatus.IN_PROGRESS, s.status)
        assertEquals(start - 1, s.livesLeft)
        assertEquals(1, s.mistakes)
        assertEquals(emptySet<Int>(), s.wrongCipherNums)
    }

    @Test
    fun `ON_CHECK loses a life only when check finds a wrong letter`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.HARD, seed = 3L)
        val start = session.state.value.livesLeft
        val last = session.numbers().last { it in session.state.value.solvableCipherNums }

        session.solveAllCorrect(except = last)
        val bogus = ('A'..'Z').first { it !in alphabetPlain }
        session.select(last)
        session.enter(bogus)

        assertEquals(start, session.state.value.livesLeft)
        assertEquals(0, session.state.value.mistakes)

        session.check()
        assertEquals(start - 1, session.state.value.livesLeft)
        assertEquals(1, session.state.value.mistakes)
        assertTrue(last in session.state.value.wrongCipherNums)

        session.select(last)
        session.enter(session.correctFor(last))
        session.check()
        assertEquals(PuzzleStatus.SOLVED, session.state.value.status)
        assertEquals(start - 1, session.state.value.livesLeft)
    }

    @Test
    fun `hint on the last unmapped number solves the puzzle`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.MEDIUM, seed = 4L)
        val last = session.numbers().last { it in session.state.value.solvableCipherNums }

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
        val unlocked = session.numbers()
            .filter { session.correctFor(it) !in session.state.value.revealed }
            .distinct()

        var wrongEntries = 0
        outer@ for (round in 0..5) {
            for (n in unlocked) {
                val wrong = ('A'..'Z').first {
                    it != session.correctFor(n) && it !in alphabetPlain
                }
                session.select(n)
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

        val before = restored.state.value.livesLeft
        val other = restored.numbers().first { it !in restored.state.value.mapping }
        restored.select(other)
        restored.enter(('A'..'Z').first { it !in alphabetPlain })
        assertEquals(before, restored.state.value.livesLeft)
        assertNotEquals(PuzzleStatus.SOLVED, restored.state.value.status)
    }
}
