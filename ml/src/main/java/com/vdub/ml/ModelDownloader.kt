package com.vdub.ml

import android.content.Context
import android.util.Log
import com.vdub.domain.entity.DownloadStatus
import com.vdub.domain.entity.ModelDownloadState
import com.vdub.domain.entity.ModelInfo
import com.vdub.domain.entity.ModelRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads ML models from HuggingFace with resume support.
 * Manages download state, progress, SHA256 verification.
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "ModelDownloader"
        private const val MODELS_DIR = "models"
        private const val TEMP_SUFFIX = ".part"
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    }

    private val downloadStates = ConcurrentHashMap<String, MutableStateFlow<ModelDownloadState>>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val modelsDir: File
        get() = File(context.filesDir, MODELS_DIR).also { it.mkdirs() }

    fun getDownloadStateFlow(modelId: String): StateFlow<ModelDownloadState> {
        return downloadStates.getOrPut(modelId) {
            MutableStateFlow(ModelDownloadState(modelId = modelId))
        }.asStateFlow()
    }

    fun getAllDownloadStateFlows(): Map<String, StateFlow<ModelDownloadState>> {
        ModelRegistry.MODELS.forEach { model ->
            downloadStates.getOrPut(model.id) {
                MutableStateFlow(
                    ModelDownloadState(
                        modelId = model.id,
                        status = if (isModelDownloaded(model)) DownloadStatus.COMPLETED else DownloadStatus.IDLE
                    )
                )
            }
        }
        return downloadStates.mapValues { it.value.asStateFlow() }
    }

    private fun updateState(modelId: String, transform: (ModelDownloadState) -> ModelDownloadState) {
        val flow = downloadStates.getOrPut(modelId) {
            MutableStateFlow(ModelDownloadState(modelId = modelId))
        }
        flow.value = transform(flow.value)
    }

    /**
     * Start downloading a model.
     */
    fun downloadModel(modelInfo: ModelInfo) {
        if (activeJobs.containsKey(modelInfo.id)) return

        val job = scope.launch {
            performDownload(modelInfo)
        }
        activeJobs[modelInfo.id] = job
        job.invokeOnCompletion {
            activeJobs.remove(modelInfo.id)
        }
    }

    private suspend fun performDownload(modelInfo: ModelInfo) {
        val targetFile = File(modelsDir, modelInfo.localPath)
        val tempFile = File("${targetFile.absolutePath}$TEMP_SUFFIX")

        updateState(modelInfo.id) { it.copy(status = DownloadStatus.DOWNLOADING, progress = 0f) }

        try {
            targetFile.parentFile?.mkdirs()

            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            // Try primary URL, then mirrors
            val urls = listOf(modelInfo.downloadUrl) + modelInfo.mirrorUrls
            var success = false
            var lastError: Exception? = null

            for (url in urls) {
                try {
                    downloadFromUrl(url, tempFile, existingBytes, modelInfo)
                    success = true
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download from $url: ${e.message}")
                    lastError = e
                    continue
                }
            }

            if (!success) throw lastError ?: Exception("Download failed")

            // Verify SHA256 if available
            if (modelInfo.sha256.isNotEmpty() && modelInfo.sha256 != "auto") {
                updateState(modelInfo.id) { it.copy(status = DownloadStatus.VERIFYING) }
                val actualHash = calculateSHA256(tempFile)
                if (actualHash.equals(modelInfo.sha256, ignoreCase = true)) {
                    Log.i(TAG, "SHA256 verified for ${modelInfo.id}")
                } else {
                    Log.w(TAG, "SHA256 mismatch for ${modelInfo.id}: expected=${modelInfo.sha256}, actual=$actualHash")
                    // Don't fail on hash mismatch for auto hashes
                    if (modelInfo.sha256 != "auto") {
                        updateState(modelInfo.id) {
                            it.copy(status = DownloadStatus.VERIFY_FAILED, error = "SHA256 verification failed")
                        }
                        tempFile.delete()
                        return
                    }
                }
            }

            // Move temp file to final location
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            updateState(modelInfo.id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length()
                )
            }

            Log.i(TAG, "Model ${modelInfo.id} downloaded successfully to ${targetFile.absolutePath}")

        } catch (e: CancellationException) {
            updateState(modelInfo.id) { it.copy(status = DownloadStatus.PAUSED) }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${modelInfo.id}", e)
            updateState(modelInfo.id) {
                it.copy(status = DownloadStatus.FAILED, error = e.message)
            }
        }
    }

    private suspend fun downloadFromUrl(
        url: String,
        outputFile: File,
        existingBytes: Long,
        modelInfo: ModelInfo
    ) {
        val request = Request.Builder()
            .url(url)
            .apply { if (existingBytes > 0) header("Range", "bytes=$existingBytes-") }
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body ?: throw Exception("Empty response body")

        val totalBytes = if (response.code == 206) {
            // Partial content - get total from Content-Range header
            val rangeHeader = response.header("Content-Range")
            val total = rangeHeader?.substringAfter("/")?.toLongOrNull() ?: (existingBytes + responseBody.contentLength())
            total
        } else {
            responseBody.contentLength()
        }

        updateState(modelInfo.id) {
            it.copy(totalBytes = total, downloadedBytes = existingBytes)
        }

        val raf = RandomAccessFile(outputFile, "rw")
        raf.seek(existingBytes)

        val inputStream = responseBody.byteStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var lastUpdateTime = System.currentTimeMillis()

        try {
            var bytesRead: Int
            var totalRead = existingBytes

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                ensureActive()

                raf.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                    val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                    val speed = (bytesRead * 1000L) / (now - lastUpdateTime + 1)

                    updateState(modelInfo.id) {
                        it.copy(
                            progress = progress,
                            downloadedBytes = totalRead,
                            speed = speed
                        )
                    }
                    lastUpdateTime = now
                }
            }
        } finally {
            inputStream.close()
            raf.close()
            response.close()
        }
    }

    /**
     * Pause a download.
     */
    fun pauseDownload(modelId: String) {
        activeJobs[modelId]?.cancel()
        updateState(modelId) { it.copy(status = DownloadStatus.PAUSED) }
    }

    /**
     * Resume a download.
     */
    fun resumeDownload(modelInfo: ModelInfo) {
        downloadModel(modelInfo)
    }

    /**
     * Retry a failed download.
     */
    fun retryDownload(modelInfo: ModelInfo) {
        val tempFile = File(modelsDir, "${modelInfo.localPath}$TEMP_SUFFIX")
        if (tempFile.exists()) tempFile.delete()
        updateState(modelInfo.id) { it.copy(status = DownloadStatus.IDLE, error = null) }
        downloadModel(modelInfo)
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(modelInfo: ModelInfo): Boolean {
        activeJobs[modelInfo.id]?.cancel()
        val modelDir = File(modelsDir, modelInfo.localPath).parentFile
        val deleted = modelDir?.deleteRecursively() ?: false
        updateState(modelInfo.id) {
            it.copy(status = DownloadStatus.IDLE, progress = 0f, downloadedBytes = 0)
        }
        return deleted
    }

    /**
     * Import a model from a local file.
     */
    fun importModel(sourcePath: String, modelInfo: ModelInfo): Boolean {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return false

        val targetFile = File(modelsDir, modelInfo.localPath)
        targetFile.parentFile?.mkdirs()

        val success = sourceFile.copyTo(targetFile, overwrite = true).exists()
        if (success) {
            updateState(modelInfo.id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length()
                )
            }
        }
        return success
    }

    /**
     * Check if a model is downloaded.
     */
    fun isModelDownloaded(modelInfo: ModelInfo): Boolean {
        return File(modelsDir, modelInfo.localPath).exists()
    }

    /**
     * Get the local path for a downloaded model.
     */
    fun getModelLocalPath(modelInfo: ModelInfo): String {
        return File(modelsDir, modelInfo.localPath).absolutePath
    }

    /**
     * Get total storage used by downloaded models.
     */
    fun getTotalStorageUsed(): Long {
        return modelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Verify model SHA256 checksum.
     */
    suspend fun verifyModel(modelInfo: ModelInfo): Boolean {
        val file = File(modelsDir, modelInfo.localPath)
        if (!file.exists()) return false
        if (modelInfo.sha256.isEmpty() || modelInfo.sha256 == "auto") return true

        return withContext(Dispatchers.IO) {
            val actualHash = calculateSHA256(file)
            actualHash.equals(modelInfo.sha256, ignoreCase = true)
        }
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        file.inputStream().use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}
