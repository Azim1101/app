package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Live processing progress state.
 */
@Serializable
data class ProcessingProgress(
    val status: AnalysisStatus = AnalysisStatus.PENDING,
    val progress: Float = 0f,
    val currentStep: String = "",
    val elapsedTimeMs: Long = 0,
    val estimatedRemainingMs: Long = 0,
    val segmentsDetected: Int = 0,
    val currentSpeaker: String = "",
    val latestSegment: Segment? = null,
    val logMessages: List<String> = emptyList(),
    val isCancellable: Boolean = true,
    val isPausable: Boolean = true,
    val isPaused: Boolean = false
)
