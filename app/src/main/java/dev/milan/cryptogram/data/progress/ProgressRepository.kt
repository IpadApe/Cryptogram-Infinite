package dev.milan.cryptogram.data.progress

import dev.milan.cryptogram.data.db.dao.LevelStars
import dev.milan.cryptogram.data.db.dao.ProgressDao
import dev.milan.cryptogram.data.db.entities.BandStats
import dev.milan.cryptogram.data.db.entities.ProgressEntity
import dev.milan.cryptogram.engine.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Wraps [ProgressDao] with [Difficulty]-typed access. */
class ProgressRepository(private val dao: ProgressDao) {

    fun solvedCount(difficulty: Difficulty): Flow<Int> = dao.countSolved(difficulty.name)

    fun bandProgress(difficulty: Difficulty): Flow<List<LevelStars>> =
        dao.bandProgress(difficulty.name)

    fun highestSolved(difficulty: Difficulty): Flow<Int> =
        dao.highestSolved(difficulty.name).map { it ?: 0 }

    /** Next unsolved level = highest solved + 1 (1 when nothing solved). */
    fun nextLevel(difficulty: Difficulty): Flow<Int> =
        dao.highestSolved(difficulty.name).map { (it ?: 0) + 1 }

    suspend fun get(difficulty: Difficulty, level: Int): ProgressEntity? =
        dao.get(difficulty.name, level)

    suspend fun statsForBand(difficulty: Difficulty): BandStats =
        dao.statsForBand(difficulty.name)

    /**
     * Records a solve. Keeps the best (lowest) time and the best (highest) star count
     * when the level was already solved.
     */
    suspend fun recordSolve(
        difficulty: Difficulty,
        level: Int,
        timeMs: Long,
        mistakes: Int,
        stars: Int,
        hintsUsed: Int,
        solvedAt: Long,
    ) {
        val existing = dao.get(difficulty.name, level)
        dao.upsert(
            ProgressEntity(
                difficulty = difficulty.name,
                level = level,
                solvedAt = existing?.solvedAt ?: solvedAt,
                bestTimeMs = existing?.bestTimeMs?.let { minOf(it, timeMs) } ?: timeMs,
                mistakes = if (existing == null) mistakes else minOf(existing.mistakes, mistakes),
                stars = maxOf(existing?.stars ?: 0, stars),
                hintsUsed = if (existing == null) hintsUsed else minOf(existing.hintsUsed, hintsUsed),
            ),
        )
    }
}
