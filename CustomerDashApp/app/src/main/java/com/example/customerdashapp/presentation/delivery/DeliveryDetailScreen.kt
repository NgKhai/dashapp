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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.presentation.util.UiText
import com.example.customerdashapp.presentation.components.*
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading && state.delivery == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.delivery != null) {
            val delivery = state.delivery!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // === LIVE TRACKING MAP ===
                val isActiveDelivery = delivery.status in listOf(
                    DeliveryStatus.ACCEPTED,
                    DeliveryStatus.PICKED_UP,
                    DeliveryStatus.DELIVERING
                )
                if (isActiveDelivery) {
                    DashCard {
                        Text(
                            text = "📍 Theo dõi tài xế",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TrackingMapView(
                            pickupLat = delivery.pickupLat,
                            pickupLng = delivery.pickupLng,
                            dropOffLat = delivery.dropOffLat,
                            dropOffLng = delivery.dropOffLng,
                            driverLat = state.driverLat,
                            driverLng = state.driverLng,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }

                // Status + Price Header
                DashCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.detail_status),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusChip(status = delivery.status)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.detail_total_price),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatPrice(delivery.totalPrice),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Status Timeline
                StatusTimeline(currentStatus = delivery.status)

                // Addresses
                DashCard {
                    Text(
                        text = stringResource(R.string.detail_order_info),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AddressRow(
                        emoji = "📦",
                        label = stringResource(R.string.detail_pickup_label),
                        address = delivery.pickupAddress
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AddressRow(
                        emoji = "📍",
                        label = stringResource(R.string.detail_dropoff_label),
                        address = delivery.dropOffAddress
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.detail_distance),
                            value = stringResource(R.string.detail_distance_value, delivery.distanceKm.toString())
                        )
                        InfoItem(
                            label = stringResource(R.string.detail_vehicle_type),
                            value = when (delivery.vehicleType) {
                                "MOTORCYCLE" -> stringResource(R.string.vehicle_motorcycle_emoji)
                                "CAR" -> stringResource(R.string.vehicle_car_emoji)
                                "VAN" -> stringResource(R.string.vehicle_van_emoji)
                                "TRUCK" -> stringResource(R.string.vehicle_truck_emoji)
                                else -> delivery.vehicleType
                            }
                        )
                    }

                    if (delivery.notes != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.detail_notes, delivery.notes),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (delivery.requiresLoadingHelp) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.detail_loading_help),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Driver Info (if assigned)
                if (delivery.driverName != null) {
                    DashCard {
                        Text(
                            text = stringResource(R.string.detail_driver_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🚗", fontSize = 20.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = delivery.driverName,
                                    fontWeight = FontWeight.Medium
                                )
                                delivery.driverPhone?.let { phone ->
                                    Text(
                                        text = phone,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Cancellation details
                if (delivery.status == DeliveryStatus.CANCELLED) {
                    DashCard {
                        Text(
                            text = stringResource(R.string.detail_cancelled_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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

                // Action message
                state.actionMessage?.let { msg ->
                    Text(
                        text = msg.asString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Buttons
                val canCancel = delivery.status in listOf(
                    DeliveryStatus.PENDING,
                    DeliveryStatus.ACCEPTED,
                    DeliveryStatus.PICKED_UP
                )
                val canRate = delivery.status == DeliveryStatus.COMPLETED

                if (canCancel) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(DeliveryDetailEvent.ShowCancelDialog) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.detail_cancel_button))
                    }
                }

                if (canRate) {
                    DashButton(
                        text = stringResource(R.string.detail_rate_button),
                        onClick = { viewModel.onEvent(DeliveryDetailEvent.ShowRateDialog) }
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = error.asString(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Cancel Dialog
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

            // Rate Dialog
            if (state.showRateDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onEvent(DeliveryDetailEvent.DismissRateDialog) },
                    title = { Text(stringResource(R.string.rate_dialog_title)) },
                    text = {
                        Column {
                            // Star Rating
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
        } else if (state.error != null) {
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

// ============================================
// HELPER COMPOSABLES
// ============================================

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

    DashCard {
        Text(
            text = stringResource(R.string.detail_timeline_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (currentStatus == DeliveryStatus.CANCELLED) {
            Text(
                text = stringResource(R.string.detail_order_cancelled),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        } else {
            allStatuses.forEachIndexed { index, status ->
                val isCompleted = index <= currentIndex
                val isCurrent = index == currentIndex

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = statusDisplayName(status),
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressRow(emoji: String, label: String, address: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = address,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
