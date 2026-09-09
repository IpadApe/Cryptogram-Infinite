package dev.milan.cryptogram.di

import android.content.Context
import dev.milan.cryptogram.data.corpus.CorpusLoader
import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.data.db.AppDatabase
import dev.milan.cryptogram.data.prefs.SettingsStore

/**
 * Manual dependency container (no Hilt/Koin). Created once in [dev.milan.cryptogram.CryptogramApp].
 *
 * Wired progressively by build brief. Still to come:
 *  - progressRepository  (Brief 2)
 *  - dailyRepository     (Brief 4)
 *  - adManager           (Brief 5)
 *  - billingManager      (Brief 6)
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    val corpusLoader: CorpusLoader by lazy {
        CorpusLoader(appContext, database.quoteDao(), settingsStore)
    }

    val quoteRepository: QuoteRepository by lazy {
        QuoteRepository(database.quoteDao(), settingsStore)
    }
}
