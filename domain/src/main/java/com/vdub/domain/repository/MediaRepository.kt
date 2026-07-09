package com.vdub.domain.repository

import com.vdub.domain.entity.MediaFile
import com.vdub.domain.entity.WaveformData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for media file operations.
 */
interface MediaRepository {

    /**
     * Get metadata for a media file.
     */
    suspend fun getMediaInfo(filePath: String): Result<MediaFile>

    /**
     * Extract audio from a video file.
     */
    suspend fun extractAudio(videoPath: String, outputDir: String): Result<String>

    /**
     * Convert audio to WAV format.
     */
    suspend fun convertToWav(inputPath: String, outputDir: String, sampleRate: Int = 16000, channels: Int = 1): Result<String>

    /**
     * Resample audio to target sample rate.
     */
    suspend fun resampleAudio(inputPath: String, outputDir: String, targetSampleRate: Int): Result<String>

    /**
     * Convert stereo to mono.
     */
    suspend fun convertToMono(inputPath: String, outputDir: String): Result<String>

    /**
     * Trim audio between start and end times.
     */
    suspend fun trimAudio(inputPath: String, outputDir: String, startMs: Long, endMs: Long): Result<String>

    /**
     * Merge multiple audio files.
     */
    suspend fun mergeAudio(inputPaths: List<String>, outputDir: String): Result<String>

    /**
     * Normalize loudness of audio.
     */
    suspend fun normalizeLoudness(inputPath: String, outputDir: String, targetLufs: Float = -16f): Result<String>

    /**
     * Change bitrate of audio.
     */
    suspend fun changeBitrate(inputPath: String, outputDir: String, bitrate: Int): Result<String>

    /**
     * Generate waveform data for visualization.
     */
    suspend fun generateWaveform(audioPath: String, samplesPerPixel: Int = 200): Result<WaveformData>

    /**
     * Get recent media files.
     */
    fun getRecentFiles(): Flow<List<MediaFile>>

    /**
     * Convert media between formats.
     */
    suspend fun convertMedia(
        inputPath: String,
        outputDir: String,
        outputFormat: String,
        sampleRate: Int? = null,
        channels: Int? = null,
        bitrate: Int? = null
    ): Result<String>

    /**
     * Get media duration in milliseconds.
     */
    suspend fun getDuration(filePath: String): Result<Long>
}
