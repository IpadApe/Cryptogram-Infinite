package dev.milan.cryptogram.data.daily

import dev.milan.cryptogram.engine.Difficulty
import kotlinx.serialization.Serializable

/** On-the-wire shape of `daily/YYYY-MM-DD.json` (design doc section 3.3). No plaintext. */
@Serializable
data class DailyFile(
    val date: String,
    val puzzles: Map<String, DailyPick>,
)

@Serializable
data class DailyPick(
    val quoteId: Int,
    val seed: Long,
)

/** Resolved daily puzzle set for one local date. */
data class DailyPuzzles(
    val date: String,
    val picks: Map<Difficulty, DailyPick>,
    val isFallback: Boolean,
)

@Serializable
data class DailyIndex(val dates: List<String> = emptyList())
