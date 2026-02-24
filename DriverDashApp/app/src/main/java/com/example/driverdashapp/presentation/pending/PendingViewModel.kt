package com.example.driverdashapp.presentation.pending

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Delivery
import com.example.driverdashapp.domain.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingUiState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val isAccepting: String? = null, // deliveryId being accepted
    val error: String? = null,
    val acceptedDeliveryId: String? = null
)

@HiltViewModel
class PendingViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    var uiState by mutableStateOf(PendingUiState())
        private set

    init { load() }

    fun load() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        when (val result = driverRepository.getPendingDeliveries(20)) {
            is AppResult.Success -> uiState = uiState.copy(deliveries = result.data, isLoading = false)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = result.message)
            is AppResult.Loading -> {}
        }
    }

    fun accept(deliveryId: String) = viewModelScope.launch {
        uiState = uiState.copy(isAccepting = deliveryId, error = null)
        when (val result = driverRepository.acceptDelivery(deliveryId)) {
            is AppResult.Success -> uiState = uiState.copy(isAccepting = null, acceptedDeliveryId = deliveryId)
            is AppResult.Error -> uiState = uiState.copy(isAccepting = null, error = result.message)
            is AppResult.Loading -> {}
        }
    }

    fun clearAccepted() { uiState = uiState.copy(acceptedDeliveryId = null) }
}
