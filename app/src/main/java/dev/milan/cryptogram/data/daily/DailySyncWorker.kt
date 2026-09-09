package dev.milan.cryptogram.data.daily

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.milan.cryptogram.CryptogramApp
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Best-effort background refresh of today's daily + the two-day prefetch (design doc section 6). */
class DailySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as CryptogramApp).container.dailyRepository
        return runCatching { repo.getToday(LocalDate.now()) }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private const val UNIQUE_NAME = "daily-sync"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
