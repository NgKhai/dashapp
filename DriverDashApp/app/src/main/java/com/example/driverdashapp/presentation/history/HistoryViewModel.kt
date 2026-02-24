package com.example.driverdashapp.presentation.history

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

data class HistoryUiState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: String = "ALL"
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val driverRepository: DriverRepository
) : ViewModel() {

    var uiState by mutableStateOf(HistoryUiState())
        private set

    init { load() }

    fun load(filter: String = uiState.selectedFilter) = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null, selectedFilter = filter)
        val status = if (filter == "ALL") null else filter
        when (val result = driverRepository.getMyDeliveries(status, 50, 0)) {
            is AppResult.Success -> uiState = uiState.copy(deliveries = result.data, isLoading = false)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = result.message)
            is AppResult.Loading -> {}
        }
    }
}
