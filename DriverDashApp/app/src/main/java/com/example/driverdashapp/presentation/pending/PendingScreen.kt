package com.example.driverdashapp.presentation.pending

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.domain.model.Delivery
import com.example.driverdashapp.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingScreen(
    uiState: PendingUiState,
    onAccept: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pending_orders)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.deliveries.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_pending_orders), fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        DashButton(stringResource(R.string.btn_refresh), onClick = onRefresh,
                            modifier = Modifier.width(200.dp))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.deliveries) { delivery ->
                        PendingOrderCard(
                            delivery = delivery,
                            isAccepting = uiState.isAccepting == delivery.deliveryId,
                            onAccept = { onAccept(delivery.deliveryId) }
                        )
                    }
                }
            }
        }

        uiState.error?.let {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
            }
        }
    }
}

@Composable
fun PendingOrderCard(delivery: Delivery, isAccepting: Boolean, onAccept: () -> Unit) {
    DashCard {
        Text("📦 ${delivery.pickupAddress}", fontSize = 14.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text("📍 ${delivery.dropOffAddress}", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(formatPrice(delivery.totalPrice), fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${String.format("%.1f", delivery.distanceKm)} km • ${delivery.vehicleType}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onAccept,
                enabled = !isAccepting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isAccepting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.btn_accept))
                }
            }
        }
        if (delivery.notes != null) {
            Spacer(Modifier.height(4.dp))
            Text("📝 ${delivery.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
