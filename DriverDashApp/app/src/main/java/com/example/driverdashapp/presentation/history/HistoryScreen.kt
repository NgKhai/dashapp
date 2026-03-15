package com.example.driverdashapp.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.presentation.components.DeliveryListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onFilterChanged: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDeliveryClick: (String) -> Unit
) {
    val filters = listOf("ALL", "COMPLETED", "CANCELLED")
    val filterLabels = mapOf(
        "ALL" to stringResource(R.string.filter_all),
        "COMPLETED" to stringResource(R.string.filter_completed),
        "CANCELLED" to stringResource(R.string.filter_cancelled)
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.history_title)) })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter tabs
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(uiState.selectedFilter),
                modifier = Modifier.fillMaxWidth()
            ) {
                filters.forEach { filter ->
                    Tab(
                        selected = uiState.selectedFilter == filter,
                        onClick = { onFilterChanged(filter) },
                        text = { Text(filterLabels[filter] ?: filter) }
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.deliveries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val listState = rememberLazyListState()

                    // Detect scroll near bottom → trigger load more
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItem >= listState.layoutInfo.totalItemsCount - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && uiState.hasMore && !uiState.isLoadingMore) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.deliveries) { delivery ->
                            DeliveryListItem(
                                pickupAddress = delivery.pickupAddress,
                                dropOffAddress = delivery.dropOffAddress,
                                price = delivery.totalPrice,
                                status = delivery.status,
                                date = delivery.deliveredAt ?: delivery.cancelledAt ?: delivery.createdAt,
                                customerName = delivery.customerName,
                                onClick = { onDeliveryClick(delivery.deliveryId) }
                            )
                        }
                        // Loading more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
