package com.example.customerdashapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DashButton
import com.example.customerdashapp.presentation.components.DashTextButton

@Composable
fun PinInputScreen(
    phone: String,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOtp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    LaunchedEffect(uiState.navigateToOtpVerify) {
        uiState.navigateToOtpVerify?.let {
            viewModel.onEvent(AuthEvent.NavigationConsumed)
            onNavigateToOtp()
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.profile_background)
    ) { paddingValues ->
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
                    text = stringResource(R.string.enter_pin_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.text_primary)
                )

                AuthSupportText(text = stringResource(R.string.enter_pin_desc, phone))

                AuthStepIndicator(
                    label = stringResource(R.string.auth_step_label, 2, 2),
                    currentStep = 2,
                    totalSteps = 2
                )

                AuthCard {
                    Text(
                        text = stringResource(R.string.pin_hint),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.text_primary)
                    )

                    AuthPinCodeField(
                        value = uiState.pin,
                        onValueChange = { viewModel.onEvent(AuthEvent.PinChanged(it)) },
                        enabled = !uiState.isLoading,
                        isError = uiState.error != null
                    )

                    AuthSupportText(text = stringResource(R.string.auth_pin_helper))

                    DashButton(
                        text = stringResource(R.string.btn_login),
                        onClick = { viewModel.onEvent(AuthEvent.LoginWithPin(phone)) },
                        isLoading = uiState.isLoading,
                        enabled = uiState.pin.length == 6
                    )

                    DashTextButton(
                        text = stringResource(R.string.forgot_pin),
                        onClick = { viewModel.onEvent(AuthEvent.ForgotPin(phone)) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

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

                DashTextButton(
                    text = stringResource(R.string.nav_back),
                    onClick = onNavigateBack
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
