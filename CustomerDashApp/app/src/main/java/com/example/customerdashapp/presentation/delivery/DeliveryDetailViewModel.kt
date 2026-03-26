package com.example.customerdashapp.presentation.delivery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.R
import com.example.customerdashapp.domain.model.*
import com.example.customerdashapp.domain.repository.DeliveryRepository
import com.example.customerdashapp.presentation.util.UiText
import com.example.customerdashapp.util.PolylineDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class DeliveryDetailState(
    val delivery: Delivery? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val showCancelDialog: Boolean = false,
    val showRateDialog: Boolean = false,
    val cancelReason: String = "",
    val rating: Int = 5,
    val review: String = "",
    val actionMessage: UiText? = null,
    // Route decoded instantly from delivery.routeEncoded — no OSRM call
    val routePoints: List<GeoPoint> = emptyList(),
    // Tracking state
    // Tracking state
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val isTracking: Boolean = false,
    val selectedPhotoUrl: String? = null
)

sealed class DeliveryDetailEvent {
    data class LoadDetail(val deliveryId: String) : DeliveryDetailEvent()
    data object ShowCancelDialog : DeliveryDetailEvent()
    data object DismissCancelDialog : DeliveryDetailEvent()
    data class UpdateCancelReason(val reason: String) : DeliveryDetailEvent()
    data class ConfirmCancel(val deliveryId: String) : DeliveryDetailEvent()
    data object ShowRateDialog : DeliveryDetailEvent()
    data object DismissRateDialog : DeliveryDetailEvent()
    data class UpdateRating(val rating: Int) : DeliveryDetailEvent()
    data class UpdateReview(val review: String) : DeliveryDetailEvent()
    data class ConfirmRate(val deliveryId: String) : DeliveryDetailEvent()
    data class SelectPhoto(val url: String?) : DeliveryDetailEvent()
}

@HiltViewModel
class DeliveryDetailViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryDetailState())
    val state: StateFlow<DeliveryDetailState> = _state.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null
    private var broadcastJob: Job? = null
    private var currentDeliveryId: String? = null

    companion object {
        private const val TAG = "DeliveryDetailVM"
    }

    fun onEvent(event: DeliveryDetailEvent) {
        when (event) {
            is DeliveryDetailEvent.LoadDetail         -> loadDetail(event.deliveryId)
            is DeliveryDetailEvent.ShowCancelDialog   -> _state.update { it.copy(showCancelDialog = true) }
            is DeliveryDetailEvent.DismissCancelDialog -> _state.update { it.copy(showCancelDialog = false, cancelReason = "") }
            is DeliveryDetailEvent.UpdateCancelReason -> _state.update { it.copy(cancelReason = event.reason) }
            is DeliveryDetailEvent.ConfirmCancel      -> cancelDelivery(event.deliveryId)
            is DeliveryDetailEvent.ShowRateDialog     -> _state.update { it.copy(showRateDialog = true) }
            is DeliveryDetailEvent.DismissRateDialog  -> _state.update { it.copy(showRateDialog = false) }
            is DeliveryDetailEvent.UpdateRating       -> _state.update { it.copy(rating = event.rating) }
            is DeliveryDetailEvent.UpdateReview       -> _state.update { it.copy(review = event.review) }
            is DeliveryDetailEvent.ConfirmRate        -> rateDelivery(event.deliveryId)
            is DeliveryDetailEvent.SelectPhoto        -> _state.update { it.copy(selectedPhotoUrl = event.url) }
        }
    }

    private fun loadDetail(deliveryId: String) {
        currentDeliveryId = deliveryId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = deliveryRepository.getDelivery(deliveryId)) {
                is AppResult.Success -> {
                    _state.update { it.copy(delivery = result.data, isLoading = false) }

                    // Decode the pre-computed route instantly — no OSRM call needed
                    decodeRoute(result.data)

                    val activeStatuses = listOf(
                        DeliveryStatus.ACCEPTED,
                        DeliveryStatus.PICKED_UP,
                        DeliveryStatus.DELIVERING
                    )
                    if (result.data.status in activeStatuses) {
                        // Seed last-known position from DB (one-time REST call)
                        seedInitialLocation(deliveryId)
                        // Then subscribe to Realtime for live updates
                        subscribeToRealtime(deliveryId)
                    }
                }
                is AppResult.Error -> _state.update { it.copy(
                    isLoading = false,
                    error = UiText.DynamicString(result.message)
                ) }
                else -> {}
            }
        }
    }

    /**
     * Decode the pre-computed encoded polyline from the delivery object.
     * Instant — no network call. Falls back to a straight pickup→dropoff line
     * if routeEncoded is null (OSRM timed out at delivery creation time).
     *
     * PolylineDecoder returns domain [LatLng]; we map to osmdroid [GeoPoint]
     * here at the presentation boundary.
     */
    private fun decodeRoute(delivery: Delivery) {
        val decoded = PolylineDecoder.decode(delivery.routeEncoded)
        val routePoints = if (decoded.isNotEmpty()) {
            decoded.map { GeoPoint(it.lat, it.lng) }
        } else {
            listOf(
                GeoPoint(delivery.pickupLat, delivery.pickupLng),
                GeoPoint(delivery.dropOffLat, delivery.dropOffLng)
            )
        }
        _state.update { it.copy(routePoints = routePoints) }
    }

    /**
     * Calls GET /deliveries/{id}/track once to seed the last persisted lat/lng.
     * This prevents an empty map while waiting for the first Realtime broadcast.
     */
    private suspend fun seedInitialLocation(deliveryId: String) {
        when (val result = deliveryRepository.trackDelivery(deliveryId)) {
            is AppResult.Success -> {
                val info = result.data
                if (info.driverLat != null && info.driverLng != null) {
                    _state.update { it.copy(
                        driverLat = info.driverLat,
                        driverLng = info.driverLng
                    ) }
                }
            }
            else -> {} // Silently ignore — Realtime will provide updates shortly
        }
    }

    /**
     * Subscribe to Supabase Realtime Broadcast channel "tracking:{deliveryId}".
     * Each broadcast event contains {lat, lng} — we update only coordinates,
     * never the full delivery object (to avoid resetting delivery info).
     */
    private fun subscribeToRealtime(deliveryId: String) {
        // Always clean up any previous subscription first — prevents zombie channels
        cleanupRealtime()

        viewModelScope.launch {
            try {
                val channel = supabaseClient.realtime.channel("tracking:$deliveryId") {
                    broadcast { }
                }
                realtimeChannel = channel

                broadcastJob = channel.broadcastFlow<JsonObject>(event = "location")
                    .onEach { payload ->
                        val lat = payload["lat"]?.jsonPrimitive?.double ?: return@onEach
                        val lng = payload["lng"]?.jsonPrimitive?.double ?: return@onEach
                        _state.update { it.copy(
                            driverLat = lat,
                            driverLng = lng
                        ) }
                        Log.d(TAG, "Location update: lat=$lat, lng=$lng")
                    }
                    .launchIn(viewModelScope)

                channel.subscribe()
                _state.update { it.copy(isTracking = true) }
                Log.d(TAG, "Subscribed to realtime channel: tracking:$deliveryId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to realtime: ${e.message}")
            }
        }
    }

    /**
     * Clean up realtime channel. Uses [NonCancellable] so the cleanup
     * completes even when called from onCleared() (viewModelScope cancelled).
     */
    private fun cleanupRealtime() {
        broadcastJob?.cancel()
        broadcastJob = null
        val channel = realtimeChannel
        realtimeChannel = null
        _state.update { it.copy(isTracking = false) }
        if (channel != null) {
            viewModelScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
                try {
                    supabaseClient.realtime.removeChannel(channel)
                    Log.d(TAG, "Removed realtime channel")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove channel: ${e.message}")
                }
            }
        }
    }

    private fun cancelDelivery(deliveryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showCancelDialog = false) }
            when (val result = deliveryRepository.cancelDelivery(
                deliveryId,
                _state.value.cancelReason.ifBlank { null }
            )) {
                is AppResult.Success -> {
                    cleanupRealtime()
                    _state.update { it.copy(
                        delivery = result.data,
                        isLoading = false,
                        actionMessage = UiText.StringResource(R.string.action_delivery_cancelled),
                        cancelReason = ""
                    ) }
                }
                is AppResult.Error -> _state.update { it.copy(
                    isLoading = false,
                    error = UiText.DynamicString(result.message)
                ) }
                else -> {}
            }
        }
    }

    private fun rateDelivery(deliveryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showRateDialog = false) }
            when (val result = deliveryRepository.rateDelivery(
                deliveryId,
                _state.value.rating,
                _state.value.review.ifBlank { null }
            )) {
                is AppResult.Success -> _state.update { it.copy(
                    isLoading = false,
                    actionMessage = UiText.StringResource(R.string.action_driver_rated),
                    review = "",
                    rating = 5
                ) }
                is AppResult.Error -> _state.update { it.copy(
                    isLoading = false,
                    error = UiText.DynamicString(result.message)
                ) }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupRealtime()
    }
}
