package com.vdub.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdub.domain.entity.MediaFile
import com.vdub.domain.entity.MediaType
import com.vdub.domain.repository.MediaRepository
import com.vdub.domain.repository.ModelRepository
import com.vdub.core.util.copyToLocalFile
import com.vdub.core.util.fileExtension
import com.vdub.core.util.fileName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<MediaFile?>(null)
    val selectedFile: StateFlow<MediaFile?> = _selectedFile.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val recentFiles: StateFlow<List<MediaFile>> = _recentFiles.asStateFlow()

    private val _modelsReady = MutableStateFlow(false)
    val modelsReady: StateFlow<Boolean> = _modelsReady.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.getRecentFiles().collect { files ->
                _recentFiles.value = files
            }
        }
        viewModelScope.launch {
            _modelsReady.value = modelRepository.areRequiredModelsReady()
        }
    }

    fun handleFileUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val tempFile = File(context.cacheDir, uri.lastPathSegment?.fileName() ?: "media_file")
                uri.copyToLocalFile(context, tempFile)

                val result = mediaRepository.getMediaInfo(tempFile.absolutePath)
                result.onSuccess { mediaFile ->
                    _selectedFile.value = mediaFile
                }.onFailure { e ->
                    _error.value = "Failed to read file: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to open file: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearSelectedFile() { _selectedFile.value = null }
}
