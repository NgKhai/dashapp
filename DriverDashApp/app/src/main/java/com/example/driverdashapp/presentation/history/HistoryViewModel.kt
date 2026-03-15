package com.example.driverdashapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Delivery
import com.example.driverdashapp.domain.repository.DriverRepository
import com.example.driverdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: UiText? = null,
    val selectedFilter: String = "ALL"
)

sealed class HistoryEvent {
    data class Load(val filter: String? = null) : HistoryEvent()
    data object LoadMore : HistoryEvent()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var currentOffset = 0

    companion object {
        private const val PAGE_SIZE = 20
    }

    init { load() }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Load -> load(event.filter ?: _state.value.selectedFilter)
            is HistoryEvent.LoadMore -> loadMore()
        }
    }

    /** Load first page (resets list) */
    private fun load(filter: String = _state.value.selectedFilter) = viewModelScope.launch {
        currentOffset = 0
        _state.update { it.copy(isLoading = true, error = null, selectedFilter = filter, hasMore = true) }
        val status = if (filter == "ALL") null else filter
        when (val result = driverRepository.getMyDeliveries(status, PAGE_SIZE, 0)) {
            is AppResult.Success -> {
                currentOffset = result.data.size
                _state.update {
                    it.copy(
                        deliveries = result.data,
                        isLoading = false,
                        hasMore = result.data.size >= PAGE_SIZE
                    )
                }
            }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
        }
    }

    /** Load next page (appends to list) */
    private fun loadMore() = viewModelScope.launch {
        if (_state.value.isLoadingMore || !_state.value.hasMore) return@launch
        _state.update { it.copy(isLoadingMore = true) }
        val status = if (_state.value.selectedFilter == "ALL") null else _state.value.selectedFilter
        when (val result = driverRepository.getMyDeliveries(status, PAGE_SIZE, currentOffset)) {
            is AppResult.Success -> {
                currentOffset += result.data.size
                _state.update {
                    it.copy(
                        deliveries = it.deliveries + result.data,
                        isLoadingMore = false,
                        hasMore = result.data.size >= PAGE_SIZE
                    )
                }
            }
            is AppResult.Error -> _state.update { it.copy(isLoadingMore = false, error = UiText.DynamicString(result.message)) }
        }
    }
}
