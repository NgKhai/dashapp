package com.example.driverdashapp.presentation.pending

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

data class PendingUiState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val isAccepting: String? = null, // deliveryId being accepted
    val error: UiText? = null,
    val acceptedDeliveryId: String? = null
)

sealed class PendingEvent {
    data object Load : PendingEvent()
    data class Accept(val deliveryId: String) : PendingEvent()
    data object ClearAccepted : PendingEvent()
}

@HiltViewModel
class PendingViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PendingUiState())
    val uiState: StateFlow<PendingUiState> = _state.asStateFlow()

    init { load() }

    fun onEvent(event: PendingEvent) {
        when (event) {
            is PendingEvent.Load -> load()
            is PendingEvent.Accept -> accept(event.deliveryId)
            is PendingEvent.ClearAccepted -> clearAccepted()
        }
    }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = driverRepository.getPendingDeliveries(20)) {
            is AppResult.Success -> _state.update { it.copy(deliveries = result.data, isLoading = false) }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
        }
    }

    private fun accept(deliveryId: String) = viewModelScope.launch {
        _state.update { it.copy(isAccepting = deliveryId, error = null) }
        when (val result = driverRepository.acceptDelivery(deliveryId)) {
            is AppResult.Success -> _state.update { it.copy(isAccepting = null, acceptedDeliveryId = deliveryId) }
            is AppResult.Error -> _state.update { it.copy(isAccepting = null, error = UiText.DynamicString(result.message)) }
        }
    }

    private fun clearAccepted() { _state.update { it.copy(acceptedDeliveryId = null) } }
}
