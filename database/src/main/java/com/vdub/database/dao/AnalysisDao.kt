package com.vdub.database.dao

import androidx.room.*
import com.vdub.database.entity.AnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM analyses ORDER BY createdAt DESC")
    fun getAllAnalyses(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE id = :id")
    suspend fun getAnalysisById(id: Long): AnalysisEntity?

    @Query("SELECT * FROM analyses WHERE fileName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchAnalyses(query: String): Flow<List<AnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity): Long

    @Update
    suspend fun updateAnalysis(analysis: AnalysisEntity)

    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun deleteAnalysis(id: Long)

    @Query("DELETE FROM analyses")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM analyses")
    suspend fun getCount(): Int
}
