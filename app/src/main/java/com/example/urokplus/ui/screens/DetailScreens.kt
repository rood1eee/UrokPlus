package com.example.urokplus.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.urokplus.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun VoiceMessageChip(msg: Message, contentColor: Color) {
    val context = LocalContext.current
    var playing by remember(msg.id) { mutableStateOf(false) }
    val player = remember(msg.id) { MediaPlayer() }
    DisposableEffect(msg.id) {
        onDispose {
            runCatching { player.release() }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                runCatching {
                    if (playing) {
                        player.pause()
                        playing = false
                    } else {
                        player.reset()
                        if (msg.text.startsWith("http")) {
                            player.setDataSource(msg.text)
                        } else {
                            player.setDataSource(File(msg.text).absolutePath)
                        }
                        player.prepare()
                        player.start()
                        playing = true
                        player.setOnCompletionListener { playing = false }
                    }
                }.onFailure { playing = false }
            }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Icon(
            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Text("Голосовое сообщение", color = contentColor, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

fun copyUriToInternalStorage(context: android.content.Context, uri: Uri): String {
    val file = File(context.filesDir, "file_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    return file.absolutePath
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    name: String,
    avatarUrl: String?,
    isOnline: Boolean,
    peerTyping: Boolean = false,
    messages: List<Message>,
    textColor: Color,
    cardColor: Color,
    isDark: Boolean,
    onSendMessage: (String, MessageType, Long?) -> Unit,
    onEditMessage: (Long, String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onTypingPulse: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val composeScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var messageMenu by remember { mutableStateOf<Message?>(null) }
    var replyDraft by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }
    var typingJob by remember { mutableStateOf<Job?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceOutputFile by remember { mutableStateOf<File?>(null) }

    val startVoiceRecording: () -> Unit = {
        runCatching {
            val f = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            voiceOutputFile = f
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            voiceRecorder = r
            isRecordingVoice = true
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording()
    }

    val listState = rememberLazyListState()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            onSendMessage(copyUriToInternalStorage(context, it), MessageType.IMAGE, replyDraft?.id)
            replyDraft = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val bgColor1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bgColor2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val mainText = if (isDark) Color.White else Color(0xFF1E2961)
    val subText = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.5f)

    val myBubbleColor = Color(0xFF3498DB)
    val otherBubbleColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgColor1, bgColor2)))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = mainText)
                }
                Box(modifier = Modifier.size(42.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = if (isDark) Color.White.copy(0.2f) else Color.Black.copy(0.05f)
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Person, null, tint = mainText, modifier = Modifier.padding(8.dp))
                        }
                    }
                    if (isOnline) {
                        Box(
                            modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape)
                                .border(2.dp, bgColor1, CircleShape).align(Alignment.BottomEnd)
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(name, color = mainText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        when {
                            peerTyping -> "печатает…"
                            isOnline -> "в сети"
                            else -> "не в сети"
                        },
                        color = when {
                            peerTyping -> Color(0xFF81D4FA)
                            isOnline -> Color(0xFF4CAF50)
                            else -> subText
                        },
                        fontSize = 12.sp
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.isMe
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                        Surface(
                            color = if (isMe) myBubbleColor else otherBubbleColor,
                            shape = RoundedCornerShape(
                                topStart = 18.dp, topEnd = 18.dp,
                                bottomStart = if (isMe) 18.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 18.dp
                            ),
                            modifier = Modifier.widthIn(max = 300.dp).combinedClickable(
                                onClick = {},
                                onLongClick = { messageMenu = msg }
                            ).shadow(if (isDark) 0.dp else 2.dp, RoundedCornerShape(18.dp)),
                            border = if (!isMe && isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                if (msg.replyToId != null && !msg.replyPreviewText.isNullOrBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .border(1.dp, Color.White.copy(0.35f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            msg.replyAuthorName ?: "Сообщение",
                                            color = Color.White.copy(0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            msg.replyPreviewText!!,
                                            color = Color.White.copy(0.75f),
                                            fontSize = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                                when (msg.type) {
                                    MessageType.IMAGE -> AsyncImage(
                                        model = if (msg.text.startsWith("http")) msg.text else File(msg.text),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { previewImageUri = msg.text }
                                    )
                                    MessageType.VOICE -> VoiceMessageChip(
                                        msg = msg,
                                        contentColor = if (isMe || isDark) Color.White else Color.Black
                                    )
                                    MessageType.TEXT -> Text(
                                        msg.text,
                                        color = if (isMe || isDark) Color.White else Color.Black,
                                        fontSize = 15.sp
                                    )
                                }
                                if (msg.editedAt != null) {
                                    Text(
                                        "изменено",
                                        color = if (isMe || isDark) Color.White.copy(0.55f) else Color.Gray,
                                        fontSize = 10.sp,
                                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        time,
                                        color = if (isMe || isDark) Color.White.copy(0.7f) else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    if (isMe && msg.type == MessageType.TEXT) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val tickColor = when (msg.deliveryStatus) {
                                            MessageDeliveryStatus.READ -> Color(0xFFB3E5FC)
                                            else -> Color.White.copy(0.75f)
                                        }
                                        Text(
                                            if (msg.deliveryStatus == MessageDeliveryStatus.READ) "✓✓" else "✓",
                                            color = tickColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            replyDraft?.let { rd ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = if (isDark) Color.White.copy(0.08f) else Color.White.copy(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3498DB).copy(0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ответ на", color = subText, fontSize = 11.sp)
                            Text(
                                rd.text.take(120),
                                color = mainText,
                                fontSize = 13.sp,
                                maxLines = 2
                            )
                        }
                        IconButton(onClick = { replyDraft = null }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = mainText)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(0.05f)),
                shadowElevation = if (isDark) 0.dp else 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.Add, null, tint = if (isDark) Color.White else Color.Gray)
                    }
                    IconButton(
                        onClick = {
                            if (isRecordingVoice) {
                                runCatching { voiceRecorder?.stop() }
                                runCatching { voiceRecorder?.release() }
                                voiceRecorder = null
                                isRecordingVoice = false
                                voiceOutputFile?.let { f ->
                                    if (f.exists() && f.length() > 200L) {
                                        onSendMessage(f.absolutePath, MessageType.VOICE, replyDraft?.id)
                                        replyDraft = null
                                    }
                                }
                                voiceOutputFile = null
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startVoiceRecording()
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isRecordingVoice) Color.Red else if (isDark) Color.White else Color.Gray
                        )
                    }
                    TextField(
                        value = inputText,
                        onValueChange = { v ->
                            inputText = v
                            typingJob?.cancel()
                            typingJob = composeScope.launch {
                                delay(400)
                                if (v.isNotBlank()) onTypingPulse()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение...", color = subText) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = mainText,
                            focusedTextColor = mainText,
                            unfocusedTextColor = mainText
                        )
                    )
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText, MessageType.TEXT, replyDraft?.id)
                                inputText = ""
                                replyDraft = null
                            }
                        },
                        modifier = Modifier.size(42.dp).background(myBubbleColor, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (previewImageUri != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { previewImageUri = null }) {
                AsyncImage(
                    model = if (previewImageUri!!.startsWith("http")) previewImageUri!! else File(previewImageUri!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        messageMenu?.let { msg ->
            AlertDialog(
                onDismissRequest = { messageMenu = null },
                title = { Text("Сообщение") },
                text = {
                    Column {
                        TextButton(onClick = { replyDraft = msg; messageMenu = null }) { Text("Ответить") }
                        if (msg.isMe && msg.type == MessageType.TEXT) {
                            TextButton(onClick = {
                                editText = msg.text
                                editingMessage = msg
                                messageMenu = null
                            }) { Text("Изменить") }
                        }
                        if (msg.isMe) {
                            TextButton(onClick = { onDeleteMessage(msg.id); messageMenu = null }) {
                                Text("Удалить", color = Color.Red)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { messageMenu = null }) { Text("Отмена") } }
            )
        }

        editingMessage?.let { em ->
            AlertDialog(
                onDismissRequest = { editingMessage = null },
                title = { Text("Изменить") },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onEditMessage(em.id, editText)
                        editingMessage = null
                    }) { Text("Сохранить") }
                },
                dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Отмена") } }
            )
        }
    }
}

@Composable
fun LessonDetailScreen(lesson: Lesson, textColor: Color, scheduleBg: Color, cardColor: Color, isDark: Boolean, teacherAssignments: List<Assignment> = emptyList(), onBack: () -> Unit) {
    val context = LocalContext.current
    val bg1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bg2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val mainText = if (isDark) Color.White else Color(0xFF1E2961)
    val subText = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.5f)

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg1, bg2)))) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = mainText) } }
            item { Text("${lesson.id}. ${lesson.name}", color = mainText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text(lesson.time, color = subText) }
            item { Spacer(Modifier.height(32.dp)); Text("Задание", color = mainText, fontWeight = FontWeight.Bold) }
            item { 
                Surface(
                    color = if(isDark) Color.White.copy(0.1f) else Color.White, 
                    shape = RoundedCornerShape(16.dp), 
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).shadow(if(isDark) 0.dp else 4.dp, RoundedCornerShape(16.dp)), 
                    border = if(isDark) BorderStroke(1.dp, Color.White.copy(0.1f)) else null
                ) { Text(lesson.homework, color = mainText, modifier = Modifier.padding(16.dp)) } 
            }
            items(teacherAssignments) { a: Assignment ->
                Surface(
                    color = if(isDark) Color.White.copy(0.15f) else Color.White, 
                    shape = RoundedCornerShape(16.dp), 
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).shadow(if(isDark) 0.dp else 4.dp, RoundedCornerShape(16.dp))
                ) {
                    Column(Modifier.padding(16.dp)) { 
                        Text(a.title, fontWeight = FontWeight.Bold, color = mainText)
                        Text(a.description, color = subText)
                        if (!a.attachmentUrl.isNullOrBlank()) {
                            Text("📎 Файл", color = Color(0xFF3498DB), modifier = Modifier.padding(top = 8.dp).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.attachmentUrl!!))) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)); Text("Учитель", color = mainText, fontWeight = FontWeight.Bold) }
            item { 
                Surface(
                    color = if(isDark) Color.White.copy(0.1f) else Color.White, 
                    shape = RoundedCornerShape(16.dp), 
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).shadow(if(isDark) 0.dp else 4.dp, RoundedCornerShape(16.dp))
                ) { 
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { 
                        Box(modifier = Modifier.size(32.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Gray.copy(0.2f), CircleShape))
                        Text(lesson.teacher, color = mainText, modifier = Modifier.padding(start = 12.dp)) 
                    } 
                } 
            }
        }
    }
}

@Composable
fun EditProfileScreen(profile: UserProfile, isDark: Boolean, onBack: () -> Unit, onSave: (UserProfile) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var school by remember { mutableStateOf(profile.school) }
    var grade by remember { mutableStateOf(profile.grade) }
    var avatarUri by remember { mutableStateOf(profile.avatarUrl) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { avatarUri = copyUriToInternalStorage(context, it) } }
    val bg1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bg2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val mainText = if (isDark) Color.White else Color(0xFF1E2961)

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg1, bg2)))) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { 
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = mainText) }
                Text("Редактировать", color = mainText, fontSize = 20.sp, fontWeight = FontWeight.Bold) 
            }
            Spacer(Modifier.height(32.dp)); Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(0.1f)).clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { if (avatarUri != null) AsyncImage(model = avatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else Icon(Icons.Default.AddAPhoto, null, tint = mainText, modifier = Modifier.align(Alignment.Center)) }
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = mainText, unfocusedTextColor = mainText,
                    focusedBorderColor = mainText, unfocusedBorderColor = mainText.copy(0.4f),
                    focusedLabelColor = mainText, unfocusedLabelColor = mainText.copy(0.7f)
                )
            )
            OutlinedTextField(
                value = school, onValueChange = { school = it }, label = { Text("Школа") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = mainText, unfocusedTextColor = mainText,
                    focusedBorderColor = mainText, unfocusedBorderColor = mainText.copy(0.4f),
                    focusedLabelColor = mainText, unfocusedLabelColor = mainText.copy(0.7f)
                )
            )
            OutlinedTextField(
                value = grade, onValueChange = { grade = it }, label = { Text("Класс") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = mainText, unfocusedTextColor = mainText,
                    focusedBorderColor = mainText, unfocusedBorderColor = mainText.copy(0.4f),
                    focusedLabelColor = mainText, unfocusedLabelColor = mainText.copy(0.7f)
                )
            )
            Button(onClick = { onSave(UserProfile(name, school, grade, avatarUri)) }, modifier = Modifier.fillMaxWidth().padding(top = 40.dp).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB))) { 
                Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold) 
            }
        }
    }
}

@Composable
fun RatingDetailScreen(ratings: List<RatingItem>, myName: String, textColor: Color, scheduleBg: Color, cardColor: Color, isDark: Boolean, onBack: () -> Unit) {
    val bg1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bg2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val mainText = if (isDark) Color.White else Color(0xFF1E2961)

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg1, bg2)))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { 
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = mainText) }
                Text("Рейтинг", color = mainText, fontWeight = FontWeight.Bold, fontSize = 20.sp) 
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 24.dp)) {
                itemsIndexed(ratings) { _: Int, item: RatingItem ->
                    val isMe = item.name == myName
                    Surface(
                        color = if (isMe) Color(0xFF68B93E).copy(0.2f) else if(isDark) Color.White.copy(0.1f) else Color.White, 
                        shape = RoundedCornerShape(12.dp), 
                        modifier = Modifier.fillMaxWidth().shadow(if(isDark) 0.dp else 2.dp, RoundedCornerShape(12.dp)), 
                        border = if(isMe) BorderStroke(1.dp, Color(0xFF68B93E)) else null
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { 
                            Text("${item.rank}", color = if (item.rank <= 3) Color(0xFFFFC107) else mainText.copy(0.5f), fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                            Text(item.name, color = if(isMe) Color(0xFF68B93E) else mainText, modifier = Modifier.weight(1f))
                            Text(item.average, color = mainText, fontWeight = FontWeight.Bold) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolEventsScreen(events: List<SchoolEvent>, textColor: Color, cardColor: Color, isDark: Boolean, onBack: () -> Unit) {
    val bg1 = if (isDark) Color(0xFF1E2961) else Color(0xFFF5F7FF)
    val bg2 = if (isDark) Color(0xFF090C22) else Color(0xFFE0E7FF)
    val mainText = if (isDark) Color.White else Color(0xFF1E2961)

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg1, bg2)))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { 
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = mainText) }
                Text("События", color = mainText, fontWeight = FontWeight.Bold, fontSize = 20.sp) 
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
                items(events) { e: SchoolEvent ->
                    Surface(
                        color = if(isDark) Color.White.copy(0.1f) else Color.White, 
                        shape = RoundedCornerShape(16.dp), 
                        modifier = Modifier.fillMaxWidth().shadow(if(isDark) 0.dp else 4.dp, RoundedCornerShape(16.dp)), 
                        border = if(isDark) BorderStroke(1.dp, Color.White.copy(0.1f)) else null
                    ) {
                        Column(Modifier.padding(14.dp)) { 
                            Text(e.title, color = mainText, fontWeight = FontWeight.Bold)
                            Text(e.eventDate, color = mainText.copy(0.5f), fontSize = 12.sp)
                            Text(e.body ?: "", color = mainText.copy(0.8f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
