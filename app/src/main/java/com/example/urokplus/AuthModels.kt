package com.example.urokplus

import java.time.LocalDate

data class AuthUiState(
    val screen: AuthScreen = AuthScreen.Login,
    val login: String = "",
    val password: String = "",
    val confirm: String = "",
    val role: UserRole = UserRole.STUDENT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

enum class AuthScreen {
    Login, Register, Main
}

enum class UserRole(val label: String) {
    STUDENT("Ученик"),
    PARENT("Родитель"),
    TEACHER("Учитель"),
    ADMIN("Администратор");

    companion object {
        fun fromString(value: String?): UserRole? =
            entries.firstOrNull { it.name == value || it.label.equals(value, ignoreCase = true) }
    }
}

sealed class AuthResult {
    data class Success(val role: UserRole) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

data class UserProfile(
    val name: String = "",
    val school: String = "",
    val grade: String = "",
    val avatarUrl: String? = null
)

data class Message(
    val id: Long = 0,
    val chatId: String,
    val text: String = "",
    val isMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val replyToId: Long? = null,
    val replyPreviewText: String? = null,
    val replyAuthorName: String? = null,
    val editedAt: Long? = null,
    val deliveryStatus: MessageDeliveryStatus? = null
)

enum class MessageDeliveryStatus { SENT, READ }

enum class MessageType { TEXT, IMAGE, VOICE }

data class Lesson(
    val id: Int,
    val name: String,
    val time: String,
    val homework: String,
    val teacher: String,
    val grade: String? = null,
    val gradeClass: String? = null
)

data class GradeEvent(
    val id: Long = 0,
    val studentName: String,
    val subject: String,
    val grade: String,
    val workType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class AssignmentProgress {
    NOT_STARTED,
    IN_PROGRESS,
    DONE
}

data class Assignment(
    val id: Long = 0,
    val title: String,
    val description: String,
    val subject: String,
    val gradeClass: String,
    val date: LocalDate,
    val dueDate: LocalDate? = null,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val teacherName: String? = null,
    val myStatus: AssignmentProgress? = null
)

data class SchoolEvent(
    val id: Long,
    val title: String,
    val body: String?,
    val eventDate: String,
    val gradeClass: String?
)

data class RatingItem(
    val name: String,
    val average: String,
    val rank: Int
)
