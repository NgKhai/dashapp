package com.example.customerdashapp.presentation.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.presentation.util.UiText
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DeliveryListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(
    viewModel: DeliveryHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(DeliveryHistoryEvent.LoadHistory)
    }

    val filterOptions = listOf(
        null to stringResource(R.string.history_filter_all),
        "PENDING" to stringResource(R.string.history_filter_pending),
        "ACCEPTED" to stringResource(R.string.history_filter_accepted),
        "DELIVERING" to stringResource(R.string.history_filter_delivering),
        "COMPLETED" to stringResource(R.string.history_filter_completed),
        "CANCELLED" to stringResource(R.string.history_filter_cancelled)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
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
        ) {
            // Filter Chips
            ScrollableTabRow(
                selectedTabIndex = filterOptions.indexOfFirst { it.first == state.selectedFilter }.coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                filterOptions.forEachIndexed { index, (statusValue, label) ->
                    Tab(
                        selected = state.selectedFilter == statusValue,
                        onClick = { viewModel.onEvent(DeliveryHistoryEvent.FilterHistory(statusValue)) },
                        text = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.deliveries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.history_empty),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.deliveries) { delivery ->
                        DeliveryListItem(
                            pickupAddress = delivery.pickupAddress,
                            dropOffAddress = delivery.dropOffAddress,
                            price = delivery.totalPrice,
                            status = delivery.status,
                            date = delivery.createdAt,
                            onClick = { onNavigateToDetail(delivery.deliveryId) }
                        )
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
