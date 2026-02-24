package com.example.driverdashapp.presentation.earnings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Earnings
import com.example.driverdashapp.domain.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EarningsUiState(
    val earnings: Earnings? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    var uiState by mutableStateOf(EarningsUiState())
        private set

    init { load() }

    fun load() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        when (val result = driverRepository.getEarnings()) {
            is AppResult.Success -> uiState = uiState.copy(earnings = result.data, isLoading = false)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = result.message)
            is AppResult.Loading -> {}
        }
    }
}
