package com.example.urokplus.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urokplus.GradeEvent
import com.example.urokplus.UserRole
import com.example.urokplus.getGradeColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val gradeTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))

@Composable
fun PerformanceScreen(
    textColor: Color,
    @Suppress("UNUSED_PARAMETER") scheduleBg: Color,
    isDark: Boolean,
    @Suppress("UNUSED_PARAMETER") userRole: UserRole,
    gradeEvents: List<GradeEvent>,
    @Suppress("UNUSED_PARAMETER") profileName: String
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    val subjects = gradeEvents.map { it.subject }.distinct().sorted()

    val filteredEvents = if (selectedSubject == null) {
        gradeEvents.sortedByDescending { it.timestamp }
    } else {
        gradeEvents.filter { it.subject == selectedSubject }.sortedByDescending { it.timestamp }
    }

    val bgColor1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bgColor2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E2961)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
    val cardBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgColor1, bgColor2)))) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Успеваемость", 
                color = mainTextColor, 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 28.sp
            )
            
            // СЕКЦИЯ: ИТОГИ ПО ПРЕДМЕТАМ (Горизонтальный список)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SubjectMiniCard(
                    subject = "Все",
                    isSelected = selectedSubject == null,
                    isDark = isDark,
                    mainTextColor = mainTextColor,
                    onClick = { selectedSubject = null }
                )
                
                subjects.forEach { sub ->
                    SubjectMiniCard(
                        subject = sub,
                        isSelected = selectedSubject == sub,
                        isDark = isDark,
                        mainTextColor = mainTextColor,
                        onClick = { selectedSubject = sub }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ДЕТАЛЬНАЯ СТАТИСТИКА (Breakdown)
            val statsToDisplay = if (selectedSubject == null) gradeEvents else gradeEvents.filter { it.subject == selectedSubject }
            val avg = if (statsToDisplay.isNotEmpty()) {
                String.format(Locale.getDefault(), "%.2f", statsToDisplay.mapNotNull { it.grade.toIntOrNull() }.average())
            } else "0.00"
            
            val gradeCounts = statsToDisplay.groupBy { it.grade }.mapValues { it.value.size }

            SubjectDetailHeader(
                title = selectedSubject ?: "Общий итог",
                avg = avg,
                gradeCounts = gradeCounts,
                isDark = isDark,
                mainTextColor = mainTextColor,
                subTextColor = subTextColor,
                cardBg = cardBg
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Журнал оценок",
                color = mainTextColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // СПИСОК ОЦЕНОК
            if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Оценок пока нет", color = subTextColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
                ) {
                    items(filteredEvents) { ev ->
                        GradeRowItemCompact(ev, isDark, mainTextColor, subTextColor, cardBg)
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectMiniCard(subject: String, isSelected: Boolean, isDark: Boolean, mainTextColor: Color, onClick: () -> Unit) {
    val unselectedBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White
    val borderCol = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(0.05f)
    
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF3498DB) else unselectedBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .shadow(if (!isDark && !isSelected) 2.dp else 0.dp, RoundedCornerShape(16.dp)),
        border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.3f) else borderCol)
    ) {
        Text(
            text = subject,
            color = if (isSelected || isDark) Color.White else mainTextColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SubjectDetailHeader(
    title: String, 
    avg: String, 
    gradeCounts: Map<String, Int>, 
    isDark: Boolean,
    mainTextColor: Color,
    subTextColor: Color,
    cardBg: Color
) {
    Surface(
        color = cardBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDark) 0.dp else 6.dp, RoundedCornerShape(24.dp)),
        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = mainTextColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                    Text("статистика успеваемости", color = subTextColor, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(avg, color = mainTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    Text("средний балл", color = subTextColor, fontSize = 10.sp)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("5", "4", "3", "2").forEach { g ->
                    GradeCountBadge(g, gradeCounts[g] ?: 0, mainTextColor, subTextColor)
                }
            }
        }
    }
}

@Composable
fun GradeCountBadge(grade: String, count: Int, mainTextColor: Color, subTextColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(getGradeColor(grade).copy(alpha = 0.15f), CircleShape)
                .border(1.5.dp, getGradeColor(grade).copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(grade, color = mainTextColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(
            text = "$count шт.",
            color = subTextColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun GradeRowItemCompact(ev: GradeEvent, isDark: Boolean, mainTextColor: Color, subTextColor: Color, cardBg: Color) {
    val timeStr = try {
        Instant.ofEpochMilli(ev.timestamp).atZone(ZoneId.systemDefault()).format(gradeTimeFormatter)
    } catch (_: Exception) { "" }

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDark) 0.dp else 2.dp, RoundedCornerShape(16.dp)),
        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(getGradeColor(ev.grade).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .border(1.dp, getGradeColor(ev.grade), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(ev.grade, color = mainTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }

            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(ev.subject, color = mainTextColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(ev.workType, color = subTextColor, fontSize = 12.sp)
            }

            Text(
                text = timeStr,
                color = subTextColor,
                fontSize = 11.sp
            )
        }
    }
}
