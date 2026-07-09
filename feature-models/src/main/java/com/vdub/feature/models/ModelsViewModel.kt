package com.vdub.feature.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdub.domain.entity.DownloadStatus
import com.vdub.domain.entity.ModelCategory
import com.vdub.domain.entity.ModelDownloadState
import com.vdub.domain.entity.ModelInfo
import com.vdub.domain.entity.ModelRegistry
import com.vdub.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _models = MutableStateFlow<List<ModelInfo>>(ModelRegistry.MODELS)
    val models: StateFlow<List<ModelInfo>> = _models.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    private val _totalStorage = MutableStateFlow(0L)
    val totalStorage: StateFlow<Long> = _totalStorage.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ModelCategory?>(null)
    val selectedCategory: StateFlow<ModelCategory?> = _selectedCategory.asStateFlow()

    init {
        viewModelScope.launch {
            modelRepository.getAvailableModels().collect { models ->
                _models.value = models
            }
        }
        viewModelScope.launch {
            modelRepository.getAllDownloadStates().collect { states ->
                _downloadStates.value = states
            }
        }
        viewModelScope.launch {
            _totalStorage.value = modelRepository.getTotalStorageUsed()
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch { modelRepository.downloadModel(modelId) }
    }

    fun pauseDownload(modelId: String) {
        viewModelScope.launch { modelRepository.pauseDownload(modelId) }
    }

    fun resumeDownload(modelId: String) {
        viewModelScope.launch { modelRepository.resumeDownload(modelId) }
    }

    fun retryDownload(modelId: String) {
        viewModelScope.launch { modelRepository.retryDownload(modelId) }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.deleteModel(modelId)
            _totalStorage.value = modelRepository.getTotalStorageUsed()
        }
    }

    fun selectCategory(category: ModelCategory?) {
        _selectedCategory.value = category
    }

    fun getFilteredModels(): List<ModelInfo> {
        val category = _selectedCategory.value
        return if (category != null) _models.value.filter { it.category == category }
        else _models.value
    }
}
