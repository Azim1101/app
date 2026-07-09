package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Application settings persisted to DataStore.
 */
@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val language: String = "en",
    val preferredSegmentationModel: String = "pyannote-segmentation-3.0",
    val preferredEmbeddingModel: String = "speechbrain-ecapa",
    val computeBackend: ComputeBackend = ComputeBackend.CPU,
    val sampleRate: Int = 16000,
    val chunkSizeMs: Int = 5000,
    val clusteringThreshold: Float = 0.7f,
    val maxSpeakers: Int = 20,
    val autoSpeakerCount: Boolean = true,
    val defaultExportFormat: ExportFormatData = ExportFormatData.TXT,
    val lowRamMode: Boolean = false,
    val threadCount: Int = 4,
    val nnapiEnabled: Boolean = true,
    val gpuDelegateEnabled: Boolean = false,
    val bufferSizeMs: Int = 30000,
    val notificationEnabled: Boolean = true
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ComputeBackend { CPU, NNAPI, GPU }

enum class ExportFormatData { TXT, CSV, JSON, SRT, VTT, RTTM }
