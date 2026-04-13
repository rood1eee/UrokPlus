package com.example.urokplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urokplus.Lesson
import com.example.urokplus.UserRole
import com.example.urokplus.getGradeColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ScheduleSection(
    lessons: List<Lesson>,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    textColor: Color,
    scheduleBg: Color,
    isDark: Boolean,
    userRole: UserRole,
    emptyStateText: String = "Уроков нет",
    onLessonClick: (Lesson) -> Unit = {}
) {
    val isTeacherLike = userRole == UserRole.TEACHER || userRole == UserRole.ADMIN
    val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(0.3f)
    Surface(
        color = scheduleBg.copy(0.9f),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                if (isTeacherLike) "Мои уроки" else "Расписание",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                val formatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yy", Locale("ru"))
                Text(date.format(formatter), color = textColor, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onDateChange(date.minusDays(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF3498DB))
                }
                IconButton(onClick = { onDateChange(date.plusDays(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF3498DB))
                }
            }
            if (lessons.isEmpty()) {
                Text(
                    emptyStateText,
                    color = textColor.copy(0.5f),
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                lessons.forEach { l ->
                    Surface(
                        color = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onLessonClick(l) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(l.name, color = textColor, fontWeight = FontWeight.Bold)
                                    if (isTeacherLike && l.gradeClass != null) {
                                        Text(
                                            " — ${l.gradeClass}",
                                            color = Color(0xFF3498DB),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                Text(l.time, color = Color.Gray, fontSize = 12.sp)
                            }
                            if (l.grade != null && !isTeacherLike) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(getGradeColor(l.grade)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(l.grade, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF3498DB))
                        }
                    }
                }
            }
        }
    }
}
