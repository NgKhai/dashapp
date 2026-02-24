package com.example.customerdashapp.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DashButton
import com.example.customerdashapp.presentation.components.DashTextField
import com.example.customerdashapp.presentation.components.DashTextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOtpVerify: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to OTP verify after registration sends OTP
    LaunchedEffect(uiState.navigateToOtpVerify) {
        uiState.navigateToOtpVerify?.let { phone ->
            viewModel.onEvent(AuthEvent.NavigationConsumed)
            onNavigateToOtpVerify(phone)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.create_account),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name input
            DashTextField(
                value = uiState.name,
                onValueChange = { viewModel.onEvent(AuthEvent.NameChanged(it)) },
                label = stringResource(R.string.name_hint),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone input
            DashTextField(
                value = uiState.phone,
                onValueChange = { viewModel.onEvent(AuthEvent.PhoneChanged(it)) },
                label = stringResource(R.string.phone_hint),
                keyboardType = KeyboardType.Phone,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Register button → sends OTP → navigates to OtpVerifyScreen
            DashButton(
                text = stringResource(R.string.btn_register),
                onClick = { viewModel.onEvent(AuthEvent.Register) },
                isLoading = uiState.isLoading
            )

            // Error message
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error!!.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Login link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.already_have_account),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DashTextButton(
                    text = stringResource(R.string.btn_login),
                    onClick = onNavigateBack
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
