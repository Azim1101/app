package com.vdub.domain.repository

import com.vdub.domain.entity.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for application settings.
 */
interface SettingsRepository {

    /**
     * Get current settings as a Flow.
     */
    fun getSettings(): Flow<AppSettings>

    /**
     * Update settings.
     */
    suspend fun updateSettings(settings: AppSettings)

    /**
     * Update a single setting.
     */
    suspend fun <T> updateSetting(key: String, value: T)
}
