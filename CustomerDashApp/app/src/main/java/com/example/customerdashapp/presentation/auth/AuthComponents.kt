package com.example.customerdashapp.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DashTextButton

@Composable
fun AuthHeader(
    emoji: String,
    title: String,
    subtitle: String
) {
    val headerGradient = Brush.linearGradient(
        colors = listOf(
            colorResource(R.color.primary_light),
            colorResource(R.color.surface_light)
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
        border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colorResource(R.color.card_background),
                border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
            ) {
                Text(
                    text = emoji,
                    modifier = Modifier.padding(10.dp),
                    fontSize = 18.sp
                )
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_primary)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colorResource(R.color.text_secondary)
            )
        }
    }
}

@Composable
fun AuthCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.card_background)),
        border = BorderStroke(1.dp, colorResource(R.color.surface_variant))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun AuthFooterLink(
    text: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.size(4.dp))
        DashTextButton(
            text = actionText,
            onClick = onClick
        )
    }
}

@Composable
fun AuthBrandHeader(
    brand: String,
    tagline: String,
    modifier: Modifier = Modifier
) {
    val appIndex = brand.indexOf("App").takeIf { it >= 0 } ?: brand.length
    val accentColor = colorResource(R.color.premium_orange)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append(brand.substring(0, appIndex))
                if (appIndex < brand.length) {
                    withStyle(SpanStyle(color = accentColor)) {
                        append(brand.substring(appIndex))
                    }
                }
            },
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorResource(R.color.text_primary)
        )
        AuthSupportText(text = tagline)
    }
}

@Composable
fun AuthStepIndicator(
    label: String,
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colorResource(R.color.text_secondary),
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (step in 1..totalSteps) {
                val isActive = step <= currentStep
                Box(
                    modifier = Modifier
                        .size(if (step == currentStep) 10.dp else 8.dp)
                        .background(
                            color = if (isActive)
                                colorResource(R.color.premium_orange)
                            else
                                colorResource(R.color.surface_variant),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun AuthSupportText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = colorResource(R.color.text_secondary),
        modifier = modifier
    )
}

@Composable
fun AuthErrorText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AuthPinCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = colorResource(R.color.premium_orange)
    val defaultBorder = colorResource(R.color.surface_variant)

    BasicTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(length)
            onValueChange(filtered)
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(if (isFocused) accentColor else Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(enabled) { focusRequester.requestFocus() },
        decorationBox = { innerTextField ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val spacing = 8.dp
                val boxWidth = calculateBoxWidth(
                    maxWidth = maxWidth,
                    spacing = spacing,
                    length = length
                )

                Box(modifier = Modifier.size(0.dp)) {
                    innerTextField()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (index in 0 until length) {
                        val char = value.getOrNull(index)?.toString() ?: ""
                        val isFilled = index < value.length
                        val isCurrent = index == value.length && value.length < length && isFocused
                        val borderColor = when {
                            isError -> MaterialTheme.colorScheme.error
                            isFilled || isCurrent -> accentColor
                            else -> defaultBorder
                        }

                        Box(
                            modifier = Modifier
                                .size(width = boxWidth, height = 56.dp)
                                .background(
                                    color = if (isCurrent) accentColor.copy(alpha = 0.08f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorResource(R.color.text_primary)
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun calculateBoxWidth(
    maxWidth: Dp,
    spacing: Dp,
    length: Int
): Dp {
    if (length <= 0) return 44.dp
    val totalSpacing = spacing * (length - 1)
    val rawSize = (maxWidth - totalSpacing) / length
    return rawSize.coerceIn(44.dp, 56.dp)
}
