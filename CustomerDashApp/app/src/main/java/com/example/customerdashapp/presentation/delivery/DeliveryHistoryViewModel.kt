package com.example.customerdashapp.presentation.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.domain.model.*
import com.example.customerdashapp.domain.repository.DeliveryRepository
import com.example.customerdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryHistoryState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: UiText? = null,
    val selectedFilter: String? = null,
    val hasMore: Boolean = true
)

sealed class DeliveryHistoryEvent {
    data object LoadHistory : DeliveryHistoryEvent()
    data object LoadMore : DeliveryHistoryEvent()
    data class FilterHistory(val status: String?) : DeliveryHistoryEvent()
}

@HiltViewModel
class DeliveryHistoryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryHistoryState())
    val state: StateFlow<DeliveryHistoryState> = _state.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    fun onEvent(event: DeliveryHistoryEvent) {
        when (event) {
            is DeliveryHistoryEvent.LoadHistory -> loadHistory()
            is DeliveryHistoryEvent.LoadMore -> loadMore()
            is DeliveryHistoryEvent.FilterHistory -> filterHistory(event.status)
        }
    }

    /** Load first page — resets the list. */
    private fun loadHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                deliveries = emptyList(),
                hasMore = true
            )

            when (val result = deliveryRepository.getMyDeliveries(
                status = _state.value.selectedFilter,
                limit = PAGE_SIZE,
                offset = 0
            )) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        deliveries = result.data,
                        isLoading = false,
                        hasMore = result.data.size >= PAGE_SIZE
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = UiText.DynamicString(result.message)
                    )
                }
                else -> {}
            }
        }
    }

    /** Load next page — appends to existing list. */
    private fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return

        viewModelScope.launch {
            _state.value = current.copy(isLoadingMore = true)

            when (val result = deliveryRepository.getMyDeliveries(
                status = current.selectedFilter,
                limit = PAGE_SIZE,
                offset = current.deliveries.size
            )) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        deliveries = _state.value.deliveries + result.data,
                        isLoadingMore = false,
                        hasMore = result.data.size >= PAGE_SIZE
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        error = UiText.DynamicString(result.message)
                    )
                }
                else -> {}
            }
        }
    }

    private fun filterHistory(status: String?) {
        _state.value = _state.value.copy(selectedFilter = status)
        loadHistory()
    }
}
