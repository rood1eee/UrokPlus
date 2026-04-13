package com.example.urokplus.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urokplus.AuthRepository
import com.example.urokplus.UserRole
import com.example.urokplus.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    textColor: Color,
    scheduleBg: Color,
    isDark: Boolean,
    cardColor: Color,
    repository: AuthRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val accent = Color(0xFF3498DB)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)

    val selectableRoles = listOf(UserRole.STUDENT, UserRole.PARENT, UserRole.TEACHER)

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }

    var name by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("Школа №4") }
    var grade by remember { mutableStateOf("5А") }

    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheduleBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Админ панель",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Surface(
            color = cardColor.copy(alpha = 0.9f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Создать аккаунт",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Логин") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Пароль") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Повтор пароля") },
                    singleLine = true
                )

                // Роль
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRole.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Роль") },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        selectableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ФИО") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Школа") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Класс") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (isLoading) return@Button
                        if (login.isBlank() || password.isBlank() || confirm.isBlank()) {
                            Toast.makeText(context, "Заполните логин и пароли", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirm) {
                            Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (name.isBlank()) {
                            Toast.makeText(context, "Введите ФИО", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            val res = repository.adminRegister(
                                login = login,
                                password = password,
                                confirm = confirm,
                                role = selectedRole,
                                name = name,
                                school = school,
                                grade = grade
                            )

                            isLoading = false
                            when (res) {
                                is Resource.Success -> {
                                    Toast.makeText(context, "Аккаунт создан", Toast.LENGTH_SHORT).show()
                                    login = ""
                                    password = ""
                                    confirm = ""
                                    name = ""
                                    grade = "5А"
                                    selectedRole = UserRole.STUDENT
                                }
                                is Resource.Error -> {
                                    Toast.makeText(context, res.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                                }
                                is Resource.Loading -> {
                                    // В реальном вызове не используется, но нужна ветка для exhaustiveness.
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = if (isLoading) "Создание..." else "Создать аккаунт", color = Color.White)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = "Админ может создавать логины и пароли для учеников/родителей/учителей.",
            color = textColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

