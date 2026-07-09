package com.vdub.domain.repository

import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.entity.ProcessingProgress
import com.vdub.domain.entity.Segment
import com.vdub.domain.entity.Speaker
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for speaker diarization operations.
 */
interface DiarizationRepository {

    /**
     * Start processing a media file for speaker diarization.
     * Returns a flow of progress updates.
     */
    fun processMedia(filePath: String, mediaType: com.vdub.domain.entity.MediaType): Flow<ProcessingProgress>

    /**
     * Get the current analysis result.
     */
    fun getCurrentResult(): Flow<AnalysisResult?>

    /**
     * Cancel an ongoing analysis.
     */
    suspend fun cancelAnalysis()

    /**
     * Pause an ongoing analysis.
     */
    suspend fun pauseAnalysis()

    /**
     * Resume a paused analysis.
     */
    suspend fun resumeAnalysis()

    /**
     * Rename a speaker in the current analysis.
     */
    suspend fun renameSpeaker(oldLabel: String, newLabel: String): Result<Unit>

    /**
     * Merge two speakers into one.
     */
    suspend fun mergeSpeakers(primaryLabel: String, secondaryLabel: String): Result<Unit>

    /**
     * Delete a speaker from the current analysis.
     */
    suspend fun deleteSpeaker(label: String): Result<Unit>

    /**
     * Re-cluster speakers with a new threshold.
     */
    suspend fun recluster(threshold: Float, maxSpeakers: Int): Result<Unit>

    /**
     * Export analysis result in the specified format.
     */
    suspend fun exportResult(result: AnalysisResult, format: com.vdub.domain.entity.ExportFormat): Result<String>
}
