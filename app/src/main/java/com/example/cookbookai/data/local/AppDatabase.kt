package com.example.cookbookai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cookbookai.data.model.AnalysisHistoryEntity
import com.example.cookbookai.data.model.BarcodeProduct
import com.example.cookbookai.data.model.Recipe

@Database(
    entities = [
        Recipe::class,
        BarcodeProduct::class,
        AnalysisHistoryEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    abstract fun barcodeProductDao(): BarcodeProductDao

    abstract fun analysisHistoryDao(): AnalysisHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cookbook_db"
                )
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_9_10 =
            object : Migration(9, 10) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS analysis_history (
                            id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            calories INTEGER NOT NULL,
                            proteins REAL NOT NULL,
                            fats REAL NOT NULL,
                            carbs REAL NOT NULL,
                            imageUri TEXT,
                            weight INTEGER NOT NULL,
                            confidence REAL NOT NULL,
                            aiSummary TEXT NOT NULL,
                            topPredictions TEXT NOT NULL,
                            date INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }
    }
}
