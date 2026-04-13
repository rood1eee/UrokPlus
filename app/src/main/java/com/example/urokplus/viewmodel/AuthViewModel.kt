package com.example.urokplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urokplus.AuthRepository
import com.example.urokplus.AuthResult
import com.example.urokplus.AuthScreen
import com.example.urokplus.AuthUiState
import com.example.urokplus.UserRole
import com.example.urokplus.util.AppLogger
import com.example.urokplus.util.FormValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel"

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setLogin(login: String) {
        _uiState.value = _uiState.value.copy(login = login, errorMessage = null)
    }

    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun setConfirm(confirm: String) {
        _uiState.value = _uiState.value.copy(confirm = confirm, errorMessage = null)
    }

    fun setRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(role = role)
    }

    fun onLoginClick() {
        val login = _uiState.value.login
        val password = _uiState.value.password

        val validation = FormValidator.validateLoginForm(login, password)
        if (!validation.isValid) {
            AppLogger.w(TAG, "Login validation failed: ${validation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = validation.errorMessage)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                AppLogger.d(TAG, "Attempting login for user: $login")
                
                when (val result = repository.login(login, password)) {
                    is AuthResult.Success -> {
                        AppLogger.i(TAG, "Login successful for user: $login with role: ${result.role}")
                        _uiState.value = _uiState.value.copy(
                            screen = AuthScreen.Main,
                            role = result.role,
                            password = "",
                            isLoading = false
                        )
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Login failed: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Login error", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    fun onRegisterClick() {
        _uiState.value = _uiState.value.copy(
            screen = AuthScreen.Register,
            errorMessage = null,
            infoMessage = null,
            password = "",
            confirm = "",
            role = UserRole.STUDENT,
            login = ""
        )
    }

    fun onSubmitRegister() {
        val login = _uiState.value.login
        val password = _uiState.value.password
        val confirm = _uiState.value.confirm

        val validation = FormValidator.validateRegistrationForm(login, password, confirm)
        if (!validation.isValid) {
            AppLogger.w(TAG, "Registration validation failed: ${validation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = validation.errorMessage)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                AppLogger.d(TAG, "Attempting registration for user: $login with role: ${_uiState.value.role}")
                
                when (val result = repository.register(login, password, confirm, _uiState.value.role)) {
                    is AuthResult.Success -> {
                        AppLogger.i(TAG, "Registration successful for user: $login")
                        _uiState.value = _uiState.value.copy(
                            infoMessage = "Регистрация успешна (${result.role.label})",
                            screen = AuthScreen.Login,
                            password = "",
                            confirm = "",
                            login = "",
                            isLoading = false
                        )
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Registration failed: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Registration error", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    fun onBackFromRegister() {
        _uiState.value = AuthUiState()
    }

    fun onLogout() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "User logging out")
                repository.clearSession()
                _uiState.value = AuthUiState()
                AppLogger.i(TAG, "Logout successful")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Logout error", e)
            }
        }
    }
}
