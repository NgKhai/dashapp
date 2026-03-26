package com.example.driverdashapp.presentation.active

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.Delivery
import com.example.driverdashapp.domain.model.DeliveryStatus
import com.example.driverdashapp.domain.model.LatLng
import com.example.driverdashapp.domain.repository.DriverRepository
import com.example.driverdashapp.presentation.util.UiText
import com.example.driverdashapp.util.PolylineDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.broadcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class ActiveDeliveryUiState(
    val delivery: Delivery? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: UiText? = null,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    // Map state
    val routePoints: List<LatLng> = emptyList(),
    val isLoadingRoute: Boolean = false,
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val selectedPhotoUrl: String? = null
)

sealed class ActiveDeliveryEvent {
    data object AdvanceStatus : ActiveDeliveryEvent()
    data class Cancel(val reason: String? = null) : ActiveDeliveryEvent()
    data object PermissionGranted : ActiveDeliveryEvent()
    data class SelectPhoto(val url: String?) : ActiveDeliveryEvent()
}

@HiltViewModel
class ActiveDeliveryViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveDeliveryUiState())
    val uiState: StateFlow<ActiveDeliveryUiState> = _state.asStateFlow()

    private val deliveryId: String = savedStateHandle["deliveryId"] ?: ""

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var realtimeChannel: RealtimeChannel? = null

    // Throttle: track last broadcast time and position
    private var lastBroadcastTime = 0L
    private var lastBroadcastLat = Double.NaN
    private var lastBroadcastLng = Double.NaN

    // DB persistence: write to REST at most every 60 seconds
    private var lastDbWriteTime = 0L

    companion object {
        private const val MIN_BROADCAST_INTERVAL_MS = 3_000L  // 3 seconds
        private const val MIN_BROADCAST_DISTANCE_M = 5f       // 5 metres
        private const val DB_PERSIST_INTERVAL_MS = 60_000L    // 60 seconds
        private const val TAG = "ActiveDeliveryVM"
    }

    init {
        loadDelivery()
    }

    fun onEvent(event: ActiveDeliveryEvent) {
        when (event) {
            is ActiveDeliveryEvent.AdvanceStatus -> advanceStatus()
            is ActiveDeliveryEvent.Cancel -> cancelDelivery(event.reason)
            is ActiveDeliveryEvent.PermissionGranted -> startLocationUpdates()
            is ActiveDeliveryEvent.SelectPhoto -> _state.update { it.copy(selectedPhotoUrl = event.url) }
        }
    }

    private fun loadDelivery() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = driverRepository.getDelivery(deliveryId)) {
            is AppResult.Success -> {
                _state.update { it.copy(delivery = result.data, isLoading = false) }
                decodeRoute(result.data)
                joinRealtimeChannel()
                startLocationUpdates()
            }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
        }
    }

    /**
     * Decode the pre-computed encoded polyline from the delivery data.
     * This is instant — no network call needed.
     * Falls back to a straight pickup→dropoff line if routeEncoded is null.
     */
    private fun decodeRoute(delivery: Delivery) {
        val points = PolylineDecoder.decode(delivery.routeEncoded)
        val routePoints = if (points.isNotEmpty()) {
            points
        } else {
            // Fallback: straight line between pickup and drop-off
            listOf(
                LatLng(delivery.pickupLat, delivery.pickupLng),
                LatLng(delivery.dropOffLat, delivery.dropOffLng)
            )
        }
        _state.update { it.copy(routePoints = routePoints, isLoadingRoute = false) }
    }

    /**
     * Join Supabase Realtime Broadcast channel for this delivery.
     * Channel name: "tracking:{deliveryId}"
     */
    private fun joinRealtimeChannel() = viewModelScope.launch {
        try {
            val channel = supabaseClient.realtime.channel("tracking:$deliveryId") {
                broadcast { }
            }
            realtimeChannel = channel
            channel.subscribe()
            Log.d(TAG, "Joined realtime channel: tracking:$deliveryId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join realtime channel: ${e.message}")
        }
    }

    /**
     * Broadcast location on the Realtime channel.
     * - Skips if moved < 5m or < 3s since last broadcast (throttle)
     * - Writes to DB (REST) at most every 60s for persistence
     */
    private fun onLocationUpdate(lat: Double, lng: Double) {
        val now = System.currentTimeMillis()

        // Update UI immediately
        _state.update { it.copy(driverLat = lat, driverLng = lng) }

        // Distance check (skip if barely moved)
        if (!lastBroadcastLat.isNaN()) {
            val results = FloatArray(1)
            Location.distanceBetween(lastBroadcastLat, lastBroadcastLng, lat, lng, results)
            if (results[0] < MIN_BROADCAST_DISTANCE_M &&
                now - lastBroadcastTime < MIN_BROADCAST_INTERVAL_MS) return
        }

        // Time check
        if (now - lastBroadcastTime < MIN_BROADCAST_INTERVAL_MS) return

        lastBroadcastTime = now
        lastBroadcastLat = lat
        lastBroadcastLng = lng

        // Broadcast via Supabase Realtime (no DB write)
        viewModelScope.launch {
            try {
                realtimeChannel?.broadcast(
                    event = "location",
                    message = buildJsonObject {
                        put("lat", lat)
                        put("lng", lng)
                        put("delivery_id", deliveryId)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast failed: ${e.message}")
            }
        }

        // DB persistence every 60 seconds
        if (now - lastDbWriteTime >= DB_PERSIST_INTERVAL_MS) {
            lastDbWriteTime = now
            viewModelScope.launch {
                driverRepository.updateLocation(lat, lng, deliveryId)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Seed with last known location immediately
            val lastLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastLoc?.let { onLocationUpdate(it.latitude, it.longitude) }

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationUpdate(location.latitude, location.longitude)
                }
                @Deprecated("Deprecated in API")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5_000L,   // 5 seconds (was 10s — fine since throttle handles excess)
                5f,        // 5 metres
                locationListener!!
            )
        } catch (_: SecurityException) {}
    }

    private fun stopLocationUpdates() {
        locationListener?.let { locationManager?.removeUpdates(it) }
        locationListener = null
        locationManager = null
    }

    private fun leaveRealtimeChannel() = viewModelScope.launch {
        try {
            realtimeChannel?.let { supabaseClient.realtime.removeChannel(it) }
            realtimeChannel = null
            Log.d(TAG, "Left realtime channel: tracking:$deliveryId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave realtime channel: ${e.message}")
        }
    }

    private fun advanceStatus() = viewModelScope.launch {
        val delivery = _state.value.delivery ?: return@launch
        _state.update { it.copy(isUpdating = true, error = null) }

        val result = when (delivery.status) {
            DeliveryStatus.ACCEPTED   -> driverRepository.pickupDelivery(deliveryId)
            DeliveryStatus.PICKED_UP  -> driverRepository.startDelivering(deliveryId)
            DeliveryStatus.DELIVERING -> driverRepository.completeDelivery(deliveryId)
            else -> return@launch
        }

        when (result) {
            is AppResult.Success -> {
                val isCompleted = result.data.status == DeliveryStatus.COMPLETED
                _state.update {
                    it.copy(delivery = result.data, isUpdating = false, isCompleted = isCompleted)
                }
                if (isCompleted) {
                    stopLocationUpdates()
                    leaveRealtimeChannel()
                }
            }
            is AppResult.Error -> _state.update { it.copy(isUpdating = false, error = UiText.DynamicString(result.message)) }
        }
    }

    private fun cancelDelivery(reason: String? = null) = viewModelScope.launch {
        _state.update { it.copy(isUpdating = true, error = null) }
        when (val result = driverRepository.cancelDelivery(deliveryId, reason)) {
            is AppResult.Success -> {
                _state.update { it.copy(isUpdating = false, isCancelled = true, delivery = result.data) }
                stopLocationUpdates()
                leaveRealtimeChannel()
            }
            is AppResult.Error -> _state.update { it.copy(isUpdating = false, error = UiText.DynamicString(result.message)) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        leaveRealtimeChannel()
    }
}
