package com.example.driverdashapp.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.domain.model.VehicleAssignment
import com.example.driverdashapp.presentation.components.DashCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onSetPrimary: (assignmentId: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile_title)) })
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.driver == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(uiState.error?.asString() ?: stringResource(R.string.profile_error)) }

            else -> {
                val driver = uiState.driver
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Profile Header ──────────────────────────────────
                    DashCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("👤", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(driver.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            driver.phone?.let {
                                Text(it, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            driver.email?.let {
                                Text(it, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (driver.rating != null) {
                                    Text("⭐ ${String.format("%.1f", driver.rating)}", fontSize = 16.sp)
                                }
                                Text("🚗 ${driver.totalDeliveries} ${stringResource(R.string.deliveries_count)}",
                                    fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (driver.isVerified) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    if (driver.isVerified) stringResource(R.string.verified)
                                    else stringResource(R.string.not_verified),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // ── Vehicles ────────────────────────────────────────
                    Text(
                        stringResource(R.string.vehicles_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.vehicles.isEmpty()) {
                        DashCard {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.no_vehicles),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        uiState.vehicles.forEach { va ->
                            VehicleCard(
                                va = va,
                                isSettingPrimary = uiState.isSettingPrimary,
                                onSetPrimary = { onSetPrimary(va.id) }
                            )
                        }
                    }

                    // Error from set-primary
                    uiState.setPrimaryError?.let { err ->
                        Text(err.asString(), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    // ── Logout ──────────────────────────────────────────
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.btn_logout))
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    va: VehicleAssignment,
    isSettingPrimary: Boolean,
    onSetPrimary: () -> Unit
) {
    DashCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = vehicleEmoji(va.vehicle?.vehicleType),
                        fontSize = 26.sp
                    )
                    Column {
                        val v = va.vehicle
                        if (v != null) {
                            Text(
                                "${v.brand ?: ""} ${v.model ?: ""}".trim().ifBlank { v.vehicleType },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                v.licensePlate,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (v.color != null || v.year != null) {
                                Text(
                                    listOfNotNull(v.color, v.year?.toString()).joinToString(" • "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(stringResource(R.string.unknown_vehicle))
                        }
                    }
                }

                if (va.isPrimary) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            stringResource(R.string.primary_vehicle),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Set as primary button (only for non-primary vehicles)
            if (!va.isPrimary) {
                OutlinedButton(
                    onClick = onSetPrimary,
                    enabled = !isSettingPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSettingPrimary) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.set_primary_vehicle))
                }
            }
        }
    }
}

private fun vehicleEmoji(vehicleType: String?): String = when (vehicleType?.uppercase()) {
    "CAR" -> "🚗"
    "TRUCK" -> "🚚"
    "BICYCLE" -> "🚲"
    else -> "🏍"  // MOTORCYCLE / default
}
