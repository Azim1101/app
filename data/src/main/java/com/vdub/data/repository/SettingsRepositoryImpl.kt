package com.vdub.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vdub.domain.entity.AppSettings
import com.vdub.domain.entity.ComputeBackend
import com.vdub.domain.entity.ExportFormatData
import com.vdub.domain.entity.ThemeMode
import com.vdub.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vdub_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("language")
        val SEG_MODEL = stringPreferencesKey("seg_model")
        val EMB_MODEL = stringPreferencesKey("emb_model")
        val COMPUTE_BACKEND = stringPreferencesKey("compute_backend")
        val SAMPLE_RATE = intPreferencesKey("sample_rate")
        val CHUNK_SIZE_MS = intPreferencesKey("chunk_size_ms")
        val CLUSTERING_THRESHOLD = floatPreferencesKey("clustering_threshold")
        val MAX_SPEAKERS = intPreferencesKey("max_speakers")
        val AUTO_SPEAKER_COUNT = booleanPreferencesKey("auto_speaker_count")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val LOW_RAM_MODE = booleanPreferencesKey("low_ram_mode")
        val THREAD_COUNT = intPreferencesKey("thread_count")
        val NNAPI_ENABLED = booleanPreferencesKey("nnapi_enabled")
        val GPU_ENABLED = booleanPreferencesKey("gpu_enabled")
        val BUFFER_SIZE_MS = intPreferencesKey("buffer_size_ms")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
    }

    override fun getSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { prefs ->
            AppSettings(
                themeMode = try { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM },
                dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
                language = prefs[Keys.LANGUAGE] ?: "en",
                preferredSegmentationModel = prefs[Keys.SEG_MODEL] ?: "pyannote-segmentation-3.0",
                preferredEmbeddingModel = prefs[Keys.EMB_MODEL] ?: "speechbrain-ecapa",
                computeBackend = try { ComputeBackend.valueOf(prefs[Keys.COMPUTE_BACKEND] ?: "CPU") } catch (_: Exception) { ComputeBackend.CPU },
                sampleRate = prefs[Keys.SAMPLE_RATE] ?: 16000,
                chunkSizeMs = prefs[Keys.CHUNK_SIZE_MS] ?: 5000,
                clusteringThreshold = prefs[Keys.CLUSTERING_THRESHOLD] ?: 0.7f,
                maxSpeakers = prefs[Keys.MAX_SPEAKERS] ?: 20,
                autoSpeakerCount = prefs[Keys.AUTO_SPEAKER_COUNT] ?: true,
                defaultExportFormat = try { ExportFormatData.valueOf(prefs[Keys.DEFAULT_EXPORT_FORMAT] ?: "TXT") } catch (_: Exception) { ExportFormatData.TXT },
                lowRamMode = prefs[Keys.LOW_RAM_MODE] ?: false,
                threadCount = prefs[Keys.THREAD_COUNT] ?: 4,
                nnapiEnabled = prefs[Keys.NNAPI_ENABLED] ?: true,
                gpuDelegateEnabled = prefs[Keys.GPU_ENABLED] ?: false,
                bufferSizeMs = prefs[Keys.BUFFER_SIZE_MS] ?: 30000,
                notificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true
            )
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColorEnabled
            prefs[Keys.LANGUAGE] = settings.language
            prefs[Keys.SEG_MODEL] = settings.preferredSegmentationModel
            prefs[Keys.EMB_MODEL] = settings.preferredEmbeddingModel
            prefs[Keys.COMPUTE_BACKEND] = settings.computeBackend.name
            prefs[Keys.SAMPLE_RATE] = settings.sampleRate
            prefs[Keys.CHUNK_SIZE_MS] = settings.chunkSizeMs
            prefs[Keys.CLUSTERING_THRESHOLD] = settings.clusteringThreshold
            prefs[Keys.MAX_SPEAKERS] = settings.maxSpeakers
            prefs[Keys.AUTO_SPEAKER_COUNT] = settings.autoSpeakerCount
            prefs[Keys.DEFAULT_EXPORT_FORMAT] = settings.defaultExportFormat.name
            prefs[Keys.LOW_RAM_MODE] = settings.lowRamMode
            prefs[Keys.THREAD_COUNT] = settings.threadCount
            prefs[Keys.NNAPI_ENABLED] = settings.nnapiEnabled
            prefs[Keys.GPU_ENABLED] = settings.gpuDelegateEnabled
            prefs[Keys.BUFFER_SIZE_MS] = settings.bufferSizeMs
            prefs[Keys.NOTIFICATION_ENABLED] = settings.notificationEnabled
        }
    }

    override suspend fun <T> updateSetting(key: String, value: T) {
        context.dataStore.edit { prefs ->
            when (value) {
                is String -> prefs[stringPreferencesKey(key)] = value
                is Int -> prefs[intPreferencesKey(key)] = value
                is Long -> prefs[longPreferencesKey(key)] = value
                is Float -> prefs[floatPreferencesKey(key)] = value
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
            }
        }
    }
}
