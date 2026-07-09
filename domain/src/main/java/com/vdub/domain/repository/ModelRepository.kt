package com.vdub.domain.repository

import com.vdub.domain.entity.ModelDownloadState
import com.vdub.domain.entity.ModelInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ML model management.
 */
interface ModelRepository {

    /**
     * Get all available models.
     */
    fun getAvailableModels(): Flow<List<ModelInfo>>

    /**
     * Get download state for a model.
     */
    fun getDownloadState(modelId: String): Flow<ModelDownloadState>

    /**
     * Get all active download states.
     */
    fun getAllDownloadStates(): Flow<Map<String, ModelDownloadState>>

    /**
     * Start downloading a model.
     */
    suspend fun downloadModel(modelId: String): Result<Unit>

    /**
     * Pause a model download.
     */
    suspend fun pauseDownload(modelId: String): Result<Unit>

    /**
     * Resume a model download.
     */
    suspend fun resumeDownload(modelId: String): Result<Unit>

    /**
     * Retry a failed download.
     */
    suspend fun retryDownload(modelId: String): Result<Unit>

    /**
     * Delete a downloaded model.
     */
    suspend fun deleteModel(modelId: String): Result<Unit>

    /**
     * Import a model from a local file path.
     */
    suspend fun importModel(filePath: String, modelId: String): Result<Unit>

    /**
     * Verify model integrity using SHA256.
     */
    suspend fun verifyModel(modelId: String): Result<Boolean>

    /**
     * Check if required models are downloaded.
     */
    suspend fun areRequiredModelsReady(): Boolean

    /**
     * Get the local file path for a model.
     */
    suspend fun getModelPath(modelId: String): String?

    /**
     * Get total storage used by models.
     */
    suspend fun getTotalStorageUsed(): Long
}
