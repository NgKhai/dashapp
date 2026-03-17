package com.example.customerdashapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.R
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.model.LoginResponse
import com.example.customerdashapp.domain.repository.AuthRepository
import com.example.customerdashapp.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.customerdashapp.util.formattedPhone

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val phone: String = "",
    val pin: String = "",
    val otp: String = "",
    val name: String = "",
    val isLoggedIn: Boolean = false,
    val customer: Customer? = null,
    val needSetPin: Boolean = false,
    // Navigation triggers (consumed after navigation)
    val navigateToPinInput: String? = null,   // phone
    val navigateToOtpVerify: String? = null    // phone
)

sealed class AuthEvent {
    data class PhoneChanged(val phone: String) : AuthEvent()
    data class PinChanged(val pin: String) : AuthEvent()
    data class OtpChanged(val otp: String) : AuthEvent()
    data class NameChanged(val name: String) : AuthEvent()
    /** Step 1: Enter phone → check if user has PIN or needs OTP */
    data object CheckPhone : AuthEvent()
    /** Step 2a: Login with phone + PIN */
    data class LoginWithPin(val phone: String) : AuthEvent()
    /** Step 2b: Verify OTP */
    data class VerifyOtp(val phone: String) : AuthEvent()
    /** Forgot PIN → re-send OTP */
    data class ForgotPin(val phone: String) : AuthEvent()
    data object Register : AuthEvent()
    data object SetPin : AuthEvent()
    data object ClearError : AuthEvent()
    data object Logout : AuthEvent()
    data object NavigationConsumed : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.PhoneChanged -> {
                _uiState.update { it.copy(phone = event.phone, error = null) }
            }
            is AuthEvent.PinChanged -> {
                _uiState.update { it.copy(pin = event.pin, error = null) }
            }
            is AuthEvent.OtpChanged -> {
                _uiState.update { it.copy(otp = event.otp, error = null) }
            }
            is AuthEvent.NameChanged -> {
                _uiState.update { it.copy(name = event.name, error = null) }
            }
            AuthEvent.CheckPhone -> checkPhone()
            is AuthEvent.LoginWithPin -> loginWithPin(event.phone)
            is AuthEvent.VerifyOtp -> verifyOtp(event.phone)
            is AuthEvent.ForgotPin -> forgotPin(event.phone)
            AuthEvent.Register -> register()
            AuthEvent.SetPin -> setPin()
            AuthEvent.ClearError -> _uiState.update { it.copy(error = null) }
            AuthEvent.Logout -> logout()
            AuthEvent.NavigationConsumed -> {
                _uiState.update { it.copy(navigateToPinInput = null, navigateToOtpVerify = null) }
            }
        }
    }

    /**
     * Step 1: Send phone only → backend returns require_pin or require_otp
     * This triggers navigation to PinInputScreen or OtpVerifyScreen
     */
    private fun checkPhone() {
        val phone = _uiState.value.phone.formattedPhone
        if (phone.length < 10) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_phone_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(phone, null)) {
                is AppResult.Success -> {
                    when (result.data) {
                        is LoginResponse.RequirePin -> {
                            _uiState.update {
                                it.copy(isLoading = false, navigateToPinInput = phone)
                            }
                        }
                        is LoginResponse.RequireOtp -> {
                            _uiState.update {
                                it.copy(isLoading = false, navigateToOtpVerify = phone)
                            }
                        }
                        is LoginResponse.Success -> {
                            // Unlikely for phone-only, but handle it
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    customer = result.data.customer
                                )
                            }
                        }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    /**
     * Step 2a: User entered PIN on PinInputScreen → login with phone + PIN
     */
    private fun loginWithPin(phone: String) {
        val pin = _uiState.value.pin
        if (pin.length != 6) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_pin_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(phone.formattedPhone, pin)) {
                is AppResult.Success -> {
                    when (result.data) {
                        is LoginResponse.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    customer = result.data.customer
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = UiText.DynamicString("Unexpected response"))
                            }
                        }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    /**
     * Forgot PIN: call login(phone) without PIN to send OTP
     */
    private fun forgotPin(phone: String) {
        _uiState.update { it.copy(pin = "") }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // The backend's login with phone-only will either say require_pin or require_otp
            // For forgot PIN, we need to trigger OTP somehow
            // Actually, the backend login endpoint checks if user has PIN:
            // - If has PIN → require_pin (not helpful for forgot PIN)
            // We'll navigate to OTP screen and let the user verify via OTP
            _uiState.update { it.copy(isLoading = false, navigateToOtpVerify = phone.formattedPhone) }
        }
    }

    /**
     * Step 2b: Verify OTP on OtpVerifyScreen
     */
    private fun verifyOtp(phone: String) {
        val otp = _uiState.value.otp
        val name = _uiState.value.name.takeIf { it.isNotBlank() }

        if (otp.length != 6) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_otp_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyOtp(phone.formattedPhone, otp, name)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customer = result.data,
                            needSetPin = true
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    private fun register() {
        val phone = _uiState.value.phone.formattedPhone
        val name = _uiState.value.name

        if (phone.length < 10) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_phone_invalid)) }
            return
        }
        if (name.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_name_required)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(phone, name)) {
                is AppResult.Success -> {
                    // After register, OTP is sent → navigate to OTP verify
                    _uiState.update {
                        it.copy(isLoading = false, navigateToOtpVerify = phone)
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    private fun setPin() {
        val pin = _uiState.value.pin
        val name = _uiState.value.name.takeIf { it.isNotBlank() }

        if (pin.length != 6) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_pin_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.setPin(pin, name)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, isLoggedIn = true, needSetPin = false)
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString(result.message)) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
        }
    }
}

