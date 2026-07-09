package com.vdub.domain.usecase

import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.entity.ExportFormat
import com.vdub.domain.repository.DiarizationRepository
import javax.inject.Inject

/**
 * Use case to export analysis results in various formats.
 */
class ExportResultUseCase @Inject constructor(
    private val diarizationRepository: DiarizationRepository
) {
    suspend operator fun invoke(result: AnalysisResult, format: ExportFormat): Result<String> {
        return diarizationRepository.exportResult(result, format)
    }
}
