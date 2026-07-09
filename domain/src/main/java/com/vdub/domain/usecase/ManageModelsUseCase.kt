package com.vdub.domain.usecase

import com.vdub.domain.entity.ModelDownloadState
import com.vdub.domain.entity.ModelInfo
import com.vdub.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing ML models.
 */
class ManageModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    fun getAvailableModels(): Flow<List<ModelInfo>> = modelRepository.getAvailableModels()
    
    fun getDownloadState(modelId: String): Flow<ModelDownloadState> = modelRepository.getDownloadState(modelId)
    
    fun getAllDownloadStates(): Flow<Map<String, ModelDownloadState>> = modelRepository.getAllDownloadStates()

    suspend fun downloadModel(modelId: String): Result<Unit> = modelRepository.downloadModel(modelId)

    suspend fun pauseDownload(modelId: String): Result<Unit> = modelRepository.pauseDownload(modelId)

    suspend fun resumeDownload(modelId: String): Result<Unit> = modelRepository.resumeDownload(modelId)

    suspend fun retryDownload(modelId: String): Result<Unit> = modelRepository.retryDownload(modelId)

    suspend fun deleteModel(modelId: String): Result<Unit> = modelRepository.deleteModel(modelId)

    suspend fun importModel(filePath: String, modelId: String): Result<Unit> = modelRepository.importModel(filePath, modelId)

    suspend fun areRequiredModelsReady(): Boolean = modelRepository.areRequiredModelsReady()

    suspend fun getModelPath(modelId: String): String? = modelRepository.getModelPath(modelId)

    suspend fun getTotalStorageUsed(): Long = modelRepository.getTotalStorageUsed()
}
