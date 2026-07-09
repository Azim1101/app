package com.vdub.domain.usecase

import com.vdub.domain.entity.AppSettings
import com.vdub.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve application settings.
 */
class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.getSettings()

    suspend fun updateSettings(settings: AppSettings) = settingsRepository.updateSettings(settings)
}
