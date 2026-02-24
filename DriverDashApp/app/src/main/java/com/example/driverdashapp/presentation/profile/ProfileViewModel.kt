package com.example.driverdashapp.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Driver
import com.example.driverdashapp.domain.model.VehicleAssignment
import com.example.driverdashapp.domain.repository.AuthRepository
import com.example.driverdashapp.domain.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val driver: Driver? = null,
    val vehicles: List<VehicleAssignment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    // Vehicle primary state
    val isSettingPrimary: Boolean = false,
    val setPrimaryError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState())
        private set

    init { load() }

    fun load() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        when (val result = driverRepository.getProfile()) {
            is AppResult.Success -> uiState = uiState.copy(driver = result.data, isLoading = false)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = result.message)
            is AppResult.Loading -> {}
        }
        when (val result = driverRepository.getVehicles()) {
            is AppResult.Success -> uiState = uiState.copy(vehicles = result.data)
            is AppResult.Error -> {}
            is AppResult.Loading -> {}
        }
    }

    fun setPrimaryVehicle(assignmentId: String) = viewModelScope.launch {
        uiState = uiState.copy(isSettingPrimary = true, setPrimaryError = null)
        when (val result = driverRepository.setPrimaryVehicle(assignmentId)) {
            is AppResult.Success -> {
                // Refresh vehicles list so primary badge updates
                when (val vResult = driverRepository.getVehicles()) {
                    is AppResult.Success -> uiState = uiState.copy(
                        vehicles = vResult.data,
                        isSettingPrimary = false
                    )
                    is AppResult.Error -> uiState = uiState.copy(
                        isSettingPrimary = false,
                        setPrimaryError = vResult.message
                    )
                    is AppResult.Loading -> {}
                }
            }
            is AppResult.Error -> uiState = uiState.copy(
                isSettingPrimary = false,
                setPrimaryError = result.message
            )
            is AppResult.Loading -> {}
        }
    }

    fun logout() = viewModelScope.launch {
        authRepository.logout()
        uiState = uiState.copy(isLoggedOut = true)
    }
}
