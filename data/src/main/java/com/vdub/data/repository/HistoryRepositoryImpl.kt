package com.vdub.data.repository

import com.vdub.database.dao.AnalysisDao
import com.vdub.database.dao.SegmentDao
import com.vdub.database.dao.SpeakerDao
import com.vdub.database.mapper.toDomain
import com.vdub.database.mapper.toEntity
import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val analysisDao: AnalysisDao,
    private val segmentDao: SegmentDao,
    private val speakerDao: SpeakerDao
) : HistoryRepository {

    override fun getAllResults(): Flow<List<AnalysisResult>> {
        return analysisDao.getAllAnalyses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getResultById(id: Long): Result<AnalysisResult> = runCatching {
        val entity = analysisDao.getAnalysisById(id) ?: throw NoSuchElementException("Analysis $id not found")
        val segments = segmentDao.getSegmentsForAnalysisSync(id).map { it.toDomain() }
        val speakers = speakerDao.getSpeakersForAnalysisSync(id).map { it.toDomain() }
        entity.toDomain(segments, speakers)
    }

    override suspend fun saveResult(result: AnalysisResult): Result<Long> = runCatching {
        val analysisId = analysisDao.insertAnalysis(result.toEntity())
        segmentDao.insertSegments(result.segments.map { it.toEntity().copy(analysisId = analysisId) })
        speakerDao.insertSpeakers(result.speakers.map { it.toEntity().copy(analysisId = analysisId) })
        analysisId
    }

    override suspend fun deleteResult(id: Long): Result<Unit> = runCatching {
        segmentDao.deleteSegmentsForAnalysis(id)
        speakerDao.deleteSpeakersForAnalysis(id)
        analysisDao.deleteAnalysis(id)
    }

    override fun searchResults(query: String): Flow<List<AnalysisResult>> {
        return analysisDao.searchAnalyses(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun clearHistory(): Result<Unit> = runCatching {
        analysisDao.deleteAll()
    }
}
