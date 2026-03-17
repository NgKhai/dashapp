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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.customerdashapp.R
import com.example.customerdashapp.presentation.components.DashButton
import com.example.customerdashapp.presentation.components.DashTextField

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOtpVerify: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToOtpVerify) {
        uiState.navigateToOtpVerify?.let { phone ->
            viewModel.onEvent(AuthEvent.NavigationConsumed)
            onNavigateToOtpVerify(phone)
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
                    text = stringResource(R.string.create_account),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.text_primary)
                )

                AuthSupportText(text = stringResource(R.string.auth_register_subtitle))

                AuthStepIndicator(
                    label = stringResource(R.string.auth_step_label, 1, 3),
                    currentStep = 1,
                    totalSteps = 3
                )

                AuthCard {
                    DashTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onEvent(AuthEvent.NameChanged(it)) },
                        label = stringResource(R.string.name_hint),
                        enabled = !uiState.isLoading,
                        isError = uiState.error != null
                    )

                    AuthSupportText(text = stringResource(R.string.auth_name_helper))

                    DashTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.onEvent(AuthEvent.PhoneChanged(it)) },
                        label = stringResource(R.string.phone_hint),
                        keyboardType = KeyboardType.Phone,
                        enabled = !uiState.isLoading,
                        isError = uiState.error != null
                    )

                    AuthSupportText(text = stringResource(R.string.auth_phone_helper))
                    AuthSupportText(text = stringResource(R.string.auth_register_note))

                    DashButton(
                        text = stringResource(R.string.btn_register),
                        onClick = { viewModel.onEvent(AuthEvent.Register) },
                        isLoading = uiState.isLoading,
                        enabled = uiState.name.isNotBlank() && uiState.phone.isNotBlank()
                    )

                    if (uiState.error != null) {
                        AuthErrorText(
                            text = stringResource(R.string.error_prefix, uiState.error!!.asString())
                        )
                    }
                }

                AuthFooterLink(
                    text = stringResource(R.string.already_have_account),
                    actionText = stringResource(R.string.btn_login),
                    onClick = onNavigateBack
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
