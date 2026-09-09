package dev.milan.cryptogram

import android.app.Application
import dev.milan.cryptogram.data.daily.DailySyncWorker
import dev.milan.cryptogram.di.AppContainer

class CryptogramApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.billingManager.connect()
        DailySyncWorker.enqueue(this)
    }
}
