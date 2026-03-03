package com.example.customerdashapp.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.RouteInfo
import com.example.customerdashapp.domain.model.SearchResult
import com.example.customerdashapp.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddressStep {
    PICKUP, DROPOFF, DONE
}

data class MapPickerState(
    val currentStep: AddressStep = AddressStep.PICKUP,
    // Pickup
    val pickupQuery: String = "",
    val pickupAddress: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val pickupSelected: Boolean = false,
    // Drop-off
    val dropOffQuery: String = "",
    val dropOffAddress: String = "",
    val dropOffLat: Double = 0.0,
    val dropOffLng: Double = 0.0,
    val dropOffSelected: Boolean = false,
    // Search
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val activeSearchField: AddressStep = AddressStep.PICKUP,
    // Reverse geocoding
    val isReverseGeocoding: Boolean = false,
    // Route
    val routeInfo: RouteInfo? = null,
    val isLoadingRoute: Boolean = false,
    // Estimated cost (VND)
    val estimatedCost: Long? = null
)

sealed class MapPickerEvent {
    data class UpdatePickupQuery(val query: String) : MapPickerEvent()
    data class UpdateDropOffQuery(val query: String) : MapPickerEvent()
    data class SelectSearchResult(val result: SearchResult) : MapPickerEvent()
    data class TapOnMap(val lat: Double, val lng: Double) : MapPickerEvent()
    data object ClearSearch : MapPickerEvent()
    data object ResetPickup : MapPickerEvent()
    data object ResetDropOff : MapPickerEvent()
    data class SetActiveField(val field: AddressStep) : MapPickerEvent()
}

@HiltViewModel
class MapPickerViewModel @Inject constructor(
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapPickerState())
    val state: StateFlow<MapPickerState> = _state.asStateFlow()

    private var searchJob: Job? = null

    // Simple in-memory search cache — avoids re-fetching the same query
    private val searchCache = LinkedHashMap<String, List<SearchResult>>(30, 0.75f, true)

    fun onEvent(event: MapPickerEvent) {
        when (event) {
            is MapPickerEvent.UpdatePickupQuery -> updateSearch(event.query, AddressStep.PICKUP)
            is MapPickerEvent.UpdateDropOffQuery -> updateSearch(event.query, AddressStep.DROPOFF)
            is MapPickerEvent.SelectSearchResult -> selectSearchResult(event.result)
            is MapPickerEvent.TapOnMap -> tapOnMap(event.lat, event.lng)
            is MapPickerEvent.ClearSearch -> {
                _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            }
            is MapPickerEvent.ResetPickup -> resetPickup()
            is MapPickerEvent.ResetDropOff -> resetDropOff()
            is MapPickerEvent.SetActiveField -> {
                _state.value = _state.value.copy(
                    activeSearchField = event.field,
                    searchResults = emptyList()
                )
            }
        }
    }

    private fun updateSearch(query: String, field: AddressStep) {
        _state.value = when (field) {
            AddressStep.PICKUP -> _state.value.copy(pickupQuery = query, activeSearchField = field)
            AddressStep.DROPOFF -> _state.value.copy(dropOffQuery = query, activeSearchField = field)
            else -> _state.value
        }

        searchJob?.cancel()
        if (query.length < 3) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        // Check cache first — instant result, no network
        val cached = searchCache[query.lowercase()]
        if (cached != null) {
            _state.value = _state.value.copy(searchResults = cached, isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce 600ms — Nominatim requires max 1 request/second
            delay(600)
            _state.value = _state.value.copy(isSearching = true)

            // Try up to 2 times (initial + 1 retry)
            var lastError: String? = null
            for (attempt in 1..2) {
                try {
                    when (val result = mapRepository.searchAddress(query)) {
                        is AppResult.Success -> {
                            // Cache the result
                            if (searchCache.size > 30) {
                                searchCache.remove(searchCache.keys.first())
                            }
                            searchCache[query.lowercase()] = result.data
                            _state.value = _state.value.copy(
                                searchResults = result.data,
                                isSearching = false
                            )
                            return@launch  // Success — done
                        }
                        is AppResult.Error -> {
                            lastError = result.message
                        }
                        else -> {}
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e  // Don’t swallow cancellation — let debounce work correctly
                } catch (e: Exception) {
                    lastError = e.message
                }
                // Retry after 1 second (only if we have another attempt)
                if (attempt < 2) delay(1000)
            }
            // Both attempts failed — keep previous results visible instead of clearing
            _state.value = _state.value.copy(isSearching = false)
        }
    }

    private fun selectSearchResult(result: SearchResult) {
        val field = _state.value.activeSearchField
        when (field) {
            AddressStep.PICKUP -> {
                _state.value = _state.value.copy(
                    pickupAddress = result.displayName,
                    pickupQuery = result.displayName,
                    pickupLat = result.lat,
                    pickupLng = result.lng,
                    pickupSelected = true,
                    currentStep = AddressStep.DROPOFF,
                    searchResults = emptyList()
                )
            }
            AddressStep.DROPOFF -> {
                _state.value = _state.value.copy(
                    dropOffAddress = result.displayName,
                    dropOffQuery = result.displayName,
                    dropOffLat = result.lat,
                    dropOffLng = result.lng,
                    dropOffSelected = true,
                    currentStep = AddressStep.DONE,
                    searchResults = emptyList()
                )
                // Both selected → load route
                loadRoute()
            }
            else -> {}
        }
    }

    private fun tapOnMap(lat: Double, lng: Double) {
        val field = _state.value.activeSearchField
        _state.value = _state.value.copy(isReverseGeocoding = true)

        viewModelScope.launch {
            val addressText = when (val result = mapRepository.reverseGeocode(lat, lng)) {
                is AppResult.Success -> result.data.displayName
                else -> "%.6f, %.6f".format(lat, lng)
            }

            when (field) {
                AddressStep.PICKUP -> {
                    _state.value = _state.value.copy(
                        pickupAddress = addressText,
                        pickupQuery = addressText,
                        pickupLat = lat,
                        pickupLng = lng,
                        pickupSelected = true,
                        currentStep = AddressStep.DROPOFF,
                        activeSearchField = AddressStep.DROPOFF,
                        isReverseGeocoding = false,
                        searchResults = emptyList()
                    )
                }
                AddressStep.DROPOFF -> {
                    _state.value = _state.value.copy(
                        dropOffAddress = addressText,
                        dropOffQuery = addressText,
                        dropOffLat = lat,
                        dropOffLng = lng,
                        dropOffSelected = true,
                        currentStep = AddressStep.DONE,
                        isReverseGeocoding = false,
                        searchResults = emptyList()
                    )
                    loadRoute()
                }
                else -> {
                    _state.value = _state.value.copy(isReverseGeocoding = false)
                }
            }
        }
    }

    private fun loadRoute() {
        val s = _state.value
        if (!s.pickupSelected || !s.dropOffSelected) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingRoute = true)
            when (val result = mapRepository.getRoute(
                s.pickupLat, s.pickupLng, s.dropOffLat, s.dropOffLng
            )) {
                is AppResult.Success -> {
                    val cost = calculateCost(result.data.distanceKm)
                    _state.value = _state.value.copy(
                        routeInfo = result.data,
                        isLoadingRoute = false,
                        estimatedCost = cost
                    )
                }
                else -> {
                    // OSRM failed/timed out — use straight-line Haversine distance as fallback
                    val fallbackKm = haversineKm(s.pickupLat, s.pickupLng, s.dropOffLat, s.dropOffLng)
                    val fallbackRoute = com.example.customerdashapp.domain.model.RouteInfo(
                        points = listOf(
                            org.osmdroid.util.GeoPoint(s.pickupLat, s.pickupLng),
                            org.osmdroid.util.GeoPoint(s.dropOffLat, s.dropOffLng)
                        ),
                        distanceKm = fallbackKm,
                        durationMinutes = (fallbackKm / 30.0) * 60.0   // estimate at 30 km/h
                    )
                    _state.value = _state.value.copy(
                        routeInfo = fallbackRoute,
                        isLoadingRoute = false,
                        estimatedCost = calculateCost(fallbackKm)
                    )
                }
            }
        }
    }

    /** Haversine great-circle distance in km */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun calculateCost(distanceKm: Double): Long {
        // Must match backend: base 20,000 VND + ceil(km) × 5,000 VND
        val baseFare = 20_000L
        val perKm = 5_000L
        val raw = baseFare + (kotlin.math.ceil(distanceKm).toLong() * perKm)
        // Round to nearest 1,000 VND for clean display
        return ((raw + 500) / 1000) * 1000
    }

    private fun resetPickup() {
        _state.value = _state.value.copy(
            pickupAddress = "",
            pickupQuery = "",
            pickupLat = 0.0,
            pickupLng = 0.0,
            pickupSelected = false,
            currentStep = AddressStep.PICKUP,
            activeSearchField = AddressStep.PICKUP,
            routeInfo = null,
            estimatedCost = null
        )
    }

    private fun resetDropOff() {
        _state.value = _state.value.copy(
            dropOffAddress = "",
            dropOffQuery = "",
            dropOffLat = 0.0,
            dropOffLng = 0.0,
            dropOffSelected = false,
            currentStep = AddressStep.DROPOFF,
            activeSearchField = AddressStep.DROPOFF,
            routeInfo = null,
            estimatedCost = null
        )
    }
}
