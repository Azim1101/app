package com.vdub.ml

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import com.vdub.domain.entity.AppSettings
import com.vdub.domain.entity.ComputeBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ONNX Runtime sessions and inference execution.
 * Supports CPU, NNAPI, and GPU delegates.
 */
@Singleton
class ONNXRuntimeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val environment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val sessions = mutableMapOf<String, OrtSession>()

    private var settings = AppSettings()

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
    }

    /**
     * Create or get an ONNX session for a model file.
     */
    @Synchronized
    fun getSession(modelId: String, modelPath: String): Result<OrtSession> = runCatching {
        sessions[modelId]?.let { return@runCatching it }

        val file = File(modelPath)
        if (!file.exists()) throw IllegalStateException("Model file not found: $modelPath")

        val options = OrtSession.SessionOptions()

        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        options.setInterOpNumThreads(settings.threadCount)
        options.setIntraOpNumThreads(settings.threadCount)

        when (settings.computeBackend) {
            ComputeBackend.NNAPI -> {
                if (settings.nnapiEnabled) {
                    try {
                        options.addNnapi(EnumSet.of(NnapiFlags.USE_FP16))
                    } catch (e: Exception) {
                        Log.w("ONNXManager", "NNAPI not available, falling back to CPU", e)
                    }
                }
            }
            ComputeBackend.GPU -> {
                if (settings.gpuDelegateEnabled) {
                    try {
                        options.addXnnpack(
                            mutableMapOf<String, String>(
                                "intra_op_num_threads" to settings.threadCount.toString()
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("ONNXManager", "XNNPACK not available, falling back to CPU", e)
                    }
                }
            }
            ComputeBackend.CPU -> {
                try {
                    options.addXnnpack(
                        mutableMapOf<String, String>(
                            "intra_op_num_threads" to settings.threadCount.toString()
                        )
                    )
                } catch (e: Exception) {
                    Log.w("ONNXManager", "XNNPACK not available, using default CPU", e)
                }
            }
        }

        if (settings.lowRamMode) {
            try {
                options.setMemoryPatternOptimization(false)
                options.setCPUArenaAllocator(false)
            } catch (e: Exception) {
                Log.w("ONNXManager", "Memory optimization options not supported", e)
            }
        }

        val session = environment.createSession(modelPath, options)
        sessions[modelId] = session
        Log.i("ONNXManager", "Created session for $modelId from $modelPath")
        session
    }

    /**
     * Run inference on a session with float array input.
     */
    suspend fun runInference(
        session: OrtSession,
        inputName: String,
        inputArray: FloatArray,
        shape: LongArray
    ): Map<String, OnnxTensor> = withContext(Dispatchers.Default) {
        val buffer = ByteBuffer.allocateDirect(inputArray.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(inputArray)

        val tensor = OnnxTensor.createTensor(environment, buffer, shape)
        val results = session.run(mapOf(inputName to tensor))
        tensor.close()

        results.mapKeys { it.key }
    }

    /**
     * Run inference with pre-constructed OnnxTensor.
     */
    suspend fun runInference(
        session: OrtSession,
        inputs: Map<String, OnnxTensor>
    ): Map<String, OnnxTensor> = withContext(Dispatchers.Default) {
        session.run(inputs)
    }

    /**
     * Run inference with long array input (for token IDs).
     */
    suspend fun runInferenceLong(
        session: OrtSession,
        inputName: String,
        inputArray: LongArray,
        shape: LongArray
    ): Map<String, OnnxTensor> = withContext(Dispatchers.Default) {
        val buffer = ByteBuffer.allocateDirect(inputArray.size * 8)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asLongBuffer().put(inputArray)

        val tensor = OnnxTensor.createTensor(environment, buffer, shape)
        val results = session.run(mapOf(inputName to tensor))
        tensor.close()

        results
    }

    /**
     * Get input names for a session.
     */
    fun getInputNames(session: OrtSession): List<String> {
        return session.inputNames.toList()
    }

    /**
     * Get output names for a session.
     */
    fun getOutputNames(session: OrtSession): List<String> {
        return session.outputNames.toList()
    }

    /**
     * Close and remove a session.
     */
    @Synchronized
    fun closeSession(modelId: String) {
        sessions.remove(modelId)?.close()
    }

    /**
     * Close all sessions and release resources.
     */
    @Synchronized
    fun closeAll() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    /**
     * Get memory info for diagnostics.
     */
    fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)
        return "Used: ${usedMem}MB / Max: ${maxMem}MB, Sessions: ${sessions.size}"
    }
}
