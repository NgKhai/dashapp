package com.example.customerdashapp.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.customerdashapp.R
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.presentation.util.UiText
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCreateDelivery: () -> Unit = {},
    onNavigateToDeliveryDetail: (String) -> Unit = {},
    onNavigateToDeliveryHistory: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    Scaffold(
        containerColor = colorResource(R.color.surface_light),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.surface_light)
                ),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_greeting),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                        Text(
                            text = state.customerName.ifEmpty { stringResource(R.string.home_default_name) },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_primary)
                        )
                    }
                },
                actions = {}
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.premium_orange))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1580674684081-7617fbf3d745?auto=format&fit=crop&q=80&w=800",
                        contentDescription = "Hero Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dim overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = stringResource(R.string.home_hero_title),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_hero_subtitle),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Inner content with padding
                AnimatedVisibility(
                    visible = !state.isLoading,
                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                        animationSpec = tween(500),
                        initialOffsetY = { 50 }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quick Action — Send Package
                        QuickActionCard(onClick = onNavigateToCreateDelivery)

                        // Active Delivery (if any)
                        state.activeDelivery?.let { delivery ->
                            ActiveDeliveryCard(
                                delivery = delivery,
                                onClick = { onNavigateToDeliveryDetail(delivery.deliveryId) }
                            )
                        }

                    // Recent Deliveries
                    if (state.recentDeliveries.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_recent_orders),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_primary)
                            )
                            TextButton(onClick = onNavigateToDeliveryHistory) {
                                Text(
                                    text = stringResource(R.string.home_view_all),
                                    color = colorResource(R.color.premium_orange),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        state.recentDeliveries.forEach { delivery ->
                            val statusModifier = if (delivery.status == DeliveryStatus.CANCELLED) {
                                Modifier.background(Color.White).clip(RoundedCornerShape(12.dp))
                            } else Modifier
                            
                            Box(modifier = statusModifier.padding(vertical = 4.dp)) {
                                DeliveryListItem(
                                    pickupAddress = delivery.pickupAddress,
                                    dropOffAddress = delivery.dropOffAddress,
                                    price = delivery.totalPrice,
                                    status = delivery.status,
                                    date = delivery.createdAt,
                                    onClick = { onNavigateToDeliveryDetail(delivery.deliveryId) }
                                )
                            }
                        }
                    }

                    // Error message
                    state.error?.let { error ->
                        Text(
                            text = error.asString(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Empty state
                    if (state.recentDeliveries.isEmpty() && state.activeDelivery == null && !state.isLoading) {
                        EmptyState(onCreateDelivery = onNavigateToCreateDelivery)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
                } // End AnimatedVisibility
            }
        }
    }
}

@Composable
private fun QuickActionCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.card_background)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorResource(R.color.primary_light),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = colorResource(R.color.premium_orange),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.home_send_package),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorResource(R.color.text_primary)
                )
                Text(
                    text = stringResource(R.string.home_create_new_delivery),
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_secondary)
                )
            }
        }
    }
}

@Composable
private fun ActiveDeliveryCard(delivery: Delivery, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.card_background)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🚚",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_active_delivery),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary)
                    )
                }
                StatusChip(status = delivery.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual route path
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorResource(R.color.premium_orange)))
                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(colorResource(R.color.surface_variant)))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorResource(R.color.secondary)))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = delivery.pickupAddress,
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = delivery.dropOffAddress,
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (delivery.driverName != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colorResource(R.color.surface_variant).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = colorResource(R.color.text_secondary),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = delivery.driverName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.text_primary)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatPrice(delivery.totalPrice),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.premium_orange)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatPrice(delivery.totalPrice),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.premium_orange),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateDelivery: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = colorResource(R.color.primary_light),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "📦", fontSize = 40.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.text_primary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            fontSize = 14.sp,
            color = colorResource(R.color.text_secondary)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateDelivery,
            modifier = Modifier.width(240.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.premium_orange)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_empty_button),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

