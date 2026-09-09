package dev.milan.cryptogram.data.corpus

import android.content.Context
import dev.milan.cryptogram.data.db.dao.QuoteDao
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.engine.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled `assets/corpus.json` into Room on first run, or when the asset's
 * version is newer than what was last loaded (design doc section 5.3).
 *
 * `frozenMaxId_<BAND>` is written only if unset, so a larger corpus in a later app
 * version never shifts existing level numbers.
 */
class CorpusLoader(
    private val context: Context,
    private val quoteDao: QuoteDao,
    private val settings: SettingsStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load() = withContext(Dispatchers.IO) {
        val loadedVersion = settings.corpusLoadedVersion.first()
        val file = readAsset()

        if (file.version <= loadedVersion && quoteDao.count() > 0) return@withContext

        quoteDao.insertAll(file.quotes.map { it.toEntity() })

        for (difficulty in Difficulty.entries) {
            val maxId = file.quotes
                .filter { it.band == difficulty.name }
                .maxOfOrNull { it.id }
            if (maxId != null) settings.setFrozenMaxIdIfUnset(difficulty, maxId)
        }

        settings.setCorpusLoadedVersion(file.version)
    }

    private fun readAsset(): CorpusFile =
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            json.decodeFromString(CorpusFile.serializer(), reader.readText())
        }

    private companion object {
        const val ASSET_NAME = "corpus.json"
    }
}
