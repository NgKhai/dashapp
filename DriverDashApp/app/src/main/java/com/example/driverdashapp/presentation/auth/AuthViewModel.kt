package com.example.driverdashapp.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driverdashapp.domain.model.AppResult
import com.example.driverdashapp.domain.model.LoginResponse
import com.example.driverdashapp.domain.repository.AuthRepository
import com.example.driverdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.driverdashapp.util.formattedPhone

data class AuthUiState(
    val phone: String = "",
    val name: String = "",
    val pin: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    // Navigation triggers
    val navigateToPin: Boolean = false,
    val navigateToOtp: String? = null, // phone
    val navigateToSetPin: Boolean = false,
    val navigateToHome: Boolean = false,
    val navigateToOtpFromRegister: String? = null, // phone
    val needSetPin: Boolean = false,
    val isLoggedIn: Boolean = false
)

sealed class AuthEvent {
    data class PhoneChanged(val phone: String) : AuthEvent()
    data class NameChanged(val name: String) : AuthEvent()
    data class PinChanged(val pin: String) : AuthEvent()
    data class OtpChanged(val otp: String) : AuthEvent()
    data object CheckPhone : AuthEvent()
    data object LoginWithPin : AuthEvent()
    data class VerifyOtp(val phone: String) : AuthEvent()
    data object SetPin : AuthEvent()
    data object Register : AuthEvent()
    data class ResendOtp(val phone: String) : AuthEvent()
    data object ClearError : AuthEvent()
    data object ClearNavigation : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.PhoneChanged -> uiState = uiState.copy(phone = event.phone, error = null)
            is AuthEvent.NameChanged -> uiState = uiState.copy(name = event.name, error = null)
            is AuthEvent.PinChanged -> uiState = uiState.copy(pin = event.pin, error = null)
            is AuthEvent.OtpChanged -> uiState = uiState.copy(otp = event.otp, error = null)
            is AuthEvent.ClearError -> uiState = uiState.copy(error = null)
            is AuthEvent.ClearNavigation -> uiState = uiState.copy(
                navigateToPin = false, navigateToOtp = null, navigateToSetPin = false,
                navigateToHome = false, navigateToOtpFromRegister = null
            )
            is AuthEvent.CheckPhone -> checkPhone()
            is AuthEvent.LoginWithPin -> loginWithPin()
            is AuthEvent.VerifyOtp -> verifyOtp(event.phone)
            is AuthEvent.SetPin -> setPin()
            is AuthEvent.Register -> register()
            is AuthEvent.ResendOtp -> resendOtp(event.phone)
        }
    }

    private fun checkPhone() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        val formattedPhone = uiState.phone.formattedPhone
        when (val result = authRepository.login(formattedPhone)) {
            is AppResult.Success -> when (result.data) {
                is LoginResponse.RequirePin -> uiState = uiState.copy(isLoading = false, navigateToPin = true)
                is LoginResponse.RequireOtp -> uiState = uiState.copy(isLoading = false, navigateToOtp = result.data.phone)
                is LoginResponse.Success -> uiState = uiState.copy(isLoading = false, navigateToHome = true)
            }
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = UiText.DynamicString(result.message))
        }
    }

    private fun loginWithPin() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        val formattedPhone = uiState.phone.formattedPhone
        when (val result = authRepository.login(formattedPhone, uiState.pin)) {
            is AppResult.Success -> when (result.data) {
                is LoginResponse.Success -> uiState = uiState.copy(isLoading = false, navigateToHome = true)
                else -> uiState = uiState.copy(isLoading = false, error = UiText.DynamicString("Không xác định"))
            }
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = UiText.DynamicString(result.message))
        }
    }

    private fun verifyOtp(phone: String) = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        when (val result = authRepository.verifyOtp(phone.formattedPhone, uiState.otp, uiState.name.ifBlank { null })) {
            is AppResult.Success -> uiState = uiState.copy(
                isLoading = false, isLoggedIn = true, needSetPin = true, navigateToSetPin = true
            )
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = UiText.DynamicString(result.message))
        }
    }

    private fun setPin() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        when (val result = authRepository.setPin(uiState.pin, uiState.name.ifBlank { null })) {
            is AppResult.Success -> uiState = uiState.copy(isLoading = false, needSetPin = false, navigateToHome = true)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = UiText.DynamicString(result.message))
        }
    }

    private fun register() = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        val formattedPhone = uiState.phone.formattedPhone
        when (val result = authRepository.register(formattedPhone, uiState.name)) {
            is AppResult.Success -> uiState = uiState.copy(isLoading = false, navigateToOtpFromRegister = formattedPhone)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false, error = UiText.DynamicString(result.message))
        }
    }

    private fun resendOtp(phone: String) = viewModelScope.launch {
        uiState = uiState.copy(isLoading = true, error = null)
        when (authRepository.login(phone.formattedPhone)) {
            is AppResult.Success -> uiState = uiState.copy(isLoading = false)
            is AppResult.Error -> uiState = uiState.copy(isLoading = false)
        }
    }
}

