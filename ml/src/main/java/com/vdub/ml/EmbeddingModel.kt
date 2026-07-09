package com.vdub.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.util.Log
import com.vdub.domain.entity.Segment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SpeechBrain ECAPA-TDNN speaker embedding model wrapper.
 * Generates 192-dimensional speaker embeddings for audio chunks.
 */
@Singleton
class EmbeddingModel @Inject constructor(
    private val onnxManager: ONNXRuntimeManager
) {
    companion object {
        private const val TAG = "EmbeddingModel"
        private const val MODEL_ID = "speechbrain-ecapa"
        private const val SAMPLE_RATE = 16000
        private const val EMBEDDING_DIM = 192
    }

    private var session: OrtSession? = null
    private var inputName: String = "waveform"

    /**
     * Initialize the embedding model.
     */
    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val file = File(modelPath)
            require(file.exists()) { "Embedding model not found at $modelPath" }

            val result = onnxManager.getSession(MODEL_ID, modelPath)
            result.onSuccess { sess ->
                session = sess
                inputName = onnxManager.getInputNames(sess).firstOrNull() ?: "waveform"
                Log.i(TAG, "Embedding model loaded. Input: $inputName")
            }.onFailure { e ->
                throw e
            }
            Unit
        }
    }

    /**
     * Generate an embedding for a single audio chunk.
     */
    suspend fun generateEmbedding(
        audioData: FloatArray,
        sampleRate: Int
    ): FloatArray = withContext(Dispatchers.IO) {
        val sess = session ?: throw IllegalStateException("Embedding model not initialized")

        val samples = if (sampleRate != SAMPLE_RATE) {
            resampleAudio(audioData, sampleRate, SAMPLE_RATE)
        } else {
            audioData
        }

        val minSamples = SAMPLE_RATE / 2
        val paddedSamples = if (samples.size < minSamples) {
            samples.copyOf(minSamples)
        } else {
            samples
        }

        // Input shape: [1, num_samples]
        val inputShape = longArrayOf(1, paddedSamples.size.toLong())

        val buffer = ByteBuffer.allocateDirect(paddedSamples.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(paddedSamples)

        val inputTensor = OnnxTensor.createTensor(onnxManager.environment, buffer, inputShape)

        val results = try {
            onnxManager.runInferenceSync(sess, mapOf(inputName to inputTensor))
        } catch (e: Exception) {
            // Try [1, 1, num_samples] shape
            inputTensor.close()
            val buffer2 = ByteBuffer.allocateDirect(paddedSamples.size * 4)
            buffer2.order(ByteOrder.nativeOrder())
            buffer2.asFloatBuffer().put(paddedSamples)
            val shape2 = longArrayOf(1, 1, paddedSamples.size.toLong())
            val tensor2 = OnnxTensor.createTensor(onnxManager.environment, buffer2, shape2)
            onnxManager.runInferenceSync(sess, mapOf(inputName to tensor2))
        }

        inputTensor.close()

        val outputKey = results.keys.firstOrNull()
        val outputTensor = if (outputKey != null) results[outputKey] else null

        val embedding = if (outputTensor != null) {
            try {
                val outputBuffer = outputTensor.floatBuffer
                val arr = FloatArray(minOf(outputBuffer.remaining(), EMBEDDING_DIM * 2))
                outputBuffer.get(arr)
                if (arr.size > EMBEDDING_DIM) arr.copyOfRange(arr.size - EMBEDDING_DIM, arr.size) else arr
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read embedding output", e)
                FloatArray(EMBEDDING_DIM)
            }
        } else {
            FloatArray(EMBEDDING_DIM)
        }

        // L2 normalize
        val norm = kotlin.math.sqrt(embedding.fold(0f) { acc, v -> acc + v * v })
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        results.values.forEach { it.close() }

        Log.d(TAG, "Generated embedding: dim=${embedding.size}, norm=${String.format("%.3f", norm)}")
        embedding
    }

    /**
     * Generate embeddings for a list of segments from audio data.
     */
    suspend fun generateEmbeddings(
        audioData: FloatArray,
        sampleRate: Int,
        segments: List<Segment>
    ): List<Segment> = withContext(Dispatchers.IO) {
        segments.map { segment ->
            val startSample = ((segment.startTimeMs * sampleRate) / 1000).toInt().coerceAtLeast(0)
            val endSample = ((segment.endTimeMs * sampleRate) / 1000).toInt().coerceAtMost(audioData.size)

            if (endSample > startSample && (endSample - startSample) >= sampleRate / 10) {
                val chunkAudio = audioData.copyOfRange(startSample, endSample)
                val embedding = generateEmbedding(chunkAudio, sampleRate)
                segment.copy(embedding = embedding.toList())
            } else {
                segment
            }
        }
    }

    private fun resampleAudio(data: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return data
        val ratio = toRate.toDouble() / fromRate.toDouble()
        val newLength = (data.size * ratio).toInt()
        val result = FloatArray(newLength)
        for (i in 0 until newLength) {
            val srcIndex = i / ratio
            val srcFloor = srcIndex.toInt()
            val srcCeil = minOf(srcFloor + 1, data.size - 1)
            val fraction = srcIndex - srcFloor
            result[i] = data[srcFloor] * (1 - fraction).toFloat() + data[srcCeil] * fraction.toFloat()
        }
        return result
    }

    fun close() {
        session = null
        onnxManager.closeSession(MODEL_ID)
    }
}
