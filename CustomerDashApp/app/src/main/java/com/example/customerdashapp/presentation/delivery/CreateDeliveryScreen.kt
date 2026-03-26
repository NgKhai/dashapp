package com.example.customerdashapp.presentation.delivery

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DashButton
import com.example.customerdashapp.presentation.components.DashTextField
import com.example.customerdashapp.presentation.components.FullScreenPhotoDialog
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
    detectedPhotoUris: List<String>? = null,
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

    // Handle photo URIs from ItemPhotoScreen
    LaunchedEffect(detectedPhotoUris) {
        if (!detectedPhotoUris.isNullOrEmpty()) {
            viewModel.onEvent(CreateDeliveryEvent.UpdatePhotoUris(detectedPhotoUris))
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
        containerColor = colorResource(R.color.profile_background),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.profile_background)
                ),
                title = {
                    Text(
                        text = stringResource(R.string.create_delivery_title),
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = colorResource(R.color.text_primary)
                        )
                    }
                }
            )
        },
        bottomBar = {
            CreateDeliveryBottomBar(
                isLoading = state.isLoading,
                enabled = state.pickupAddress.isNotBlank() && state.dropOffAddress.isNotBlank(),
                price = state.vehiclePrices[state.vehicleType],
                requiresLoadingHelp = state.requiresLoadingHelp,
                loadingHelpFee = state.loadingHelpFee,
                onSubmit = { viewModel.onEvent(CreateDeliveryEvent.SubmitDelivery) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard()

            if (state.savedAddresses.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.saved_addresses_title))
                state.savedAddresses.forEach { address ->
                    SavedAddressCard(
                        label = address.label,
                        address = address.address,
                        onClick = {
                            if (state.pickupAddress.isBlank()) {
                                viewModel.onEvent(
                                    CreateDeliveryEvent.SelectSavedAddress(address, isPickup = true)
                                )
                            } else {
                                viewModel.onEvent(
                                    CreateDeliveryEvent.SelectSavedAddress(address, isPickup = false)
                                )
                            }
                        }
                    )
                }
            }

            SectionHeader(title = stringResource(R.string.create_delivery_address_title))
            if (state.pickupAddress.isNotBlank() && state.dropOffAddress.isNotBlank()) {
                AddressSummaryCard(
                    pickupAddress = state.pickupAddress,
                    dropOffAddress = state.dropOffAddress,
                    routeDistanceKm = state.routeDistanceKm,
                    routeDurationMinutes = state.routeDurationMinutes,
                    onClick = onNavigateToMapPicker
                )
            } else {
                OutlinedButton(
                    onClick = onNavigateToMapPicker,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.map_select_on_map),
                        fontSize = 15.sp
                    )
                }
            }

            SectionHeader(title = stringResource(R.string.vehicle_section_title))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        VehicleOptionData("MOTORCYCLE", R.string.vehicle_motorcycle, "🏍️"),
                        VehicleOptionData("CAR", R.string.vehicle_car, "🚗"),
                        VehicleOptionData("VAN", R.string.vehicle_van, "🚐"),
                        VehicleOptionData("TRUCK", R.string.vehicle_truck, "🚚")
                    ),
                    key = { it.type }
                ) { option ->
                    VehicleTypeOption(
                        label = stringResource(option.labelRes),
                        emoji = option.emoji,
                        isSelected = state.vehicleType == option.type,
                        price = state.vehiclePrices[option.type],
                        onClick = { viewModel.onEvent(CreateDeliveryEvent.UpdateVehicleType(option.type)) }
                    )
                }
            }

            SectionHeader(
                title = stringResource(R.string.ai_scan_title),
                subtitle = stringResource(R.string.ai_scan_hint)
            )
            OutlinedButton(
                onClick = onNavigateToItemPhoto,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(R.string.ai_scan_items_button), fontSize = 15.sp)
            }

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

            // Photo thumbnails
            if (state.photoUris.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.card_background)
                    ),
                    border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.photo_section_title, state.photoUris.size),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(state.photoUris) { index, uriString ->
                                Box {
                                    AsyncImage(
                                        model = Uri.parse(uriString),
                                        contentDescription = stringResource(R.string.photo_thumbnail_desc, index + 1),
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.onEvent(CreateDeliveryEvent.SelectPhoto(uriString)) },
                                        contentScale = ContentScale.Crop
                                    )
                                    // Remove button
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .clickable {
                                                viewModel.onEvent(CreateDeliveryEvent.RemovePhoto(index))
                                            },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_photo),
                                            tint = Color.White,
                                            modifier = Modifier.padding(2.dp).size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.create_delivery_notes_title))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
                border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    DashTextField(
                        value = state.notes,
                        onValueChange = { viewModel.onEvent(CreateDeliveryEvent.UpdateNotes(it)) },
                        label = stringResource(R.string.notes_hint),
                        singleLine = false
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
                border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.loading_help),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (state.loadingHelpFee > 0)
                                stringResource(R.string.loading_help_fee, formatVND(state.loadingHelpFee))
                            else
                                stringResource(R.string.loading_help_fee, "..."),
                            fontSize = 12.sp,
                            color = if (state.requiresLoadingHelp)
                                colorResource(R.color.premium_orange)
                            else
                                colorResource(R.color.text_secondary)
                        )
                    }
                    Switch(
                        checked = state.requiresLoadingHelp,
                        onCheckedChange = { viewModel.onEvent(CreateDeliveryEvent.ToggleLoadingHelp(it)) }
                    )
                }
            }

            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    state.selectedPhotoUrl?.let { url ->
        FullScreenPhotoDialog(
            photoUrl = url,
            onDismiss = { viewModel.onEvent(CreateDeliveryEvent.SelectPhoto(null)) }
        )
    }
}

@Composable
private fun HeaderCard() {
    val headerGradient = Brush.linearGradient(
        colors = listOf(
            colorResource(R.color.primary_light),
            colorResource(R.color.surface_light)
        )
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = colorResource(R.color.card_background),
                    border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
                ) {
                    Text(
                        text = "📦",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.create_delivery_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(R.color.text_primary)
                    )
                    Text(
                        text = stringResource(R.string.create_delivery_subtitle),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.text_primary)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colorResource(R.color.text_secondary)
            )
        }
    }
}

@Composable
private fun SavedAddressCard(
    label: String,
    address: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
        border = BorderStroke(1.dp, colorResource(R.color.surface_variant)),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colorResource(R.color.primary_light)
            ) {
                Text(
                    text = when (label.lowercase()) {
                        "home", "nhà" -> "🏠"
                        "work", "công ty" -> "🏢"
                        else -> "📍"
                    },
                    modifier = Modifier.padding(10.dp),
                    fontSize = 18.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_primary)
                )
                Text(
                    text = address,
                    fontSize = 12.sp,
                    color = colorResource(R.color.text_secondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddressSummaryCard(
    pickupAddress: String,
    dropOffAddress: String,
    routeDistanceKm: Double?,
    routeDurationMinutes: Double?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
        border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AddressRow(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.pickup_section_title),
                value = pickupAddress,
                tint = colorResource(R.color.secondary)
            )

            HorizontalDivider(thickness = 0.5.dp, color = colorResource(R.color.surface_variant))

            AddressRow(
                icon = Icons.Default.Place,
                label = stringResource(R.string.dropoff_section_title),
                value = dropOffAddress,
                tint = colorResource(R.color.premium_orange)
            )

            if (routeDistanceKm != null && routeDurationMinutes != null) {
                HorizontalDivider(thickness = 0.5.dp, color = colorResource(R.color.surface_variant))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "%.1f km".format(routeDistanceKm),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "%.0f phút".format(routeDurationMinutes),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.create_delivery_change_address),
                fontSize = 12.sp,
                color = colorResource(R.color.premium_orange),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun AddressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(8.dp).size(16.dp)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = colorResource(R.color.text_secondary)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CreateDeliveryBottomBar(
    isLoading: Boolean,
    enabled: Boolean,
    price: Long?,
    requiresLoadingHelp: Boolean = false,
    loadingHelpFee: Long = 0L,
    onSubmit: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        color = colorResource(R.color.card_background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (price != null) {
                // Show fee breakdown when loading help is enabled
                if (requiresLoadingHelp && loadingHelpFee > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.loading_help),
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                        Text(
                            text = stringResource(R.string.loading_help_fee, formatVND(loadingHelpFee)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.premium_orange)
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = colorResource(R.color.surface_variant)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.create_delivery_estimate_label),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Text(
                        text = formatVND(if (requiresLoadingHelp) (price + loadingHelpFee) else price),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.premium_orange)
                    )
                }
            }
            DashButton(
                text = stringResource(R.string.submit_delivery),
                onClick = onSubmit,
                isLoading = isLoading,
                enabled = enabled
            )
        }
    }
}

private fun formatVND(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount)}đ"
}

private data class VehicleOptionData(
    val type: String,
    val labelRes: Int,
    val emoji: String
)

@Composable
private fun VehicleTypeOption(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    price: Long? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(110.dp)
            .heightIn(min = 96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                colorResource(R.color.card_background)
        ),
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, colorResource(R.color.surface_variant))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (price != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatVND(price),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
