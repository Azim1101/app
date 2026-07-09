package com.vdub.domain.usecase

import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve analysis history.
 */
class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(): Flow<List<AnalysisResult>> {
        return historyRepository.getAllResults()
    }
}
