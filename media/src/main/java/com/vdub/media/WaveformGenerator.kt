package com.vdub.media

import android.content.Context
import com.vdub.domain.entity.WaveformData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates waveform data from WAV audio files for visualization.
 */
@Singleton
class WaveformGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WaveformGenerator"
        private const val WAV_HEADER_SIZE = 44
        private const val TARGET_SAMPLES = 2000
    }

    /**
     * Generate waveform data from a WAV file.
     */
    fun generateFromWav(wavPath: String, samplesPerPixel: Int = 200): WaveformData {
        val file = File(wavPath)
        if (!file.exists()) return WaveformData(emptyList(), 16000, 0)

        val raf = RandomAccessFile(file, "r")

        try {
            // Parse WAV header
            raf.seek(22)
            val channels = raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8)
            raf.seek(24)
            val sampleRate = raf.readUnsignedByte() or
                    (raf.readUnsignedByte() shl 8) or
                    (raf.readUnsignedByte() shl 16) or
                    (raf.readUnsignedByte() shl 24)
            raf.seek(34)
            val bitsPerSample = raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8)

            val bytesPerSample = bitsPerSample / 8
            val totalFrames = ((file.length() - WAV_HEADER_SIZE) / (bytesPerSample * channels)).toInt()
            val durationMs = (totalFrames.toLong() * 1000) / sampleRate

            // Calculate downsampling
            val totalSamples = totalFrames
            val targetSamples = minOf(TARGET_SAMPLES, totalSamples)
            val samplesPerPoint = totalSamples / targetSamples

            val samples = mutableListOf<Float>()
            val peaks = mutableListOf<Float>()

            raf.seek(WAV_HEADER_SIZE.toLong())

            // Read all samples for downsampling
            var maxAmplitude = 0f
            for (i in 0 until targetSamples) {
                var max = 0f
                var min = 0f
                var sum = 0f

                for (j in 0 until samplesPerPoint) {
                    val frameIndex = i * samplesPerPoint + j
                    if (frameIndex >= totalFrames) break

                    val offset = WAV_HEADER_SIZE + (frameIndex * bytesPerSample * channels).toLong()
                    raf.seek(offset)

                    val sample = when (bitsPerSample) {
                        16 -> {
                            val lo = raf.readUnsignedByte()
                            val hi = raf.readByte()
                            ((hi shl 8) or lo) / 32768f
                        }
                        8 -> (raf.readUnsignedByte() - 128) / 128f
                        24 -> {
                            val b0 = raf.readUnsignedByte()
                            val b1 = raf.readUnsignedByte()
                            val b2 = raf.readByte()
                            ((b2 shl 16) or (b1 shl 8) or b0) / 8388608f
                        }
                        32 -> {
                            val b0 = raf.readUnsignedByte()
                            val b1 = raf.readUnsignedByte()
                            val b2 = raf.readUnsignedByte()
                            val b3 = raf.readByte()
                            ((b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0) / 2147483648f
                        }
                        else -> 0f
                    }

                    if (sample > max) max = sample
                    if (sample < min) min = sample
                    sum += sample
                }

                val avg = if (samplesPerPoint > 0) sum / samplesPerPoint else 0f
                samples.add(avg)
                peaks.add(maxOf(kotlin.math.abs(max), kotlin.math.abs(min)))
                if (kotlin.math.abs(avg) > maxAmplitude) maxAmplitude = kotlin.math.abs(avg)
            }

            // Normalize
            val normalized = if (maxAmplitude > 0f) {
                samples.map { it / maxAmplitude }
            } else samples

            val normalizedPeaks = if (maxAmplitude > 0f) {
                peaks.map { it / maxAmplitude }
            } else peaks

            return WaveformData(
                samples = normalized,
                sampleRate = sampleRate,
                durationMs = durationMs,
                channels = channels,
                peaks = normalizedPeaks,
                downsampled = normalized
            )
        } finally {
            raf.close()
        }
    }

    /**
     * Generate waveform from raw float array.
     */
    fun generateFromFloatArray(
        audioData: FloatArray,
        sampleRate: Int
    ): WaveformData {
        val totalSamples = audioData.size
        val durationMs = (totalSamples.toLong() * 1000) / sampleRate
        val targetSamples = minOf(TARGET_SAMPLES, totalSamples)
        val samplesPerPoint = totalSamples / targetSamples

        val samples = mutableListOf<Float>()
        val peaks = mutableListOf<Float>()
        var maxAmplitude = 0f

        for (i in 0 until targetSamples) {
            var sum = 0f
            var max = 0f

            for (j in 0 until samplesPerPoint) {
                val idx = i * samplesPerPoint + j
                if (idx >= totalSamples) break
                val sample = audioData[idx]
                sum += sample
                if (kotlin.math.abs(sample) > max) max = kotlin.math.abs(sample)
            }

            val avg = if (samplesPerPoint > 0) sum / samplesPerPoint else 0f
            samples.add(avg)
            peaks.add(max)
            if (kotlin.math.abs(avg) > maxAmplitude) maxAmplitude = kotlin.math.abs(avg)
        }

        val normalized = if (maxAmplitude > 0f) samples.map { it / maxAmplitude } else samples
        val normalizedPeaks = if (maxAmplitude > 0f) peaks.map { it / maxAmplitude } else peaks

        return WaveformData(
            samples = normalized,
            sampleRate = sampleRate,
            durationMs = durationMs,
            peaks = normalizedPeaks,
            downsampled = normalized
        )
    }
}
