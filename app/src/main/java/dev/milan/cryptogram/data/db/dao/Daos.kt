package dev.milan.cryptogram.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.ColumnInfo
import dev.milan.cryptogram.data.db.entities.BandStats
import dev.milan.cryptogram.data.db.entities.DailyCacheEntity
import dev.milan.cryptogram.data.db.entities.DailyResultEntity
import dev.milan.cryptogram.data.db.entities.InProgressEntity
import dev.milan.cryptogram.data.db.entities.ProgressEntity
import dev.milan.cryptogram.data.db.entities.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun count(): Int

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: Int): QuoteEntity?

    @Query("SELECT id FROM quotes WHERE band = :band ORDER BY id")
    suspend fun idsForBand(band: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(quotes: List<QuoteEntity>)
}

data class LevelStars(
    @ColumnInfo(name = "level") val level: Int,
    @ColumnInfo(name = "stars") val stars: Int,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE difficulty = :difficulty AND level = :level")
    suspend fun get(difficulty: String, level: Int): ProgressEntity?

    @Upsert
    suspend fun upsert(entity: ProgressEntity)

    @Query("SELECT level, stars FROM progress WHERE difficulty = :difficulty")
    fun bandProgress(difficulty: String): Flow<List<LevelStars>>

    @Query("SELECT MAX(level) FROM progress WHERE difficulty = :difficulty")
    fun highestSolved(difficulty: String): Flow<Int?>

    @Query("SELECT COUNT(*) FROM progress WHERE difficulty = :difficulty")
    fun countSolved(difficulty: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(*)        AS count,
               MIN(bestTimeMs) AS minBestTimeMs,
               AVG(bestTimeMs) AS avgBestTimeMs,
               COALESCE(SUM(stars), 0) AS sumStars
        FROM progress
        WHERE difficulty = :difficulty
        """
    )
    suspend fun statsForBand(difficulty: String): BandStats
}

@Dao
interface InProgressDao {
    @Query("SELECT * FROM in_progress WHERE kind = :kind AND difficulty = :difficulty")
    suspend fun get(kind: String, difficulty: String): InProgressEntity?

    @Query("SELECT * FROM in_progress WHERE kind = :kind AND difficulty = :difficulty")
    fun observe(kind: String, difficulty: String): Flow<InProgressEntity?>

    @Upsert
    suspend fun upsert(entity: InProgressEntity)

    @Query("DELETE FROM in_progress WHERE kind = :kind AND difficulty = :difficulty")
    suspend fun delete(kind: String, difficulty: String)
}

@Dao
interface DailyCacheDao {
    @Query("SELECT * FROM daily_cache WHERE date = :date")
    suspend fun get(date: String): DailyCacheEntity?

    @Upsert
    suspend fun upsert(entity: DailyCacheEntity)

    @Query("DELETE FROM daily_cache WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}

@Dao
interface DailyResultDao {
    @Query("SELECT * FROM daily_result WHERE date = :date AND difficulty = :difficulty")
    suspend fun get(date: String, difficulty: String): DailyResultEntity?

    @Query("SELECT * FROM daily_result WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DailyResultEntity>>

    @Query("SELECT COUNT(*) FROM daily_result")
    fun totalSolved(): Flow<Int>

    @Upsert
    suspend fun upsert(entity: DailyResultEntity)

    /** Dates on which all four bands were solved, ascending. Used for streak calculation. */
    @Query(
        """
        SELECT date FROM daily_result
        GROUP BY date
        HAVING COUNT(DISTINCT difficulty) = 4
        ORDER BY date
        """
    )
    fun allDatesSolvedAllFour(): Flow<List<String>>
}
