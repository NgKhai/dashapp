package com.example.driverdashapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.*
import com.example.driverdashapp.domain.repository.DriverRepository
import com.example.driverdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val error: UiText? = null,
    val driverName: String = ""
)

sealed class HomeEvent {
    data object Refresh : HomeEvent()
    data object ToggleOnline : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> refresh()
            is HomeEvent.ToggleOnline -> toggleOnline()
        }
    }

    private fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        // Fire independent calls in parallel
        val profileDeferred = async { driverRepository.getProfile() }
        val earningsDeferred = async { driverRepository.getEarnings() }

        // Process profile result (needed for isOnline check)
        when (val result = profileDeferred.await()) {
            is AppResult.Success -> _state.update {
                it.copy(
                    isOnline = result.data.isOnline,
                    driverName = result.data.name,
                    rating = result.data.rating
                )
            }
            is AppResult.Error -> _state.update { it.copy(error = UiText.DynamicString(result.message)) }
        }

        // Process earnings result
        when (val result = earningsDeferred.await()) {
            is AppResult.Success -> _state.update {
                it.copy(
                    todayEarnings = result.data.todayEarnings,
                    totalDeliveries = result.data.totalDeliveries
                )
            }
            is AppResult.Error -> {}
        }

        // Load pending count (depends on isOnline from profile)
        if (_state.value.isOnline) {
            when (val result = driverRepository.getPendingDeliveries(1)) {
                is AppResult.Success -> _state.update { it.copy(pendingCount = result.data.size) }
                is AppResult.Error -> {}
            }
        }

        // Check for active delivery (sequential chain — each depends on previous)
        when (val result = driverRepository.getMyDeliveries("ACCEPTED", 1, 0)) {
            is AppResult.Success -> _state.update { it.copy(activeDelivery = result.data.firstOrNull()) }
            is AppResult.Error -> {}
        }
        if (_state.value.activeDelivery == null) {
            when (val result = driverRepository.getMyDeliveries("PICKED_UP", 1, 0)) {
                is AppResult.Success -> _state.update { it.copy(activeDelivery = result.data.firstOrNull()) }
                is AppResult.Error -> {}
            }
        }
        if (_state.value.activeDelivery == null) {
            when (val result = driverRepository.getMyDeliveries("DELIVERING", 1, 0)) {
                is AppResult.Success -> _state.update { it.copy(activeDelivery = result.data.firstOrNull()) }
                is AppResult.Error -> {}
            }
        }

        _state.update { it.copy(isLoading = false) }
    }

    private fun toggleOnline() = viewModelScope.launch {
        _state.update { it.copy(isTogglingStatus = true) }
        when (val result = driverRepository.updateStatus(!_state.value.isOnline)) {
            is AppResult.Success -> {
                _state.update { it.copy(isOnline = result.data.isOnline, isTogglingStatus = false) }
                if (result.data.isOnline) refresh()
            }
            is AppResult.Error -> _state.update { it.copy(isTogglingStatus = false, error = UiText.DynamicString(result.message)) }
        }
    }
}
