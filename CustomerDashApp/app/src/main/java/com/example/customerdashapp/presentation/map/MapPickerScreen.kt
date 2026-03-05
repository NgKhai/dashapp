package com.example.customerdashapp.presentation.map

import android.Manifest
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.customerdashapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.NumberFormat
import java.util.Locale

// Vietnam bounding box
private const val VN_NORTH = 23.5
private const val VN_SOUTH = 8.2
private const val VN_EAST = 109.5
private const val VN_WEST = 102.1

// Default center: Ho Chi Minh City
private const val DEFAULT_LAT = 10.7769
private const val DEFAULT_LNG = 106.7009
private const val DEFAULT_ZOOM = 13.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    viewModel: MapPickerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onAddressesConfirmed: (
        pickupAddress: String, pickupLat: Double, pickupLng: Double,
        dropOffAddress: String, dropOffLat: Double, dropOffLng: Double,
        distanceKm: Double, durationMinutes: Double, estimatedCost: Long,
        routeEncoded: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Location permission
    var hasLocationPermission by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            moveToUserLocation(context, mapView)
        }
    }

    // Initialize osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "DashApp/1.0"
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Update markers when pickup is selected
    LaunchedEffect(state.pickupLat, state.pickupLng, state.pickupSelected) {
        if (state.pickupSelected && mapView != null) {
            updateMarker(context, mapView!!, state.pickupLat, state.pickupLng, true, "pickup_marker")
            mapView?.controller?.animateTo(GeoPoint(state.pickupLat, state.pickupLng))
            mapView?.controller?.setZoom(15.0)
        }
    }

    // Update markers when drop-off is selected
    LaunchedEffect(state.dropOffLat, state.dropOffLng, state.dropOffSelected) {
        if (state.dropOffSelected && mapView != null) {
            updateMarker(context, mapView!!, state.dropOffLat, state.dropOffLng, false, "dropoff_marker")
            mapView?.controller?.animateTo(GeoPoint(state.dropOffLat, state.dropOffLng))
        }
    }

    // Draw route when ready — key on BOTH routeInfo and mapView to handle the race
    // condition where mapView is null when routeInfo first arrives
    LaunchedEffect(state.routeInfo, mapView) {
        val route = state.routeInfo ?: return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        // Map domain LatLng → osmdroid GeoPoint at the presentation boundary
        val geoPoints = route.points.map { GeoPoint(it.lat, it.lng) }
        drawRoute(map, geoPoints)
        // Zoom to fit both markers + route
        if (geoPoints.size >= 2) {
            val allPoints = geoPoints.toMutableList()
            allPoints.add(GeoPoint(state.pickupLat, state.pickupLng))
            allPoints.add(GeoPoint(state.dropOffLat, state.dropOffLng))
            val bbox = BoundingBox.fromGeoPoints(allPoints)
            map.zoomToBoundingBox(bbox, true, 100)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_delivery_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    IconButton(onClick = { moveToUserLocation(context, mapView) }) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ===== MAP =====
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(DEFAULT_ZOOM)
                        controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LNG))

                        setScrollableAreaLimitDouble(
                            BoundingBox(VN_NORTH, VN_EAST, VN_SOUTH, VN_WEST)
                        )
                        minZoomLevel = 6.0
                        maxZoomLevel = 19.0

                        // Map tap handler
                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    viewModel.onEvent(MapPickerEvent.TapOnMap(it.latitude, it.longitude))
                                }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        })
                        overlays.add(0, eventsOverlay)
                        mapView = this

                        if (hasLocationPermission) {
                            moveToUserLocation(ctx, this)
                        }
                    }
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

            // ===== TOP: Two Search Bars =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Search bars card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ── Pickup search bar ──
                        AddressSearchField(
                            query = state.pickupQuery,
                            onQueryChange = { viewModel.onEvent(MapPickerEvent.UpdatePickupQuery(it)) },
                            placeholder = stringResource(R.string.pickup_address),
                            isActive = state.activeSearchField == AddressStep.PICKUP,
                            isCompleted = state.pickupSelected,
                            isPickup = true,
                            onFocus = { viewModel.onEvent(MapPickerEvent.SetActiveField(AddressStep.PICKUP)) },
                            onClear = { viewModel.onEvent(MapPickerEvent.ResetPickup) }
                        )

                        // Divider with dots (like Grab/Google Maps)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }

                        // ── Drop-off search bar ──
                        AddressSearchField(
                            query = state.dropOffQuery,
                            onQueryChange = { viewModel.onEvent(MapPickerEvent.UpdateDropOffQuery(it)) },
                            placeholder = stringResource(R.string.dropoff_address),
                            isActive = state.activeSearchField == AddressStep.DROPOFF,
                            isCompleted = state.dropOffSelected,
                            isPickup = false,
                            enabled = state.pickupSelected, // Only enabled after pickup is set
                            onFocus = {
                                if (state.pickupSelected) {
                                    viewModel.onEvent(MapPickerEvent.SetActiveField(AddressStep.DROPOFF))
                                }
                            },
                            onClear = { viewModel.onEvent(MapPickerEvent.ResetDropOff) }
                        )
                    }
                }

                // ── Search results dropdown ──
                AnimatedVisibility(
                    visible = state.searchResults.isNotEmpty() || state.isSearching,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        if (state.isSearching) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.map_searching), fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 220.dp)
                            ) {
                                items(state.searchResults) { result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.onEvent(MapPickerEvent.SelectSearchResult(result))
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = result.displayName,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== CENTER: Step hint =====
            if (!state.pickupSelected || !state.dropOffSelected) {
                val hintText = if (!state.pickupSelected) {
                    stringResource(R.string.map_hint_pickup)
                } else {
                    stringResource(R.string.map_hint_dropoff)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = hintText,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ===== Reverse geocoding indicator =====
            if (state.isReverseGeocoding) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.map_searching), fontSize = 14.sp)
                    }
                }
            }

            // ===== BOTTOM: Route info + Confirm =====
            AnimatedVisibility(
                visible = state.routeInfo != null && state.estimatedCost != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Route summary
                        state.routeInfo?.let { route ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Distance
                                RouteInfoChip(
                                    icon = Icons.Default.LocationOn,
                                    label = stringResource(R.string.map_route_distance_label),
                                    value = "%.1f km".format(route.distanceKm)
                                )
                                // Duration
                                RouteInfoChip(
                                    icon = Icons.Default.DateRange,
                                    label = stringResource(R.string.map_route_duration_label),
                                    value = "%.0f phút".format(route.durationMinutes)
                                )
                                // Cost
                                state.estimatedCost?.let { cost ->
                                    RouteInfoChip(
                                        icon = Icons.Default.Star,
                                        label = stringResource(R.string.map_estimated_cost_label),
                                        value = formatVND(cost)
                                    )
                                }
                            }
                        }

                        // Confirm button
                        Button(
                            onClick = {
                                state.routeInfo?.let { route ->
                                    onAddressesConfirmed(
                                        state.pickupAddress, state.pickupLat, state.pickupLng,
                                        state.dropOffAddress, state.dropOffLat, state.dropOffLng,
                                        route.distanceKm, route.durationMinutes,
                                        state.estimatedCost ?: 0L,
                                        route.routeEncoded
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.map_confirm_delivery),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ===== Loading route indicator =====
            if (state.isLoadingRoute) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.map_loading_route), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ===== Components =====

@Composable
private fun AddressSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isPickup: Boolean,
    enabled: Boolean = true,
    onFocus: () -> Unit,
    onClear: () -> Unit
) {
    val dotColor = if (isPickup) {
        if (isCompleted) ComposeColor(0xFF2E7D32) else MaterialTheme.colorScheme.outline
    } else {
        if (isCompleted) ComposeColor(0xFFC62828) else MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (!enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled) { onFocus() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(10.dp))

        // Text field
        OutlinedTextField(
            value = query,
            onValueChange = {
                onFocus()
                onQueryChange(it)
            },
            placeholder = {
                Text(
                    placeholder,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 0.7f else 0.4f
                    )
                )
            },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ComposeColor.Transparent,
                unfocusedBorderColor = ComposeColor.Transparent,
                disabledBorderColor = ComposeColor.Transparent,
                focusedContainerColor = ComposeColor.Transparent,
                unfocusedContainerColor = ComposeColor.Transparent,
                disabledContainerColor = ComposeColor.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
        )

        // Clear / check icon
        if (isCompleted) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClear() },
                tint = dotColor
            )
        } else if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RouteInfoChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ===== Helper functions =====

private fun formatVND(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount)}đ"
}

private fun createMarkerDrawable(context: Context, isPickup: Boolean): BitmapDrawable {
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

private fun updateMarker(
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

private fun drawRoute(mapView: MapView, points: List<GeoPoint>) {
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

@Suppress("MissingPermission")
private fun moveToUserLocation(context: Context, mapView: MapView?) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        location?.let {
            mapView?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
            mapView?.controller?.setZoom(15.0)
        }
    } catch (_: SecurityException) {
        // Permission not granted
    }
}
