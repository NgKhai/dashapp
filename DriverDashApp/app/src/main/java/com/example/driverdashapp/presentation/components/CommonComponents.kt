package com.example.driverdashapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.domain.model.DeliveryStatus
import com.example.driverdashapp.ui.theme.*

@Composable
fun DashTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun DashButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.height(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text = text)
        }
    }
}

@Composable
fun DashTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DashCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun StatusChip(status: DeliveryStatus) {
    val (bg, text) = when (status) {
        DeliveryStatus.PENDING -> StatusPendingBg to StatusPendingText
        DeliveryStatus.ACCEPTED -> StatusAcceptedBg to StatusAcceptedText
        DeliveryStatus.PICKED_UP -> StatusPickedUpBg to StatusPickedUpText
        DeliveryStatus.DELIVERING -> StatusDeliveringBg to StatusDeliveringText
        DeliveryStatus.COMPLETED -> StatusCompletedBg to StatusCompletedText
        DeliveryStatus.CANCELLED -> StatusCancelledBg to StatusCancelledText
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = statusDisplayName(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeliveryListItem(
    pickupAddress: String,
    dropOffAddress: String,
    price: Double,
    status: DeliveryStatus,
    date: String?,
    customerName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashCard(onClick = onClick, modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("📦 $pickupAddress", fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("📍 $dropOffAddress", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(status)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatPrice(price), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Column(horizontalAlignment = Alignment.End) {
                if (customerName != null) {
                    Text("👤 $customerName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (date != null) {
                    Text(formatDate(date), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun statusDisplayName(status: DeliveryStatus): String = when (status) {
    DeliveryStatus.PENDING -> stringResource(R.string.status_pending)
    DeliveryStatus.ACCEPTED -> stringResource(R.string.status_accepted)
    DeliveryStatus.PICKED_UP -> stringResource(R.string.status_picked_up)
    DeliveryStatus.DELIVERING -> stringResource(R.string.status_delivering)
    DeliveryStatus.COMPLETED -> stringResource(R.string.status_completed)
    DeliveryStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}

fun formatPrice(price: Double): String = "${String.format("%,.0f", price)}đ"

fun formatDate(isoDate: String): String = try {
    val parts = isoDate.substring(0, 10).split("-")
    if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
} catch (e: Exception) { isoDate }
