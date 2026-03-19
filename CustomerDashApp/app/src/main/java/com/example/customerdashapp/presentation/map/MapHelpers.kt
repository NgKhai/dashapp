package com.example.customerdashapp.presentation.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.NumberFormat
import java.util.Locale

fun formatVND(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount)}đ"
}

fun createMarkerDrawable(context: Context, isPickup: Boolean): BitmapDrawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Pin body color
    paint.color = if (isPickup) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
    paint.style = Paint.Style.FILL

    // Circle head
    canvas.drawCircle(size / 2f, size / 3f, size / 3f, paint)

    // Triangle pointer
    val path = android.graphics.Path()
    path.moveTo(size / 2f - size / 5f, size / 2.5f)
    path.lineTo(size / 2f, size.toFloat() - 4)
    path.lineTo(size / 2f + size / 5f, size / 2.5f)
    path.close()
    canvas.drawPath(path, paint)

    // White inner circle
    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 3f, size / 5f, paint)

    // Letter
    paint.color = if (isPickup) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
    paint.textSize = size / 4f
    paint.textAlign = Paint.Align.CENTER
    val letter = if (isPickup) "A" else "B"
    canvas.drawText(letter, size / 2f, size / 3f + size / 12f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

fun updateMarker(
    context: Context,
    mapView: MapView,
    lat: Double,
    lng: Double,
    isPickup: Boolean,
    markerId: String
) {
    // Remove existing marker with same ID
    mapView.overlays.removeAll { it is Marker && it.id == markerId }

    val marker = Marker(mapView).apply {
        id = markerId
        position = GeoPoint(lat, lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        icon = createMarkerDrawable(context, isPickup)
    }
    mapView.overlays.add(marker)
    mapView.invalidate()
}

fun drawRoute(mapView: MapView, points: List<GeoPoint>) {
    // Remove existing polylines
    mapView.overlays.removeAll { it is Polyline }

    if (points.size < 2) return

    val polyline = Polyline().apply {
        setPoints(points)
        outlinePaint.color = Color.parseColor("#1565C0")
        outlinePaint.strokeWidth = 10f
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.isAntiAlias = true
    }
    mapView.overlays.add(polyline)
    mapView.invalidate()
}

/** Remove a single marker by ID */
fun clearMarker(mapView: MapView, markerId: String) {
    mapView.overlays.removeAll { it is Marker && it.id == markerId }
    mapView.invalidate()
}

/** Remove all polylines (route) from the map */
fun clearRoute(mapView: MapView) {
    mapView.overlays.removeAll { it is Polyline }
    mapView.invalidate()
}

/**
 * Get the user's current location reliably.
 * 1. Try getLastKnownLocation (GPS → NETWORK)
 * 2. If null, request a single fresh update from NETWORK provider
 */
@SuppressLint("MissingPermission")
fun getUserLocation(
    context: Context,
    onLocationResult: (latitude: Double, longitude: Double) -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try last known first
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnown != null) {
            onLocationResult(lastKnown.latitude, lastKnown.longitude)
            return
        }

        // No cached location — request a single fresh update
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return // No providers available
        }

        locationManager.requestSingleUpdate(
            provider,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationResult(location.latitude, location.longitude)
                }
                @Deprecated("Deprecated")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            },
            Looper.getMainLooper()
        )
    } catch (_: SecurityException) {
        // Permission not granted
    }
}

/**
 * Move the map camera to the user's current location.
 */
fun moveToUserLocation(
    context: Context,
    mapView: MapView?,
    onLocationFound: ((Double, Double) -> Unit)? = null
) {
    getUserLocation(context) { lat, lng ->
        mapView?.controller?.animateTo(GeoPoint(lat, lng))
        mapView?.controller?.setZoom(15.0)
        onLocationFound?.invoke(lat, lng)
    }
}
