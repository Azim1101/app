package com.vdub.data.repository

import android.content.Context
import com.vdub.domain.entity.DownloadStatus
import com.vdub.domain.entity.ModelDownloadState
import com.vdub.domain.entity.ModelInfo
import com.vdub.domain.entity.ModelRegistry
import com.vdub.domain.repository.ModelRepository
import com.vdub.ml.ModelDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloader: ModelDownloader
) : ModelRepository {

    override fun getAvailableModels(): Flow<List<ModelInfo>> {
        val downloadStates = modelDownloader.getAllDownloadStateFlows()
        return combine(downloadStates.values.toList()) { states ->
            ModelRegistry.MODELS.map { model ->
                val state = states.find { it.modelId == model.id }
                model.copy(isDownloaded = state?.status == DownloadStatus.COMPLETED)
            }
        }
    }

    override fun getDownloadState(modelId: String): Flow<ModelDownloadState> {
        return modelDownloader.getDownloadStateFlow(modelId)
    }

    override fun getAllDownloadStates(): Flow<Map<String, ModelDownloadState>> {
        val states = modelDownloader.getAllDownloadStateFlows()
        return combine(states.values.toList()) { stateArray ->
            stateArray.associateBy { it.modelId }
        }
    }

    override suspend fun downloadModel(modelId: String): Result<Unit> = runCatching {
        val model = ModelRegistry.getById(modelId) ?: throw IllegalArgumentException("Unknown model: $modelId")
        modelDownloader.downloadModel(model)
    }

    override suspend fun pauseDownload(modelId: String): Result<Unit> = runCatching {
        modelDownloader.pauseDownload(modelId)
    }

    override suspend fun resumeDownload(modelId: String): Result<Unit> = runCatching {
        val model = ModelRegistry.getById(modelId) ?: throw IllegalArgumentException("Unknown model: $modelId")
        modelDownloader.resumeDownload(model)
    }

    override suspend fun retryDownload(modelId: String): Result<Unit> = runCatching {
        val model = ModelRegistry.getById(modelId) ?: throw IllegalArgumentException("Unknown model: $modelId")
        modelDownloader.retryDownload(model)
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> = runCatching {
        val model = ModelRegistry.getById(modelId) ?: throw IllegalArgumentException("Unknown model: $modelId")
        modelDownloader.deleteModel(model)
    }

    override suspend fun importModel(filePath: String, modelId: String): Result<Unit> = runCatching {
        val model = ModelRegistry.getById(modelId) ?: throw IllegalArgumentException("Unknown model: $modelId")
        require(modelDownloader.importModel(filePath, model)) { "Failed to import model" }
    }

    override suspend fun verifyModel(modelId: String): Result<Boolean> = runCatching {
        val model = ModelRegistry.getById(modelId) ?: throw IllegalArgumentException("Unknown model: $modelId")
        modelDownloader.verifyModel(model)
    }

    override suspend fun areRequiredModelsReady(): Boolean {
        return ModelRegistry.getRequiredModels().all { model ->
            modelDownloader.isModelDownloaded(model)
        }
    }

    override suspend fun getModelPath(modelId: String): String? {
        val model = ModelRegistry.getById(modelId) ?: return null
        return if (modelDownloader.isModelDownloaded(model)) {
            modelDownloader.getModelLocalPath(model)
        } else null
    }

    override suspend fun getTotalStorageUsed(): Long {
        return modelDownloader.getTotalStorageUsed()
    }
}
