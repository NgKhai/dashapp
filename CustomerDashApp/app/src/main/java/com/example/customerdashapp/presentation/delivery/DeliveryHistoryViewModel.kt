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
    val error: UiText? = null,
    val selectedFilter: String? = null
)

sealed class DeliveryHistoryEvent {
    data object LoadHistory : DeliveryHistoryEvent()
    data class FilterHistory(val status: String?) : DeliveryHistoryEvent()
}

@HiltViewModel
class DeliveryHistoryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryHistoryState())
    val state: StateFlow<DeliveryHistoryState> = _state.asStateFlow()

    fun onEvent(event: DeliveryHistoryEvent) {
        when (event) {
            is DeliveryHistoryEvent.LoadHistory -> loadHistory()
            is DeliveryHistoryEvent.FilterHistory -> filterHistory(event.status)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = deliveryRepository.getMyDeliveries(
                status = _state.value.selectedFilter,
                limit = 50
            )) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        deliveries = result.data,
                        isLoading = false
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

    private fun filterHistory(status: String?) {
        _state.value = _state.value.copy(selectedFilter = status)
        loadHistory()
    }
}
