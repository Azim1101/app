package com.vdub.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _results = MutableStateFlow<List<AnalysisResult>>(emptyList())
    val results: StateFlow<List<AnalysisResult>> = _results.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.getAllResults().collect { results ->
                _results.value = results
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            historyRepository.searchResults(query).collect { results ->
                _results.value = results
            }
        }
    }

    fun deleteResult(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteResult(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
