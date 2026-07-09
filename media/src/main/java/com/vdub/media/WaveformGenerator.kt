package com.vdub.media

import android.content.Context
import com.vdub.domain.entity.WaveformData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
            val channels = readLittleEndianUShort(raf)
            raf.seek(24)
            val sampleRate = readLittleEndianUInt(raf).toInt()
            raf.seek(34)
            val bitsPerSample = readLittleEndianUShort(raf)

            // Find data chunk
            raf.seek(12)
            var dataOffset = 0L
            var dataSize = 0L
            var foundData = false
            for (attempt in 0 until 20) {
                val chunkId = ByteArray(4)
                raf.read(chunkId)
                val chunkSize = readLittleEndianUInt(raf)
                if (String(chunkId) == "data") {
                    dataOffset = raf.filePointer
                    dataSize = chunkSize
                    foundData = true
                    break
                }
                raf.skipBytes(chunkSize.toInt())
            }
            if (!foundData) {
                dataOffset = WAV_HEADER_SIZE.toLong()
                dataSize = file.length() - WAV_HEADER_SIZE
            }

            val bytesPerSample = bitsPerSample / 8
            val totalFrames = (dataSize / (bytesPerSample * channels)).toInt()
            val durationMs = if (sampleRate > 0) (totalFrames.toLong() * 1000) / sampleRate else 0L

            // Calculate downsampling
            val targetSamples = minOf(TARGET_SAMPLES, totalFrames)
            val samplesPerPoint = if (totalFrames > 0) totalFrames / targetSamples else 1

            val samples = mutableListOf<Float>()
            val peaks = mutableListOf<Float>()
            var maxAmplitude = 0f

            for (i in 0 until targetSamples) {
                var sum = 0f
                var peak = 0f

                for (j in 0 until samplesPerPoint) {
                    val frameIndex = i * samplesPerPoint + j
                    if (frameIndex >= totalFrames) break

                    val offset = dataOffset + (frameIndex * bytesPerSample * channels).toLong()
                    raf.seek(offset)

                    val sample = readSample(raf, bitsPerSample)
                    sum += sample
                    val absSample = kotlin.math.abs(sample)
                    if (absSample > peak) peak = absSample
                }

                val avg = if (samplesPerPoint > 0) sum / samplesPerPoint else 0f
                samples.add(avg)
                peaks.add(peak)
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
     * Read a single audio sample from WAV file.
     */
    private fun readSample(raf: RandomAccessFile, bitsPerSample: Int): Float {
        return when (bitsPerSample) {
            16 -> {
                val b0 = raf.readUnsignedByte()
                val b1 = raf.readByte().toInt()
                val raw = (b1 shl 8) or b0
                raw / 32768f
            }
            8 -> {
                val raw = raf.readUnsignedByte() - 128
                raw / 128f
            }
            24 -> {
                val b0 = raf.readUnsignedByte()
                val b1 = raf.readUnsignedByte()
                val b2 = raf.readByte().toInt()
                val raw = (b2 shl 16) or (b1 shl 8) or b0
                raw / 8388608f
            }
            32 -> {
                val bytes = ByteArray(4)
                raf.read(bytes)
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                buf.float
            }
            else -> 0f
        }
    }

    /**
     * Read unsigned short (2 bytes, little-endian).
     */
    private fun readLittleEndianUShort(raf: RandomAccessFile): Int {
        val b0 = raf.readUnsignedByte()
        val b1 = raf.readUnsignedByte()
        return (b1 shl 8) or b0
    }

    /**
     * Read unsigned int (4 bytes, little-endian).
     */
    private fun readLittleEndianUInt(raf: RandomAccessFile): Long {
        val b0 = raf.readUnsignedByte().toLong()
        val b1 = raf.readUnsignedByte().toLong()
        val b2 = raf.readUnsignedByte().toLong()
        val b3 = raf.readUnsignedByte().toLong()
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    /**
     * Generate waveform from raw float array.
     */
    fun generateFromFloatArray(
        audioData: FloatArray,
        sampleRate: Int
    ): WaveformData {
        val totalSamples = audioData.size
        val durationMs = if (sampleRate > 0) (totalSamples.toLong() * 1000) / sampleRate else 0L
        val targetSamples = minOf(TARGET_SAMPLES, totalSamples)
        val samplesPerPoint = if (totalSamples > 0) totalSamples / targetSamples else 1

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
