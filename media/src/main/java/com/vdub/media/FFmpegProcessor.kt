package com.vdub.media

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FFmpeg command execution wrapper.
 * Supports both FFmpeg Kit (if available) and direct FFmpeg binary execution.
 * Falls back to ProcessBuilder for command execution.
 */
@Singleton
class FFmpegProcessor @Inject constructor() {

    companion object {
        private const val TAG = "FFmpegProcessor"
    }

    /**
     * Execute an FFmpeg command.
     * First tries FFmpeg Kit, then falls back to direct binary execution.
     */
    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Try FFmpeg Kit first
            try {
                val clazz = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
                val method = clazz.getMethod("execute", String::class.java)
                val session = method.invoke(null, command)

                val returnCodeClazz = Class.forName("com.arthenica.ffmpegkit.ReturnCode")
                val isSuccessMethod = returnCodeClazz.getMethod("isSuccess", Int::class.javaPrimitiveType)
                val getReturnCodeMethod = session.javaClass.getMethod("getReturnCode")
                val returnCode = getReturnCodeMethod.invoke(session)
                val returnCodeValue = returnCode?.let { 
                    val getValueMethod = it.javaClass.getMethod("getValue")
                    getValueMethod.invoke(it) as Int 
                } ?: -1

                val isSuccess = isSuccessMethod.invoke(null, returnCodeValue) as Boolean

                if (isSuccess) {
                    Log.d(TAG, "FFmpeg Kit success: $command")
                    val outputMethod = session.javaClass.getMethod("getOutput")
                    outputMethod.invoke(session) as? String ?: ""
                } else {
                    throw Exception("FFmpeg Kit failed with return code $returnCodeValue")
                }
            } catch (e: ClassNotFoundException) {
                // FFmpeg Kit not available, use direct execution
                executeDirect(command)
            } catch (e: Exception) {
                // FFmpeg Kit failed, try direct execution
                Log.w(TAG, "FFmpeg Kit failed, trying direct execution", e)
                executeDirect(command)
            }
        }
    }

    /**
     * Execute FFmpeg command using ProcessBuilder.
     */
    private fun executeDirect(command: String): String {
        val parts = command.trim().split("\\s+".toRegex()).toMutableList()
        // Ensure ffmpeg is the first part
        if (parts.isNotEmpty() && parts[0] != "ffmpeg") {
            parts.add(0, "ffmpeg")
        }

        Log.d(TAG, "Executing: ${parts.joinToString(" ")}")

        val process = ProcessBuilder(parts)
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.appendLine(line)
        }

        val exitCode = process.waitFor()
        reader.close()

        if (exitCode == 0) {
            Log.d(TAG, "FFmpeg success")
            return output.toString()
        } else {
            throw Exception("FFmpeg failed with exit code $exitCode: $output")
        }
    }

    /**
     * Execute FFmpeg with progress callback.
     */
    suspend fun executeWithProgress(
        command: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = execute(command)

    /**
     * Get media information using ffprobe.
     */
    fun getMediaInfo(filePath: String): Any? {
        try {
            val process = ProcessBuilder("ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", filePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return output
        } catch (e: Exception) {
            Log.w(TAG, "ffprobe not available", e)
            return null
        }
    }

    /**
     * Get media duration in milliseconds.
     */
    fun getDuration(filePath: String): Long {
        try {
            val process = ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            return ((output.toDoubleOrNull() ?: 0.0) * 1000).toLong()
        } catch (e: Exception) {
            Log.w(TAG, "ffprobe failed", e)
            return 0L
        }
    }

    /**
     * Get sample rate from media file.
     */
    fun getSampleRate(filePath: String): Int {
        try {
            val process = ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", "a:0",
                "-show_entries", "stream=sample_rate",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            return output.toIntOrNull() ?: 44100
        } catch (e: Exception) {
            return 44100
        }
    }

    /**
     * Get number of audio channels.
     */
    fun getChannels(filePath: String): Int {
        try {
            val process = ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", "a:0",
                "-show_entries", "stream=channels",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            return output.toIntOrNull() ?: 2
        } catch (e: Exception) {
            return 2
        }
    }

    /**
     * Get audio bitrate.
     */
    fun getBitrate(filePath: String): Long {
        try {
            val process = ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", "a:0",
                "-show_entries", "stream=bit_rate",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            return output.toLongOrNull() ?: 128000L
        } catch (e: Exception) {
            return 128000L
        }
    }

    /**
     * Get the FFmpeg version string.
     */
    fun getVersion(): String {
        return try {
            val process = ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readLine() ?: "Unknown"
            process.waitFor()
            output
        } catch (e: Exception) {
            "FFmpeg not available - install ffmpeg-kit or system ffmpeg"
        }
    }
}
