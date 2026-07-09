package com.vdub.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.vdub.domain.entity.AppSettings
import com.vdub.domain.entity.ComputeBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
                    Log.i(TAG, "NNAPI requested; using default ONNX Runtime session options")
                }
            }
            ComputeBackend.GPU, ComputeBackend.CPU -> {
                Log.i(TAG, "Using ${settings.computeBackend} backend with default ONNX Runtime session options")
            }
        }

        if (settings.lowRamMode) {
            runCatching { options.setMemoryPatternOptimization(false) }
                .onFailure { Log.w(TAG, "Failed to disable memory pattern optimization", it) }
        }

        val session = environment.createSession(modelPath, options)
        sessions[modelId] = session
        Log.i(TAG, "Created session for $modelId")
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
    ): InferenceOutputs {
        val buffer = ByteBuffer.allocateDirect(inputArray.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(inputArray)

        val tensor = OnnxTensor.createTensor(environment, buffer, shape)
        return try {
            InferenceOutputs(session.run(mapOf(inputName to tensor)))
        } finally {
            tensor.close()
        }
    }

    /**
     * Run inference with pre-constructed tensor inputs.
     */
    fun runInferenceSync(
        session: OrtSession,
        inputs: Map<String, OnnxTensor>
    ): InferenceOutputs {
        return InferenceOutputs(session.run(inputs))
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

    class InferenceOutputs(
        private val result: OrtSession.Result
    ) : AutoCloseable {
        private val tensors: Map<String, OnnxTensor> = result.associate { entry ->
            entry.key to (entry.value as OnnxTensor)
        }

        val keys: Set<String>
            get() = tensors.keys

        val values: Collection<OnnxTensor>
            get() = tensors.values

        operator fun get(name: String): OnnxTensor? = tensors[name]

        override fun close() {
            result.close()
        }
    }

    companion object {
        private const val TAG = "ONNXManager"
    }
}
