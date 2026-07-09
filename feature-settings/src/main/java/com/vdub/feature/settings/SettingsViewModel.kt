package com.vdub.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdub.domain.entity.AppSettings
import com.vdub.domain.entity.ComputeBackend
import com.vdub.domain.entity.ThemeMode
import com.vdub.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(themeMode = mode))
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(dynamicColorEnabled = enabled))
        }
    }

    fun updateComputeBackend(backend: ComputeBackend) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(computeBackend = backend))
        }
    }

    fun updateSampleRate(rate: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(sampleRate = rate))
        }
    }

    fun updateThreadCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(threadCount = count))
        }
    }

    fun updateClusteringThreshold(threshold: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(clusteringThreshold = threshold))
        }
    }

    fun updateLowRamMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(lowRamMode = enabled))
        }
    }

    fun updateMaxSpeakers(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(maxSpeakers = count))
        }
    }

    fun updateAutoSpeakerCount(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(autoSpeakerCount = enabled))
        }
    }
}
