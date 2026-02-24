package com.example.driverdashapp.presentation.earnings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.presentation.components.DashCard
import com.example.driverdashapp.presentation.components.formatPrice
import com.example.driverdashapp.ui.theme.EarningsGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(uiState: EarningsUiState, onRefresh: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.earnings_title)) }) }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.earnings == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: stringResource(R.string.no_earnings_data))
            }
            else -> {
                val e = uiState.earnings
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Earnings
                    DashCard {
                        Text("💰 ${stringResource(R.string.total_earnings)}", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(formatPrice(e.totalEarnings), fontSize = 36.sp,
                            fontWeight = FontWeight.Bold, color = EarningsGreen)
                    }

                    // Today's Earnings
                    DashCard {
                        Text("📅 ${stringResource(R.string.today_earnings)}", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(formatPrice(e.todayEarnings), fontSize = 28.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Stats Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashCard(modifier = Modifier.weight(1f)) {
                            Text("🚗", fontSize = 24.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${e.totalDeliveries}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.total_deliveries), fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DashCard(modifier = Modifier.weight(1f)) {
                            Text("⭐", fontSize = 24.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(e.rating?.let { String.format("%.1f", it) } ?: "—",
                                fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.rating_label, e.totalRatings), fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
