package com.vdub.domain.usecase

import com.vdub.domain.entity.MediaFile
import com.vdub.domain.entity.MediaType
import com.vdub.domain.entity.WaveformData
import com.vdub.domain.repository.MediaRepository
import javax.inject.Inject

/**
 * Use case for media file processing operations.
 */
class ProcessMediaUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend fun getMediaInfo(filePath: String): Result<MediaFile> = 
        mediaRepository.getMediaInfo(filePath)

    suspend fun extractAudio(videoPath: String, outputDir: String): Result<String> = 
        mediaRepository.extractAudio(videoPath, outputDir)

    suspend fun convertToWav(inputPath: String, outputDir: String, sampleRate: Int = 16000, channels: Int = 1): Result<String> = 
        mediaRepository.convertToWav(inputPath, outputDir, sampleRate, channels)

    suspend fun generateWaveform(audioPath: String): Result<WaveformData> = 
        mediaRepository.generateWaveform(audioPath)
}
