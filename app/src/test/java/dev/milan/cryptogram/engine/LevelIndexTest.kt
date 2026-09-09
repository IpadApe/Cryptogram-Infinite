package dev.milan.cryptogram.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LevelIndexTest {

    private fun index(easyIds: List<Int>, frozenMax: Int) = LevelIndex(
        quotesByBand = mapOf(
            Difficulty.EASY to easyIds,
            Difficulty.MEDIUM to emptyList(),
            Difficulty.HARD to emptyList(),
            Difficulty.EXTREME to emptyList(),
        ),
        frozenMaxId = mapOf(
            Difficulty.EASY to frozenMax,
            Difficulty.MEDIUM to 0,
            Difficulty.HARD to 0,
            Difficulty.EXTREME to 0,
        ),
    )

    @Test
    fun `level mapping is unchanged when new ids are appended`() {
        val original = index((1..50).toList(), frozenMax = 50)
        val grown = index((1..80).toList(), frozenMax = 50) // 51..80 added later

        for (level in 1..50) {
            assertEquals(
                "level $level shifted after corpus grew",
                original.quoteIdFor(Difficulty.EASY, level),
                grown.quoteIdFor(Difficulty.EASY, level),
            )
        }
    }

    @Test
    fun `appended ids extend the sequence in ascending id order`() {
        val grown = index((1..80).toList(), frozenMax = 50)
        val appendedInOrder = (51..80).toList()
        val actual = (51..80).map { grown.quoteIdFor(Difficulty.EASY, it) }
        assertEquals(appendedInOrder, actual)
    }

    @Test
    fun `sequence cycles once the band is exhausted`() {
        val idx = index((1..10).toList(), frozenMax = 10)
        assertEquals(idx.quoteIdFor(Difficulty.EASY, 1), idx.quoteIdFor(Difficulty.EASY, 11))
        assertEquals(1, idx.cycleFor(Difficulty.EASY, 11))
        assertEquals(0, idx.cycleFor(Difficulty.EASY, 10))
    }

    @Test
    fun `frozen ids are shuffled not identity ordered`() {
        val idx = index((1..50).toList(), frozenMax = 50)
        val order = (1..50).map { idx.quoteIdFor(Difficulty.EASY, it) }
        assertNotEquals((1..50).toList(), order)
        assertEquals((1..50).toSet(), order.toSet())
    }
}
