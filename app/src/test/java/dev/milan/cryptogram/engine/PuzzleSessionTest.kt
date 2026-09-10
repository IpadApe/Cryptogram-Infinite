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
        val last = session.numbers().last { session.correctFor(it) !in session.state.value.revealed }

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
    fun `ON_CHECK marks wrong tiles without costing a life`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.HARD, seed = 3L)
        val start = session.state.value.livesLeft
        val last = session.numbers().last { session.correctFor(it) !in session.state.value.revealed }

        session.solveAllCorrect(except = last)
        val bogus = ('A'..'Z').first { it !in alphabetPlain }
        session.select(last)
        session.enter(bogus)

        assertEquals(start, session.state.value.livesLeft)
        assertEquals(0, session.state.value.mistakes)

        session.check()
        assertEquals("check does not cost a life", start, session.state.value.livesLeft)
        assertEquals(1, session.state.value.mistakes)
        assertTrue(last in session.state.value.wrongCipherNums)

        session.select(last)
        session.enter(session.correctFor(last))
        session.check()
        assertEquals(PuzzleStatus.SOLVED, session.state.value.status)
        assertEquals(start, session.state.value.livesLeft)
    }

    @Test
    fun `ON_CHECK auto-marks wrong tiles once the grid is full`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.HARD, seed = 7L)
        val last = session.numbers().last { session.correctFor(it) !in session.state.value.revealed }
        session.solveAllCorrect(except = last)
        val bogus = ('A'..'Z').first { it !in alphabetPlain }
        session.select(last)
        session.enter(bogus) // grid now full, one wrong — no CHECK pressed

        val s = session.state.value
        assertEquals(PuzzleStatus.IN_PROGRESS, s.status)
        assertTrue("wrong tile surfaced automatically", last in s.wrongCipherNums)

        session.select(last)
        session.enter(session.correctFor(last))
        assertEquals(PuzzleStatus.SOLVED, session.state.value.status)
    }

    @Test
    fun `hint on the last unmapped number solves the puzzle`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.MEDIUM, seed = 4L)
        val last = session.numbers().last { session.correctFor(it) !in session.state.value.revealed }

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
    fun `autofill fills every tile of a number at once`() {
        val plain = "BANANA BAND" // number for A/B/N repeats
        val session = PuzzleSession(plain, Difficulty.EXTREME, seed = 9L, autofill = true)
        val first = session.numbers().first { session.correctFor(it) !in session.state.value.revealed }
        val positions = session.state.value.letterNums.withIndex().filter { it.value == first }.map { it.index }
        session.select(first)
        session.enter(session.correctFor(first))
        assertTrue(positions.all { session.state.value.isPositionFilled(it) })
    }

    @Test
    fun `manual mode fills one tile at a time`() {
        val plain = "BANANA BAND"
        val session = PuzzleSession(plain, Difficulty.EXTREME, seed = 9L, autofill = false)
        val target = session.numbers().first { session.correctFor(it) !in session.state.value.revealed }
        val positions = session.state.value.letterNums.withIndex()
            .filter { it.value == target }.map { it.index }
        if (positions.size < 2) return // need a repeated number for this check

        session.selectAt(positions[0])
        session.enter(session.correctFor(target))
        assertTrue("first tile filled", session.state.value.isPositionFilled(positions[0]))
        assertTrue("second tile not yet filled", !session.state.value.isPositionFilled(positions[1]))

        session.selectAt(positions[1])
        session.enter(session.correctFor(target))
        assertTrue(session.state.value.isPositionFilled(positions[1]))
    }

    @Test
    fun `switching to manual mid-game keeps placed letters visible`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EXTREME, seed = 11L, autofill = true)
        val target = session.numbers().first { session.correctFor(it) !in session.state.value.revealed }
        session.select(target)
        session.enter(session.correctFor(target))
        val positions = session.state.value.letterNums.withIndex()
            .filter { it.value == target }.map { it.index }

        session.setAutofill(false)
        assertTrue("progress carries across the mode switch",
            positions.all { session.state.value.isPositionFilled(it) })
    }

    @Test
    fun `manual mode is solved only when every tile is filled`() {
        val session = PuzzleSession(alphabetPlain, Difficulty.EXTREME, seed = 10L, autofill = false)
        val inv = Cipher.invert(session.state.value.key)
        val letterNums = session.state.value.letterNums
        letterNums.indices.forEach { pos ->
            if (session.state.value.isPositionFilled(pos)) return@forEach
            session.selectAt(pos)
            session.enter(inv[letterNums[pos]])
        }
        assertEquals(PuzzleStatus.SOLVED, session.state.value.status)
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
