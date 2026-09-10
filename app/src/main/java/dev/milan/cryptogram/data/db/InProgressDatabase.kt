package dev.milan.cryptogram.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.milan.cryptogram.data.db.dao.InProgressDao
import dev.milan.cryptogram.data.db.entities.InProgressEntity

/**
 * Autosave slots only, in their own file so cloud backup can exclude them
 * (design doc section 9). An in-progress puzzle should never restore onto another
 * device / reinstall.
 */
@Database(entities = [InProgressEntity::class], version = 1, exportSchema = true)
abstract class InProgressDatabase : RoomDatabase() {
    abstract fun inProgressDao(): InProgressDao

    companion object {
        fun build(context: Context): InProgressDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                InProgressDatabase::class.java,
                "in_progress.db",
            ).build()
    }
}
