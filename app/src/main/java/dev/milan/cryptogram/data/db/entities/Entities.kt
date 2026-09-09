package dev.milan.cryptogram.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A verified quote loaded from the bundled corpus. `band` holds a [Difficulty] name. */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: Int,
    val text: String,
    val author: String,
    val source: String,
    val sourceUrl: String,
    val band: String,
)

/** One row per solved level. Keyed by (difficulty, level). */
@Entity(tableName = "progress", primaryKeys = ["difficulty", "level"])
data class ProgressEntity(
    val difficulty: String,
    val level: Int,
    val solvedAt: Long,
    val bestTimeMs: Long,
    val mistakes: Int,
    val stars: Int,
    val hintsUsed: Int,
)

/**
 * Autosave slot for an in-progress puzzle. One slot per (kind, difficulty).
 * `kind` is "LEVEL" or "DAILY"; `date` is set only for daily puzzles.
 */
@Entity(tableName = "in_progress", primaryKeys = ["kind", "difficulty"])
data class InProgressEntity(
    val kind: String,
    val difficulty: String,
    val level: Int,
    val date: String?,
    val stateJson: String,
    val updatedAt: Long,
)

/** Cached daily puzzle file for a given local date. */
@Entity(tableName = "daily_cache")
data class DailyCacheEntity(
    @PrimaryKey val date: String,
    val json: String,
    val fetchedAt: Long,
    val isFallback: Boolean,
)

/** One row per solved daily puzzle. Keyed by (date, difficulty). */
@Entity(tableName = "daily_result", primaryKeys = ["date", "difficulty"])
data class DailyResultEntity(
    val date: String,
    val difficulty: String,
    val solvedAt: Long,
    val timeMs: Long,
    val mistakes: Int,
    val stars: Int,
)

/** Aggregate stats for one band, produced by [ProgressDao.statsForBand]. */
data class BandStats(
    val count: Int,
    val minBestTimeMs: Long?,
    val avgBestTimeMs: Double?,
    val sumStars: Int,
)
