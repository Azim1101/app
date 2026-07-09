package com.vdub.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Silero VAD ONNX model wrapper.
 * Detects voice activity in audio chunks for pre-filtering.
 */
@Singleton
class VADModel @Inject constructor(
    private val onnxManager: ONNXRuntimeManager
) {
    companion object {
        private const val TAG = "VADModel"
        private const val MODEL_ID = "silero-vad"
        private const val SAMPLE_RATE = 16000
        private const val WINDOW_SIZE = 512
        private const val SPEECH_THRESHOLD = 0.5f
    }

    private var session: OrtSession? = null
    private var hState: FloatArray = FloatArray(64)
    private var cState: FloatArray = FloatArray(64)

    /**
     * Initialize the VAD model.
     */
    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val file = File(modelPath)
            require(file.exists()) { "VAD model not found at $modelPath" }

            val result = onnxManager.getSession(MODEL_ID, modelPath)
            result.onSuccess { sess ->
                session = sess
                resetState()
                Log.i(TAG, "VAD model loaded successfully")
            }.onFailure { e ->
                throw e
            }
        }
    }

    private fun resetState() {
        hState = FloatArray(64)
        cState = FloatArray(64)
    }

    /**
     * Detect if speech is present in an audio window.
     */
    suspend fun detectSpeech(audioChunk: FloatArray, sampleRate: Int): Boolean = withContext(Dispatchers.Default) {
        val sess = session ?: return@withContext true

        val samples = if (sampleRate != SAMPLE_RATE) {
            resampleAudio(audioChunk, sampleRate, SAMPLE_RATE)
        } else {
            audioChunk
        }

        val chunk = if (samples.size > WINDOW_SIZE) {
            samples.copyOfRange(0, WINDOW_SIZE)
        } else if (samples.size < WINDOW_SIZE) {
            samples.copyOf(WINDOW_SIZE)
        } else {
            samples
        }

        try {
            val score = runVadInference(chunk)
            score > SPEECH_THRESHOLD
        } catch (e: Exception) {
            Log.w(TAG, "VAD inference failed", e)
            true // Default to speech if model fails
        }
    }

    /**
     * Get speech probability for an audio chunk.
     */
    suspend fun getSpeechProbability(audioChunk: FloatArray, sampleRate: Int): Float = withContext(Dispatchers.Default) {
        val sess = session ?: return@withContext 1f

        val samples = if (sampleRate != SAMPLE_RATE) resampleAudio(audioChunk, sampleRate, SAMPLE_RATE) else audioChunk
        val chunk = samples.copyOf(WINDOW_SIZE)

        try {
            runVadInference(chunk)
        } catch (e: Exception) {
            Log.w(TAG, "VAD inference failed", e)
            1f
        }
    }

    private suspend fun runVadInference(chunk: FloatArray): Float {
        val sess = session ?: return 1f

        // Create input tensors
        val inputBuffer = ByteBuffer.allocateDirect(chunk.size * 4).order(ByteOrder.nativeOrder())
        inputBuffer.asFloatBuffer().put(chunk)
        val inputTensor = OnnxTensor.createTensor(onnxManager.environment, inputBuffer, longArrayOf(1, chunk.size.toLong()))

        val srBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        srBuffer.asLongBuffer().put(SAMPLE_RATE.toLong())
        val srTensor = OnnxTensor.createTensor(onnxManager.environment, srBuffer, longArrayOf(1))

        val hBuffer = ByteBuffer.allocateDirect(64 * 4).order(ByteOrder.nativeOrder())
        hBuffer.asFloatBuffer().put(hState)
        val hTensor = OnnxTensor.createTensor(onnxManager.environment, hBuffer, longArrayOf(1, 64))

        val cBuffer = ByteBuffer.allocateDirect(64 * 4).order(ByteOrder.nativeOrder())
        cBuffer.asFloatBuffer().put(cState)
        val cTensor = OnnxTensor.createTensor(onnxManager.environment, cBuffer, longArrayOf(1, 64))

        val inputs = mapOf(
            "input" to inputTensor,
            "sr" to srTensor,
            "h" to hTensor,
            "c" to cTensor
        )

        val results = onnxManager.runInference(sess, inputs)

        val score = try {
            val outputKey = results.keys.firstOrNull() ?: return 0f
            results[outputKey]?.floatBuffer?.get(0) ?: 0f
        } catch (e: Exception) {
            0f
        }

        // Update hidden states from output
        try {
            val outputList = results.values.toList()
            if (outputList.size >= 3) {
                val hBuf = outputList[1].floatBuffer
                for (i in 0 until minOf(64, hBuf.remaining())) {
                    hState[i] = hBuf.get(i)
                }
                val cBuf = outputList[2].floatBuffer
                for (i in 0 until minOf(64, cBuf.remaining())) {
                    cState[i] = cBuf.get(i)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update VAD states", e)
        }

        results.values.forEach { it.close() }
        inputTensor.close()
        srTensor.close()
        hTensor.close()
        cTensor.close()

        return score
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
