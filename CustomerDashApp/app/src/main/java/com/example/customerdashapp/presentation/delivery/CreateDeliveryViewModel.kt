package com.example.customerdashapp.presentation.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.R
import com.example.customerdashapp.domain.model.*
import com.example.customerdashapp.domain.repository.DeliveryRepository
import com.example.customerdashapp.domain.repository.MapRepository
import com.example.customerdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateDeliveryState(
    val pickupAddress: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val dropOffAddress: String = "",
    val dropOffLat: Double = 0.0,
    val dropOffLng: Double = 0.0,
    val vehicleType: String = "MOTORCYCLE",
    val notes: String = "",
    val requiresLoadingHelp: Boolean = false,
    val items: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val createdDeliveryId: String? = null,
    val savedAddresses: List<Address> = emptyList(),
    // Route info
    val routeDistanceKm: Double? = null,
    val routeDurationMinutes: Double? = null
)

sealed class CreateDeliveryEvent {
    data class UpdatePickupAddress(val address: String) : CreateDeliveryEvent()
    data class UpdatePickupCoords(val lat: Double, val lng: Double) : CreateDeliveryEvent()
    data class UpdateDropOffAddress(val address: String) : CreateDeliveryEvent()
    data class UpdateDropOffCoords(val lat: Double, val lng: Double) : CreateDeliveryEvent()
    data class UpdateVehicleType(val type: String) : CreateDeliveryEvent()
    data class UpdateNotes(val notes: String) : CreateDeliveryEvent()
    data class ToggleLoadingHelp(val value: Boolean) : CreateDeliveryEvent()
    data class SelectSavedAddress(val address: Address, val isPickup: Boolean) : CreateDeliveryEvent()
    data class UpdateItems(val items: List<String>) : CreateDeliveryEvent()
    data class UpdateRouteInfo(val distanceKm: Double, val durationMinutes: Double) : CreateDeliveryEvent()
    data object LoadAddresses : CreateDeliveryEvent()
    data object SubmitDelivery : CreateDeliveryEvent()
    data object LoadRoute : CreateDeliveryEvent()
}

@HiltViewModel
class CreateDeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateDeliveryState())
    val state: StateFlow<CreateDeliveryState> = _state.asStateFlow()

    fun onEvent(event: CreateDeliveryEvent) {
        when (event) {
            is CreateDeliveryEvent.UpdatePickupAddress -> _state.value = _state.value.copy(pickupAddress = event.address)
            is CreateDeliveryEvent.UpdatePickupCoords -> _state.value = _state.value.copy(pickupLat = event.lat, pickupLng = event.lng)
            is CreateDeliveryEvent.UpdateDropOffAddress -> _state.value = _state.value.copy(dropOffAddress = event.address)
            is CreateDeliveryEvent.UpdateDropOffCoords -> _state.value = _state.value.copy(dropOffLat = event.lat, dropOffLng = event.lng)
            is CreateDeliveryEvent.UpdateVehicleType -> _state.value = _state.value.copy(vehicleType = event.type)
            is CreateDeliveryEvent.UpdateNotes -> _state.value = _state.value.copy(notes = event.notes)
            is CreateDeliveryEvent.ToggleLoadingHelp -> _state.value = _state.value.copy(requiresLoadingHelp = event.value)
            is CreateDeliveryEvent.SelectSavedAddress -> selectSavedAddress(event.address, event.isPickup)
            is CreateDeliveryEvent.UpdateItems -> _state.value = _state.value.copy(items = event.items)
            is CreateDeliveryEvent.UpdateRouteInfo -> _state.value = _state.value.copy(
                routeDistanceKm = event.distanceKm,
                routeDurationMinutes = event.durationMinutes
            )
            is CreateDeliveryEvent.LoadAddresses -> loadAddresses()
            is CreateDeliveryEvent.SubmitDelivery -> submitDelivery()
            is CreateDeliveryEvent.LoadRoute -> loadRoute()
        }
    }

    private fun selectSavedAddress(address: Address, isPickup: Boolean) {
        if (isPickup) {
            _state.value = _state.value.copy(
                pickupAddress = address.address,
                pickupLat = address.lat,
                pickupLng = address.lng
            )
        } else {
            _state.value = _state.value.copy(
                dropOffAddress = address.address,
                dropOffLat = address.lat,
                dropOffLng = address.lng
            )
        }
    }

    private fun loadAddresses() {
        viewModelScope.launch {
            when (val result = deliveryRepository.getAddresses()) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(savedAddresses = result.data)
                }
                is AppResult.Error -> { /* silently ignore */ }
                else -> {}
            }
        }
    }

    private fun loadRoute() {
        val state = _state.value
        if (state.pickupLat != 0.0 && state.dropOffLat != 0.0) {
            viewModelScope.launch {
                when (val result = mapRepository.getRoute(
                    state.pickupLat, state.pickupLng,
                    state.dropOffLat, state.dropOffLng
                )) {
                    is AppResult.Success -> {
                        _state.value = _state.value.copy(
                            routeDistanceKm = result.data.distanceKm,
                            routeDurationMinutes = result.data.durationMinutes
                        )
                    }
                    else -> { /* silently ignore */ }
                }
            }
        }
    }

    private fun submitDelivery() {
        val state = _state.value
        if (state.pickupAddress.isBlank() || state.dropOffAddress.isBlank()) {
            _state.value = state.copy(error = UiText.StringResource(R.string.error_address_required))
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Use default coords if not set (Ho Chi Minh City center area)
            val pickupLat = if (state.pickupLat == 0.0) 10.7769 else state.pickupLat
            val pickupLng = if (state.pickupLng == 0.0) 106.7009 else state.pickupLng
            val dropOffLat = if (state.dropOffLat == 0.0) 10.7869 else state.dropOffLat
            val dropOffLng = if (state.dropOffLng == 0.0) 106.7109 else state.dropOffLng

            when (val result = deliveryRepository.createDelivery(
                CreateDeliveryParams(
                    pickupAddress = state.pickupAddress,
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    dropOffAddress = state.dropOffAddress,
                    dropOffLat = dropOffLat,
                    dropOffLng = dropOffLng,
                    vehicleType = state.vehicleType,
                    notes = state.notes.ifBlank { null },
                    items = state.items.ifEmpty { null },
                    requiresLoadingHelp = state.requiresLoadingHelp
                )
            )) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        createdDeliveryId = result.data.deliveryId
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = UiText.DynamicString(result.message)
                    )
                }
                else -> {}
            }
        }
    }
}
