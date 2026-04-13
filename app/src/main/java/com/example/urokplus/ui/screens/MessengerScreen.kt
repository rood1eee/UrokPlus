package com.example.urokplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.urokplus.AuthRepository
import com.example.urokplus.UserRole
import com.example.urokplus.api.ChatDto
import com.example.urokplus.api.UserDto
import com.example.urokplus.util.Resource
import kotlinx.coroutines.delay
import java.io.File

private enum class ChatRoleFilter { ALL, TEACHER, STUDENT, PARENT }

@Composable
fun MessengerScreen(
    textColor: Color,
    @Suppress("UNUSED_PARAMETER") scheduleBg: Color,
    isDark: Boolean,
    @Suppress("UNUSED_PARAMETER") role: UserRole,
    repository: AuthRepository,
    onChat: (String, String, String?, Boolean) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var foundUsers by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var activeChats by remember { mutableStateOf<List<ChatDto>>(emptyList()) }
    var roleFilter by remember { mutableStateOf(ChatRoleFilter.ALL) }

    val myId = repository.getUserId()
    val filteredChats = remember(activeChats, roleFilter) {
        activeChats.filter { chat ->
            when (roleFilter) {
                ChatRoleFilter.ALL -> true
                ChatRoleFilter.TEACHER -> chat.peerRole == "TEACHER"
                ChatRoleFilter.STUDENT -> chat.peerRole == "STUDENT"
                ChatRoleFilter.PARENT -> chat.peerRole == "PARENT"
            }
        }
    }
    val mainTextColor = if (isDark) textColor else Color(0xFF1E2961)
    val subTextColor = if (isDark) textColor.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)

    // Поллинг списка активных чатов (раз в 5 секунд)
    LaunchedEffect(Unit) {
        while (true) {
            val res = repository.getActiveChats()
            if (res is Resource.Success && res.data != null) {
                activeChats = res.data
            }
            delay(5000)
        }
    }

    // Поиск новых людей
    LaunchedEffect(search) {
        if (search.length >= 2) {
            delay(300)
            val res = repository.searchUsers(search)
            if (res is Resource.Success && res.data != null) {
                foundUsers = res.data
            }
        } else {
            foundUsers = emptyList()
        }
    }

    val fieldBorder = if (isDark) Color.White.copy(0.14f) else Color.Black.copy(0.08f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Чаты", color = mainTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(0.5f) else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            },
            placeholder = {
                Text(
                    "Поиск учеников и учителей",
                    color = subTextColor,
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = mainTextColor, fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = if (isDark) Color.White.copy(0.06f) else Color.White.copy(0.7f),
                focusedContainerColor = if (isDark) Color.White.copy(0.09f) else Color.White,
                unfocusedBorderColor = fieldBorder,
                focusedBorderColor = Color(0xFF3498DB),
                cursorColor = Color(0xFF3498DB)
            )
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ChatRoleFilter.ALL to "Все",
                ChatRoleFilter.TEACHER to "Учителя",
                ChatRoleFilter.STUDENT to "Ученики",
                ChatRoleFilter.PARENT to "Родители"
            ).forEach { (value, label) ->
                val selected = roleFilter == value
                FilterChip(
                    selected = selected,
                    onClick = { roleFilter = value },
                    label = { Text(label, fontSize = 13.sp) },
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black.copy(0.12f),
                        selectedBorderColor = Color(0xFF3498DB)
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3498DB).copy(0.35f),
                        selectedLabelColor = mainTextColor,
                        labelColor = subTextColor
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (search.length >= 2) {
                item {
                    Text(
                        "Результаты поиска",
                        color = subTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                items(foundUsers) { user ->
                    val chatId = if (myId < user.id) "chat_${myId}_${user.id}" else "chat_${user.id}_$myId"
                    ChatListItem(
                        name = user.name ?: user.login,
                        avatarUrl = user.avatarUrl,
                        isOnline = user.isOnline,
                        lastMessage = "Начать диалог",
                        time = "",
                        mainTextColor = mainTextColor,
                        subTextColor = subTextColor,
                        isDark = isDark,
                        onClick = { onChat(chatId, user.name ?: user.login, user.avatarUrl, user.isOnline) }
                    )
                }
            } else {
                if (activeChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(0.7f), contentAlignment = Alignment.Center) {
                            Text("У вас пока нет чатов", color = subTextColor)
                        }
                    }
                } else if (activeChats.isNotEmpty() && filteredChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(0.5f), contentAlignment = Alignment.Center) {
                            Text("Нет чатов в выбранной категории", color = subTextColor)
                        }
                    }
                } else {
                    items(filteredChats) { chat ->
                        val timeStr = if (chat.timestamp > 0) {
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(chat.timestamp))
                        } else ""

                        ChatListItem(
                            name = chat.name,
                            avatarUrl = chat.avatarUrl,
                            isOnline = chat.isOnline,
                            lastMessage = chat.lastMessage,
                            time = timeStr,
                            unreadCount = chat.unreadCount,
                            mainTextColor = mainTextColor,
                            subTextColor = subTextColor,
                            isDark = isDark,
                            onClick = { onChat(chat.id, chat.name, chat.avatarUrl, chat.isOnline) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    name: String,
    avatarUrl: String?,
    isOnline: Boolean,
    lastMessage: String,
    time: String,
    unreadCount: Int = 0,
    mainTextColor: Color,
    subTextColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val rowBg = if (isDark) Color.White.copy(0.07f) else Color.White
    val rowBorder = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isDark) 0.dp else 4.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = if (avatarUrl.startsWith("http")) avatarUrl else File(avatarUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = subTextColor, modifier = Modifier.size(28.dp))
                    }
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(2.dp, if(isDark) Color(0xFF17212B) else Color.White, CircleShape)
                    )
                }
                if (unreadCount > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                        shape = CircleShape,
                        color = Color(0xFFE53935),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = name,
                        color = mainTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (time.isNotEmpty()) {
                        Text(text = time, color = subTextColor, fontSize = 12.sp)
                    }
                }
                Text(
                    text = lastMessage,
                    color = subTextColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
