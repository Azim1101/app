package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Waveform visualization data.
 */
@Serializable
data class WaveformData(
    val samples: List<Float>,
    val sampleRate: Int,
    val durationMs: Long,
    val channels: Int = 1,
    val peaks: List<Float> = emptyList(),
    val downsampled: List<Float> = emptyList()
) {
    val durationSec: Double get() = durationMs / 1000.0
}
