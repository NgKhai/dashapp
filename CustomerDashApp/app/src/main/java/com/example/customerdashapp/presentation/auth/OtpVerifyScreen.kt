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
fun OtpVerifyScreen(
    phone: String,
    onNavigateBack: () -> Unit,
    onNavigateToSetPin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to set PIN after OTP verification
    LaunchedEffect(uiState.needSetPin) {
        if (uiState.needSetPin) {
            onNavigateToSetPin()
        }
    }

    // Navigate to home if already logged in
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.otp_screen_title)) },
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
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "📱",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.enter_otp_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.enter_otp_desc, phone),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // OTP input
            DashTextField(
                value = uiState.otp,
                onValueChange = { if (it.length <= 6) viewModel.onEvent(AuthEvent.OtpChanged(it)) },
                label = stringResource(R.string.otp_hint),
                keyboardType = KeyboardType.Number,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Verify button
            DashButton(
                text = stringResource(R.string.btn_verify),
                onClick = { viewModel.onEvent(AuthEvent.VerifyOtp(phone)) },
                isLoading = uiState.isLoading,
                enabled = uiState.otp.length == 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Resend OTP
            DashTextButton(
                text = stringResource(R.string.btn_resend_otp),
                onClick = { viewModel.onEvent(AuthEvent.CheckPhone) }
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
        }
    }
}
