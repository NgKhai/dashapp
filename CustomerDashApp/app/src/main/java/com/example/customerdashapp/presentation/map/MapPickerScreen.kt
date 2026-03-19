package com.example.customerdashapp.presentation.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.*
import com.example.customerdashapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay

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
            // Move map to user location AND auto-set pickup
            moveToUserLocation(context, mapView) { lat, lng ->
                viewModel.onEvent(MapPickerEvent.SetPickupFromLocation(lat, lng))
            }
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
        if (mapView != null) {
            if (state.pickupSelected) {
                updateMarker(context, mapView!!, state.pickupLat, state.pickupLng, true, "pickup_marker")
                mapView?.controller?.animateTo(GeoPoint(state.pickupLat, state.pickupLng))
                mapView?.controller?.setZoom(15.0)
            } else {
                // Clear marker when pickup is reset
                clearMarker(mapView!!, "pickup_marker")
                clearRoute(mapView!!)
            }
        }
    }

    // Update markers when drop-off is selected
    LaunchedEffect(state.dropOffLat, state.dropOffLng, state.dropOffSelected) {
        if (mapView != null) {
            if (state.dropOffSelected) {
                updateMarker(context, mapView!!, state.dropOffLat, state.dropOffLng, false, "dropoff_marker")
                mapView?.controller?.animateTo(GeoPoint(state.dropOffLat, state.dropOffLng))
            } else {
                // Clear marker when drop-off is reset
                clearMarker(mapView!!, "dropoff_marker")
                clearRoute(mapView!!)
            }
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

    // Current step number for the app bar
    val currentStepNumber = when {
        !state.pickupSelected -> 1
        !state.dropOffSelected -> 2
        else -> 2
    }

    Scaffold(
        containerColor = colorResource(R.color.surface_light),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.surface_light),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = colorResource(R.color.text_primary)
                        )
                    }

                    // Title + step indicator
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.map_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colorResource(R.color.text_primary)
                        )
                        AnimatedContent(
                            targetState = currentStepNumber,
                            label = "step_anim"
                        ) { step ->
                            Text(
                                text = stringResource(R.string.map_step_label, step),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorResource(R.color.premium_orange)
                            )
                        }
                    }

                    // Animated progress dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        repeat(2) { index ->
                            val isActive = index < currentStepNumber
                            val width by animateDpAsState(
                                targetValue = if (isActive) 20.dp else 8.dp,
                                animationSpec = tween(300),
                                label = "dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .background(
                                        color = if (isActive) colorResource(R.color.premium_orange)
                                        else colorResource(R.color.map_search_border),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    // My location button
                    IconButton(onClick = { moveToUserLocation(context, mapView) }) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = colorResource(R.color.premium_orange)
                        )
                    }
                }
            }
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
                            moveToUserLocation(ctx, this) { lat, lng ->
                                viewModel.onEvent(MapPickerEvent.SetPickupFromLocation(lat, lng))
                            }
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
                    color = colorResource(R.color.map_search_bg),
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
                        color = colorResource(R.color.map_search_bg),
                        tonalElevation = 4.dp
                    ) {
                        if (state.isSearching) {
                            // Lottie search loading
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(R.raw.anim_search)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LottieAnimation(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.map_searching),
                                    fontSize = 14.sp,
                                    color = colorResource(R.color.text_secondary)
                                )
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
                                            tint = colorResource(R.color.premium_orange),
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
                                        color = colorResource(R.color.map_search_border)
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
                    color = colorResource(R.color.map_hint_bg).copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_step_label,
                                if (!state.pickupSelected) 1 else 2
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.premium_orange)
                        )
                        Text(
                            text = hintText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.text_primary)
                        )
                    }
                }
            }

            // ===== Reverse geocoding indicator =====
            if (state.isReverseGeocoding) {
                val geoComposition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.anim_search)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = colorResource(R.color.map_search_bg).copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LottieAnimation(
                            composition = geoComposition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.map_searching),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_secondary)
                        )
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
                    color = colorResource(R.color.map_bottom_sheet_bg),
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.premium_orange)
                            )
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = ComposeColor.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.map_confirm_delivery),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComposeColor.White
                            )
                        }
                    }
                }
            }

            // ===== Loading route indicator =====
            if (state.isLoadingRoute) {
                val routeComposition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.anim_route)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = colorResource(R.color.map_search_bg).copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LottieAnimation(
                            composition = routeComposition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = stringResource(R.string.map_loading_route),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                    }
                }
            }
        }
    }
}
