package com.example.customerdashapp.presentation.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeliveryScreen(
    viewModel: CreateDeliveryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onDeliveryCreated: (String) -> Unit = {},
    onNavigateToItemPhoto: () -> Unit = {},
    onNavigateToMapPicker: () -> Unit = {},
    detectedItems: List<String>? = null,
    // Combined map result
    pickupAddress: String? = null,
    pickupLat: Double? = null,
    pickupLng: Double? = null,
    dropOffAddress: String? = null,
    dropOffLat: Double? = null,
    dropOffLng: Double? = null,
    routeDistanceKm: Double? = null,
    routeDurationMin: Double? = null,
    estimatedCost: Long? = null,
    routeEncoded: String? = null
) {
    val state by viewModel.state.collectAsState()

    // Load saved addresses on first composition
    LaunchedEffect(Unit) {
        viewModel.onEvent(CreateDeliveryEvent.LoadAddresses)
    }

    // Handle detected items from ItemPhotoScreen
    LaunchedEffect(detectedItems) {
        if (!detectedItems.isNullOrEmpty()) {
            viewModel.onEvent(CreateDeliveryEvent.UpdateItems(detectedItems))
        }
    }

    // Handle combined map result
    LaunchedEffect(pickupAddress, dropOffAddress) {
        if (pickupAddress != null && pickupLat != null && pickupLng != null) {
            viewModel.onEvent(CreateDeliveryEvent.UpdatePickupAddress(pickupAddress))
            viewModel.onEvent(CreateDeliveryEvent.UpdatePickupCoords(pickupLat, pickupLng))
        }
        if (dropOffAddress != null && dropOffLat != null && dropOffLng != null) {
            viewModel.onEvent(CreateDeliveryEvent.UpdateDropOffAddress(dropOffAddress))
            viewModel.onEvent(CreateDeliveryEvent.UpdateDropOffCoords(dropOffLat, dropOffLng))
        }
        if (routeDistanceKm != null && routeDurationMin != null) {
            viewModel.onEvent(CreateDeliveryEvent.UpdateRouteInfo(routeDistanceKm, routeDurationMin))
        }
        if (routeEncoded != null) {
            viewModel.onEvent(CreateDeliveryEvent.UpdateRouteEncoded(routeEncoded))
        }
    }

    // Navigate on successful creation
    LaunchedEffect(state.createdDeliveryId) {
        state.createdDeliveryId?.let { onDeliveryCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_delivery_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Saved Addresses
            if (state.savedAddresses.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.saved_addresses_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                state.savedAddresses.forEach { address ->
                    DashCard(
                        onClick = {
                            if (state.pickupAddress.isBlank()) {
                                viewModel.onEvent(CreateDeliveryEvent.SelectSavedAddress(address, isPickup = true))
                            } else {
                                viewModel.onEvent(CreateDeliveryEvent.SelectSavedAddress(address, isPickup = false))
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                when (address.label.lowercase()) {
                                    "home", "nhà" -> Icons.Default.Home
                                    "work", "công ty" -> Icons.Default.Face
                                    else -> Icons.Default.LocationOn
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = address.label,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = address.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ===== Map Picker Button =====
            Text(
                text = "📍 Địa chỉ giao hàng",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Show selected addresses or prompt to pick on map
            if (state.pickupAddress.isNotBlank() && state.dropOffAddress.isNotBlank()) {
                // Both addresses selected - show summary
                Card(
                    onClick = onNavigateToMapPicker,
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pickup
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🟢", fontSize = 14.sp)
                            Column {
                                Text(
                                    text = stringResource(R.string.pickup_section_title),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.pickupAddress,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp)

                        // Drop-off
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🔴", fontSize = 14.sp)
                            Column {
                                Text(
                                    text = stringResource(R.string.dropoff_section_title),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.dropOffAddress,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }

                        // Route info
                        if (state.routeDistanceKm != null && state.routeDurationMinutes != null) {
                            HorizontalDivider(thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "%.1f km".format(state.routeDistanceKm),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "%.0f phút".format(state.routeDurationMinutes),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (estimatedCost != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = formatVND(estimatedCost),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Tap to change
                        Text(
                            text = "Chạm để thay đổi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            } else {
                // No addresses yet - show map picker button
                OutlinedButton(
                    onClick = onNavigateToMapPicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.map_select_on_map),
                        fontSize = 15.sp
                    )
                }
            }

            // Vehicle Type
            Text(
                text = stringResource(R.string.vehicle_section_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleTypeOption(
                    label = stringResource(R.string.vehicle_motorcycle),
                    emoji = "🏍️",
                    isSelected = state.vehicleType == "MOTORCYCLE",
                    onClick = { viewModel.onEvent(CreateDeliveryEvent.UpdateVehicleType("MOTORCYCLE")) },
                    modifier = Modifier.weight(1f)
                )
                VehicleTypeOption(
                    label = stringResource(R.string.vehicle_car),
                    emoji = "🚗",
                    isSelected = state.vehicleType == "CAR",
                    onClick = { viewModel.onEvent(CreateDeliveryEvent.UpdateVehicleType("CAR")) },
                    modifier = Modifier.weight(1f)
                )
                VehicleTypeOption(
                    label = stringResource(R.string.vehicle_truck),
                    emoji = "🚚",
                    isSelected = state.vehicleType == "TRUCK",
                    onClick = { viewModel.onEvent(CreateDeliveryEvent.UpdateVehicleType("TRUCK")) },
                    modifier = Modifier.weight(1f)
                )
            }

            // ===== AI Item Scan Section =====
            Text(
                text = stringResource(R.string.ai_scan_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onNavigateToItemPhoto,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(R.string.ai_scan_items_button), fontSize = 15.sp)
            }

            // Detected Items Display
            if (state.items.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_detected_items_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        state.items.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(text = item, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Notes
            DashTextField(
                value = state.notes,
                onValueChange = { viewModel.onEvent(CreateDeliveryEvent.UpdateNotes(it)) },
                label = stringResource(R.string.notes_hint),
                singleLine = false
            )

            // Loading Help Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.loading_help),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.loading_help_fee),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.requiresLoadingHelp,
                    onCheckedChange = { viewModel.onEvent(CreateDeliveryEvent.ToggleLoadingHelp(it)) }
                )
            }

            // Error
            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            // Submit Button
            DashButton(
                text = stringResource(R.string.submit_delivery),
                onClick = { viewModel.onEvent(CreateDeliveryEvent.SubmitDelivery) },
                isLoading = state.isLoading,
                enabled = state.pickupAddress.isNotBlank() && state.dropOffAddress.isNotBlank()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatVND(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount)}đ"
}

@Composable
private fun VehicleTypeOption(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            CardDefaults.outlinedCardBorder()
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
