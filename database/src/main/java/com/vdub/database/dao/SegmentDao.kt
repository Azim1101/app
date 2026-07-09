package com.vdub.database.dao

import androidx.room.*
import com.vdub.database.entity.SegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {

    @Query("SELECT * FROM segments WHERE analysisId = :analysisId ORDER BY startTimeMs ASC")
    fun getSegmentsForAnalysis(analysisId: Long): Flow<List<SegmentEntity>>

    @Query("SELECT * FROM segments WHERE analysisId = :analysisId ORDER BY startTimeMs ASC")
    suspend fun getSegmentsForAnalysisSync(analysisId: Long): List<SegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<SegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: SegmentEntity): Long

    @Query("DELETE FROM segments WHERE analysisId = :analysisId")
    suspend fun deleteSegmentsForAnalysis(analysisId: Long)

    @Query("DELETE FROM segments WHERE speakerLabel = :label AND analysisId = :analysisId")
    suspend fun deleteSegmentsBySpeaker(analysisId: Long, label: String)

    @Query("UPDATE segments SET speakerLabel = :newLabel WHERE speakerLabel = :oldLabel AND analysisId = :analysisId")
    suspend fun updateSpeakerLabel(analysisId: Long, oldLabel: String, newLabel: String)

    @Query("SELECT COUNT(*) FROM segments WHERE analysisId = :analysisId")
    suspend fun getSegmentCount(analysisId: Long): Int
}
