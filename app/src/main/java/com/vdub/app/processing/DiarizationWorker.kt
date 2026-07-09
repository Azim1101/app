package com.vdub.app.processing

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.vdub.domain.entity.MediaType
import com.vdub.domain.repository.DiarizationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for background diarization processing.
 * Continues processing even when the app is minimized.
 */
@HiltWorker
class DiarizationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val diarizationRepository: DiarizationRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_MEDIA_TYPE = "media_type"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "DiarizationWorker"
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val mediaTypeStr = inputData.getString(KEY_MEDIA_TYPE) ?: "AUDIO"
        val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (_: Exception) { MediaType.AUDIO }

        Log.i(TAG, "Starting background diarization: $filePath")

        return try {
            val progressFlow = diarizationRepository.processMedia(filePath, mediaType)
            progressFlow.collect { progress ->
                setProgress(
                    androidx.work.Data.Builder()
                        .putFloat("progress", progress.progress)
                        .putString("step", progress.currentStep)
                        .build()
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background diarization failed", e)
            Result.failure(
                androidx.work.Data.Builder()
                    .putString("error", e.message)
                    .build()
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            "vdub_processing"
        )
            .setContentTitle("VDub Processing")
            .setContentText("Processing audio for speaker diarization...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
