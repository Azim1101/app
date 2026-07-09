package com.vdub.feature.processing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdub.domain.entity.*
import com.vdub.domain.repository.DiarizationRepository
import com.vdub.domain.usecase.StartDiarizationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val diarizationRepository: DiarizationRepository,
    private val startDiarizationUseCase: StartDiarizationUseCase
) : ViewModel() {

    private val filePath: String = savedStateHandle["filePath"] ?: ""
    private val mediaTypeStr: String = savedStateHandle["mediaType"] ?: "AUDIO"

    private val _progress = MutableStateFlow(ProcessingProgress())
    val progress: StateFlow<ProcessingProgress> = _progress.asStateFlow()

    private val _result = MutableStateFlow<AnalysisResult?>(null)
    val result: StateFlow<AnalysisResult?> = _result.asStateFlow()

    private val _segments = MutableStateFlow<List<Segment>>(emptyList())
    val segments: StateFlow<List<Segment>> = _segments.asStateFlow()

    private val _speakers = MutableStateFlow<List<Speaker>>(emptyList())
    val speakers: StateFlow<List<Speaker>> = _speakers.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    fun startProcessing() {
        if (_isRunning.value) return
        val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (_: Exception) { MediaType.AUDIO }

        viewModelScope.launch {
            _isRunning.value = true
            val startTime = System.currentTimeMillis()

            startDiarizationUseCase(filePath, mediaType).collect { progress ->
                _progress.value = progress
                _elapsedTime.value = System.currentTimeMillis() - startTime

                if (progress.latestSegment != null) {
                    _segments.value = _segments.value + progress.latestSegment!!
                }

                if (progress.status == AnalysisStatus.COMPLETED) {
                    diarizationRepository.getCurrentResult().collect { result ->
                        result?.let {
                            _result.value = it
                            _segments.value = it.segments
                            _speakers.value = it.speakers
                        }
                    }
                }
            }
            _isRunning.value = false
        }
    }

    fun cancelProcessing() {
        viewModelScope.launch { diarizationRepository.cancelAnalysis() }
    }

    fun pauseProcessing() {
        viewModelScope.launch { diarizationRepository.pauseAnalysis() }
    }

    fun resumeProcessing() {
        viewModelScope.launch { diarizationRepository.resumeAnalysis() }
    }

    fun renameSpeaker(oldLabel: String, newLabel: String) {
        viewModelScope.launch {
            diarizationRepository.renameSpeaker(oldLabel, newLabel)
            _result.value?.let { result ->
                _segments.value = result.segments
                _speakers.value = result.speakers
            }
        }
    }

    fun mergeSpeakers(primaryLabel: String, secondaryLabel: String) {
        viewModelScope.launch {
            diarizationRepository.mergeSpeakers(primaryLabel, secondaryLabel)
            _result.value?.let { result ->
                _segments.value = result.segments
                _speakers.value = result.speakers
            }
        }
    }

    fun recluster(threshold: Float, maxSpeakers: Int = 20) {
        viewModelScope.launch {
            diarizationRepository.recluster(threshold, maxSpeakers)
            _result.value?.let { result ->
                _segments.value = result.segments
                _speakers.value = result.speakers
            }
        }
    }

    fun exportResult(format: ExportFormat) {
        viewModelScope.launch {
            _result.value?.let { result ->
                diarizationRepository.exportResult(result, format)
            }
        }
    }
}
