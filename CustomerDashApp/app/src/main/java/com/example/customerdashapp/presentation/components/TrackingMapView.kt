package com.example.customerdashapp.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Vietnam defaults (Ho Chi Minh City)
private const val DEFAULT_LAT = 10.7769
private const val DEFAULT_LNG = 106.7009
private const val DEFAULT_ZOOM = 12.0

// Vietnam bounding box limits
private const val VN_NORTH = 23.5
private const val VN_SOUTH = 8.0
private const val VN_WEST = 102.0
private const val VN_EAST = 110.0

/**
 * Map composable for showing real-time driver tracking.
 * Shows: pickup (green A), dropoff (red B), driver (orange dot).
 * Map is restricted to Vietnam only.
 */
@Composable
fun TrackingMapView(
    pickupLat: Double,
    pickupLng: Double,
    dropOffLat: Double,
    dropOffLng: Double,
    driverLat: Double?,
    driverLng: Double?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var initialZoomDone by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(DEFAULT_ZOOM)
                    controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LNG))

                    // Restrict to Vietnam
                    setScrollableAreaLimitDouble(
                        BoundingBox(VN_NORTH, VN_EAST, VN_SOUTH, VN_WEST)
                    )
                    minZoomLevel = 6.0
                    maxZoomLevel = 19.0

                    mapView = this
                }
            },
            update = { map ->
                map.overlays.clear()

                // Pickup marker (green A)
                val pickupMarker = Marker(map).apply {
                    id = "pickup_marker"
                    position = GeoPoint(pickupLat, pickupLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createPinDrawable(context, isPickup = true)
                    title = "Điểm lấy hàng"
                }
                map.overlays.add(pickupMarker)

                // Dropoff marker (red B)
                val dropoffMarker = Marker(map).apply {
                    id = "dropoff_marker"
                    position = GeoPoint(dropOffLat, dropOffLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createPinDrawable(context, isPickup = false)
                    title = "Điểm giao hàng"
                }
                map.overlays.add(dropoffMarker)

                // Driver location (orange dot) — animated move
                if (driverLat != null && driverLng != null) {
                    val driverMarker = Marker(map).apply {
                        id = "driver_marker"
                        position = GeoPoint(driverLat, driverLng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createDriverDotDrawable(context)
                        title = "Tài xế"
                    }
                    map.overlays.add(driverMarker)
                }

                // Zoom to fit all points on first load
                if (!initialZoomDone && map.width > 0 && map.height > 0) {
                    val allPoints = mutableListOf(
                        GeoPoint(pickupLat, pickupLng),
                        GeoPoint(dropOffLat, dropOffLng)
                    )
                    if (driverLat != null && driverLng != null) {
                        allPoints.add(GeoPoint(driverLat, driverLng))
                    }
                    try {
                        val bbox = BoundingBox.fromGeoPoints(allPoints)
                        map.zoomToBoundingBox(bbox, true, 80)
                        initialZoomDone = true
                    } catch (_: Exception) {
                        map.controller.setCenter(GeoPoint(pickupLat, pickupLng))
                        map.controller.setZoom(14.0)
                        initialZoomDone = true
                    }
                } else if (!initialZoomDone) {
                    map.post {
                        try {
                            val allPoints = mutableListOf(
                                GeoPoint(pickupLat, pickupLng),
                                GeoPoint(dropOffLat, dropOffLng)
                            )
                            if (driverLat != null && driverLng != null) {
                                allPoints.add(GeoPoint(driverLat, driverLng))
                            }
                            val bbox = BoundingBox.fromGeoPoints(allPoints)
                            map.zoomToBoundingBox(bbox, true, 80)
                        } catch (_: Exception) {
                            map.controller.setCenter(GeoPoint(pickupLat, pickupLng))
                            map.controller.setZoom(14.0)
                        }
                        initialZoomDone = true
                    }
                }

                map.invalidate()
            }
        )

        // Lifecycle management
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView?.onDetach()
            }
        }

        // Driver status overlay
        if (driverLat != null && driverLng != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = "🚗 Tài xế đang di chuyển...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ===== Marker drawables =====

private fun createPinDrawable(context: Context, isPickup: Boolean): BitmapDrawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = if (isPickup) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 3f, size / 3f, paint)

    val path = android.graphics.Path()
    path.moveTo(size / 2f - size / 5f, size / 2.5f)
    path.lineTo(size / 2f, size.toFloat() - 4)
    path.lineTo(size / 2f + size / 5f, size / 2.5f)
    path.close()
    canvas.drawPath(path, paint)

    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 3f, size / 5f, paint)

    paint.color = if (isPickup) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
    paint.textSize = size / 4f
    paint.textAlign = Paint.Align.CENTER
    val letter = if (isPickup) "A" else "B"
    canvas.drawText(letter, size / 2f, size / 3f + size / 12f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createDriverDotDrawable(context: Context): BitmapDrawable {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = Color.WHITE
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    paint.color = Color.parseColor("#FF8C00")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)

    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)

    return BitmapDrawable(context.resources, bitmap)
}
