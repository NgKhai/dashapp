package com.example.customerdashapp.presentation.auth

import androidx.compose.foundation.layout.*
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

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPinInput: (String) -> Unit,
    onNavigateToOtpVerify: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when already logged in (session restored)
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    // Navigate to PIN input
    LaunchedEffect(uiState.navigateToPinInput) {
        uiState.navigateToPinInput?.let { phone ->
            viewModel.onEvent(AuthEvent.NavigationConsumed)
            onNavigateToPinInput(phone)
        }
    }

    // Navigate to OTP verify
    LaunchedEffect(uiState.navigateToOtpVerify) {
        uiState.navigateToOtpVerify?.let { phone ->
            viewModel.onEvent(AuthEvent.NavigationConsumed)
            onNavigateToOtpVerify(phone)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // Logo
        Text(
            text = "🚚",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.welcome_back),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Phone input
        DashTextField(
            value = uiState.phone,
            onValueChange = { viewModel.onEvent(AuthEvent.PhoneChanged(it)) },
            label = stringResource(R.string.phone_hint),
            keyboardType = KeyboardType.Phone,
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button → checks phone with backend
        DashButton(
            text = stringResource(R.string.btn_continue),
            onClick = { viewModel.onEvent(AuthEvent.CheckPhone) },
            isLoading = uiState.isLoading
        )

        // Error
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

        // Register link
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.no_account),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DashTextButton(
                text = stringResource(R.string.btn_register),
                onClick = onNavigateToRegister
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
