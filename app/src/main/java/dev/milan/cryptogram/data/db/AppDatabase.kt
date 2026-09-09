package dev.milan.cryptogram.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.milan.cryptogram.data.db.dao.DailyCacheDao
import dev.milan.cryptogram.data.db.dao.DailyResultDao
import dev.milan.cryptogram.data.db.dao.InProgressDao
import dev.milan.cryptogram.data.db.dao.ProgressDao
import dev.milan.cryptogram.data.db.dao.QuoteDao
import dev.milan.cryptogram.data.db.entities.DailyCacheEntity
import dev.milan.cryptogram.data.db.entities.DailyResultEntity
import dev.milan.cryptogram.data.db.entities.InProgressEntity
import dev.milan.cryptogram.data.db.entities.ProgressEntity
import dev.milan.cryptogram.data.db.entities.QuoteEntity

@Database(
    entities = [
        QuoteEntity::class,
        ProgressEntity::class,
        InProgressEntity::class,
        DailyCacheEntity::class,
        DailyResultEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun progressDao(): ProgressDao
    abstract fun inProgressDao(): InProgressDao
    abstract fun dailyCacheDao(): DailyCacheDao
    abstract fun dailyResultDao(): DailyResultDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cryptogram.db",
            ).build()
    }
}
