package dev.milan.cryptogram.daily

import dev.milan.cryptogram.data.daily.DailyRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyStreakTest {

    private val today = LocalDate.of(2026, 9, 10)

    @Test
    fun `no solved dates gives zero`() {
        assertEquals(0, DailyRepository.currentStreak(emptyList(), today))
    }

    @Test
    fun `streak counts back from today`() {
        val dates = listOf("2026-09-10", "2026-09-09", "2026-09-08")
        assertEquals(3, DailyRepository.currentStreak(dates, today))
    }

    @Test
    fun `streak may end yesterday`() {
        val dates = listOf("2026-09-09", "2026-09-08")
        assertEquals(2, DailyRepository.currentStreak(dates, today))
    }

    @Test
    fun `a two day gap breaks the streak`() {
        val dates = listOf("2026-09-07", "2026-09-06")
        assertEquals(0, DailyRepository.currentStreak(dates, today))
    }

    @Test
    fun `gap inside history does not extend the streak`() {
        val dates = listOf("2026-09-10", "2026-09-09", "2026-09-07")
        assertEquals(2, DailyRepository.currentStreak(dates, today))
    }
}
