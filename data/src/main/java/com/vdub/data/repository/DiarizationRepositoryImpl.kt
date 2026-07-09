package com.vdub.data.repository

import android.content.Context
import android.util.Log
import com.vdub.domain.entity.*
import com.vdub.domain.repository.DiarizationRepository
import com.vdub.domain.repository.HistoryRepository
import com.vdub.domain.repository.ModelRepository
import com.vdub.media.AudioReader
import com.vdub.media.AudioExtractor
import com.vdub.media.WaveformGenerator
import com.vdub.ml.EmbeddingModel
import com.vdub.ml.SegmentationModel
import com.vdub.ml.SpeakerClustering
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiarizationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val segmentationModel: SegmentationModel,
    private val embeddingModel: EmbeddingModel,
    private val speakerClustering: SpeakerClustering,
    private val modelRepository: ModelRepository,
    private val mediaRepository: com.vdub.domain.repository.MediaRepository,
    private val historyRepository: HistoryRepository,
    private val audioReader: AudioReader,
    private val audioExtractor: AudioExtractor,
    private val waveformGenerator: WaveformGenerator
) : DiarizationRepository {

    companion object {
        private const val TAG = "DiarizationRepo"
    }

    private val _currentResult = MutableStateFlow<AnalysisResult?>(null)
    private val _progress = MutableStateFlow(ProcessingProgress())
    private var processingJob: Job? = null
    private var isPaused = false
    private var isCancelled = false

    private val workDir: File
        get() = File(context.cacheDir, "processing").also { it.mkdirs() }

    override fun processMedia(filePath: String, mediaType: MediaType): Flow<ProcessingProgress> = flow {
        isPaused = false
        isCancelled = false
        val startTime = System.currentTimeMillis()
        val logMessages = mutableListOf<String>()

        fun log(msg: String) {
            Log.d(TAG, msg)
            logMessages.add(msg)
        }

        fun updateProgress(block: ProcessingProgress.() -> ProcessingProgress) {
            _progress.value = _progress.value.block()
        }

        try {
            val file = File(filePath)
            log("Processing: ${file.name}")

            _currentResult.value = AnalysisResult(
                fileName = file.name,
                filePath = filePath,
                fileSize = file.size,
                mediaType = mediaType,
                status = AnalysisStatus.PENDING
            )

            // Step 1: Extract audio if video
            updateProgress { copy(status = AnalysisStatus.EXTRACTING_AUDIO, currentStep = "Extracting audio...", progress = 0.05f) }
            emit(_progress.value)

            val audioPath = if (mediaType == MediaType.VIDEO) {
                val result = audioExtractor.extractAudio(filePath)
                result.getOrElse { throw Exception("Audio extraction failed: ${it.message}") }
            } else if (!filePath.endsWith(".wav")) {
                val result = audioExtractor.convertToWav(filePath)
                result.getOrElse { throw Exception("WAV conversion failed: ${it.message}") }
            } else {
                filePath
            }
            log("Audio path: $audioPath")

            // Step 2: Resample to 16kHz mono WAV
            updateProgress { copy(status = AnalysisStatus.RESAMPLING, currentStep = "Resampling audio...", progress = 0.15f) }
            emit(_progress.value)

            val wavPath = if (!audioPath.endsWith(".wav")) {
                audioPath
            } else {
                // Verify it's 16kHz mono
                val result = audioExtractor.convertToWav(audioPath, 16000, 1)
                result.getOrElse { audioPath }
            }
            log("WAV path: $wavPath")

            // Step 3: Read audio data
            updateProgress { copy(status = AnalysisStatus.SEGMENTING, currentStep = "Reading audio...", progress = 0.2f) }
            emit(_progress.value)

            val audioData = audioReader.readWav(wavPath)
            log("Audio: ${audioData.totalSamples} samples, ${audioData.sampleRate}Hz, ${audioData.durationMs}ms")

            _currentResult.value = _currentResult.value?.copy(
                durationMs = audioData.durationMs,
                sampleRate = audioData.sampleRate,
                channels = audioData.channels
            )

            // Step 4: Initialize models
            log("Initializing segmentation model...")
            val segModelPath = modelRepository.getModelPath("pyannote-segmentation-3.0")
                ?: throw Exception("Segmentation model not downloaded. Please download it first.")
            segmentationModel.initialize(segModelPath)

            log("Initializing embedding model...")
            val embModelPath = modelRepository.getModelPath("speechbrain-ecapa")
                ?: throw Exception("Embedding model not downloaded. Please download it first.")
            embeddingModel.initialize(embModelPath)

            // Step 5: Segmentation (streaming)
            updateProgress { copy(status = AnalysisStatus.SEGMENTING, currentStep = "Detecting speech segments...", progress = 0.3f) }
            emit(_progress.value)

            val segments = mutableListOf<Segment>()
            segmentationModel.processStream(audioData.samples, audioData.sampleRate) { segment ->
                if (isCancelled) return@processStream
                while (isPaused) delay(100)

                segments.add(segment)
                log("Segment: ${segment.speakerLabel} ${segment.formatTimestamp()} conf=${String.format("%.2f", segment.confidence)}")

                updateProgress {
                    copy(
                        segmentsDetected = segments.size,
                        currentSpeaker = segment.speakerLabel,
                        latestSegment = segment,
                        elapsedTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            }

            log("Segmentation complete: ${segments.size} segments")

            // Step 6: Generate embeddings
            updateProgress { copy(status = AnalysisStatus.GENERATING_EMBEDDINGS, currentStep = "Generating speaker embeddings...", progress = 0.6f) }
            emit(_progress.value)

            val segmentsWithEmbeddings = mutableListOf<Segment>()
            segments.forEachIndexed { index, segment ->
                if (isCancelled) return@forEachIndexed
                while (isPaused) delay(100)

                val startSample = ((segment.startTimeMs * audioData.sampleRate) / 1000).toInt().coerceAtLeast(0)
                val endSample = ((segment.endTimeMs * audioData.sampleRate) / 1000).toInt().coerceAtMost(audioData.samples.size)

                if (endSample > startSample && (endSample - startSample) >= audioData.sampleRate / 10) {
                    val chunkAudio = audioData.samples.copyOfRange(startSample, endSample)
                    val embedding = embeddingModel.generateEmbedding(chunkAudio, audioData.sampleRate)
                    segmentsWithEmbeddings.add(segment.copy(embedding = embedding.toList()))
                } else {
                    segmentsWithEmbeddings.add(segment)
                }

                updateProgress {
                    copy(
                        progress = 0.6f + (0.2f * (index + 1) / segments.size),
                        currentStep = "Embedding ${index + 1}/${segments.size}"
                    )
                }
            }

            log("Embeddings generated: ${segmentsWithEmbeddings.count { it.embedding.isNotEmpty() }}")

            // Step 7: Speaker clustering
            updateProgress { copy(status = AnalysisStatus.CLUSTERING, currentStep = "Clustering speakers...", progress = 0.85f) }
            emit(_progress.value)

            val (finalSegments, speakers) = speakerClustering.cluster(
                segments = segmentsWithEmbeddings,
                threshold = 0.7f,
                maxSpeakers = 20,
                autoSpeakerCount = true
            )

            log("Clustering complete: ${speakers.size} speakers")

            // Step 8: Compute statistics
            val totalSpeechDuration = finalSegments.sumOf { it.durationMs }
            val totalDuration = audioData.durationMs
            val silenceDuration = totalDuration - totalSpeechDuration
            val longestSpeaker = speakers.maxByOrNull { it.totalDurationMs }

            val result = AnalysisResult(
                fileName = file.name,
                filePath = filePath,
                fileSize = file.length(),
                mediaType = mediaType,
                durationMs = totalDuration,
                speechDurationMs = totalSpeechDuration,
                silenceDurationMs = silenceDuration,
                speakerCount = speakers.size,
                segmentCount = finalSegments.size,
                averageConfidence = if (finalSegments.isNotEmpty()) finalSegments.map { it.confidence }.average().toFloat() else 0f,
                embeddingCount = finalSegments.count { it.embedding.isNotEmpty() },
                longestSpeakerLabel = longestSpeaker?.label ?: "",
                longestSpeakerDurationMs = longestSpeaker?.totalDurationMs ?: 0,
                averageSegmentLengthMs = if (finalSegments.isNotEmpty()) totalSpeechDuration / finalSegments.size else 0,
                createdAt = System.currentTimeMillis(),
                completedAt = System.currentTimeMillis(),
                status = AnalysisStatus.COMPLETED,
                segments = finalSegments,
                speakers = speakers,
                sampleRate = audioData.sampleRate,
                channels = audioData.channels,
                modelUsed = "pyannote-3.0 + ecapa",
                processingTimeMs = System.currentTimeMillis() - startTime
            )

            _currentResult.value = result

            // Save to history
            historyRepository.saveResult(result)

            updateProgress {
                copy(
                    status = AnalysisStatus.COMPLETED,
                    progress = 1f,
                    currentStep = "Complete!",
                    elapsedTimeMs = System.currentTimeMillis() - startTime
                )
            }
            emit(_progress.value)

        } catch (e: CancellationException) {
            updateProgress { copy(status = AnalysisStatus.CANCELLED, currentStep = "Cancelled") }
            emit(_progress.value)
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            updateProgress { copy(status = AnalysisStatus.FAILED, currentStep = "Error: ${e.message}") }
            emit(_progress.value)
        }
    }

    override fun getCurrentResult(): Flow<AnalysisResult?> = _currentResult.asStateFlow()

    override suspend fun cancelAnalysis() {
        isCancelled = true
        processingJob?.cancel()
    }

    override suspend fun pauseAnalysis() {
        isPaused = true
        _progress.value = _progress.value.copy(isPaused = true)
    }

    override suspend fun resumeAnalysis() {
        isPaused = false
        _progress.value = _progress.value.copy(isPaused = false)
    }

    override suspend fun renameSpeaker(oldLabel: String, newLabel: String): Result<Unit> = runCatching {
        val result = _currentResult.value ?: throw IllegalStateException("No active result")
        val updatedSegments = result.segments.map { seg ->
            if (seg.speakerLabel == oldLabel) seg.copy(speakerLabel = newLabel) else seg
        }
        val updatedSpeakers = result.speakers.map { spk ->
            if (spk.label == oldLabel) spk.copy(label = newLabel) else spk
        }
        _currentResult.value = result.copy(segments = updatedSegments, speakers = updatedSpeakers)
    }

    override suspend fun mergeSpeakers(primaryLabel: String, secondaryLabel: String): Result<Unit> = runCatching {
        val result = _currentResult.value ?: throw IllegalStateException("No active result")
        val (updatedSegments, updatedSpeakers) = speakerClustering.mergeSpeakers(
            result.segments, result.speakers, primaryLabel, secondaryLabel
        )
        _currentResult.value = result.copy(
            segments = updatedSegments,
            speakers = updatedSpeakers,
            speakerCount = updatedSpeakers.size
        )
    }

    override suspend fun deleteSpeaker(label: String): Result<Unit> = runCatching {
        val result = _currentResult.value ?: throw IllegalStateException("No active result")
        _currentResult.value = result.copy(
            segments = result.segments.filter { it.speakerLabel != label },
            speakers = result.speakers.filter { it.label != label },
            speakerCount = result.speakers.size - 1
        )
    }

    override suspend fun recluster(threshold: Float, maxSpeakers: Int): Result<Unit> = runCatching {
        val result = _currentResult.value ?: throw IllegalStateException("No active result")
        val (updatedSegments, updatedSpeakers) = speakerClustering.recluster(
            result.segments, threshold, maxSpeakers
        )
        _currentResult.value = result.copy(
            segments = updatedSegments,
            speakers = updatedSpeakers,
            speakerCount = updatedSpeakers.size
        )
    }

    override suspend fun exportResult(result: AnalysisResult, format: ExportFormat): Result<String> = runCatching {
        val exportDir = File(context.filesDir, "exports").also { it.mkdirs() }
        val fileName = result.fileName.substringBeforeLast('.') + ".${format.extension}"
        val outputFile = File(exportDir, fileName)

        val content = when (format) {
            ExportFormat.TXT -> exportAsTxt(result)
            ExportFormat.CSV -> exportAsCsv(result)
            ExportFormat.JSON -> exportAsJson(result)
            ExportFormat.SRT -> exportAsSrt(result)
            ExportFormat.VTT -> exportAsVtt(result)
            ExportFormat.RTTM -> exportAsRttm(result)
        }

        outputFile.writeText(content)
        outputFile.absolutePath
    }

    private fun exportAsTxt(result: AnalysisResult): String = buildString {
        appendLine("VDub Speaker Diarization Report")
        appendLine("File: ${result.fileName}")
        appendLine("Duration: ${result.durationMs / 1000.0}s")
        appendLine("Speakers: ${result.speakerCount}")
        appendLine("Segments: ${result.segmentCount}")
        appendLine("---")
        result.segments.forEach { seg ->
            appendLine("${seg.speakerLabel} ${seg.formatTimestamp()}")
        }
    }

    private fun exportAsCsv(result: AnalysisResult): String = buildString {
        appendLine("speaker,start_ms,end_ms,duration_ms,confidence")
        result.segments.forEach { seg ->
            appendLine("${seg.speakerLabel},${seg.startTimeMs},${seg.endTimeMs},${seg.durationMs},${String.format("%.3f", seg.confidence)}")
        }
    }

    private fun exportAsJson(result: AnalysisResult): String = buildString {
        append("{")
        append("\"file\":\"${result.fileName}\",")
        append("\"duration_ms\":${result.durationMs},")
        append("\"speaker_count\":${result.speakerCount},")
        append("\"segment_count\":${result.segmentCount},")
        append("\"speech_duration_ms\":${result.speechDurationMs},")
        append("\"silence_duration_ms\":${result.silenceDurationMs},")
        append("\"segments\":[")
        result.segments.forEachIndexed { i, seg ->
            if (i > 0) append(",")
            append("{")
            append("\"speaker\":\"${seg.speakerLabel}\",")
            append("\"start_ms\":${seg.startTimeMs},")
            append("\"end_ms\":${seg.endTimeMs},")
            append("\"duration_ms\":${seg.durationMs},")
            append("\"confidence\":${String.format("%.3f", seg.confidence)},")
            append("\"cluster_id\":${seg.clusterId}")
            append("}")
        }
        append("],")
        append("\"speakers\":[")
        result.speakers.forEachIndexed { i, spk ->
            if (i > 0) append(",")
            append("{")
            append("\"label\":\"${spk.label}\",")
            append("\"cluster_id\":${spk.clusterId},")
            append("\"duration_ms\":${spk.totalDurationMs},")
            append("\"segments\":${spk.segmentCount}")
            append("}")
        }
        append("]}")
    }

    private fun exportAsSrt(result: AnalysisResult): String = buildString {
        result.segments.forEachIndexed { index, seg ->
            val num = index + 1
            val start = formatSrtTime(seg.startTimeMs)
            val end = formatSrtTime(seg.endTimeMs)
            appendLine(num.toString())
            appendLine("$start --> $end")
            appendLine(seg.speakerLabel)
            appendLine()
        }
    }

    private fun exportAsVtt(result: AnalysisResult): String = buildString {
        appendLine("WEBVTT")
        appendLine()
        result.segments.forEach { seg ->
            val start = formatVttTime(seg.startTimeMs)
            val end = formatVttTime(seg.endTimeMs)
            appendLine("$start --> $end")
            appendLine("<v ${seg.speakerLabel}>${seg.speakerLabel}")
            appendLine()
        }
    }

    private fun exportAsRttm(result: AnalysisResult): String = buildString {
        val fileName = result.fileName.substringBeforeLast('.')
        result.segments.forEach { seg ->
            val start = seg.startTimeSec
            val duration = seg.durationSec
            appendLine("SPEAKER $fileName 1 $start $duration <NA> <NA> ${seg.speakerLabel} <NA> <NA>")
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val mil = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", h, m, s, mil)
    }

    private fun formatVttTime(ms: Long): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val mil = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", h, m, s, mil)
    }
}
