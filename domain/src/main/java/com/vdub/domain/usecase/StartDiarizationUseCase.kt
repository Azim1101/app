package com.vdub.domain.usecase

import com.vdub.domain.entity.MediaType
import com.vdub.domain.entity.ProcessingProgress
import com.vdub.domain.repository.DiarizationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to start speaker diarization on a media file.
 */
class StartDiarizationUseCase @Inject constructor(
    private val diarizationRepository: DiarizationRepository
) {
    operator fun invoke(filePath: String, mediaType: MediaType): Flow<ProcessingProgress> {
        return diarizationRepository.processMedia(filePath, mediaType)
    }
}
