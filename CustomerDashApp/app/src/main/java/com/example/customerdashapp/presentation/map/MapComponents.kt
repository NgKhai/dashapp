package com.example.customerdashapp.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.customerdashapp.R

@Composable
fun AddressSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isPickup: Boolean,
    enabled: Boolean = true,
    onFocus: () -> Unit,
    onClear: () -> Unit
) {
    val dotColor = if (isPickup) {
        if (isCompleted) colorResource(R.color.map_pickup_dot) else colorResource(R.color.outline)
    } else {
        if (isCompleted) colorResource(R.color.map_dropoff_dot) else colorResource(R.color.outline)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier.border(
                    1.5.dp,
                    colorResource(R.color.premium_orange),
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (!enabled) colorResource(R.color.surface_variant).copy(alpha = 0.5f)
                else if (isActive) colorResource(R.color.primary_light).copy(alpha = 0.15f)
                else colorResource(R.color.surface_variant).copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled) { onFocus() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(10.dp))

        // Text field
        OutlinedTextField(
            value = query,
            onValueChange = {
                onFocus()
                onQueryChange(it)
            },
            placeholder = {
                Text(
                    placeholder,
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_secondary).copy(
                        alpha = if (enabled) 0.7f else 0.4f
                    )
                )
            },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ComposeColor.Transparent,
                unfocusedBorderColor = ComposeColor.Transparent,
                disabledBorderColor = ComposeColor.Transparent,
                focusedContainerColor = ComposeColor.Transparent,
                unfocusedContainerColor = ComposeColor.Transparent,
                disabledContainerColor = ComposeColor.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
        )

        // Clear
        if (isCompleted) {
            Icon(
                Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClear() },
                tint = dotColor
            )
        } else if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun RouteInfoChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colorResource(R.color.premium_orange)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.text_primary)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = colorResource(R.color.text_secondary)
        )
    }
}
