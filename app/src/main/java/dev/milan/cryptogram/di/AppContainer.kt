package dev.milan.cryptogram.di

import android.content.Context
import dev.milan.cryptogram.BuildConfig
import dev.milan.cryptogram.data.corpus.CorpusLoader
import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.data.daily.DailyRemoteSource
import dev.milan.cryptogram.data.daily.DailyRepository
import dev.milan.cryptogram.data.db.AppDatabase
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.data.progress.ProgressRepository
import okhttp3.OkHttpClient

/**
 * Manual dependency container (no Hilt/Koin). Created once in [dev.milan.cryptogram.CryptogramApp].
 *
 * Wired progressively by build brief. Still to come:
 *  - adManager        (Brief 5)
 *  - billingManager   (Brief 6)
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    val corpusLoader: CorpusLoader by lazy {
        CorpusLoader(appContext, database.quoteDao(), settingsStore)
    }

    val quoteRepository: QuoteRepository by lazy {
        QuoteRepository(database.quoteDao(), settingsStore)
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(database.progressDao())
    }

    val dailyRepository: DailyRepository by lazy {
        DailyRepository(
            remote = DailyRemoteSource(BuildConfig.CONTENT_BASE_URL, okHttpClient),
            cacheDao = database.dailyCacheDao(),
            quoteDao = database.quoteDao(),
            quotes = quoteRepository,
            resultDao = database.dailyResultDao(),
        )
    }
}
