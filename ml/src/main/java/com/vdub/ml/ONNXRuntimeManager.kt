package com.vdub.ml

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import com.vdub.domain.entity.AppSettings
import com.vdub.domain.entity.ComputeBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ONNX Runtime sessions and inference execution.
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
                        Log.w("ONNXManager", "NNAPI not available", e)
                    }
                }
            }
            ComputeBackend.GPU, ComputeBackend.CPU -> {
                try {
                    options.addXnnpack(emptyMap())
                } catch (e: Exception) {
                    Log.w("ONNXManager", "XNNPACK not available", e)
                }
            }
        }

        if (settings.lowRamMode) {
            try {
                options.setMemoryPatternOptimization(false)
            } catch (_: Exception) { }
        }

        val session = environment.createSession(modelPath, options)
        sessions[modelId] = session
        Log.i("ONNXManager", "Created session for $modelId")
        session
    }

    /**
     * Run inference with float array input.
     */
    fun runInferenceSync(
        session: OrtSession,
        inputName: String,
        inputArray: FloatArray,
        shape: LongArray
    ): Map<String, OnnxTensor> {
        val buffer = ByteBuffer.allocateDirect(inputArray.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(inputArray)

        val tensor = OnnxTensor.createTensor(environment, buffer, shape)
        val result = session.run(mapOf(inputName to tensor))
        tensor.close()
        return result
    }

    /**
     * Run inference with pre-constructed tensor inputs.
     */
    fun runInferenceSync(
        session: OrtSession,
        inputs: Map<String, OnnxTensor>
    ): Map<String, OnnxTensor> {
        val result = session.run(inputs)
        return result
    }

    fun getInputNames(session: OrtSession): List<String> = session.inputNames.toList()
    fun getOutputNames(session: OrtSession): List<String> = session.outputNames.toList()

    @Synchronized
    fun closeSession(modelId: String) {
        sessions.remove(modelId)?.close()
    }

    @Synchronized
    fun closeAll() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }
}
