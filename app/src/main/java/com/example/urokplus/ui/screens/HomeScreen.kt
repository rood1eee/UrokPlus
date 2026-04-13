package com.example.urokplus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.urokplus.*
import com.example.urokplus.api.StudentItemDto
import java.io.File
import java.time.LocalDate

@Composable
fun HomeScreen(
    p: UserProfile,
    date: LocalDate,
    lessons: List<Lesson>,
    emptyStateText: String,
    onDateChange: (LocalDate) -> Unit,
    textColor: Color,
    cardColor: Color,
    scheduleBg: Color,
    isDark: Boolean,
    userRole: UserRole,
    gradeEvents: List<GradeEvent>,
    schoolEvents: List<SchoolEvent>,
    assignments: List<Assignment>,
    parentChildren: List<StudentItemDto> = emptyList(),
    selectedChildUserId: Long? = null,
    onChildSelected: (Long) -> Unit = {},
    onAssignmentStatusChange: (Long, AssignmentProgress) -> Unit = { _, _ -> },
    onRating: () -> Unit,
    onOpenSchoolEvents: () -> Unit,
    onLessonClick: (Lesson) -> Unit
) {
    val subTextColor = textColor.copy(0.6f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(0.3f)
    val isTeacherLike = userRole == UserRole.TEACHER || userRole == UserRole.ADMIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (userRole == UserRole.PARENT && parentChildren.size > 1) {
            var childMenu by remember { mutableStateOf(false) }
            val childName = parentChildren.find { it.id == selectedChildUserId }?.name ?: "Выберите ребёнка"
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { childMenu = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ученик: $childName", color = textColor, fontSize = 14.sp)
                }
                DropdownMenu(expanded = childMenu, onDismissRequest = { childMenu = false }) {
                    parentChildren.forEach { ch ->
                        DropdownMenuItem(
                            text = { Text(ch.name, color = textColor) },
                            onClick = {
                                onChildSelected(ch.id)
                                childMenu = false
                            }
                        )
                    }
                }
            }
        }
        if (isTeacherLike) {
            TeacherHomeGrid(p, textColor, subTextColor, cardColor, borderColor, schoolEvents, onOpenSchoolEvents)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileCardItem(
                        if (userRole == UserRole.PARENT) "Ребенок: ${p.name}" else p.name,
                        p.grade,
                        p.avatarUrl,
                        textColor,
                        subTextColor,
                        cardColor,
                        borderColor
                    )
                    RatingCardItem(textColor, cardColor, borderColor, onRating)
                    EventsFeedSection(schoolEvents, textColor, cardColor, borderColor, onOpenSchoolEvents)
                }
                HomeworkCardItem(
                    lessons = lessons,
                    assignments = assignments,
                    userRole = userRole,
                    onAssignmentStatusChange = onAssignmentStatusChange,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        if (userRole == UserRole.PARENT) {
            Surface(
                color = Color(0xFF3498DB).copy(0.15f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF3498DB).copy(0.3f), RoundedCornerShape(20.dp))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF68B93E), CircleShape))
                    Text(
                        "Ребенок в школе",
                        color = textColor,
                        modifier = Modifier.padding(start = 12.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text("с 8:15", color = subTextColor, fontSize = 12.sp)
                }
            }
        }

        Text(
            if (isTeacherLike) "Мои уроки сегодня" else "Последние оценки",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        if (!isTeacherLike) {
            if (gradeEvents.isEmpty()) {
                Text(
                    "Оценок пока нет. Они появятся, когда учитель их выставит.",
                    color = subTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gradeEvents.take(5).forEach { g ->
                        GradeItem(g.subject, g.grade, Modifier.width(100.dp), textColor, cardColor, borderColor)
                    }
                }
            }
        }

        ScheduleSection(
            lessons = lessons,
            date = date,
            onDateChange = onDateChange,
            textColor = textColor,
            scheduleBg = scheduleBg,
            isDark = isDark,
            userRole = userRole,
            emptyStateText = emptyStateText,
            onLessonClick = onLessonClick
        )
    }
}

@Composable
fun TeacherHomeGrid(
    p: UserProfile,
    textColor: Color,
    subTextColor: Color,
    cardColor: Color,
    borderColor: Color,
    schoolEvents: List<SchoolEvent>,
    onOpenSchoolEvents: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileCardItem("Учитель: ${p.name}", "5А", p.avatarUrl, textColor, subTextColor, cardColor, borderColor)
            Surface(color = cardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(20.dp))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Активность", color = textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF68B93E), modifier = Modifier.size(14.dp))
                        Text(" 92% посещаемость", color = textColor, fontSize = 10.sp)
                    }
                }
            }
            EventsFeedSection(schoolEvents, textColor, cardColor, borderColor, onOpenSchoolEvents)
        }
        Surface(color = cardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, borderColor, RoundedCornerShape(20.dp))) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("На проверку", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text("• 12 работ 5А", color = textColor, fontSize = 12.sp)
                Text("• 5 тестов 6Б", color = textColor, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))) {
                    Text("Открыть", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileCardItem(name: String, grade: String, avatar: String?, textColor: Color, subTextColor: Color, cardColor: Color, borderColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardColor).border(1.dp, borderColor, RoundedCornerShape(20.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(textColor.copy(0.1f))) {
                if (avatar != null) {
                    val model = if (avatar.startsWith("http")) avatar else File(avatar)
                    AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.fillMaxSize().padding(10.dp), tint = textColor.copy(0.5f))
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(name, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(grade, color = subTextColor, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RatingCardItem(textColor: Color, cardColor: Color, borderColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardColor).border(1.dp, borderColor, RoundedCornerShape(20.dp)).clickable { onClick() }.padding(12.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Рейтинг", color = textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("3 место", color = Color(0xFF68B93E), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Text("в классе", color = textColor.copy(0.5f), fontSize = 9.sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("1", "2", "3", "4").forEach {
                    val isMe = it == "3"
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (isMe) Color(0xFFFFC107) else textColor.copy(0.1f)), contentAlignment = Alignment.Center) {
                        Text(it, color = if (isMe) Color.White else textColor, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EventsFeedSection(
    events: List<SchoolEvent>,
    textColor: Color,
    cardColor: Color,
    borderColor: Color,
    onOpenAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFF3498DB),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable(onClick = onOpenAll)
            ) {
                Text(
                    "События",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "${events.size}",
                color = textColor.copy(0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HomeworkCardItem(
    lessons: List<Lesson>,
    assignments: List<Assignment>,
    userRole: UserRole,
    onAssignmentStatusChange: (Long, AssignmentProgress) -> Unit,
    textColor: Color,
    subTextColor: Color,
    cardColor: Color,
    borderColor: Color,
    modifier: Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(cardColor).border(1.dp, borderColor, RoundedCornerShape(20.dp)).padding(12.dp)) {
        Column {
            Text("Домашние задания", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            when {
                assignments.isNotEmpty() -> {
                    assignments.take(4).forEach { a ->
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(a.subject, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(a.title, color = subTextColor, fontSize = 10.sp)
                            a.dueDate?.let { d ->
                                Text(
                                    "Сдать до: ${d.dayOfMonth}.${d.monthValue}.${d.year}",
                                    color = subTextColor.copy(0.9f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (userRole == UserRole.STUDENT) {
                                val st = a.myStatus ?: AssignmentProgress.NOT_STARTED
                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AssignmentProgress.entries.forEach { pr ->
                                        val sel = st == pr
                                        val label = when (pr) {
                                            AssignmentProgress.NOT_STARTED -> "Не начато"
                                            AssignmentProgress.IN_PROGRESS -> "Делаю"
                                            AssignmentProgress.DONE -> "Сдано"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (sel) Color(0xFF3498DB).copy(0.35f) else textColor.copy(0.08f))
                                                .clickable { onAssignmentStatusChange(a.id, pr) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(label, fontSize = 9.sp, color = textColor)
                                        }
                                    }
                                }
                            }
                            a.attachmentUrl?.let { url ->
                                // Не показываем имя файла, только факт наличия вложения.
                                Text(
                                    text = "Прикреплён файл",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                )
                            }
                        }
                    }
                }
                lessons.isNotEmpty() -> {
                    lessons.take(3).forEach { l ->
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(l.name, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(l.homework, color = subTextColor, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                else -> {
                    Text("Нет заданий", color = subTextColor, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun GradeItem(sub: String, grade: String, modifier: Modifier, textColor: Color, cardColor: Color, borderColor: Color) {
    Box(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(cardColor).border(1.dp, borderColor, RoundedCornerShape(20.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(10.dp)).background(getGradeColor(grade)), contentAlignment = Alignment.Center) {
                Text(grade, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Text(sub, color = textColor, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        }
    }
}
