package com.vdub.ml

import android.util.Log
import com.vdub.domain.entity.Segment
import com.vdub.domain.entity.Speaker
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Agglomerative clustering for speaker diarization.
 * Uses cosine similarity between speaker embeddings to cluster segments into speakers.
 * Supports dynamic threshold and automatic speaker count estimation.
 */
@Singleton
class SpeakerClustering @Inject constructor() {

    companion object {
        private const val TAG = "SpeakerClustering"
        private const val DEFAULT_THRESHOLD = 0.7f
        private const val MIN_CLUSTER_SIZE = 2
        private const val STOP_THRESHOLD = 0.3f
    }

    /**
     * Cluster segments into speakers using agglomerative clustering.
     * Segments must have embeddings generated.
     *
     * @param segments List of segments with embeddings
     * @param threshold Similarity threshold for merging clusters (0.0 - 1.0)
     * @param maxSpeakers Maximum number of speakers to detect
     * @param autoSpeakerCount Whether to automatically estimate speaker count
     * @return Pair of updated segments with cluster IDs and list of speakers
     */
    fun cluster(
        segments: List<Segment>,
        threshold: Float = DEFAULT_THRESHOLD,
        maxSpeakers: Int = 20,
        autoSpeakerCount: Boolean = true
    ): Pair<List<Segment>, List<Speaker>> {
        if (segments.isEmpty()) return Pair(emptyList(), emptyList())

        // Filter segments that have embeddings
        val segmentsWithEmbeddings = segments.filter { it.embedding.isNotEmpty() }
        if (segmentsWithEmbeddings.isEmpty()) {
            // If no embeddings, assign all to Speaker 1
            val updatedSegments = segments.map { it.copy(clusterId = 0, speakerLabel = "Speaker 1") }
            val speaker = createSpeaker(0, "Speaker 1", updatedSegments)
            return Pair(updatedSegments, listOf(speaker))
        }

        // Initialize: each segment is its own cluster
        val clusters = segmentsWithEmbeddings.mapIndexed { index, segment ->
            Cluster(
                id = index,
                segments = mutableListOf(segment),
                centroid = segment.embedding.toFloatArray()
            )
        }.toMutableList()

        // Agglomerative clustering
        while (clusters.size > 1) {
            // Check max speakers constraint
            if (!autoSpeakerCount && clusters.size <= maxSpeakers) break

            // Find most similar pair of clusters
            var bestSimilarity = -1f
            var bestI = -1
            var bestJ = -1

            for (i in clusters.indices) {
                for (j in i + 1 until clusters.size) {
                    val sim = cosineSimilarity(clusters[i].centroid, clusters[j].centroid)
                    if (sim > bestSimilarity) {
                        bestSimilarity = sim
                        bestI = i
                        bestJ = j
                    }
                }
            }

            // Stop if similarity is below threshold
            if (bestSimilarity < threshold) break

            // Also stop if we've reached a reasonable speaker count
            if (autoSpeakerCount && bestSimilarity < threshold * 1.2f && clusters.size <= estimateSpeakerCount(clusters)) break

            // Merge the two most similar clusters
            val merged = mergeClusters(clusters[bestI], clusters[bestJ])
            clusters.removeAt(bestJ)
            clusters.removeAt(bestI)
            clusters.add(merged)

            Log.d(TAG, "Merged clusters ${bestI} and ${bestJ} (similarity=${"%.3f".format(bestSimilarity)}), remaining=${clusters.size}")
        }

        // Assign final cluster IDs and create speaker labels
        val updatedSegments = segments.map { segment ->
            val clusterIndex = clusters.indexOfFirst { cluster ->
                cluster.segments.any { it.startTimeMs == segment.startTimeMs && it.endTimeMs == segment.endTimeMs }
            }
            if (clusterIndex >= 0) {
                segment.copy(
                    clusterId = clusterIndex,
                    speakerLabel = "Speaker ${clusterIndex + 1}"
                )
            } else {
                // Segments without embeddings get assigned to nearest cluster
                val nearestCluster = findNearestCluster(segment, clusters)
                segment.copy(
                    clusterId = nearestCluster,
                    speakerLabel = "Speaker ${nearestCluster + 1}"
                )
            }
        }

        val speakers = clusters.mapIndexed { index, cluster ->
            createSpeaker(index, "Speaker ${index + 1}", updatedSegments.filter { it.clusterId == index })
        }

        Log.i(TAG, "Clustering complete: ${clusters.size} speakers from ${segments.size} segments")
        return Pair(updatedSegments, speakers)
    }

    /**
     * Re-cluster with a new threshold.
     */
    fun recluster(
        segments: List<Segment>,
        threshold: Float,
        maxSpeakers: Int
    ): Pair<List<Segment>, List<Speaker>> {
        return cluster(segments, threshold, maxSpeakers, autoSpeakerCount = false)
    }

    /**
     * Merge two speakers.
     */
    fun mergeSpeakers(
        segments: List<Segment>,
        speakers: List<Speaker>,
        primaryLabel: String,
        secondaryLabel: String
    ): Pair<List<Segment>, List<Speaker>> {
        val primaryCluster = segments.find { it.speakerLabel == primaryLabel }?.clusterId ?: return Pair(segments, speakers)
        val secondaryCluster = segments.find { it.speakerLabel == secondaryLabel }?.clusterId ?: return Pair(segments, speakers)

        val updatedSegments = segments.map { segment ->
            if (segment.clusterId == secondaryCluster) {
                segment.copy(clusterId = primaryCluster, speakerLabel = primaryLabel)
            } else segment
        }

        val updatedSpeakers = speakers
            .filter { it.label != secondaryLabel }
            .map { speaker ->
                if (speaker.label == primaryLabel) {
                    createSpeaker(speaker.clusterId, primaryLabel, updatedSegments.filter { it.clusterId == primaryCluster })
                } else speaker
            }

        return Pair(updatedSegments, updatedSpeakers)
    }

    /**
     * Estimate optimal number of speakers using eigenvalue analysis of similarity matrix.
     */
    private fun estimateSpeakerCount(clusters: List<Cluster>): Int {
        if (clusters.size <= 2) return clusters.size

        // Build similarity matrix
        val n = clusters.size
        val simMatrix = Array(n) { FloatArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                simMatrix[i][j] = if (i == j) 1f else cosineSimilarity(clusters[i].centroid, clusters[j].centroid)
            }
        }

        // Simple eigengap heuristic: count eigenvalues > 0.5
        var count = 0
        for (i in 0 until n) {
            val rowSum = simMatrix[i].sum()
            if (rowSum / n > 0.5f) count++
        }

        return maxOf(2, minOf(count, n))
    }

    private fun mergeClusters(a: Cluster, b: Cluster): Cluster {
        val mergedSegments = (a.segments + b.segments).toMutableList()
        val totalSize = a.segments.size + b.segments.size

        // Compute weighted average centroid
        val centroid = FloatArray(a.centroid.size) { i ->
            (a.centroid[i] * a.segments.size + b.centroid[i] * b.segments.size) / totalSize
        }

        // L2 normalize centroid
        val norm = sqrt(centroid.fold(0f) { acc, v -> acc + v * v })
        if (norm > 0f) {
            for (i in centroid.indices) centroid[i] /= norm
        }

        return Cluster(id = minOf(a.id, b.id), segments = mergedSegments, centroid = centroid)
    }

    private fun findNearestCluster(segment: Segment, clusters: List<Cluster>): Int {
        if (segment.embedding.isEmpty()) return 0
        val embedding = segment.embedding.toFloatArray()
        var bestSim = -1f
        var bestCluster = 0
        clusters.forEachIndexed { index, cluster ->
            val sim = cosineSimilarity(embedding, cluster.centroid)
            if (sim > bestSim) {
                bestSim = sim
                bestCluster = index
            }
        }
        return bestCluster
    }

    private fun createSpeaker(clusterId: Int, label: String, segments: List<Segment>): Speaker {
        val totalDurationMs = segments.sumOf { it.durationMs }
        val avgConfidence = if (segments.isNotEmpty()) segments.map { it.confidence }.average().toFloat() else 0f
        val avgEmbedding = if (segments.isNotEmpty() && segments[0].embedding.isNotEmpty()) {
            val dim = segments[0].embedding.size
            FloatArray(dim) { i ->
                segments.mapNotNull { it.embedding.getOrNull(i) }.average().toFloat()
            }.toList()
        } else emptyList()

        val colors = longArrayOf(
            0xFF2196F3, 0xFFF44336, 0xFF4CAF50, 0xFFFF9800, 0xFF9C27B0,
            0xFF00BCD4, 0xFFFFEB3B, 0xFFE91E63, 0xFF8BC34A, 0xFF673AB7,
            0xFF03A9F4, 0xFFCDDC39, 0xFFC107FF, 0xFF009688, 0xFFFF5722,
            0xFF607D8B, 0xFF795548, 0xFF00E676, 0xFFFF6D00, 0xFFAA00FF
        )

        return Speaker(
            clusterId = clusterId,
            label = label,
            totalDurationMs = totalDurationMs,
            segmentCount = segments.size,
            averageConfidence = avgConfidence,
            averageEmbedding = avgEmbedding,
            color = colors[clusterId % colors.size]
        )
    }

    /**
     * Compute cosine similarity between two vectors.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0f) dotProduct / denominator else 0f
    }

    private data class Cluster(
        val id: Int,
        val segments: MutableList<Segment>,
        val centroid: FloatArray
    ) {
        override fun equals(other: Any?): Boolean = other is Cluster && id == other.id
        override fun hashCode(): Int = id
    }
}
