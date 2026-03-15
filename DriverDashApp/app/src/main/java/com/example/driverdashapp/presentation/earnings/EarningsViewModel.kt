package com.example.driverdashapp.presentation.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Earnings
import com.example.driverdashapp.domain.repository.DriverRepository
import com.example.driverdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EarningsUiState(
    val earnings: Earnings? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null
)

sealed class EarningsEvent {
    data object Refresh : EarningsEvent()
}

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EarningsUiState())
    val uiState: StateFlow<EarningsUiState> = _state.asStateFlow()

    init { load() }

    fun onEvent(event: EarningsEvent) {
        when (event) {
            is EarningsEvent.Refresh -> load()
        }
    }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = driverRepository.getEarnings()) {
            is AppResult.Success -> _state.update { it.copy(earnings = result.data, isLoading = false) }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
        }
    }
}
