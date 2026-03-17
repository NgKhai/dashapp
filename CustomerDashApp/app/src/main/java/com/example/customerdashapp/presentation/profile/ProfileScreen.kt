package com.example.customerdashapp.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.customerdashapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.profile_saved)

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLogout()
    }

    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) {
            snackbarHostState.showSnackbar(savedMessage)
            viewModel.clearSavedSuccess()
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.profile_background),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.profile_background)
                ),
                title = {
                    Text(
                        text = stringResource(R.string.profile),
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary)
                    )
                },
                actions = {
                    if (state.customer != null) {
                        IconButton(onClick = viewModel::showEditDialog) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.profile_edit_action),
                                tint = colorResource(R.color.text_primary)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.premium_orange))
            }

            state.customer == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.error ?: stringResource(R.string.error_unknown),
                        color = colorResource(R.color.text_secondary)
                    )
                    Button(
                        onClick = viewModel::loadProfile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.premium_orange)
                        )
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            else -> {
                val customer = state.customer!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileHeaderCard(
                        name = customer.name,
                        phone = customer.phone,
                        email = customer.email,
                        onEdit = viewModel::showEditDialog
                    )

                    ProfileInfoCard(title = stringResource(R.string.profile_account_info_title)) {
                        ProfileInfoRow(
                            icon = Icons.Default.Person,
                            label = stringResource(R.string.profile_full_name_label),
                            value = customer.name
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = colorResource(R.color.surface_variant).copy(alpha = 0.6f)
                        )
                        ProfileInfoRow(
                            icon = Icons.Default.Phone,
                            label = stringResource(R.string.profile_phone_label),
                            value = customer.phone
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = colorResource(R.color.surface_variant).copy(alpha = 0.6f)
                        )
                        val emailValue = customer.email.takeIf { !it.isNullOrBlank() }
                            ?: stringResource(R.string.profile_email_empty)
                        ProfileInfoRow(
                            icon = Icons.Default.Email,
                            label = stringResource(R.string.profile_email_label),
                            value = emailValue,
                            isPlaceholder = customer.email.isNullOrBlank()
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::logout,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.logout), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    // Edit Profile Bottom Sheet
    if (state.showEditDialog) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissEditDialog,
            containerColor = colorResource(R.color.card_background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.text_primary)
                )

                OutlinedTextField(
                    value = state.editName,
                    onValueChange = viewModel::updateEditName,
                    label = { Text(stringResource(R.string.profile_edit_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.saveError == "name_empty",
                    supportingText = if (state.saveError == "name_empty") {
                        { Text(stringResource(R.string.error_name_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = state.editEmail,
                    onValueChange = viewModel::updateEditEmail,
                    label = { Text(stringResource(R.string.profile_edit_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )

                if (state.saveError != null && state.saveError != "name_empty") {
                    Text(
                        text = state.saveError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.premium_orange)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.profile_save_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    phone: String,
    email: String?,
    onEdit: () -> Unit
) {
    val headerGradient = Brush.linearGradient(
        colors = listOf(
            colorResource(R.color.profile_header_start),
            colorResource(R.color.profile_header_end)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(headerGradient)
    ) {
        AsyncImage(
            model = stringResource(R.string.profile_header_image_url),
            contentDescription = stringResource(R.string.profile_header_image_desc),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colorResource(R.color.profile_header_scrim)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = stringResource(R.string.profile_header_title),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colorResource(R.color.card_background))
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    AsyncImage(
                        model = stringResource(R.string.profile_avatar_image_url),
                        contentDescription = stringResource(R.string.profile_avatar_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = phone,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_edit_action),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.card_background)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.text_primary)
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPlaceholder: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.primary_light)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorResource(R.color.premium_orange),
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = colorResource(R.color.text_secondary)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPlaceholder) {
                    colorResource(R.color.text_secondary)
                } else {
                    colorResource(R.color.text_primary)
                }
            )
        }
    }
}
