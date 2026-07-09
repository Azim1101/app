package com.vdub.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.util.Log
import com.vdub.domain.entity.Segment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pyannote Segmentation 3.0 ONNX model wrapper.
 * Performs speech activity detection, segmentation, and timestamp generation.
 * Supports streaming: segments are emitted as soon as they are detected.
 */
@Singleton
class SegmentationModel @Inject constructor(
    private val onnxManager: ONNXRuntimeManager
) {
    companion object {
        private const val TAG = "SegmentationModel"
        private const val MODEL_ID = "pyannote-segmentation-3.0"
        private const val SAMPLE_RATE = 16000
        private const val CHUNK_DURATION_SEC = 10.0
        private const val STEP_DURATION_SEC = 2.5
        private const val CHUNK_SAMPLES = (SAMPLE_RATE * CHUNK_DURATION_SEC).toInt()
        private const val STEP_SAMPLES = (SAMPLE_RATE * STEP_DURATION_SEC).toInt()
        private const val SPEECH_THRESHOLD = 0.5f
    }

    private var session: OrtSession? = null
    private var inputName: String = "waveform"

    /**
     * Initialize the segmentation model.
     */
    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val file = File(modelPath)
            require(file.exists()) { "Segmentation model not found at $modelPath" }

            val result = onnxManager.getSession(MODEL_ID, modelPath)
            result.onSuccess { sess ->
                session = sess
                inputName = onnxManager.getInputNames(sess).firstOrNull() ?: "waveform"
                Log.i(TAG, "Segmentation model loaded. Input: $inputName")
            }.onFailure { e ->
                throw e
            }
        }
    }

    /**
     * Process audio data in a streaming fashion, emitting segments as they are detected.
     */
    suspend fun processStream(
        audioData: FloatArray,
        sampleRate: Int,
        emitter: FlowCollector<Segment>
    ): Unit = withContext(Dispatchers.Default) {
        val sess = session ?: throw IllegalStateException("Segmentation model not initialized")

        // Resample if needed (we expect 16kHz)
        val samples = if (sampleRate != SAMPLE_RATE) {
            resampleAudio(audioData, sampleRate, SAMPLE_RATE)
        } else {
            audioData
        }

        val totalSamples = samples.size
        var offset = 0
        var segmentIndex = 0L

        while (offset + CHUNK_SAMPLES <= totalSamples) {
            val chunk = samples.copyOfRange(offset, offset + CHUNK_SAMPLES)

            // Prepare input tensor: shape [1, chunk_samples]
            val shape = longArrayOf(1, chunk.size.toLong())
            val buffer = ByteBuffer.allocateDirect(chunk.size * 4)
            buffer.order(ByteOrder.nativeOrder())
            buffer.asFloatBuffer().put(chunk)

            val inputTensor = OnnxTensor.createTensor(onnxManager.environment, buffer, shape)

            var results: Map<String, OnnxTensor>? = null
            try {
                results = onnxManager.runInference(sess, mapOf(inputName to inputTensor))
            } catch (e: Exception) {
                // If [1, samples] fails, try [1, 1, samples]
                inputTensor.close()
                val buffer2 = ByteBuffer.allocateDirect(chunk.size * 4)
                buffer2.order(ByteOrder.nativeOrder())
                buffer2.asFloatBuffer().put(chunk)
                val shape2 = longArrayOf(1, 1, chunk.size.toLong())
                val tensor2 = OnnxTensor.createTensor(onnxManager.environment, buffer2, shape2)
                try {
                    results = onnxManager.runInference(sess, mapOf(inputName to tensor2))
                } catch (e2: Exception) {
                    Log.e(TAG, "Inference failed with both shapes", e2)
                }
                tensor2.close()
            }

            inputTensor.close()

            if (results == null) {
                offset += STEP_SAMPLES
                continue
            }

            // Parse output: shape [1, num_frames, num_speakers]
            val outputKey = results.keys.firstOrNull()
            val outputTensor = if (outputKey != null) results[outputKey] else null

            if (outputTensor != null) {
                try {
                    val outputBuffer = outputTensor.floatBuffer
                    val outputShape = outputTensor.info.shape
                    val numFrames = if (outputShape.size >= 2) outputShape[1].toInt() else 1
                    val numSpeakers = if (outputShape.size >= 3) outputShape[2].toInt() else 1

                    val frameDurationMs = (CHUNK_DURATION_SEC * 1000.0 / numFrames).toLong()
                    val chunkStartMs = (offset * 1000L) / SAMPLE_RATE

                    // Parse segments from the output matrix
                    var currentSpeaker = -1
                    var segStartMs = 0L
                    var maxConfidence = 0f

                    for (frame in 0 until numFrames) {
                        var bestSpeaker = -1
                        var bestScore = 0f

                        for (spk in 0 until numSpeakers) {
                            val idx = frame * numSpeakers + spk
                            val score = if (idx < outputBuffer.capacity()) outputBuffer.get(idx) else 0f
                            if (score > bestScore && score > SPEECH_THRESHOLD) {
                                bestScore = score
                                bestSpeaker = spk
                            }
                        }

                        val frameMs = chunkStartMs + frame * frameDurationMs

                        if (bestSpeaker != currentSpeaker) {
                            // Emit previous segment if it exists
                            if (currentSpeaker >= 0 && maxConfidence > SPEECH_THRESHOLD) {
                                val segment = Segment(
                                    id = segmentIndex++,
                                    speakerLabel = "Speaker ${currentSpeaker + 1}",
                                    startTimeMs = segStartMs,
                                    endTimeMs = frameMs,
                                    confidence = maxConfidence,
                                    clusterId = currentSpeaker
                                )
                                emitter.emit(segment)
                            }

                            // Start new segment
                            currentSpeaker = bestSpeaker
                            segStartMs = frameMs
                            maxConfidence = bestScore
                        } else {
                            maxConfidence = maxOf(maxConfidence, bestScore)
                        }
                    }

                    // Emit last segment in chunk
                    if (currentSpeaker >= 0 && maxConfidence > SPEECH_THRESHOLD) {
                        val endMs = chunkStartMs + numFrames * frameDurationMs
                        val segment = Segment(
                            id = segmentIndex++,
                            speakerLabel = "Speaker ${currentSpeaker + 1}",
                            startTimeMs = segStartMs,
                            endTimeMs = endMs,
                            confidence = maxConfidence,
                            clusterId = currentSpeaker
                        )
                        emitter.emit(segment)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse segmentation output", e)
                }
            }

            results.values.forEach { it.close() }
            offset += STEP_SAMPLES
        }
    }

    /**
     * Process entire audio at once (non-streaming mode).
     */
    suspend fun processFull(
        audioData: FloatArray,
        sampleRate: Int
    ): List<Segment> = withContext(Dispatchers.Default) {
        val segments = mutableListOf<Segment>()
        processStream(audioData, sampleRate) { segment ->
            segments.add(segment)
        }
        mergeOverlappingSegments(segments)
    }

    /**
     * Merge overlapping or adjacent segments of the same speaker.
     */
    private fun mergeOverlappingSegments(segments: List<Segment>): List<Segment> {
        if (segments.isEmpty()) return emptyList()

        val sorted = segments.sortedBy { it.startTimeMs }
        val merged = mutableListOf<Segment>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.clusterId == current.clusterId && next.startTimeMs <= current.endTimeMs + 200) {
                current = current.copy(
                    endTimeMs = maxOf(current.endTimeMs, next.endTimeMs),
                    confidence = maxOf(current.confidence, next.confidence)
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        return merged.mapIndexed { index, seg -> seg.copy(id = index.toLong()) }
    }

    /**
     * Simple linear interpolation resampling.
     */
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

    /**
     * Release model resources.
     */
    fun close() {
        session = null
        onnxManager.closeSession(MODEL_ID)
    }
}
