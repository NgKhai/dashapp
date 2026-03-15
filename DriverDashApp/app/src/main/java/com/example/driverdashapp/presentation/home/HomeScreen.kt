package com.example.driverdashapp.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.presentation.components.*
import com.example.driverdashapp.ui.theme.EarningsGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleOnline: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToPending: () -> Unit,
    onNavigateToActiveDelivery: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_greeting, uiState.driverName.ifBlank { "Tài xế" }),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (uiState.rating != null) {
                            Text("⭐ ${String.format("%.1f", uiState.rating)}", fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Online/Offline Toggle
            DashCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (uiState.isOnline) stringResource(R.string.status_online)
                            else stringResource(R.string.status_offline),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = if (uiState.isOnline) EarningsGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (uiState.isOnline) stringResource(R.string.status_online_desc)
                            else stringResource(R.string.status_offline_desc),
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isOnline,
                        onCheckedChange = { onToggleOnline() },
                        enabled = !uiState.isTogglingStatus,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EarningsGreen,
                            checkedTrackColor = EarningsGreen.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Today's Earnings
            DashCard {
                Text(stringResource(R.string.today_earnings), fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(formatPrice(uiState.todayEarnings), fontSize = 32.sp,
                    fontWeight = FontWeight.Bold, color = EarningsGreen)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.total_deliveries_count, uiState.totalDeliveries),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Active Delivery
            uiState.activeDelivery?.let { delivery ->
                Text(stringResource(R.string.active_delivery), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                DeliveryListItem(
                    pickupAddress = delivery.pickupAddress,
                    dropOffAddress = delivery.dropOffAddress,
                    price = delivery.totalPrice,
                    status = delivery.status,
                    date = delivery.acceptedAt,
                    customerName = delivery.customerName,
                    onClick = { onNavigateToActiveDelivery(delivery.deliveryId) }
                )
            }

            // Pending Orders
            if (uiState.isOnline) {
                DashCard(onClick = onNavigateToPending) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.pending_orders), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.tap_to_view), fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("📦", fontSize = 32.sp)
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Error
            uiState.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }
}
