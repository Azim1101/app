package com.vdub.domain.repository

import com.vdub.domain.entity.AnalysisResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for analysis history management.
 */
interface HistoryRepository {

    /**
     * Get all past analysis results.
     */
    fun getAllResults(): Flow<List<AnalysisResult>>

    /**
     * Get a specific analysis result by ID.
     */
    suspend fun getResultById(id: Long): Result<AnalysisResult>

    /**
     * Save an analysis result.
     */
    suspend fun saveResult(result: AnalysisResult): Result<Long>

    /**
     * Delete an analysis result.
     */
    suspend fun deleteResult(id: Long): Result<Unit>

    /**
     * Search analysis results by file name.
     */
    fun searchResults(query: String): Flow<List<AnalysisResult>>

    /**
     * Delete all history.
     */
    suspend fun clearHistory(): Result<Unit>
}
