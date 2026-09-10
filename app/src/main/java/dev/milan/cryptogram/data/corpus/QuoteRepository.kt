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
 *
 * [ensureCorpusLoaded] is the single gate every consumer must pass through before
 * reading the corpus, so a background caller (e.g. DailySyncWorker) can never observe
 * an empty database and cache a zero-size [LevelIndex].
 */
class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val settings: SettingsStore,
    private val corpusLoader: CorpusLoader,
) {
    private val corpusMutex = Mutex()
    @Volatile private var corpusReady = false

    private val indexLock = Mutex()
    @Volatile private var cachedIndex: LevelIndex? = null

    suspend fun ensureCorpusLoaded() {
        if (corpusReady) return
        corpusMutex.withLock {
            if (corpusReady) return
            corpusLoader.load()
            corpusReady = true
        }
    }

    suspend fun byId(id: Int): QuoteEntity? = quoteDao.getById(id)

    suspend fun idsByBand(): Map<Difficulty, List<Int>> =
        Difficulty.entries.associateWith { quoteDao.idsForBand(it.name) }

    suspend fun levelIndex(): LevelIndex {
        cachedIndex?.let { return it }
        ensureCorpusLoaded()
        return indexLock.withLock {
            cachedIndex ?: buildIndex().also { built ->
                // Only memoise once there is real content, so an early empty read can't stick.
                if (built.hasContent()) cachedIndex = built
            }
        }
    }

    private suspend fun buildIndex(): LevelIndex {
        val byBand = idsByBand()
        val frozen = Difficulty.entries.associateWith { d ->
            settings.frozenMaxId(d).first() ?: (byBand[d]?.maxOrNull() ?: 0)
        }
        return LevelIndex(quotesByBand = byBand, frozenMaxId = frozen)
    }

    private fun LevelIndex.hasContent(): Boolean =
        Difficulty.entries.any { bandSize(it) > 0 }
}
