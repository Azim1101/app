package com.vdub.media

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts audio from video files and performs audio conversion using FFmpeg.
 */
@Singleton
class AudioExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegProcessor: FFmpegProcessor
) {
    companion object {
        private const val TAG = "AudioExtractor"
    }

    private val cacheDir: File
        get() = File(context.cacheDir, "audio_extracted").also { it.mkdirs() }

    /**
     * Extract audio from a video file to WAV format.
     */
    suspend fun extractAudio(
        videoPath: String,
        sampleRate: Int = 16000,
        channels: Int = 1
    ): Result<String> {
        val outputFile = File(cacheDir, "extracted_${System.currentTimeMillis()}.wav")
        val command = buildString {
            append("-y ")
            append("-i \"$videoPath\" ")
            append("-vn ")
            append("-acodec pcm_s16le ")
            append("-ar $sampleRate ")
            append("-ac $channels ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * Convert any audio file to WAV format.
     */
    suspend fun convertToWav(
        inputPath: String,
        sampleRate: Int = 16000,
        channels: Int = 1
    ): Result<String> {
        val outputFile = File(cacheDir, "converted_${System.currentTimeMillis()}.wav")
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            append("-acodec pcm_s16le ")
            append("-ar $sampleRate ")
            append("-ac $channels ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * Resample audio to a different sample rate.
     */
    suspend fun resample(
        inputPath: String,
        targetSampleRate: Int
    ): Result<String> {
        val outputFile = File(cacheDir, "resampled_${System.currentTimeMillis()}.wav")
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            append("-acodec pcm_s16le ")
            append("-ar $targetSampleRate ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * Convert stereo audio to mono.
     */
    suspend fun toMono(inputPath: String): Result<String> {
        val outputFile = File(cacheDir, "mono_${System.currentTimeMillis()}.wav")
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            append("-acodec pcm_s16le ")
            append("-ac 1 ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * Trim audio between start and end times.
     */
    suspend fun trim(
        inputPath: String,
        startMs: Long,
        endMs: Long
    ): Result<String> {
        val outputFile = File(cacheDir, "trimmed_${System.currentTimeMillis()}.wav")
        val startTime = formatTime(startMs)
        val duration = formatTime(endMs - startMs)
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            append("-ss $startTime ")
            append("-t $duration ")
            append("-acodec pcm_s16le ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * Merge multiple audio files into one.
     */
    suspend fun merge(inputPaths: List<String>): Result<String> {
        if (inputPaths.isEmpty()) return Result.failure(Exception("No input files"))
        if (inputPaths.size == 1) return Result.success(inputPaths[0])

        val listFile = File(cacheDir, "merge_list_${System.currentTimeMillis()}.txt")
        listFile.writeText(inputPaths.joinToString("\n") { "file '${it}'" })

        val outputFile = File(cacheDir, "merged_${System.currentTimeMillis()}.wav")
        val command = buildString {
            append("-y ")
            append("-f concat -safe 0 ")
            append("-i \"${listFile.absolutePath}\" ")
            append("-acodec pcm_s16le ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map {
            listFile.delete()
            outputFile.absolutePath
        }
    }

    /**
     * Normalize loudness to a target LUFS.
     */
    suspend fun normalizeLoudness(
        inputPath: String,
        targetLufs: Float = -16f
    ): Result<String> {
        val outputFile = File(cacheDir, "normalized_${System.currentTimeMillis()}.wav")
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            append("-af loudnorm=I=$targetLufs:TP=-1.5:LRA=11 ")
            append("-acodec pcm_s16le ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * Change audio bitrate.
     */
    suspend fun changeBitrate(
        inputPath: String,
        bitrate: Int,
        outputFormat: String = "mp3"
    ): Result<String> {
        val outputFile = File(cacheDir, "bitrate_${System.currentTimeMillis()}.$outputFormat")
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            append("-b:a ${bitrate}k ")
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    /**
     * General media conversion.
     */
    suspend fun convert(
        inputPath: String,
        outputFormat: String,
        sampleRate: Int? = null,
        channels: Int? = null,
        bitrate: Int? = null
    ): Result<String> {
        val outputFile = File(cacheDir, "converted_${System.currentTimeMillis()}.$outputFormat")
        val command = buildString {
            append("-y ")
            append("-i \"$inputPath\" ")
            sampleRate?.let { append("-ar $it ") }
            channels?.let { append("-ac $it ") }
            bitrate?.let { append("-b:a ${it}k ") }
            append("\"${outputFile.absolutePath}\"")
        }

        return ffmpegProcessor.execute(command).map { outputFile.absolutePath }
    }

    private fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}
