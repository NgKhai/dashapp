package com.example.customerdashapp.presentation.delivery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.presentation.components.DashButton
import com.example.customerdashapp.presentation.components.DashTextField
import com.example.customerdashapp.presentation.components.StatusChip
import com.example.customerdashapp.presentation.components.TrackingMapView
import com.example.customerdashapp.presentation.components.formatDate
import com.example.customerdashapp.presentation.components.formatPrice
import com.example.customerdashapp.presentation.components.statusDisplayName
import com.example.customerdashapp.ui.theme.RatingStar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailScreen(
    deliveryId: String,
    viewModel: DeliveryDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(deliveryId) {
        viewModel.onEvent(DeliveryDetailEvent.LoadDetail(deliveryId))
    }

    Scaffold(
        containerColor = colorResource(R.color.detail_background),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.detail_background)
                ),
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
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
        }
    ) { paddingValues ->
        when {
            state.isLoading && state.delivery == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorResource(R.color.premium_orange))
                }
            }

            state.delivery != null -> {
                val delivery = state.delivery!!
                val isActiveDelivery = delivery.status in listOf(
                    DeliveryStatus.ACCEPTED,
                    DeliveryStatus.PICKED_UP,
                    DeliveryStatus.DELIVERING
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailHeroCard(delivery)

                    if (isActiveDelivery) {
                        DetailCard(title = stringResource(R.string.detail_live_tracking_title)) {
                            TrackingMapView(
                                pickupLat = delivery.pickupLat,
                                pickupLng = delivery.pickupLng,
                                dropOffLat = delivery.dropOffLat,
                                dropOffLng = delivery.dropOffLng,
                                driverLat = state.driverLat,
                                driverLng = state.driverLng,
                                routePoints = state.routePoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
                    }

                    StatusTimeline(currentStatus = delivery.status)

                    DetailCard(title = stringResource(R.string.detail_route_title)) {
                        AddressRow(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.detail_pickup_label),
                            address = delivery.pickupAddress,
                            tint = colorResource(R.color.secondary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AddressRow(
                            icon = Icons.Default.Place,
                            label = stringResource(R.string.detail_dropoff_label),
                            address = delivery.dropOffAddress,
                            tint = colorResource(R.color.premium_orange)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colorResource(R.color.surface_variant))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItem(
                                label = stringResource(R.string.detail_distance),
                                value = stringResource(
                                    R.string.detail_distance_value,
                                    String.format("%.1f", delivery.distanceKm)
                                )
                            )
                            InfoItem(
                                label = stringResource(R.string.detail_vehicle_type),
                                value = vehicleLabel(delivery.vehicleType)
                            )
                        }

                        if (!delivery.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailNoteRow(
                                title = stringResource(R.string.detail_notes_title),
                                value = delivery.notes
                            )
                        }

                        if (delivery.requiresLoadingHelp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailBadge(text = stringResource(R.string.detail_loading_help))
                        }
                    }

                    if (delivery.items.isNotEmpty()) {
                        DetailCard(title = stringResource(R.string.detail_items_title)) {
                            delivery.items.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colorResource(R.color.premium_orange),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(text = item, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    if (delivery.driverName != null) {
                        DetailCard(title = stringResource(R.string.detail_driver_title)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorResource(R.color.primary_light),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = colorResource(R.color.premium_orange)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = delivery.driverName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    delivery.driverPhone?.let { phone ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = colorResource(R.color.text_secondary)
                                            )
                                            Text(
                                                text = phone,
                                                fontSize = 13.sp,
                                                color = colorResource(R.color.text_secondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (delivery.status == DeliveryStatus.CANCELLED) {
                        DetailCard(
                            title = stringResource(R.string.detail_cancelled_title),
                            titleColor = MaterialTheme.colorScheme.error,
                            containerColor = colorResource(R.color.status_cancelled_bg)
                        ) {
                            if (delivery.cancelledBy != null) {
                                Text(
                                    text = stringResource(
                                        R.string.detail_cancelled_by,
                                        if (delivery.cancelledBy == "CUSTOMER")
                                            stringResource(R.string.detail_cancelled_by_customer)
                                        else
                                            stringResource(R.string.detail_cancelled_by_driver)
                                    ),
                                    fontSize = 13.sp
                                )
                            }
                            if (delivery.cancellationReason != null) {
                                Text(
                                    text = stringResource(R.string.detail_cancel_reason, delivery.cancellationReason),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    state.actionMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorResource(R.color.detail_chip_bg)
                        ) {
                            Text(
                                text = msg.asString(),
                                color = colorResource(R.color.detail_chip_text),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }
                    }

                    ActionSection(
                        delivery = delivery,
                        onCancel = { viewModel.onEvent(DeliveryDetailEvent.ShowCancelDialog) },
                        onRate = { viewModel.onEvent(DeliveryDetailEvent.ShowRateDialog) }
                    )

                    state.error?.let { error ->
                        Text(
                            text = error.asString(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (state.showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(DeliveryDetailEvent.DismissCancelDialog) },
                        title = { Text(stringResource(R.string.cancel_dialog_title)) },
                        text = {
                            Column {
                                Text(stringResource(R.string.cancel_dialog_message))
                                Spacer(modifier = Modifier.height(12.dp))
                                DashTextField(
                                    value = state.cancelReason,
                                    onValueChange = { viewModel.onEvent(DeliveryDetailEvent.UpdateCancelReason(it)) },
                                    label = stringResource(R.string.cancel_dialog_reason_hint)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onEvent(DeliveryDetailEvent.ConfirmCancel(deliveryId)) }) {
                                Text(stringResource(R.string.cancel_dialog_confirm), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.onEvent(DeliveryDetailEvent.DismissCancelDialog) }) {
                                Text(stringResource(R.string.cancel_dialog_dismiss))
                            }
                        }
                    )
                }

                if (state.showRateDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(DeliveryDetailEvent.DismissRateDialog) },
                        title = { Text(stringResource(R.string.rate_dialog_title)) },
                        text = {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    (1..5).forEach { star ->
                                        IconButton(onClick = { viewModel.onEvent(DeliveryDetailEvent.UpdateRating(star)) }) {
                                            Icon(
                                                if (star <= state.rating) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = stringResource(R.string.rate_dialog_star_desc, star),
                                                tint = if (star <= state.rating) RatingStar else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                DashTextField(
                                    value = state.review,
                                    onValueChange = { viewModel.onEvent(DeliveryDetailEvent.UpdateReview(it)) },
                                    label = stringResource(R.string.rate_dialog_review_hint),
                                    singleLine = false
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onEvent(DeliveryDetailEvent.ConfirmRate(deliveryId)) }) {
                                Text(stringResource(R.string.rate_dialog_submit))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.onEvent(DeliveryDetailEvent.DismissRateDialog) }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error?.asString() ?: stringResource(R.string.error_unknown),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeroCard(delivery: Delivery) {
    val headerGradient = Brush.linearGradient(
        colors = listOf(
            colorResource(R.color.detail_header_start),
            colorResource(R.color.detail_header_end)
        )
    )
    val createdAt = delivery.createdAt?.let { formatDate(it) } ?: stringResource(R.string.detail_date_unknown)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
        border = BorderStroke(1.dp, colorResource(R.color.detail_section_border))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.detail_summary_title),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(status = delivery.status)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.detail_total_price),
                        fontSize = 12.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Text(
                        text = formatPrice(delivery.totalPrice),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailMetaItem(
                    label = stringResource(R.string.detail_order_id_label),
                    value = delivery.deliveryId
                )
                DetailMetaItem(
                    label = stringResource(R.string.detail_created_label),
                    value = createdAt
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailBadge(
                    text = stringResource(
                        R.string.detail_distance_value,
                        String.format("%.1f", delivery.distanceKm)
                    )
                )
                DetailBadge(text = vehicleLabel(delivery.vehicleType))
            }
        }
    }
}

@Composable
private fun DetailMetaItem(label: String, value: String) {
    Column(modifier = Modifier.width(140.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorResource(R.color.text_secondary)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = colorResource(R.color.detail_chip_bg)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = colorResource(R.color.detail_chip_text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    titleColor: Color = colorResource(R.color.text_primary),
    containerColor: Color = colorResource(R.color.card_background),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, colorResource(R.color.detail_section_border))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            content()
        }
    }
}

@Composable
private fun StatusTimeline(currentStatus: DeliveryStatus) {
    val allStatuses = listOf(
        DeliveryStatus.PENDING,
        DeliveryStatus.ACCEPTED,
        DeliveryStatus.PICKED_UP,
        DeliveryStatus.DELIVERING,
        DeliveryStatus.COMPLETED
    )
    val currentIndex = allStatuses.indexOf(currentStatus)

    DetailCard(title = stringResource(R.string.detail_timeline_title)) {
        if (currentStatus == DeliveryStatus.CANCELLED) {
            Text(
                text = stringResource(R.string.detail_order_cancelled),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        } else {
            allStatuses.forEachIndexed { index, status ->
                val isCompleted = index < currentIndex
                val isCurrent = index == currentIndex
                val lineColor = if (isCompleted || isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = if (isCompleted || isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = CircleShape
                                )
                        )
                        if (index != allStatuses.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(22.dp)
                                    .background(lineColor)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = statusDisplayName(status),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCompleted || isCurrent)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    address: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                text = address,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorResource(R.color.text_secondary)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailNoteRow(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = colorResource(R.color.text_secondary)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionSection(
    delivery: Delivery,
    onCancel: () -> Unit,
    onRate: () -> Unit
) {
    val canCancel = delivery.status in listOf(
        DeliveryStatus.PENDING,
        DeliveryStatus.ACCEPTED,
        DeliveryStatus.PICKED_UP
    )
    val canRate = delivery.status == DeliveryStatus.COMPLETED

    if (!canCancel && !canRate) return

    DetailCard(title = stringResource(R.string.detail_actions_title)) {
        if (canCancel) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.detail_cancel_button))
            }
        }
        if (canRate) {
            DashButton(
                text = stringResource(R.string.detail_rate_button),
                onClick = onRate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun vehicleLabel(vehicleType: String): String {
    return when (vehicleType) {
        "MOTORCYCLE" -> stringResource(R.string.vehicle_motorcycle_emoji)
        "CAR" -> stringResource(R.string.vehicle_car_emoji)
        "VAN" -> stringResource(R.string.vehicle_van_emoji)
        "TRUCK" -> stringResource(R.string.vehicle_truck_emoji)
        else -> vehicleType
    }
}
