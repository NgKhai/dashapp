package com.example.customerdashapp.presentation.delivery

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.presentation.components.DeliveryListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(
    viewModel: DeliveryHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(DeliveryHistoryEvent.LoadHistory)
    }

    // Trigger load-more when the user scrolls near the bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 3 && totalItems > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state.hasMore && !state.isLoadingMore && !state.isLoading) {
            viewModel.onEvent(DeliveryHistoryEvent.LoadMore)
        }
    }

    val filterOptions = listOf(
        null to stringResource(R.string.history_filter_all),
        "PENDING" to stringResource(R.string.history_filter_pending),
        "ACCEPTED" to stringResource(R.string.history_filter_accepted),
        "DELIVERING" to stringResource(R.string.history_filter_delivering),
        "COMPLETED" to stringResource(R.string.history_filter_completed),
        "CANCELLED" to stringResource(R.string.history_filter_cancelled)
    )

    val completedCount = remember(state.deliveries) {
        state.deliveries.count { it.status == DeliveryStatus.COMPLETED }
    }
    val cancelledCount = remember(state.deliveries) {
        state.deliveries.count { it.status == DeliveryStatus.CANCELLED }
    }
    val inProgressCount = remember(state.deliveries, completedCount, cancelledCount) {
        state.deliveries.size - completedCount - cancelledCount
    }

    Scaffold(
        containerColor = colorResource(R.color.history_background),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.history_background)
                ),
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HistoryHeaderCard(
                totalCount = state.deliveries.size,
                filterLabel = filterOptions.firstOrNull { it.first == state.selectedFilter }?.second
                    ?: stringResource(R.string.history_filter_all)
            )

            HistorySummaryRow(
                completedCount = completedCount,
                inProgressCount = inProgressCount,
                cancelledCount = cancelledCount
            )

            FilterChipsRow(
                filterOptions = filterOptions,
                selectedFilter = state.selectedFilter,
                onSelect = { viewModel.onEvent(DeliveryHistoryEvent.FilterHistory(it)) }
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorResource(R.color.premium_orange))
                }
            } else if (state.deliveries.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.deliveries, key = { it.deliveryId }) { delivery ->
                        DeliveryListItem(
                            pickupAddress = delivery.pickupAddress,
                            dropOffAddress = delivery.dropOffAddress,
                            price = delivery.totalPrice,
                            status = delivery.status,
                            date = delivery.createdAt,
                            onClick = { onNavigateToDetail(delivery.deliveryId) }
                        )
                    }

                    // Loading-more indicator at the bottom
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryHeaderCard(
    totalCount: Int,
    filterLabel: String
) {
    val headerGradient = Brush.linearGradient(
        colors = listOf(
            colorResource(R.color.history_header_start),
            colorResource(R.color.history_header_end)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(headerGradient)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.history_pill_bg))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.history_pill_bg))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = colorResource(R.color.history_pill_bg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.history_header_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.history_header_subtitle, totalCount),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryPill(text = stringResource(R.string.history_filter_applied, filterLabel))
                    HistoryPill(text = stringResource(R.string.history_loaded_label, totalCount))
                }
            }
        }
    }
}

@Composable
private fun HistoryPill(text: String) {
    Surface(
        color = colorResource(R.color.history_pill_bg),
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistorySummaryRow(
    completedCount: Int,
    inProgressCount: Int,
    cancelledCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.history_summary_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.text_primary)
        )
        Spacer(Modifier.height(8.dp))
        val items = listOf(
            SummaryItem(
                label = stringResource(R.string.history_summary_in_progress),
                count = inProgressCount,
                containerColor = R.color.status_pending_bg,
                accentColor = R.color.status_pending_text
            ),
            SummaryItem(
                label = stringResource(R.string.history_summary_completed),
                count = completedCount,
                containerColor = R.color.status_completed_bg,
                accentColor = R.color.status_completed_text
            ),
            SummaryItem(
                label = stringResource(R.string.history_summary_cancelled),
                count = cancelledCount,
                containerColor = R.color.status_cancelled_bg,
                accentColor = R.color.status_cancelled_text
            )
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.label }) { item ->
                SummaryCard(item = item)
            }
        }
    }
}

@Composable
private fun SummaryCard(item: SummaryItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(item.containerColor)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .height(72.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colorResource(item.accentColor))
            )
            Column {
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    color = colorResource(item.accentColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.count.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(item.accentColor)
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    filterOptions: List<Pair<String?, String>>,
    selectedFilter: String?,
    onSelect: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.history_filter_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.text_primary)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filterOptions, key = { it.first ?: "ALL" }) { option ->
                val (value, label) = option
                val selected = selectedFilter == value
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(value) },
                    label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .padding(vertical = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorResource(R.color.history_chip_selected_bg),
                        selectedLabelColor = colorResource(R.color.history_chip_selected_text),
                        selectedLeadingIconColor = colorResource(R.color.history_chip_selected_text),
                        containerColor = colorResource(R.color.history_chip_unselected_bg),
                        labelColor = colorResource(R.color.history_chip_unselected_text)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = colorResource(R.color.history_chip_border)
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = colorResource(R.color.history_empty_icon_bg),
            shape = RoundedCornerShape(18.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = colorResource(R.color.history_empty_icon_tint),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.text_primary)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.history_empty_subtitle),
            fontSize = 14.sp,
            color = colorResource(R.color.text_secondary),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

private data class SummaryItem(
    val label: String,
    val count: Int,
    val containerColor: Int,
    val accentColor: Int
)
