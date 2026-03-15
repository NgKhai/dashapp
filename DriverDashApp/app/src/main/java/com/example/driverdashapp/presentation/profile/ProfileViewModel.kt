package com.example.driverdashapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Driver
import com.example.driverdashapp.domain.model.VehicleAssignment
import com.example.driverdashapp.domain.repository.AuthRepository
import com.example.driverdashapp.domain.repository.DriverRepository
import com.example.driverdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val driver: Driver? = null,
    val vehicles: List<VehicleAssignment> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isLoggedOut: Boolean = false,
    // Vehicle primary state
    val isSettingPrimary: Boolean = false,
    val setPrimaryError: UiText? = null
)

sealed class ProfileEvent {
    data object Load : ProfileEvent()
    data class SetPrimary(val assignmentId: String) : ProfileEvent()
    data object Logout : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Load -> load()
            is ProfileEvent.SetPrimary -> setPrimaryVehicle(event.assignmentId)
            is ProfileEvent.Logout -> logout()
        }
    }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = driverRepository.getProfile()) {
            is AppResult.Success -> _state.update { it.copy(driver = result.data, isLoading = false) }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
        }
        when (val result = driverRepository.getVehicles()) {
            is AppResult.Success -> _state.update { it.copy(vehicles = result.data) }
            is AppResult.Error -> {}
        }
    }

    private fun setPrimaryVehicle(assignmentId: String) = viewModelScope.launch {
        _state.update { it.copy(isSettingPrimary = true, setPrimaryError = null) }
        when (val result = driverRepository.setPrimaryVehicle(assignmentId)) {
            is AppResult.Success -> {
                // Refresh vehicles list so primary badge updates
                when (val vResult = driverRepository.getVehicles()) {
                    is AppResult.Success -> _state.update {
                        it.copy(vehicles = vResult.data, isSettingPrimary = false)
                    }
                    is AppResult.Error -> _state.update {
                        it.copy(isSettingPrimary = false, setPrimaryError = UiText.DynamicString(vResult.message))
                    }
                }
            }
            is AppResult.Error -> _state.update {
                it.copy(isSettingPrimary = false, setPrimaryError = UiText.DynamicString(result.message))
            }
        }
    }

    private fun logout() = viewModelScope.launch {
        authRepository.logout()
        _state.update { it.copy(isLoggedOut = true) }
    }
}
