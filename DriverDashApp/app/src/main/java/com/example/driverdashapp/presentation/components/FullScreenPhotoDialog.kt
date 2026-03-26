package com.example.driverdashapp.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage

@Composable
fun FullScreenPhotoDialog(
    photoUrl: Any,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var hasAnimatedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        hasAnimatedIn = true
    }

    val triggerDismiss = {
        isVisible = false
    }

    LaunchedEffect(isVisible) {
        if (!isVisible && hasAnimatedIn) {
            delay(300)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = triggerDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        triggerDismiss()
                    }
            ) {
                ZoomableAsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onClick = { triggerDismiss() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                // Close button
                IconButton(
                    onClick = triggerDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 44.dp, end = 44.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
