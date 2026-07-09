package com.vdub.domain.usecase

import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case to retrieve a specific analysis result by ID.
 */
class GetResultByIdUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(id: Long): Result<AnalysisResult> {
        return historyRepository.getResultById(id)
    }
}
