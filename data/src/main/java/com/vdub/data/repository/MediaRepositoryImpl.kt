package com.vdub.data.repository

import android.content.Context
import android.net.Uri
import com.vdub.domain.entity.MediaFile
import com.vdub.domain.entity.MediaType
import com.vdub.domain.entity.WaveformData
import com.vdub.domain.repository.MediaRepository
import com.vdub.media.AudioExtractor
import com.vdub.media.FFmpegProcessor
import com.vdub.media.WaveformGenerator
import com.vdub.media.AudioReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioExtractor: AudioExtractor,
    private val ffmpegProcessor: FFmpegProcessor,
    private val waveformGenerator: WaveformGenerator,
    private val audioReader: AudioReader
) : MediaRepository {

    private val recentFilesDir: File
        get() = File(context.filesDir, "recent").also { it.mkdirs() }

    override suspend fun getMediaInfo(filePath: String): Result<MediaFile> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            val info = ffmpegProcessor.getMediaInfo(filePath)
            val durationMs = ffmpegProcessor.getDuration(filePath)
            val sampleRate = ffmpegProcessor.getSampleRate(filePath)
            val channels = ffmpegProcessor.getChannels(filePath)
            val bitrate = ffmpegProcessor.getBitrate(filePath)
            val name = file.name
            val ext = name.substringAfterLast('.', "").lowercase()

            MediaFile(
                name = name,
                path = filePath,
                size = file.length(),
                mimeType = getMimeType(ext),
                mediaType = MediaType.fromExtension(ext),
                durationMs = durationMs,
                sampleRate = sampleRate,
                channels = channels,
                bitrate = bitrate
            )
        }
    }

    override suspend fun extractAudio(videoPath: String, outputDir: String): Result<String> {
        return audioExtractor.extractAudio(videoPath)
    }

    override suspend fun convertToWav(inputPath: String, outputDir: String, sampleRate: Int, channels: Int): Result<String> {
        return audioExtractor.convertToWav(inputPath, sampleRate, channels)
    }

    override suspend fun resampleAudio(inputPath: String, outputDir: String, targetSampleRate: Int): Result<String> {
        return audioExtractor.resample(inputPath, targetSampleRate)
    }

    override suspend fun convertToMono(inputPath: String, outputDir: String): Result<String> {
        return audioExtractor.toMono(inputPath)
    }

    override suspend fun trimAudio(inputPath: String, outputDir: String, startMs: Long, endMs: Long): Result<String> {
        return audioExtractor.trim(inputPath, startMs, endMs)
    }

    override suspend fun mergeAudio(inputPaths: List<String>, outputDir: String): Result<String> {
        return audioExtractor.merge(inputPaths)
    }

    override suspend fun normalizeLoudness(inputPath: String, outputDir: String, targetLufs: Float): Result<String> {
        return audioExtractor.normalizeLoudness(inputPath, targetLufs)
    }

    override suspend fun changeBitrate(inputPath: String, outputDir: String, bitrate: Int): Result<String> {
        return audioExtractor.changeBitrate(inputPath, bitrate)
    }

    override suspend fun generateWaveform(audioPath: String, samplesPerPixel: Int): Result<WaveformData> {
        return runCatching { waveformGenerator.generateFromWav(audioPath, samplesPerPixel) }
    }

    override fun getRecentFiles(): Flow<List<MediaFile>> = flow {
        val files = recentFilesDir.listFiles()?.mapNotNull { file ->
            try {
                getMediaInfo(file.absolutePath).getOrNull()
            } catch (_: Exception) { null }
        } ?: emptyList()
        emit(files.sortedByDescending { it.durationMs })
    }

    override suspend fun convertMedia(
        inputPath: String,
        outputDir: String,
        outputFormat: String,
        sampleRate: Int?,
        channels: Int?,
        bitrate: Int?
    ): Result<String> {
        return audioExtractor.convert(inputPath, outputFormat, sampleRate, channels, bitrate)
    }

    override suspend fun getDuration(filePath: String): Result<Long> = runCatching {
        ffmpegProcessor.getDuration(filePath)
    }

    private fun getMimeType(ext: String): String = when (ext) {
        "wav" -> "audio/wav"
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "mpeg" -> "video/mpeg"
        "3gp" -> "video/3gpp"
        else -> "application/octet-stream"
    }
}
