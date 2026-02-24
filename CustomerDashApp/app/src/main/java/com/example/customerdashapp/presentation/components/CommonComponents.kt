package com.example.customerdashapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.customerdashapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.ui.theme.*

@Composable
fun DashTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
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
        isError = isError,
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
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text)
        }
    }
}

@Composable
fun DashTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ============================================
// NEW SHARED COMPONENTS
// ============================================

@Composable
fun DashCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier.fillMaxWidth()
    val cardShape = RoundedCornerShape(16.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = cardShape,
            colors = cardColors,
            elevation = cardElevation
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = cardShape,
            colors = cardColors,
            elevation = cardElevation
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun StatusChip(status: DeliveryStatus) {
    val (backgroundColor, textColor) = when (status) {
        DeliveryStatus.PENDING -> StatusPendingBg to StatusPendingText
        DeliveryStatus.ACCEPTED -> StatusAcceptedBg to StatusAcceptedText
        DeliveryStatus.PICKED_UP -> StatusPickedUpBg to StatusPickedUpText
        DeliveryStatus.DELIVERING -> StatusDeliveringBg to StatusDeliveringText
        DeliveryStatus.COMPLETED -> StatusCompletedBg to StatusCompletedText
        DeliveryStatus.CANCELLED -> StatusCancelledBg to StatusCancelledText
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = statusDisplayName(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📦 $pickupAddress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 $dropOffAddress",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(status = status)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPrice(price),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (date != null) {
                Text(
                    text = formatDate(date),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// STATUS DISPLAY NAME RESOLVER (Presentation Layer)
// ============================================

@Composable
fun statusDisplayName(status: DeliveryStatus): String {
    return when (status) {
        DeliveryStatus.PENDING -> stringResource(R.string.status_pending)
        DeliveryStatus.ACCEPTED -> stringResource(R.string.status_accepted)
        DeliveryStatus.PICKED_UP -> stringResource(R.string.status_picked_up)
        DeliveryStatus.DELIVERING -> stringResource(R.string.status_delivering)
        DeliveryStatus.COMPLETED -> stringResource(R.string.status_completed)
        DeliveryStatus.CANCELLED -> stringResource(R.string.status_cancelled)
    }
}

// ============================================
// HELPER FUNCTIONS
// ============================================

fun formatPrice(price: Double): String {
    val formatted = String.format("%,.0f", price)
    return "${formatted}đ"
}

fun formatDate(isoDate: String): String {
    return try {
        // Simple extraction: "2024-01-15T10:30:00.000Z" -> "15/01/2024"
        val parts = isoDate.substring(0, 10).split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
    } catch (e: Exception) {
        isoDate
    }
}

@Composable
fun DashCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun DashButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
