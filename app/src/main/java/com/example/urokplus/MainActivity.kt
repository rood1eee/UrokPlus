package com.example.urokplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.urokplus.ui.theme.UrokPlusTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            UrokPlusTheme {
                val context = LocalContext.current
                val repository = remember { AuthRepository(context) }
                val scope = rememberCoroutineScope()

                var uiState by remember { mutableStateOf(AuthUiState()) }

                fun setError(msg: String?) {
                    uiState = uiState.copy(errorMessage = msg, infoMessage = null)
                }

                fun setInfo(msg: String?) {
                    uiState = uiState.copy(infoMessage = msg, errorMessage = null)
                }

                when (uiState.screen) {
                    AuthScreen.Login -> LoginScreen(
                        login = uiState.login,
                        password = uiState.password,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        infoMessage = uiState.infoMessage,
                        onLoginChange = { uiState = uiState.copy(login = it) },
                        onPasswordChange = { uiState = uiState.copy(password = it) },
                        onLoginClick = {
                            scope.launch {
                                uiState = uiState.copy(isLoading = true)
                                when (val result = repository.login(uiState.login, uiState.password)) {
                                    is AuthResult.Success -> {
                                        uiState = uiState.copy(
                                            screen = AuthScreen.Main,
                                            role = result.role,
                                            password = ""
                                        )
                                    }
                                    is AuthResult.Error -> setError(result.message)
                                }
                                uiState = uiState.copy(isLoading = false)
                            }
                        }
                    )

                    AuthScreen.Register -> RegistrationScreen(
                        login = uiState.login,
                        password = uiState.password,
                        confirm = uiState.confirm,
                        selectedRole = uiState.role,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        infoMessage = uiState.infoMessage,
                        onLoginChange = { uiState = uiState.copy(login = it) },
                        onPasswordChange = { uiState = uiState.copy(password = it) },
                        onConfirmChange = { uiState = uiState.copy(confirm = it) },
                        onRoleChange = { uiState = uiState.copy(role = it) },
                        onSubmit = {
                            scope.launch {
                                uiState = uiState.copy(isLoading = true)
                                when (val result = repository.register(
                                    uiState.login,
                                    uiState.password,
                                    uiState.confirm,
                                    uiState.role
                                )) {
                                    is AuthResult.Success -> {
                                        setInfo("Регистрация успешна (${result.role.label})")
                                        uiState = uiState.copy(screen = AuthScreen.Login)
                                    }
                                    is AuthResult.Error -> setError(result.message)
                                }
                                uiState = uiState.copy(isLoading = false)
                            }
                        },
                        onBack = {
                            uiState = uiState.copy(
                                screen = AuthScreen.Login,
                                errorMessage = null,
                                infoMessage = null,
                                password = "",
                                confirm = ""
                            )
                        }
                    )

                    AuthScreen.Main -> {
                        MainScreen(
                            userRole = uiState.role,
                            onLogout = {
                                repository.clearSession()
                                uiState = AuthUiState()
                            }
                        )
                    }
                }
            }
        }
    }
}
