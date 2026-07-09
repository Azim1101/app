package com.vdub.database.dao

import androidx.room.*
import com.vdub.database.entity.SpeakerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeakerDao {

    @Query("SELECT * FROM speakers WHERE analysisId = :analysisId ORDER BY totalDurationMs DESC")
    fun getSpeakersForAnalysis(analysisId: Long): Flow<List<SpeakerEntity>>

    @Query("SELECT * FROM speakers WHERE analysisId = :analysisId ORDER BY totalDurationMs DESC")
    suspend fun getSpeakersForAnalysisSync(analysisId: Long): List<SpeakerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakers(speakers: List<SpeakerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeaker(speaker: SpeakerEntity): Long

    @Query("DELETE FROM speakers WHERE analysisId = :analysisId")
    suspend fun deleteSpeakersForAnalysis(analysisId: Long)

    @Query("DELETE FROM speakers WHERE label = :label AND analysisId = :analysisId")
    suspend fun deleteSpeaker(analysisId: Long, label: String)

    @Query("UPDATE speakers SET label = :newLabel WHERE label = :oldLabel AND analysisId = :analysisId")
    suspend fun updateSpeakerLabel(analysisId: Long, oldLabel: String, newLabel: String)

    @Query("SELECT * FROM speakers WHERE label = :label AND analysisId = :analysisId")
    suspend fun getSpeakerByLabel(analysisId: Long, label: String): SpeakerEntity?
}
