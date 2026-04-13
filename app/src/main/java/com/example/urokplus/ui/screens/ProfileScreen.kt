package com.example.urokplus.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.urokplus.UserProfile
import java.io.File

@Composable
fun ProfileScreen(
    p: UserProfile,
    role: String,
    isDark: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onEdit: () -> Unit,
    cardColor: Color
) {
    val bgColor1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bgColor2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val accentColor = Color(0xFF3498DB)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E2961)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
    val glassColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f)

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgColor1, bgColor2)))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Профиль", 
                color = mainTextColor, 
                fontSize = 28.sp, 
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
            )

            // КАРТОЧКА ПОЛЬЗОВАТЕЛЯ
            Surface(
                color = glassColor,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if(isDark) 0.dp else 12.dp, shape = RoundedCornerShape(32.dp)),
                border = BorderStroke(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(110.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxSize().border(2.dp, accentColor, CircleShape),
                            shape = CircleShape,
                            color = if(isDark) Color.White.copy(0.1f) else Color(0xFFF0F2F5)
                        ) {
                            if (p.avatarUrl != null) {
                                val model = if (p.avatarUrl.startsWith("http")) p.avatarUrl else File(p.avatarUrl)
                                AsyncImage(
                                    model = model,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person, 
                                    null, 
                                    modifier = Modifier.fillMaxSize().padding(25.dp), 
                                    tint = if(isDark) Color.White.copy(0.7f) else Color.Gray
                                )
                            }
                        }
                        Surface(
                            color = accentColor,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp).align(Alignment.BottomEnd).border(2.dp, if(isDark) bgColor2 else Color.White, CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(p.name, color = mainTextColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(role, color = subTextColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    if (p.grade.isNotBlank() || p.school.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp).width(100.dp), 
                            color = borderColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            ProfileStatItem("Класс", p.grade, mainTextColor, subTextColor)
                            ProfileStatItem("Школа", p.school, mainTextColor, subTextColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("Редактировать данные", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                color = glassColor,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if(isDark) 0.dp else 4.dp, shape = RoundedCornerShape(20.dp)),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(accentColor.copy(0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, null, tint = accentColor)
                    }
                    Text(
                        "Тёмная тема", 
                        color = mainTextColor, 
                        modifier = Modifier.padding(start = 16.dp).weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isDark, 
                        onCheckedChange = onThemeToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Выйти из аккаунта", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String, mainColor: Color, subColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = mainColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = subColor, fontSize = 12.sp)
    }
}
