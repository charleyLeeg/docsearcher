package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.DocumentDao
import com.example.data.dao.ScopeDao
import com.example.data.dao.SearchHistoryDao
import com.example.data.model.IndexedDocument
import com.example.data.model.IndexedScope
import com.example.data.model.SearchHistory

@Database(
    entities = [
        IndexedDocument::class,
        IndexedScope::class,
        SearchHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun scopeDao(): ScopeDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docfetcher_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
