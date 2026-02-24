package com.example.driverdashapp.presentation.active

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.domain.model.DeliveryStatus
import com.example.driverdashapp.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDeliveryScreen(
    uiState: ActiveDeliveryUiState,
    onAdvanceStatus: () -> Unit,
    onCancel: (String?) -> Unit,
    onBack: () -> Unit,
    onLocationPermissionGranted: () -> Unit = {}
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    // Request location permission on screen entry
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            onLocationPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.active_delivery)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.delivery == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.delivery_not_found))
            }
            else -> {
                val delivery = uiState.delivery
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status
                    DashCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.current_status), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            StatusChip(delivery.status)
                        }
                    }

                    // ===== MAP =====
                    DashCard {
                        Text("🗺️ Bản đồ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        DeliveryMapView(
                            pickupLat = delivery.pickupLat,
                            pickupLng = delivery.pickupLng,
                            dropOffLat = delivery.dropOffLat,
                            dropOffLng = delivery.dropOffLng,
                            driverLat = uiState.driverLat,
                            driverLng = uiState.driverLng,
                            routePoints = uiState.routePoints,
                            isLoadingRoute = uiState.isLoadingRoute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    // Addresses
                    DashCard {
                        Text("📦 ${stringResource(R.string.pickup)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(delivery.pickupAddress, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("📍 ${stringResource(R.string.dropoff)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(delivery.dropOffAddress, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Customer info
                    if (delivery.customerName != null || delivery.customerPhone != null) {
                        DashCard {
                            Text("👤 ${stringResource(R.string.customer_info)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            delivery.customerName?.let { Text(it, fontSize = 14.sp) }
                            delivery.customerPhone?.let {
                                Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Price & Distance
                    DashCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.price_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatPrice(delivery.totalPrice), fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.distance_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", delivery.distanceKm)} km", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Notes
                    if (delivery.notes != null) {
                        DashCard {
                            Text("📝 ${stringResource(R.string.notes)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(delivery.notes, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Action buttons
                    if (delivery.status in listOf(DeliveryStatus.ACCEPTED, DeliveryStatus.PICKED_UP, DeliveryStatus.DELIVERING)) {
                        val actionText = when (delivery.status) {
                            DeliveryStatus.ACCEPTED -> stringResource(R.string.btn_pickup)
                            DeliveryStatus.PICKED_UP -> stringResource(R.string.btn_delivering)
                            DeliveryStatus.DELIVERING -> stringResource(R.string.btn_complete)
                            else -> ""
                        }
                        DashButton(
                            text = actionText,
                            onClick = onAdvanceStatus,
                            isLoading = uiState.isUpdating
                        )
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !uiState.isUpdating,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Cancel dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_delivery_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.cancel_delivery_desc))
                    Spacer(Modifier.height(8.dp))
                    DashTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = stringResource(R.string.cancel_reason_hint)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCancelDialog = false; onCancel(cancelReason.ifBlank { null }) }) {
                    Text(stringResource(R.string.btn_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.btn_back))
                }
            }
        )
    }
}
