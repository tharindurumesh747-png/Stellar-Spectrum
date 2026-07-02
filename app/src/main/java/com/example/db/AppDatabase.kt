package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// FIX: bumped version from 1 to 2 because UserProgress gained new columns
// (unlockedTrailsStr, unlockedExplosionsStr, selectedTrail, selectedExplosion).
// fallbackToDestructiveMigration() means Room just rebuilds the table cleanly
// instead of crashing — saves data is lost but the app runs correctly.
@Database(entities = [UserProgress::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stellar_spectrum_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
