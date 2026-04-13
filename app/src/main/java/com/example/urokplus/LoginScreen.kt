package com.example.urokplus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    login: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(authBackgroundBrush())) {
        
        Box(modifier = Modifier.size(200.dp).offset(x = (-60).dp, y = 100.dp).background(Color(0xFFFFB7B2).copy(0.3f), CircleShape))
        Box(modifier = Modifier.size(150.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = (-80).dp).background(Color(0xFFAEC6CF).copy(0.3f), CircleShape))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF32325D).copy(0.7f))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Заменяем проблемный mipmap на безопасную иконку
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "UrokPlus", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = login, onValueChange = onLoginChange, label = { Text("Логин") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedLabelColor = Color.Gray, focusedLabelColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f)))
                OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedLabelColor = Color.Gray, focusedLabelColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f)))
            }

            MessagesBlock(errorMessage = errorMessage, infoMessage = infoMessage)

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)), enabled = !isLoading) {
                    Text(text = if (isLoading) "..." else "Войти", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    login: String,
    password: String,
    confirm: String,
    selectedRole: UserRole,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(authBackgroundBrush())) {
        
        Box(modifier = Modifier.size(220.dp).offset(x = 180.dp, y = (-40).dp).background(Color(0xFFB2E2F2).copy(0.3f), CircleShape))
        Box(modifier = Modifier.size(140.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = (-120).dp).background(Color(0xFFFDFD96).copy(0.3f), CircleShape))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF32325D).copy(0.7f))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Регистрация", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = login, onValueChange = onLoginChange, label = { Text("Логин") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f)))
                OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f)))
                OutlinedTextField(value = confirm, onValueChange = onConfirmChange, label = { Text("Повтор пароля") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f)))
                RoleDropdown(selected = selectedRole, onSelected = onRoleChange)
            }

            MessagesBlock(errorMessage = errorMessage, infoMessage = infoMessage)

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)), enabled = !isLoading) {
                    Text(text = if (isLoading) "..." else "Зарегистрироваться", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Уже есть аккаунт? Войти", color = Color.White.copy(0.7f))
                }
            }
        }
    }
}

@Composable
private fun authBackgroundBrush(): Brush = Brush.verticalGradient(listOf(Color(0xFF050816), Color(0xFF1E2961)))

@Composable
private fun MessagesBlock(errorMessage: String?, infoMessage: String?) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (errorMessage != null) Text(text = errorMessage, color = Color(0xFFFF6B6B), fontSize = 12.sp)
        if (infoMessage != null) Text(text = infoMessage, color = Color(0xFF34D399), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(selected: UserRole, onSelected: (UserRole) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        // Регистрация доступна только для учеников/родителей/учителей.
        // Админские аккаунты создаются через отдельный экран админа.
        val selectableRoles = remember {
            UserRole.entries.filter { it != UserRole.ADMIN }
        }
        OutlinedTextField(
            value = selected.label, onValueChange = {}, readOnly = true, label = { Text("Роль") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f)),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            selectableRoles.forEach { role ->
                DropdownMenuItem(text = { Text(role.label) }, onClick = { onSelected(role); expanded = false })
            }
        }
    }
}
