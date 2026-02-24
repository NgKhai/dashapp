package com.example.driverdashapp.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driverdashapp.R
import com.example.driverdashapp.presentation.components.DashButton
import com.example.driverdashapp.presentation.components.DashTextField
import com.example.driverdashapp.presentation.components.DashTextButton

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚗", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_title),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        DashTextField(
            value = uiState.phone,
            onValueChange = { onEvent(AuthEvent.PhoneChanged(it)) },
            label = stringResource(R.string.phone_hint),
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        DashButton(
            text = stringResource(R.string.btn_continue),
            onClick = { onEvent(AuthEvent.CheckPhone) },
            isLoading = uiState.isLoading,
            enabled = uiState.phone.length >= 10
        )
        Spacer(Modifier.height(16.dp))
        DashTextButton(
            text = stringResource(R.string.create_account),
            onClick = onNavigateToRegister
        )
    }
}

@Composable
fun PinInputScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onForgotPin: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔐", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.enter_pin_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.enter_pin_desc, uiState.phone), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        DashTextField(
            value = uiState.pin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onEvent(AuthEvent.PinChanged(it)) },
            label = stringResource(R.string.pin_hint),
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        DashButton(
            text = stringResource(R.string.btn_login),
            onClick = { onEvent(AuthEvent.LoginWithPin) },
            isLoading = uiState.isLoading,
            enabled = uiState.pin.length == 6
        )
        Spacer(Modifier.height(12.dp))
        Row {
            DashTextButton(stringResource(R.string.forgot_pin), onClick = onForgotPin)
            DashTextButton(stringResource(R.string.btn_back), onClick = onBack)
        }
    }
}

@Composable
fun OtpVerifyScreen(
    phone: String,
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.enter_otp_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.enter_otp_desc, phone), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        DashTextField(
            value = uiState.otp,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onEvent(AuthEvent.OtpChanged(it)) },
            label = stringResource(R.string.otp_hint),
            keyboardType = KeyboardType.NumberPassword
        )
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        DashButton(
            text = stringResource(R.string.btn_verify),
            onClick = { onEvent(AuthEvent.VerifyOtp(phone)) },
            isLoading = uiState.isLoading,
            enabled = uiState.otp.length == 6
        )
        Spacer(Modifier.height(12.dp))
        Row {
            DashTextButton(stringResource(R.string.resend_otp), onClick = { onEvent(AuthEvent.ResendOtp(phone)) })
            DashTextButton(stringResource(R.string.btn_back), onClick = onBack)
        }
    }
}

@Composable
fun SetPinScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔑", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.setup_pin), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.setup_pin_desc), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        DashTextField(
            value = uiState.name,
            onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
            label = stringResource(R.string.name_hint)
        )
        Spacer(Modifier.height(12.dp))
        DashTextField(
            value = uiState.pin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onEvent(AuthEvent.PinChanged(it)) },
            label = stringResource(R.string.pin_hint),
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        DashButton(
            text = stringResource(R.string.btn_confirm),
            onClick = { onEvent(AuthEvent.SetPin) },
            isLoading = uiState.isLoading,
            enabled = uiState.pin.length == 6
        )
    }
}

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚗", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.register_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        DashTextField(
            value = uiState.phone,
            onValueChange = { onEvent(AuthEvent.PhoneChanged(it)) },
            label = stringResource(R.string.phone_hint),
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(12.dp))
        DashTextField(
            value = uiState.name,
            onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
            label = stringResource(R.string.name_hint)
        )
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        DashButton(
            text = stringResource(R.string.btn_register),
            onClick = { onEvent(AuthEvent.Register) },
            isLoading = uiState.isLoading,
            enabled = uiState.phone.length >= 10 && uiState.name.isNotBlank()
        )
        Spacer(Modifier.height(12.dp))
        DashTextButton(stringResource(R.string.btn_back), onClick = onBack)
    }
}
