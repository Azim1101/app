package com.vdub.database.mapper

import com.vdub.database.entity.AnalysisEntity
import com.vdub.database.entity.SegmentEntity
import com.vdub.database.entity.SpeakerEntity
import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.entity.AnalysisStatus
import com.vdub.domain.entity.MediaType
import com.vdub.domain.entity.Segment
import com.vdub.domain.entity.Speaker
import org.json.JSONArray

/**
 * Convert domain AnalysisResult to database entity.
 */
fun AnalysisResult.toEntity(audioPath: String = ""): AnalysisEntity = AnalysisEntity(
    id = if (id > 0) id else 0,
    fileName = fileName,
    filePath = filePath,
    fileSize = fileSize,
    mediaType = mediaType.name,
    durationMs = durationMs,
    speechDurationMs = speechDurationMs,
    silenceDurationMs = silenceDurationMs,
    speakerCount = speakerCount,
    segmentCount = segmentCount,
    averageConfidence = averageConfidence,
    embeddingCount = embeddingCount,
    longestSpeakerLabel = longestSpeakerLabel,
    longestSpeakerDurationMs = longestSpeakerDurationMs,
    averageSegmentLengthMs = averageSegmentLengthMs,
    createdAt = createdAt,
    completedAt = completedAt,
    status = status.name,
    sampleRate = sampleRate,
    channels = channels,
    bitrate = bitrate,
    modelUsed = modelUsed,
    processingTimeMs = processingTimeMs,
    audioPath = audioPath
)

/**
 * Convert database entity to domain AnalysisResult.
 */
fun AnalysisEntity.toDomain(segments: List<Segment> = emptyList(), speakers: List<Speaker> = emptyList()): AnalysisResult = AnalysisResult(
    id = id,
    fileName = fileName,
    filePath = filePath,
    fileSize = fileSize,
    mediaType = try { MediaType.valueOf(mediaType) } catch (_: Exception) { MediaType.AUDIO },
    durationMs = durationMs,
    speechDurationMs = speechDurationMs,
    silenceDurationMs = silenceDurationMs,
    speakerCount = speakerCount,
    segmentCount = segmentCount,
    averageConfidence = averageConfidence,
    embeddingCount = embeddingCount,
    longestSpeakerLabel = longestSpeakerLabel,
    longestSpeakerDurationMs = longestSpeakerDurationMs,
    averageSegmentLengthMs = averageSegmentLengthMs,
    createdAt = createdAt,
    completedAt = completedAt,
    status = try { AnalysisStatus.valueOf(status) } catch (_: Exception) { AnalysisStatus.FAILED },
    segments = segments,
    speakers = speakers,
    sampleRate = sampleRate,
    channels = channels,
    bitrate = bitrate,
    modelUsed = modelUsed,
    processingTimeMs = processingTimeMs
)

/**
 * Convert domain Segment to database entity.
 */
fun Segment.toEntity(): SegmentEntity = SegmentEntity(
    id = if (id > 0) id else 0,
    analysisId = analysisId,
    speakerLabel = speakerLabel,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    confidence = confidence,
    clusterId = clusterId,
    embeddingJson = embeddingToJson(embedding)
)

/**
 * Convert database entity to domain Segment.
 */
fun SegmentEntity.toDomain(): Segment = Segment(
    id = id,
    analysisId = analysisId,
    speakerLabel = speakerLabel,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    confidence = confidence,
    clusterId = clusterId,
    embedding = jsonToEmbedding(embeddingJson)
)

/**
 * Convert domain Speaker to database entity.
 */
fun Speaker.toEntity(): SpeakerEntity = SpeakerEntity(
    id = if (id > 0) id else 0,
    analysisId = analysisId,
    label = label,
    clusterId = clusterId,
    totalDurationMs = totalDurationMs,
    segmentCount = segmentCount,
    averageConfidence = averageConfidence,
    averageEmbeddingJson = embeddingToJson(averageEmbedding),
    color = color
)

/**
 * Convert database entity to domain Speaker.
 */
fun SpeakerEntity.toDomain(): Speaker = Speaker(
    id = id,
    analysisId = analysisId,
    label = label,
    clusterId = clusterId,
    totalDurationMs = totalDurationMs,
    segmentCount = segmentCount,
    averageConfidence = averageConfidence,
    averageEmbedding = jsonToEmbedding(averageEmbeddingJson),
    color = color
)

private fun embeddingToJson(embedding: List<Float>): String {
    val arr = JSONArray()
    embedding.forEach { arr.put(it) }
    return arr.toString()
}

private fun jsonToEmbedding(json: String): List<Float> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getDouble(it).toFloat() }
    } catch (_: Exception) {
        emptyList()
    }
}
