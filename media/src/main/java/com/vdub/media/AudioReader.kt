package com.vdub.media

import android.util.Log
import com.vdub.media.AudioData
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
            val channels = readUShort(raf)
            raf.seek(24)
            val sampleRate = readUInt(raf).toInt()
            raf.seek(34)
            val bitsPerSample = readUShort(raf)

            // Find data chunk
            raf.seek(12)
            var dataOffset = 0L
            var dataSize = 0L
            var foundData = false
            for (attempt in 0 until 20) {
                val chunkId = ByteArray(4)
                raf.read(chunkId)
                val chunkSize = readUInt(raf)
                if (String(chunkId) == "data") {
                    dataOffset = raf.filePointer
                    dataSize = chunkSize
                    foundData = true
                    break
                }
                if (chunkSize > 0) raf.skipBytes(chunkSize.toInt())
            }
            if (!foundData) {
                dataOffset = WAV_HEADER_SIZE.toLong()
                dataSize = file.length() - WAV_HEADER_SIZE
            }

            val bytesPerSample = bitsPerSample / 8
            val totalFrames = (dataSize / (bytesPerSample * channels)).toInt()

            // Read audio data
            raf.seek(dataOffset)
            val samples = FloatArray(totalFrames)

            for (i in 0 until totalFrames) {
                var sampleValue = 0f
                for (ch in 0 until channels) {
                    val raw = readSample(raf, bitsPerSample)
                    sampleValue += raw
                }
                samples[i] = sampleValue / channels // Average all channels for mono
            }

            Log.i(TAG, "Read WAV: ${samples.size} samples, ${sampleRate}Hz, ${channels}ch, ${bitsPerSample}bit")

            return AudioData(
                samples = samples,
                sampleRate = sampleRate,
                channels = 1,
                durationMs = if (sampleRate > 0) (totalFrames.toLong() * 1000) / sampleRate else 0L
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
            val channels = readUShort(raf)
            raf.seek(24)
            val sampleRate = readUInt(raf).toInt()
            raf.seek(34)
            val bitsPerSample = readUShort(raf)
            val bytesPerSample = bitsPerSample / 8

            val offset = WAV_HEADER_SIZE.toLong() + (startFrame.toLong() * bytesPerSample * channels)
            raf.seek(offset)

            val samples = FloatArray(numFrames)
            for (i in 0 until numFrames) {
                var sampleValue = 0f
                for (ch in 0 until channels) {
                    sampleValue += readSample(raf, bitsPerSample)
                }
                samples[i] = sampleValue / channels
            }

            return AudioData(
                samples = samples,
                sampleRate = sampleRate,
                channels = 1,
                durationMs = if (sampleRate > 0) (numFrames.toLong() * 1000) / sampleRate else 0L
            )
        } finally {
            raf.close()
        }
    }

    private fun readSample(raf: RandomAccessFile, bitsPerSample: Int): Float {
        return when (bitsPerSample) {
            16 -> {
                val b0 = raf.readUnsignedByte()
                val b1 = raf.readByte().toInt()
                val raw = (b1 shl 8) or b0
                raw / 32768f
            }
            8 -> {
                (raf.readUnsignedByte() - 128) / 128f
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
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
            }
            else -> 0f
        }
    }

    private fun readUShort(raf: RandomAccessFile): Int {
        val lo = raf.readUnsignedByte()
        val hi = raf.readUnsignedByte()
        return (hi shl 8) or lo
    }

    private fun readUInt(raf: RandomAccessFile): Long {
        val b0 = raf.readUnsignedByte().toLong()
        val b1 = raf.readUnsignedByte().toLong()
        val b2 = raf.readUnsignedByte().toLong()
        val b3 = raf.readUnsignedByte().toLong()
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
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
