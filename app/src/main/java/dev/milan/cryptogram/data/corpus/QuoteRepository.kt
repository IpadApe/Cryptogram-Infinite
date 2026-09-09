package dev.milan.cryptogram.data.corpus

import dev.milan.cryptogram.data.db.dao.QuoteDao
import dev.milan.cryptogram.data.db.entities.QuoteEntity
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.engine.LevelIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Read access to the loaded quote corpus, plus a lazily-built, memoised [LevelIndex].
 */
class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val settings: SettingsStore,
) {
    private val indexLock = Mutex()
    @Volatile private var cachedIndex: LevelIndex? = null

    suspend fun byId(id: Int): QuoteEntity? = quoteDao.getById(id)

    suspend fun idsByBand(): Map<Difficulty, List<Int>> =
        Difficulty.entries.associateWith { quoteDao.idsForBand(it.name) }

    suspend fun levelIndex(): LevelIndex {
        cachedIndex?.let { return it }
        return indexLock.withLock {
            cachedIndex ?: buildIndex().also { cachedIndex = it }
        }
    }

    private suspend fun buildIndex(): LevelIndex {
        val byBand = idsByBand()
        val frozen = Difficulty.entries.associateWith { d ->
            settings.frozenMaxId(d).first() ?: (byBand[d]?.maxOrNull() ?: 0)
        }
        return LevelIndex(quotesByBand = byBand, frozenMaxId = frozen)
    }
}
