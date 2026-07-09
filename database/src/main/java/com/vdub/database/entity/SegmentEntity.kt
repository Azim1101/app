package com.vdub.database.entity

import androidx.room.*

@Entity(
    tableName = "segments",
    foreignKeys = [ForeignKey(
        entity = AnalysisEntity::class,
        parentColumns = ["id"],
        childColumns = ["analysisId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["analysisId"]), Index(value = ["speakerLabel"])]
)
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisId: Long = 0,
    val speakerLabel: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 0f,
    val clusterId: Int = -1,
    val embeddingJson: String = "[]"
)
