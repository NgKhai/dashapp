package com.example.customerdashapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.repository.AuthRepository
import com.example.customerdashapp.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val customer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    // Edit dialog state
    val showEditDialog: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = customerRepository.getProfile()) {
                is AppResult.Success -> _state.update { it.copy(
                    customer = result.data,
                    isLoading = false
                ) }
                is AppResult.Error -> _state.update { it.copy(
                    isLoading = false,
                    error = result.message
                ) }
                else -> {}
            }
        }
    }

    fun showEditDialog() {
        val customer = _state.value.customer ?: return
        _state.update { it.copy(
            showEditDialog = true,
            editName = customer.name,
            editEmail = customer.email ?: "",
            saveError = null,
            savedSuccess = false
        ) }
    }

    fun dismissEditDialog() {
        _state.update { it.copy(showEditDialog = false, saveError = null) }
    }

    fun updateEditName(name: String) {
        _state.update { it.copy(editName = name) }
    }

    fun updateEditEmail(email: String) {
        _state.update { it.copy(editEmail = email) }
    }

    fun saveProfile() {
        val name = _state.value.editName.trim()
        if (name.isBlank()) {
            _state.update { it.copy(saveError = "name_empty") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            val email = _state.value.editEmail.trim().ifBlank { null }
            when (val result = customerRepository.updateProfile(name, email)) {
                is AppResult.Success -> _state.update { it.copy(
                    customer = result.data,
                    isSaving = false,
                    showEditDialog = false,
                    savedSuccess = true
                ) }
                is AppResult.Error -> _state.update { it.copy(
                    isSaving = false,
                    saveError = result.message
                ) }
                else -> {}
            }
        }
    }

    fun clearSavedSuccess() {
        _state.update { it.copy(savedSuccess = false) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.update { it.copy(isLoggedOut = true) }
        }
    }
}
