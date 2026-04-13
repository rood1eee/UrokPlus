package com.example.urokplus.util

object FormValidator {
    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

    fun validateLoginForm(login: String, pass: String): ValidationResult {
        if (login.isBlank()) return ValidationResult(false, "Логин не может быть пустым")
        if (pass.isBlank()) return ValidationResult(false, "Пароль не может быть пустым")
        return ValidationResult(true)
    }

    fun validateRegistrationForm(login: String, pass: String, confirm: String): ValidationResult {
        if (login.isBlank()) return ValidationResult(false, "Логин не может быть пустым")
        if (pass.length < 4) return ValidationResult(false, "Пароль слишком короткий")
        if (pass != confirm) return ValidationResult(false, "Пароли не совпадают")
        return ValidationResult(true)
    }
}
