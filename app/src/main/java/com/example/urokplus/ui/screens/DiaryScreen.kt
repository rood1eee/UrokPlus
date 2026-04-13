package com.example.urokplus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urokplus.Lesson
import com.example.urokplus.UserRole
import java.time.LocalDate

@Composable
fun DiaryScreen(
    date: LocalDate,
    lessons: List<Lesson>,
    emptyStateText: String,
    onDateChange: (LocalDate) -> Unit,
    textColor: Color,
    scheduleBg: Color,
    isDark: Boolean,
    onLessonClick: (Lesson) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Дневник", color = textColor, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(Modifier.height(16.dp))
        ScheduleSection(
            lessons = lessons,
            date = date,
            onDateChange = onDateChange,
            textColor = textColor,
            scheduleBg = scheduleBg,
            isDark = isDark,
            userRole = UserRole.STUDENT,
            emptyStateText = emptyStateText,
            onLessonClick = onLessonClick
        )
    }
}
