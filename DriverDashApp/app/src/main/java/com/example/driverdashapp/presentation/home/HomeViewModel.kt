package com.example.driverdashapp.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.*
import com.example.driverdashapp.domain.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isOnline: Boolean = false,
    val todayEarnings: Double = 0.0,
    val totalDeliveries: Int = 0,
    val rating: Double? = null,
    val pendingCount: Int = 0,
    val activeDelivery: Delivery? = null,
    val isLoading: Boolean = false,
    val isTogglingStatus: Boolean = false,
    val error: String? = null,
    val driverName: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)

        // Load profile
        when (val result = driverRepository.getProfile()) {
            is AppResult.Success -> uiState = uiState.copy(
                isOnline = result.data.isOnline,
                driverName = result.data.name,
                rating = result.data.rating
            )
            is AppResult.Error -> uiState = uiState.copy(error = result.message)
            is AppResult.Loading -> {}
        }

        // Load earnings
        when (val result = driverRepository.getEarnings()) {
            is AppResult.Success -> uiState = uiState.copy(
                todayEarnings = result.data.todayEarnings,
                totalDeliveries = result.data.totalDeliveries
            )
            is AppResult.Error -> {}
            is AppResult.Loading -> {}
        }

        // Load pending count
        if (uiState.isOnline) {
            when (val result = driverRepository.getPendingDeliveries(1)) {
                is AppResult.Success -> uiState = uiState.copy(pendingCount = result.data.size)
                is AppResult.Error -> {}
                is AppResult.Loading -> {}
            }
        }

        // Check for active delivery
        when (val result = driverRepository.getMyDeliveries("ACCEPTED", 1, 0)) {
            is AppResult.Success -> uiState = uiState.copy(activeDelivery = result.data.firstOrNull())
            is AppResult.Error -> {}
            is AppResult.Loading -> {}
        }
        if (uiState.activeDelivery == null) {
            when (val result = driverRepository.getMyDeliveries("PICKED_UP", 1, 0)) {
                is AppResult.Success -> uiState = uiState.copy(activeDelivery = result.data.firstOrNull())
                is AppResult.Error -> {}
                is AppResult.Loading -> {}
            }
        }
        if (uiState.activeDelivery == null) {
            when (val result = driverRepository.getMyDeliveries("DELIVERING", 1, 0)) {
                is AppResult.Success -> uiState = uiState.copy(activeDelivery = result.data.firstOrNull())
                is AppResult.Error -> {}
                is AppResult.Loading -> {}
            }
        }

        uiState = uiState.copy(isLoading = false)
    }

    fun toggleOnline() = viewModelScope.launch {
        uiState = uiState.copy(isTogglingStatus = true)
        when (val result = driverRepository.updateStatus(!uiState.isOnline)) {
            is AppResult.Success -> {
                uiState = uiState.copy(isOnline = result.data.isOnline, isTogglingStatus = false)
                if (result.data.isOnline) refresh()
            }
            is AppResult.Error -> uiState = uiState.copy(isTogglingStatus = false, error = result.message)
            is AppResult.Loading -> {}
        }
    }
}
