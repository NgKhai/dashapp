package com.example.customerdashapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.presentation.util.UiText
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.domain.repository.AuthRepository
import com.example.customerdashapp.domain.repository.DeliveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val customerName: String = "",
    val activeDelivery: Delivery? = null,
    val recentDeliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null
)

sealed class HomeEvent {
    object LoadHome : HomeEvent()
    object Logout : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    init {
        onEvent(HomeEvent.LoadHome)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadHome -> loadHome()
            is HomeEvent.Logout -> logout()
        }
    }

    private fun loadHome() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load customer name through AuthRepository (not TokenManager directly)
            val name = authRepository.getCustomerName()
            _state.update { it.copy(customerName = name ?: "") }

            // Load recent deliveries
            when (val result = deliveryRepository.getMyDeliveries(limit = 10)) {
                is AppResult.Success -> {
                    val deliveries = result.data
                    val active = deliveries.find {
                        it.status in listOf(
                            DeliveryStatus.PENDING,
                            DeliveryStatus.ACCEPTED,
                            DeliveryStatus.PICKED_UP,
                            DeliveryStatus.DELIVERING
                        )
                    }
                    _state.update { it.copy(
                        activeDelivery = active,
                        recentDeliveries = deliveries.take(5),
                        isLoading = false
                    ) }
                }
                is AppResult.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = UiText.DynamicString(result.message)
                    ) }
                }
                else -> {}
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loggedOut.value = true
        }
    }
}
