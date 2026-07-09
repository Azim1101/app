package com.vdub.media

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads audio files into float arrays for ML processing.
 * Supports WAV (PCM) format natively.
 */
@Singleton
class AudioReader @Inject constructor() {

    companion object {
        private const val TAG = "AudioReader"
        private const val WAV_HEADER_SIZE = 44
    }

    /**
     * Read a WAV file into a float array.
     * Returns mono float samples normalized to [-1.0, 1.0].
     */
    fun readWav(wavPath: String): AudioData {
        val file = File(wavPath)
        if (!file.exists()) throw FileNotFoundException("WAV file not found: $wavPath")

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
            val dataSize = file.length() - WAV_HEADER_SIZE
            val totalFrames = (dataSize / (bytesPerSample * channels)).toInt()

            // Read audio data
            raf.seek(WAV_HEADER_SIZE.toLong())
            val samples = FloatArray(totalFrames)

            for (i in 0 until totalFrames) {
                var sampleValue = 0f
                for (ch in 0 until channels) {
                    val raw = when (bitsPerSample) {
                        16 -> {
                            val lo = raf.readUnsignedByte()
                            val hi = raf.readByte()
                            ((hi.toInt() shl 8) or lo) / 32768f
                        }
                        8 -> (raf.readUnsignedByte() - 128) / 128f
                        24 -> {
                            val b0 = raf.readUnsignedByte()
                            val b1 = raf.readUnsignedByte()
                            val b2 = raf.readByte()
                            ((b2.toInt() shl 16) or (b1.toInt() shl 8) or b0) / 8388608f
                        }
                        32 -> {
                            val b0 = raf.readUnsignedByte()
                            val b1 = raf.readUnsignedByte()
                            val b2 = raf.readUnsignedByte()
                            val b3 = raf.readByte()
                            ((b3.toInt() shl 24) or (b2.toInt() shl 16) or (b1.toInt() shl 8) or b0) / 2147483648f
                        }
                        else -> 0f
                    }
                    sampleValue += raw
                }
                samples[i] = sampleValue / channels // Average all channels for mono
            }

            Log.i(TAG, "Read WAV: ${samples.size} samples, ${sampleRate}Hz, ${channels}ch, ${bitsPerSample}bit")

            return AudioData(
                samples = samples,
                sampleRate = sampleRate,
                channels = 1, // Mixed to mono
                durationMs = (totalFrames.toLong() * 1000) / sampleRate
            )
        } finally {
            raf.close()
        }
    }

    /**
     * Read a portion of a WAV file (for large file support).
     */
    fun readWavChunk(wavPath: String, startFrame: Int, numFrames: Int): AudioData {
        val file = File(wavPath)
        if (!file.exists()) throw FileNotFoundException("WAV file not found: $wavPath")

        val raf = RandomAccessFile(file, "r")
        try {
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

            val offset = WAV_HEADER_SIZE + (startFrame.toLong() * bytesPerSample * channels)
            raf.seek(offset)

            val samples = FloatArray(numFrames)
            for (i in 0 until numFrames) {
                var sampleValue = 0f
                for (ch in 0 until channels) {
                    val raw = when (bitsPerSample) {
                        16 -> {
                            val lo = raf.readUnsignedByte()
                            val hi = raf.readByte()
                            ((hi.toInt() shl 8) or lo) / 32768f
                        }
                        8 -> (raf.readUnsignedByte() - 128) / 128f
                        else -> 0f
                    }
                    sampleValue += raw
                }
                samples[i] = sampleValue / channels
            }

            return AudioData(
                samples = samples,
                sampleRate = sampleRate,
                channels = 1,
                durationMs = (numFrames.toLong() * 1000) / sampleRate
            )
        } finally {
            raf.close()
        }
    }
}

data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long
) {
    val durationSec: Double get() = durationMs / 1000.0
    val totalSamples: Int get() = samples.size
}

class FileNotFoundException(message: String) : Exception(message)
