package dev.milan.cryptogram.data.daily

import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.data.db.dao.DailyCacheDao
import dev.milan.cryptogram.data.db.dao.DailyResultDao
import dev.milan.cryptogram.data.db.dao.QuoteDao
import dev.milan.cryptogram.data.db.entities.DailyCacheEntity
import dev.milan.cryptogram.data.db.entities.DailyResultEntity
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.engine.splitmix64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Resolves the daily puzzle set for a local date (design doc section 6):
 * cache -> network -> validate -> cache; on any failure a deterministic bundled
 * fallback; best-effort prefetch of the next two days; 7-day cache prune.
 */
class DailyRepository(
    private val remote: DailyRemoteSource,
    private val cacheDao: DailyCacheDao,
    private val quoteDao: QuoteDao,
    private val quotes: QuoteRepository,
    private val resultDao: DailyResultDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // --- results / streak ------------------------------------------------

    fun solvedDatesAllFour(): Flow<List<String>> = resultDao.allDatesSolvedAllFour()

    fun resultsForDate(date: String): Flow<List<DailyResultEntity>> = resultDao.observeForDate(date)

    fun totalDailiesSolved(): Flow<Int> = resultDao.totalSolved()

    suspend fun recordSolve(
        date: String,
        difficulty: Difficulty,
        timeMs: Long,
        mistakes: Int,
        stars: Int,
    ) {
        val existing = resultDao.get(date, difficulty.name)
        resultDao.upsert(
            DailyResultEntity(
                date = date,
                difficulty = difficulty.name,
                solvedAt = existing?.solvedAt ?: System.currentTimeMillis(),
                timeMs = existing?.timeMs?.let { minOf(it, timeMs) } ?: timeMs,
                mistakes = if (existing == null) mistakes else minOf(existing.mistakes, mistakes),
                stars = maxOf(existing?.stars ?: 0, stars),
            ),
        )
    }

    companion object {
        /** Consecutive local dates, ending today or yesterday, on which all four bands were solved. */
        fun currentStreak(datesAllFour: Collection<String>, today: LocalDate): Int {
            val set = datesAllFour.toHashSet()
            var cursor = when {
                set.contains(today.toString()) -> today
                set.contains(today.minusDays(1).toString()) -> today.minusDays(1)
                else -> return 0
            }
            var count = 0
            while (set.contains(cursor.toString())) {
                count++
                cursor = cursor.minusDays(1)
            }
            return count
        }
    }

    suspend fun getToday(localDate: LocalDate): DailyPuzzles = withContext(Dispatchers.IO) {
        val date = localDate.toString()

        cacheDao.get(date)?.let { cached ->
            if (!cached.isFallback) return@withContext cached.toPuzzles()
            // fallback in cache: try once more to get the real thing, else reuse it
            runCatching { fetchAndValidate(date) }
                .getOrNull()
                ?.let { file ->
                    cacheDao.upsert(entity(date, file, isFallback = false))
                    prefetch(localDate)
                    prune(localDate)
                    return@withContext file.toPuzzles(isFallback = false)
                }
            return@withContext cached.toPuzzles()
        }

        val resolved = runCatching { fetchAndValidate(date) }.getOrNull()
        if (resolved != null) {
            cacheDao.upsert(entity(date, resolved, isFallback = false))
            prefetch(localDate)
            prune(localDate)
            return@withContext resolved.toPuzzles(isFallback = false)
        }

        val fallback = buildFallback(date)
        cacheDao.upsert(
            DailyCacheEntity(
                date = date,
                json = json.encodeToString(DailyFile.serializer(), fallback),
                fetchedAt = System.currentTimeMillis(),
                isFallback = true,
            ),
        )
        prune(localDate)
        fallback.toPuzzles(isFallback = true)
    }

    private suspend fun fetchAndValidate(date: String): DailyFile {
        val file = remote.fetchDaily(date)
        require(file.date == date) { "date mismatch" }
        require(Difficulty.entries.all { it.name in file.puzzles }) { "missing bands" }
        for (d in Difficulty.entries) {
            val pick = file.puzzles.getValue(d.name)
            val quote = quoteDao.getById(pick.quoteId) ?: error("quote ${pick.quoteId} not bundled")
            require(quote.band == d.name) { "band mismatch for ${pick.quoteId}" }
        }
        return file
    }

    private suspend fun buildFallback(date: String): DailyFile {
        val index = quotes.levelIndex()
        val ld = LocalDate.parse(date)
        val picks = Difficulty.entries.associate { d ->
            val bandSize = index.bandSize(d).coerceAtLeast(1)
            val level = (Math.floorMod(splitmix64(ld.toEpochDay()), bandSize.toLong()).toInt()) + 1
            val quoteId = index.quoteIdFor(d, level)
            val seed = splitmix64(ld.toEpochDay() * 7 + d.ordinal)
            d.name to DailyPick(quoteId, seed)
        }
        return DailyFile(date = date, puzzles = picks)
    }

    private suspend fun prefetch(localDate: LocalDate) {
        for (offset in 1..2) {
            val d = localDate.plusDays(offset.toLong())
            val key = d.toString()
            if (cacheDao.get(key) != null) continue
            runCatching { fetchAndValidate(key) }.getOrNull()?.let {
                cacheDao.upsert(entity(key, it, isFallback = false))
            }
        }
    }

    private suspend fun prune(localDate: LocalDate) {
        cacheDao.deleteOlderThan(localDate.minusDays(7).toString())
    }

    private fun entity(date: String, file: DailyFile, isFallback: Boolean) = DailyCacheEntity(
        date = date,
        json = json.encodeToString(DailyFile.serializer(), file),
        fetchedAt = System.currentTimeMillis(),
        isFallback = isFallback,
    )

    private fun DailyCacheEntity.toPuzzles(): DailyPuzzles =
        this@DailyRepository.json
            .decodeFromString(DailyFile.serializer(), this.json)
            .toPuzzles(isFallback)

    private fun DailyFile.toPuzzles(isFallback: Boolean) = DailyPuzzles(
        date = date,
        picks = Difficulty.entries.associateWith { puzzles.getValue(it.name) },
        isFallback = isFallback,
    )
}
