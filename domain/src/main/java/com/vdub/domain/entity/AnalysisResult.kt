package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Complete diarization analysis result.
 */
@Serializable
data class AnalysisResult(
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = 0,
    val mediaType: MediaType = MediaType.AUDIO,
    val durationMs: Long = 0,
    val speechDurationMs: Long = 0,
    val silenceDurationMs: Long = 0,
    val speakerCount: Int = 0,
    val segmentCount: Int = 0,
    val averageConfidence: Float = 0f,
    val embeddingCount: Int = 0,
    val longestSpeakerLabel: String = "",
    val longestSpeakerDurationMs: Long = 0,
    val averageSegmentLengthMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0,
    val status: AnalysisStatus = AnalysisStatus.PENDING,
    val segments: List<Segment> = emptyList(),
    val speakers: List<Speaker> = emptyList(),
    val sampleRate: Int = 16000,
    val channels: Int = 1,
    val bitrate: Long = 0,
    val modelUsed: String = "",
    val processingTimeMs: Long = 0
)

enum class AnalysisStatus {
    PENDING,
    EXTRACTING_AUDIO,
    RESAMPLING,
    SEGMENTING,
    GENERATING_EMBEDDINGS,
    CLUSTERING,
    COMPLETED,
    FAILED,
    CANCELLED
}
