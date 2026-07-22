package com.example.cookbookai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cookbookai.data.model.AnalysisHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisHistoryDao {

    @Query("SELECT * FROM analysis_history ORDER BY date DESC")
    fun observeAll(): Flow<List<AnalysisHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AnalysisHistoryEntity)

    @Query("DELETE FROM analysis_history WHERE id = :id")
    suspend fun deleteById(id: String)
}
