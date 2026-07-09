package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Represents a single speaker segment with timing information.
 */
@Serializable
data class Segment(
    val id: Long = 0,
    val analysisId: Long = 0,
    val speakerLabel: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 0f,
    val embedding: List<Float> = emptyList(),
    val clusterId: Int = -1
) {
    val durationMs: Long get() = endTimeMs - startTimeMs
    val startTimeSec: Double get() = startTimeMs / 1000.0
    val endTimeSec: Double get() = endTimeMs / 1000.0
    val durationSec: Double get() = durationMs / 1000.0

    fun formatTimestamp(): String {
        val startMin = startTimeMs / 60000
        val startSec = (startTimeMs % 60000) / 1000
        val endMin = endTimeMs / 60000
        val endSec = (endTimeMs % 60000) / 1000
        return String.format("%02d:%02d-%02d:%02d", startMin, startSec, endMin, endSec)
    }
}
