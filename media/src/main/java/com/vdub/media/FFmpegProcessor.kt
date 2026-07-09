package com.vdub.media

import android.util.Log
import com.arthenica.ffmpegkit.ExecuteCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FFmpeg command execution wrapper with coroutine support.
 */
@Singleton
class FFmpegProcessor @Inject constructor() {

    companion object {
        private const val TAG = "FFmpegProcessor"
    }

    /**
     * Execute an FFmpeg command and wait for completion.
     */
    suspend fun execute(command: String): Result<String> = suspendCancellableCoroutine { cont ->
        val session = FFmpegKit.execute(command)

        if (ReturnCode.isSuccess(session.returnCode)) {
            val output = session.output
            Log.d(TAG, "FFmpeg success: $command")
            cont.resume(Result.success(output ?: ""))
        } else {
            val error = "FFmpeg failed with return code \${session.returnCode}: \${session.failStackTrace}"
            Log.e(TAG, error)
            cont.resume(Result.failure(Exception(error)))
        }
    }

    /**
     * Execute an FFmpeg command with progress tracking.
     */
    suspend fun executeWithProgress(
        command: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = suspendCancellableCoroutine { cont ->
        val session = FFmpegKit.executeWithStatistics(command, object : StatisticsCallback {
            override fun apply(statistics: Statistics?) {
                statistics?.let { stats ->
                    if (stats.time > 0) {
                        onProgress(stats.time.toFloat() / 1000f)
                    }
                }
            }
        }, object : ExecuteCallback {
            override fun apply(session: com.arthenica.ffmpegkit.FFmpegSession?) {
                session?.let { s ->
                    if (ReturnCode.isSuccess(s.returnCode)) {
                        cont.resume(Result.success(s.output ?: ""))
                    } else {
                        cont.resume(Result.failure(
                            Exception("FFmpeg failed: \${s.returnCode} - \${s.failStackTrace}")
                        ))
                    }
                }
            }
        })

        cont.invokeOnCancellation {
            FFmpegKit.cancel(session.sessionId)
        }
    }

    /**
     * Get media information using FFprobe.
     */
    fun getMediaInfo(filePath: String): MediaInformation? {
        return FFprobeKit.getMediaInformation(filePath).mediaInformation
    }

    /**
     * Get media duration in milliseconds.
     */
    fun getDuration(filePath: String): Long {
        val info = getMediaInfo(filePath)
        val durationStr = info?.duration
        return ((durationStr?.toDoubleOrNull() ?: 0.0) * 1000).toLong()
    }

    /**
     * Get sample rate from media file.
     */
    fun getSampleRate(filePath: String): Int {
        val info = getMediaInfo(filePath)
        return info?.streams?.firstOrNull { it.type == "audio" }?.sampleRate?.toIntOrNull() ?: 44100
    }

    /**
     * Get number of audio channels.
     */
    fun getChannels(filePath: String): Int {
        val info = getMediaInfo(filePath)
        return info?.streams?.firstOrNull { it.type == "audio" }?.channels?.toIntOrNull() ?: 2
    }

    /**
     * Get audio bitrate.
     */
    fun getBitrate(filePath: String): Long {
        val info = getMediaInfo(filePath)
        return info?.streams?.firstOrNull { it.type == "audio" }?.bitRate?.toLongOrNull() ?: 128000L
    }

    /**
     * Get the FFmpeg version string.
     */
    fun getVersion(): String = FFmpegKitConfig.getVersion()
}
