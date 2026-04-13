package com.example.urokplus.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urokplus.AuthRepository
import com.example.urokplus.GradeEvent
import com.example.urokplus.getGradeColor
import com.example.urokplus.showNotification
import com.example.urokplus.api.StudentItemDto
import com.example.urokplus.util.Resource
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun JournalScreen(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    textColor: Color,
    scheduleBg: Color,
    isDark: Boolean,
    cardColor: Color,
    teacherName: String,
    repository: AuthRepository
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var classList by remember { mutableStateOf(listOf("5А", "6Б")) }
    var selectedClass by remember { mutableStateOf("5А") }
    var subjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSubject by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<StudentItemDto>>(emptyList()) }

    var assignmentText by remember { mutableStateOf("") }
    var attachmentUrl by remember { mutableStateOf<String?>(null) }
    var attachmentLabel by remember { mutableStateOf<String?>(null) }
    var attachmentUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when (val res = repository.getClasses()) {
            is Resource.Success -> if (res.data != null && res.data.isNotEmpty()) classList = res.data
            else -> {}
        }
    }

    LaunchedEffect(selectedClass) {
        when (val sRes = repository.getSubjectsForClass(selectedClass)) {
            is Resource.Success -> {
                val list = sRes.data ?: emptyList()
                subjects = if (list.isNotEmpty()) list else listOf(
                    "Математика", "Русский язык", "Литература", "История", "Английский язык"
                )
                selectedSubject = subjects.firstOrNull() ?: ""
            }
            else -> {}
        }
        when (val stRes = repository.getStudentsForClass(selectedClass)) {
            is Resource.Success -> if (stRes.data != null) students = stRes.data
            else -> students = emptyList()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            attachmentUploading = true
            try {
                val file = copyUriToCache(context, uri)
                val up = repository.uploadAssignmentFile(file)
                if (up != null) {
                    attachmentUrl = up.first
                    attachmentLabel = up.second
                    Toast.makeText(context, "Файл прикреплён", Toast.LENGTH_SHORT).show()
                } else {
                    attachmentUrl = null
                    attachmentLabel = null
                    Toast.makeText(context, "Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                }
            } finally {
                attachmentUploading = false
            }
        }
    }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    var classMenu by remember { mutableStateOf(false) }
    var subjectMenu by remember { mutableStateOf(false) }

    val borderSubtle = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            color = scheduleBg.copy(0.92f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxSize(),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Классный журнал", color = textColor, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        val formatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yy", Locale("ru"))
                        Text(date.format(formatter), color = textColor.copy(0.85f), fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Row {
                            IconButton(onClick = { onDateChange(date.minusDays(1)) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF3498DB))
                            }
                            IconButton(onClick = { onDateChange(date.plusDays(1)) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF3498DB))
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(expanded = classMenu, onExpandedChange = { classMenu = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Класс", fontSize = 12.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                                classList.forEach { c ->
                                    DropdownMenuItem(text = { Text(c) }, onClick = { selectedClass = c; classMenu = false })
                                }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = subjectMenu, onExpandedChange = { subjectMenu = it }, modifier = Modifier.weight(1.15f)) {
                            OutlinedTextField(
                                value = selectedSubject,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Предмет", fontSize = 12.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenu) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(expanded = subjectMenu, onDismissRequest = { subjectMenu = false }) {
                                subjects.forEach { s ->
                                    DropdownMenuItem(text = { Text(s) }, onClick = { selectedSubject = s; subjectMenu = false })
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск по ФИО…", color = textColor.copy(0.4f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = textColor.copy(0.45f), modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Black.copy(0.06f),
                            focusedContainerColor = Color.Black.copy(0.06f),
                            unfocusedBorderColor = borderSubtle,
                            focusedBorderColor = Color(0xFF3498DB).copy(0.5f)
                        )
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(if (isDark) 0.12f else 0.04f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Задание на урок", color = textColor.copy(0.9f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            OutlinedTextField(
                                value = assignmentText,
                                onValueChange = { assignmentText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Кратко опишите задание…", fontSize = 13.sp, color = textColor.copy(0.35f)) },
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { if (!attachmentUploading) filePicker.launch("*/*") },
                                    enabled = !attachmentUploading,
                                    modifier = Modifier.height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (attachmentUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = LocalContentColor.current
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Загрузка…", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            attachmentLabel?.take(12)?.let { "$it…" } ?: "Файл",
                                            maxLines = 1,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (attachmentUrl != null) {
                                    TextButton(
                                        onClick = { attachmentUrl = null; attachmentLabel = null },
                                        modifier = Modifier.height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Сброс", color = Color(0xFFE57373), fontSize = 13.sp)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        if (assignmentText.isNotBlank() && selectedSubject.isNotBlank()) {
                                            if (attachmentUploading) {
                                                Toast.makeText(context, "Дождитесь окончания загрузки файла", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            scope.launch {
                                                val ok = repository.createAssignment(
                                                    date = date,
                                                    gradeClass = selectedClass,
                                                    subject = selectedSubject,
                                                    title = "ДЗ: $selectedSubject",
                                                    description = assignmentText,
                                                    attachmentUrl = attachmentUrl,
                                                    attachmentName = attachmentLabel,
                                                    teacherName = teacherName.ifBlank { "Учитель" }
                                                )
                                                if (ok) {
                                                    assignmentText = ""
                                                    attachmentUrl = null
                                                    attachmentLabel = null
                                                    Toast.makeText(context, "Задание сохранено", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !attachmentUploading,
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB))
                                ) {
                                    Text("Сохранить", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(color = borderSubtle, thickness = 1.dp)
                }

                item {
                    Text(
                        "Ученики · ${filteredStudents.size}",
                        color = textColor.copy(0.7f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.2.sp
                    )
                }

                if (filteredStudents.isEmpty()) {
                    item {
                        Text(
                            "В этом классе пока нет учеников в базе.",
                            color = textColor.copy(0.55f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                items(filteredStudents, key = { it.id }) { st ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(if (isDark) 0.06f else 0.08f))
                            .border(1.dp, borderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = st.name,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("2", "3", "4", "5").forEach { grade ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(getGradeColor(grade).copy(if (isDark) 0.22f else 0.18f))
                                        .clickable {
                                            scope.launch {
                                                repository.addGrade(
                                                    GradeEvent(
                                                        studentName = st.name,
                                                        subject = selectedSubject,
                                                        grade = grade,
                                                        workType = "Ответ на уроке"
                                                    )
                                                )
                                                Toast.makeText(context, "Оценка выставлена", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        grade,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showNotification(context, "Уведомление", "Родители уведомлены") },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB39DDB))
                    ) {
                        Text("Уведомить родителей", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun copyUriToCache(context: Context, uri: Uri): File {
    val resolver = context.contentResolver
    val name = resolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
    } ?: "file.bin"
    val safe = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val out = File(context.cacheDir, "hw_${System.currentTimeMillis()}_$safe")
    resolver.openInputStream(uri)?.use { input ->
        FileOutputStream(out).use { output -> input.copyTo(output) }
    }
    return out
}
