package com.example.urokplus

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urokplus.SchoolEvent
import com.example.urokplus.ui.screens.AdminScreen
import com.example.urokplus.ui.screens.*
import com.example.urokplus.api.StudentItemDto
import com.example.urokplus.util.russianNoLessonsReason
import com.example.urokplus.util.subjectNamesMatch
import com.example.urokplus.util.Resource
import com.example.urokplus.util.showUrokNotification
import com.example.urokplus.work.HomeworkReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

// --- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ---
fun getGradeColor(grade: String?): Color {
    return when(grade) {
        "5" -> Color(0xFF68B93E) 
        "4" -> Color(0xFF8BC34A) 
        "3" -> Color(0xFFFFA726) 
        "2", "1" -> Color(0xFFEF5350) 
        else -> Color.Gray.copy(alpha = 0.3f)
    }
}

fun showNotification(context: Context, title: String, message: String) {
    showUrokNotification(context, title, message)
}

// --- ГЛАВНЫЙ КОНТЕЙНЕР ---

@Composable
fun MainScreen(userRole: UserRole, onLogout: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    var isDarkTheme by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    var activeChat by remember { mutableStateOf<String?>(null) }
    var activeChatName by remember { mutableStateOf("") }
    var activeChatAvatar by remember { mutableStateOf<String?>(null) }
    var activeChatOnline by remember { mutableStateOf(false) }
    var peerTypingInChat by remember { mutableStateOf(false) }
    var activeLesson by remember { mutableStateOf<Lesson?>(null) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var isShowingRating by remember { mutableStateOf(false) }
    var isShowingSchoolEvents by remember { mutableStateOf(false) }

    var profile by remember { mutableStateOf(UserProfile()) }
    val chatMessages = remember { mutableStateMapOf<String, List<Message>>() }
    var gradeEvents by remember { mutableStateOf(listOf<GradeEvent>()) }
    var assignments by remember { mutableStateOf(listOf<Assignment>()) }
    var ratings by remember { mutableStateOf(listOf<RatingItem>()) }
    var lessons by remember { mutableStateOf(listOf<Lesson>()) }
    var schoolEvents by remember { mutableStateOf(listOf<SchoolEvent>()) }
    var parentChildren by remember { mutableStateOf<List<StudentItemDto>>(emptyList()) }
    var selectedChildUserId by remember { mutableStateOf<Long?>(null) }

    val effectiveGradeClass = remember(userRole, profile.grade, parentChildren, selectedChildUserId) {
        if (userRole == UserRole.PARENT) {
            parentChildren.find { it.id == selectedChildUserId }?.gradeClass?.takeIf { it.isNotBlank() }
                ?: profile.grade.ifBlank { "5А" }
        } else {
            profile.grade.ifBlank { "5А" }
        }
    }

    val activeChatCurrent by rememberUpdatedState(activeChat)

    // Периодическое обновление: оценки, чаты (уведомления о сообщениях), рейтинг
    LaunchedEffect(userRole, selectedChildUserId) {
        if (userRole == UserRole.PARENT) {
            val ch = repository.getParentChildren()
            if (ch is Resource.Success && ch.data != null) {
                parentChildren = ch.data
                if (selectedChildUserId == null && ch.data.isNotEmpty()) {
                    selectedChildUserId = ch.data.first().id
                }
            }
        }
        profile = repository.getProfile()
        val chatSnap = mutableMapOf<String, Pair<Long, String>>()
        while (true) {
            if (userRole == UserRole.STUDENT || userRole == UserRole.PARENT) {
                val forChild = if (userRole == UserRole.PARENT) selectedChildUserId else null
                val res = repository.getGradeEvents(forChild)
                if (res is Resource.Success && res.data != null) {
                    val fresh = res.data
                    if (gradeEvents.isNotEmpty() && fresh.isNotEmpty()) {
                        val prevMaxTs = gradeEvents.maxOf { it.timestamp }
                        val newest = fresh.maxBy { it.timestamp }
                        if (newest.timestamp > prevMaxTs) {
                            showNotification(context, "Новая оценка!", "${newest.subject}: ${newest.grade}")
                        }
                    }
                    gradeEvents = fresh
                }
            }
            val chatsRes = repository.getActiveChats()
            if (chatsRes is Resource.Success && chatsRes.data != null) {
                for (c in chatsRes.data) {
                    val prev = chatSnap[c.id]
                    if (prev != null && (c.timestamp > prev.first || c.lastMessage != prev.second) && c.id != activeChatCurrent) {
                        showNotification(context, "Новое сообщение", "${c.name}: ${c.lastMessage}")
                    }
                    chatSnap[c.id] = c.timestamp to c.lastMessage
                }
            }
            val ratRes = repository.getRating()
            if (ratRes is Resource.Success && ratRes.data != null) {
                ratings = ratRes.data
            }
            delay(3000)
        }
    }

    LaunchedEffect(selectedDate, effectiveGradeClass, userRole, selectedChildUserId) {
        val gradeClass = effectiveGradeClass.ifBlank { "5А" }
        val lessonsRes = repository.getLessons(selectedDate, gradeClass)
        lessons = if (lessonsRes is Resource.Success && lessonsRes.data != null) lessonsRes.data else emptyList()

        val res = repository.getAssignments(selectedDate, gradeClass)
        if (res is Resource.Success && res.data != null) assignments = res.data
    }

    LaunchedEffect(assignments) {
        HomeworkReminderScheduler.schedule(context, assignments)
    }

    LaunchedEffect(profile.grade, userRole) {
        val gc = if (userRole == UserRole.TEACHER || userRole == UserRole.ADMIN) null else profile.grade.takeIf { it.isNotBlank() }
        val ev = repository.getSchoolEvents(gc)
        if (ev is Resource.Success && ev.data != null) schoolEvents = ev.data
    }
    val lessonsEmptyStateText = russianNoLessonsReason(selectedDate) ?: "Уроков нет"


    LaunchedEffect(activeChat) {
        val chatId = activeChat
        if (chatId != null) {
            val myId = repository.getUserId()
            while (activeChat == chatId) {
                val res = repository.getMessages(chatId)
                if (res is Resource.Success && res.data != null) {
                    chatMessages[chatId] = res.data
                    val list = res.data
                    if (list.isNotEmpty()) {
                        repository.markChatRead(chatId, list.maxOf { it.id })
                    }
                }
                val tid = repository.getTypingUserId(chatId)
                peerTypingInChat = tid != null && tid != myId
                delay(2000)
            }
        } else {
            peerTypingInChat = false
        }
    }

    val tabs = remember(userRole) {
        when(userRole) {
            UserRole.STUDENT -> listOf("Главная", "Чаты", "Дневник", "Успехи", "Профиль")
            UserRole.PARENT -> listOf("Главная", "Чаты", "Успехи", "Посещение", "Профиль")
            UserRole.TEACHER -> listOf("Главная", "Чаты", "Журнал", "Расписание", "Профиль")
            UserRole.ADMIN -> listOf("Главная", "Чаты", "Журнал", "Расписание", "Админ", "Профиль")
        }
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    val bgColor1 = if (isDarkTheme) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bgColor2 = if (isDarkTheme) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF202124)
    val cardColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.White
    val scheduleBg = if (isDarkTheme) Color(0xFF121212) else Color.White

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgColor1, bgColor2)))) {
        
        val circleAlpha = if (isDarkTheme) 0.15f else 0.25f
        BackgroundCircle(250.dp, Color(0xFFFFB7B2), circleAlpha, (-60).dp, 100.dp)
        BackgroundCircle(200.dp, Color(0xFFAEC6CF), circleAlpha, 250.dp, 450.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            if (activeChat == null && activeLesson == null && !isEditingProfile && !isShowingRating && !isShowingSchoolEvents) {
                Spacer(modifier = Modifier.statusBarsPadding())
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    when (tabs[page]) {
                        "Главная" -> HomeScreen(
                            p = profile,
                            date = selectedDate,
                            lessons = lessons,
                            emptyStateText = lessonsEmptyStateText,
                            onDateChange = { selectedDate = it },
                            textColor = textColor,
                            cardColor = cardColor,
                            scheduleBg = scheduleBg,
                            isDark = isDarkTheme,
                            userRole = userRole,
                            gradeEvents = gradeEvents,
                            schoolEvents = schoolEvents,
                            assignments = assignments,
                            parentChildren = parentChildren,
                            selectedChildUserId = selectedChildUserId,
                            onChildSelected = { selectedChildUserId = it },
                            onAssignmentStatusChange = { id, status ->
                                scope.launch {
                                    if (repository.setAssignmentStatus(id, status)) {
                                        val gc = effectiveGradeClass.ifBlank { "5А" }
                                        val r = repository.getAssignments(selectedDate, gc)
                                        if (r is Resource.Success && r.data != null) assignments = r.data
                                    }
                                }
                            },
                            onRating = { isShowingRating = true },
                            onOpenSchoolEvents = { isShowingSchoolEvents = true },
                            onLessonClick = { lesson -> activeLesson = lesson }
                        )
                        "Чаты" -> MessengerScreen(
                            textColor = textColor,
                            scheduleBg = scheduleBg,
                            isDark = isDarkTheme,
                            role = userRole,
                            repository = repository
                        ) { id, name, avatar, isOnline ->
                            activeChat = id
                            activeChatName = name
                            activeChatAvatar = avatar
                            activeChatOnline = isOnline
                        }
                        "Дневник", "Расписание" -> DiaryScreen(selectedDate, lessons, lessonsEmptyStateText, { selectedDate = it }, textColor, scheduleBg, isDarkTheme) { activeLesson = it }
                        "Успехи" -> PerformanceScreen(textColor, scheduleBg, isDarkTheme, userRole, gradeEvents, profile.name)
                        "Посещение" -> AttendanceScreen(textColor, scheduleBg, isDarkTheme)
                        "Журнал" -> JournalScreen(selectedDate, { selectedDate = it }, textColor, scheduleBg, isDarkTheme, cardColor, profile.name, repository)
                        "Админ" -> AdminScreen(
                            textColor = textColor,
                            scheduleBg = scheduleBg,
                            isDark = isDarkTheme,
                            cardColor = cardColor,
                            repository = repository
                        )
                        "Профиль" -> ProfileScreen(profile, userRole.label, isDarkTheme, { isDarkTheme = it }, onLogout, { isEditingProfile = true }, cardColor)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    if (activeChat != null) {
                        BackHandler { activeChat = null }
                        ChatDetailScreen(
                            name = activeChatName,
                            avatarUrl = activeChatAvatar,
                            isOnline = activeChatOnline,
                            peerTyping = peerTypingInChat,
                            messages = chatMessages[activeChat!!] ?: emptyList(),
                            textColor = textColor,
                            cardColor = cardColor,
                            isDark = isDarkTheme,
                            onTypingPulse = {
                                val id = activeChat
                                if (id != null) scope.launch { repository.sendTypingPulse(id) }
                            },
                            onSendMessage = { t, type, replyToId ->
                                scope.launch {
                                    val ok = repository.sendMessage(
                                        Message(
                                            chatId = activeChat!!,
                                            text = t,
                                            isMe = true,
                                            type = type,
                                            replyToId = replyToId
                                        )
                                    )
                                    if (!ok) {
                                        Toast.makeText(context, "Ошибка отправки", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEditMessage = { messageId, newText ->
                                scope.launch {
                                    val ok = repository.updateMessage(activeChat!!, messageId, newText, MessageType.TEXT)
                                    if (!ok) Toast.makeText(context, "Не удалось изменить сообщение", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteMessage = { messageId ->
                                scope.launch {
                                    val ok = repository.deleteMessage(activeChat!!, messageId)
                                    if (!ok) Toast.makeText(context, "Не удалось удалить сообщение", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = {
                                activeChat = null
                                activeChatAvatar = null
                                activeChatOnline = false
                            }
                        )
                    } else if (activeLesson != null) {
                        BackHandler { activeLesson = null }
                        val lsn = activeLesson!!
                        val lessonGc = lsn.gradeClass?.takeIf { it.isNotBlank() }
                            ?: profile.grade.ifBlank { "5А" }
                        val relatedAssignments = assignments.filter { a ->
                            a.date == selectedDate &&
                                a.gradeClass == lessonGc &&
                                subjectNamesMatch(lsn.name, a.subject)
                        }.sortedByDescending { it.id }
                        LessonDetailScreen(
                            lsn,
                            textColor,
                            scheduleBg,
                            cardColor,
                            isDarkTheme,
                            teacherAssignments = relatedAssignments
                        ) { activeLesson = null }
                    } else if (isEditingProfile) {
                        BackHandler { isEditingProfile = false }
                        EditProfileScreen(profile, isDarkTheme, { isEditingProfile = false }) { scope.launch { repository.saveProfile(it); profile = it; isEditingProfile = false } }
                    } else if (isShowingSchoolEvents) {
                        BackHandler { isShowingSchoolEvents = false }
                        SchoolEventsScreen(schoolEvents, textColor, cardColor, isDarkTheme) { isShowingSchoolEvents = false }
                    } else if (isShowingRating) {
                        BackHandler { isShowingRating = false }
                        RatingDetailScreen(ratings, profile.name, textColor, scheduleBg, cardColor, isDarkTheme) { isShowingRating = false }
                    }
                }
            }
            if (activeChat == null && activeLesson == null && !isEditingProfile && !isShowingRating && !isShowingSchoolEvents) Spacer(modifier = Modifier.height(85.dp))
        }

        if (activeChat == null && activeLesson == null && !isEditingProfile && !isShowingRating && !isShowingSchoolEvents) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).navigationBarsPadding()) {
                CustomBottomBar(pagerState, tabs, isDarkTheme) { scope.launch { pagerState.animateScrollToPage(it) } }
            }
        }
    }
}

@Composable
fun BackgroundCircle(size: Dp, color: Color, alpha: Float, x: Dp, y: Dp) {
    Box(modifier = Modifier.offset(x = x, y = y).size(size).background(color.copy(alpha = alpha), CircleShape))
}

@Composable
fun CustomBottomBar(pagerState: PagerState, tabs: List<String>, isDark: Boolean, onTabSelected: (Int) -> Unit) {
    val icons = listOf(Icons.Default.Home, Icons.Default.Email, Icons.Default.DateRange, Icons.Default.Star, Icons.Default.Person)
    Surface(color = if (isDark) Color.White.copy(0.1f) else Color.White, shape = RoundedCornerShape(35.dp), modifier = Modifier.fillMaxWidth().height(70.dp), shadowElevation = 10.dp) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / tabs.size.toFloat(); val indicatorOffset by animateDpAsState(targetValue = itemWidth * (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction), label = "")
            Box(modifier = Modifier.offset(x = indicatorOffset + (itemWidth / 2) - 25.dp, y = 10.dp).size(50.dp).background(Color(0xFF3498DB).copy(0.2f), CircleShape))
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = pagerState.currentPage == index
                    Column(modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when(label) {
                            "Журнал" -> Icons.AutoMirrored.Filled.MenuBook
                            "Посещение" -> Icons.Default.CheckCircle
                            else -> icons[index % icons.size]
                        }
                        Icon(icon, null, tint = if (isSelected) Color(0xFF3498DB) else (if(isDark) Color.Gray else Color.DarkGray))
                        Text(label, fontSize = 8.sp, color = if (isSelected) Color(0xFF3498DB) else (if(isDark) Color.Gray else Color.DarkGray))
                    }
                }
            }
        }
    }
}
