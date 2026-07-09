package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Represents an identified speaker with aggregated statistics.
 */
@Serializable
data class Speaker(
    val id: Long = 0,
    val analysisId: Long = 0,
    val label: String,
    val clusterId: Int,
    val totalDurationMs: Long = 0,
    val segmentCount: Int = 0,
    val averageConfidence: Float = 0f,
    val averageEmbedding: List<Float> = emptyList(),
    val color: Long = 0xFF2196F3
) {
    val totalDurationSec: Double get() = totalDurationMs / 1000.0
    val averageSegmentLengthMs: Long get() = if (segmentCount > 0) totalDurationMs / segmentCount else 0L
}
