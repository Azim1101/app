package com.vdub.database.entity

import androidx.room.*

@Entity(
    tableName = "speakers",
    foreignKeys = [ForeignKey(
        entity = AnalysisEntity::class,
        parentColumns = ["id"],
        childColumns = ["analysisId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["analysisId"]), Index(value = ["label"])]
)
data class SpeakerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisId: Long = 0,
    val label: String,
    val clusterId: Int,
    val totalDurationMs: Long = 0,
    val segmentCount: Int = 0,
    val averageConfidence: Float = 0f,
    val averageEmbeddingJson: String = "[]",
    val color: Long = 0xFF2196F3
)
