package com.example.customerdashapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DashButton
import com.example.customerdashapp.presentation.components.DashTextButton
import com.example.customerdashapp.presentation.components.DashTextField

@Composable
fun OtpVerifyScreen(
    phone: String,
    onNavigateBack: () -> Unit,
    onNavigateToSetPin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.needSetPin) {
        if (uiState.needSetPin) {
            onNavigateToSetPin()
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.profile_background)
    ) { paddingValues ->
        val totalSteps = if (uiState.name.isNotBlank()) 3 else 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthBrandHeader(
                    brand = stringResource(R.string.app_name_short),
                    tagline = stringResource(R.string.login_brand_tagline)
                )

                Text(
                    text = stringResource(R.string.enter_otp_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.text_primary)
                )

                AuthSupportText(text = stringResource(R.string.enter_otp_desc, phone))

                AuthStepIndicator(
                    label = stringResource(R.string.auth_step_label, 2, totalSteps),
                    currentStep = 2,
                    totalSteps = totalSteps
                )

                AuthCard {
                    DashTextField(
                        value = uiState.otp,
                        onValueChange = { if (it.length <= 6) viewModel.onEvent(AuthEvent.OtpChanged(it)) },
                        label = stringResource(R.string.otp_hint),
                        keyboardType = KeyboardType.Number,
                        enabled = !uiState.isLoading,
                        isError = uiState.error != null
                    )

                    AuthSupportText(text = stringResource(R.string.auth_otp_helper))

                    DashButton(
                        text = stringResource(R.string.btn_verify),
                        onClick = { viewModel.onEvent(AuthEvent.VerifyOtp(phone)) },
                        isLoading = uiState.isLoading,
                        enabled = uiState.otp.length == 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DashTextButton(
                            text = stringResource(R.string.btn_resend_otp),
                            onClick = { viewModel.onEvent(AuthEvent.CheckPhone) }
                        )
                        DashTextButton(
                            text = stringResource(R.string.auth_change_phone),
                            onClick = onNavigateBack
                        )
                    }

                    AuthSupportText(
                        text = stringResource(R.string.auth_security_note),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    if (uiState.error != null) {
                        AuthErrorText(
                            text = stringResource(R.string.error_prefix, uiState.error!!.asString())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
