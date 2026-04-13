package com.example.urokplus.api

import com.example.urokplus.BuildConfig
import com.example.urokplus.UserProfile
import com.example.urokplus.AssignmentProgress
import com.example.urokplus.Message
import com.example.urokplus.MessageDeliveryStatus
import com.example.urokplus.MessageType
import com.example.urokplus.GradeEvent
import com.example.urokplus.Assignment
import com.example.urokplus.Lesson
import com.example.urokplus.RatingItem
import com.example.urokplus.SchoolEvent
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDate
import java.util.Locale

// --- DTO для API ---

data class LoginRequest(
    val login: String,
    val password: String
)

data class RegisterRequest(
    val login: String,
    val password: String,
    val confirm: String,
    val role: String
)

data class AdminRegisterRequest(
    val login: String,
    val password: String,
    val confirm: String,
    val role: String,
    val name: String,
    val school: String,
    val grade: String
)

data class UserDto(
    val id: Long,
    val login: String,
    val role: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false
)

data class AuthResponse(
    val success: Boolean,
    val error: String? = null,
    val user: UserDto? = null
)

data class ProfileDto(
    val name: String?,
    val school: String?,
    val grade: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?
)

data class MessageDto(
    val id: Long,
    @SerializedName("chatId") val chatId: String,
    val text: String,
    @SerializedName("isMe") val isMe: Boolean,
    val type: String,
    val timestamp: Long,
    @SerializedName("senderUserId") val senderUserId: Long? = null,
    @SerializedName("replyToId") val replyToId: Long? = null,
    @SerializedName("replyPreviewText") val replyPreviewText: String? = null,
    @SerializedName("replyAuthorName") val replyAuthorName: String? = null,
    @SerializedName("editedAt") val editedAt: Long? = null,
    @SerializedName("deliveryStatus") val deliveryStatus: String? = null
)

data class SendMessageBody(
    val text: String,
    val type: String = "TEXT",
    val timestamp: Long? = null,
    @SerializedName("replyToId") val replyToId: Long? = null
)

data class ChatDto(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val lastMessage: String,
    val timestamp: Long,
    val peerRole: String? = null,
    val unreadCount: Int = 0
)

data class MarkChatReadBody(
    @SerializedName("lastReadMessageId") val lastReadMessageId: Long
)

data class TypingResponse(
    @SerializedName("typingUserId") val typingUserId: Long?
)

data class GradeEventDto(
    val id: Long,
    @SerializedName("studentName") val studentName: String,
    val subject: String,
    val grade: String,
    @SerializedName("workType") val workType: String,
    val timestamp: Long,
    @SerializedName("isRead") val isRead: Boolean
)

data class AddGradeBody(
    @SerializedName("studentName") val studentName: String,
    val subject: String,
    val grade: String,
    @SerializedName("workType") val workType: String,
    val timestamp: Long? = null
)

data class FileUploadResponse(
    val url: String,
    val filename: String?,
    val size: Long?
)

data class AvatarUploadResponse(
    val success: Boolean,
    val avatarUrl: String?
)

data class AssignmentDto(
    val id: Long,
    val title: String,
    val description: String?,
    val subject: String,
    @SerializedName("gradeClass") val gradeClass: String,
    val date: String,
    val dueDate: String?,
    val attachmentUrl: String?,
    @SerializedName("attachmentName") val attachmentName: String?,
    val teacherName: String?,
    @SerializedName("myStatus") val myStatus: String? = null
)

data class AssignmentStatusBody(
    val status: String
)

data class SchoolEventDto(
    val id: Long,
    val title: String,
    val body: String?,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("gradeClass") val gradeClass: String?
)

data class SubjectItemDto(
    val name: String
)

data class StudentItemDto(
    val id: Long,
    val name: String,
    @SerializedName("gradeClass") val gradeClass: String?
)

data class LessonDto(
    val id: Int,
    val name: String,
    val time: String,
    val homework: String,
    val teacher: String,
    @SerializedName("gradeClass") val gradeClass: String?
)

data class CreateAssignmentBody(
    val title: String,
    val description: String,
    val subject: String,
    @SerializedName("gradeClass") val gradeClass: String,
    val date: String,
    @SerializedName("dueDate") val dueDate: String? = null,
    @SerializedName("attachmentUrl") val attachmentUrl: String? = null,
    @SerializedName("attachmentName") val attachmentName: String? = null,
    @SerializedName("teacherName") val teacherName: String? = null
)

data class UpdateMessageBody(
    val text: String,
    val type: String
)

// --- Retrofit API ---

interface UrokPlusApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/admin/register")
    suspend fun adminRegister(
        @Header("X-User-Id") userId: Long,
        @Body body: AdminRegisterRequest
    ): Response<AuthResponse>

    @GET("api/profile")
    suspend fun getProfile(@Header("X-User-Id") userId: Long): Response<ProfileDto>

    @PUT("api/profile")
    suspend fun saveProfile(
        @Header("X-User-Id") userId: Long,
        @Body body: ProfileDto
    ): Response<Unit>

    @GET("api/users/search")
    suspend fun searchUsers(@Query("query") query: String, @Header("X-User-Id") userId: Long): Response<List<UserDto>>

    @GET("api/chats")
    suspend fun getChats(@Header("X-User-Id") userId: Long): Response<List<ChatDto>>

    @GET("api/chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Header("X-User-Id") userId: Long
    ): Response<List<MessageDto>>

    @POST("api/chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Header("X-User-Id") userId: Long,
        @Body body: SendMessageBody
    ): Response<MessageDto>

    @PUT("api/chats/{chatId}/messages/{messageId}")
    suspend fun updateMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: Long,
        @Header("X-User-Id") userId: Long,
        @Body body: UpdateMessageBody
    ): Response<MessageDto>

    @DELETE("api/chats/{chatId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: Long,
        @Header("X-User-Id") userId: Long
    ): Response<Unit>

    @POST("api/chats/{chatId}/read")
    suspend fun markChatRead(
        @Path("chatId") chatId: String,
        @Header("X-User-Id") userId: Long,
        @Body body: MarkChatReadBody
    ): Response<Unit>

    @GET("api/chats/{chatId}/typing")
    suspend fun getTyping(
        @Path("chatId") chatId: String,
        @Header("X-User-Id") userId: Long
    ): Response<TypingResponse>

    @POST("api/chats/{chatId}/typing")
    suspend fun sendTyping(
        @Path("chatId") chatId: String,
        @Header("X-User-Id") userId: Long
    ): Response<Unit>

    @GET("api/grades")
    suspend fun getGradeEvents(
        @Header("X-User-Id") userId: Long,
        @Query("studentUserId") forStudentUserId: Long?
    ): Response<List<GradeEventDto>>

    @POST("api/grades")
    suspend fun addGrade(@Body body: AddGradeBody): Response<GradeEventDto>

    @Multipart
    @POST("api/files")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<FileUploadResponse>

    @Multipart
    @POST("api/profile/avatar")
    suspend fun uploadAvatar(
        @Header("X-User-Id") userId: Long,
        @Part avatar: MultipartBody.Part
    ): Response<AvatarUploadResponse>

    @GET("api/assignments")
    suspend fun getAssignments(
        @Query("date") date: String,
        @Query("gradeClass") gradeClass: String,
        @Header("X-User-Id") userId: Long
    ): Response<List<AssignmentDto>>

    @PUT("api/assignments/{id}/status")
    suspend fun setAssignmentStatus(
        @Path("id") assignmentId: Long,
        @Header("X-User-Id") userId: Long,
        @Body body: AssignmentStatusBody
    ): Response<Unit>

    @GET("api/parent/children")
    suspend fun getParentChildren(@Header("X-User-Id") userId: Long): Response<List<StudentItemDto>>

    @POST("api/assignments")
    suspend fun createAssignment(
        @Body body: CreateAssignmentBody
    ): Response<AssignmentDto>

    @GET("api/lessons")
    suspend fun getLessons(
        @Query("date") date: String,
        @Query("gradeClass") gradeClass: String
    ): Response<List<LessonDto>>

    @GET("api/rating")
    suspend fun getRating(): Response<List<RatingItem>>

    @GET("api/events")
    suspend fun getSchoolEvents(@Query("gradeClass") gradeClass: String?): Response<List<SchoolEventDto>>

    @GET("api/classes")
    suspend fun getClasses(): Response<List<String>>

    @GET("api/students")
    suspend fun getStudents(@Query("gradeClass") gradeClass: String): Response<List<StudentItemDto>>

    @GET("api/subjects")
    suspend fun getSubjects(@Query("gradeClass") gradeClass: String): Response<List<SubjectItemDto>>
}

// Маппинг DTO -> доменные модели

private fun normalizeAvatarUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    if (!raw.startsWith("http")) return raw
    val low = raw.lowercase(Locale.ROOT)
    val isLocal = low.contains("://localhost") || low.contains("://127.0.0.1")
    if (!isLocal) return raw
    val base = BuildConfig.API_BASE_URL.trim().removeSuffix("/")
    val uploadsIdx = low.indexOf("/uploads/")
    if (uploadsIdx >= 0) {
        return base + raw.substring(uploadsIdx)
    }
    return raw
}

fun ProfileDto.toUserProfile(): UserProfile = UserProfile(
    name = name ?: "Загрузка...",
    school = school ?: "Школа не указана",
    grade = grade ?: "—",
    avatarUrl = normalizeAvatarUrl(avatarUrl)
)

fun MessageDto.toMessage(): Message {
    val ds = when (deliveryStatus?.lowercase()) {
        "read" -> MessageDeliveryStatus.READ
        "sent", "delivered" -> MessageDeliveryStatus.SENT
        else -> null
    }
    return Message(
        id = id,
        chatId = chatId,
        text = text,
        isMe = isMe,
        type = try { MessageType.valueOf(type) } catch (e: Exception) { MessageType.TEXT },
        timestamp = timestamp,
        replyToId = replyToId,
        replyPreviewText = replyPreviewText,
        replyAuthorName = replyAuthorName,
        editedAt = editedAt,
        deliveryStatus = if (isMe) (ds ?: MessageDeliveryStatus.SENT) else null
    )
}

fun GradeEventDto.toGradeEvent(): GradeEvent = GradeEvent(
    id = id,
    studentName = studentName,
    subject = subject,
    grade = grade,
    workType = workType,
    timestamp = timestamp,
    isRead = isRead
)

fun AssignmentDto.toAssignment(): Assignment {
    val st = myStatus?.trim()?.uppercase()
    val progress = when (st) {
        "IN_PROGRESS", "DONE", "NOT_STARTED" ->
            AssignmentProgress.entries.first { it.name == st }
        else -> null
    }
    return Assignment(
        id = id,
        title = title,
        description = description ?: "",
        subject = subject,
        gradeClass = gradeClass,
        date = LocalDate.parse(date),
        dueDate = dueDate?.let { LocalDate.parse(it) },
        attachmentUrl = attachmentUrl,
        attachmentName = attachmentName,
        teacherName = teacherName,
        myStatus = progress
    )
}

fun SchoolEventDto.toSchoolEvent(): SchoolEvent = SchoolEvent(
    id = id,
    title = title,
    body = body,
    eventDate = eventDate,
    gradeClass = gradeClass
)

fun LessonDto.toLesson(): Lesson = Lesson(
    id = id,
    name = name,
    time = time,
    homework = homework,
    teacher = teacher,
    gradeClass = gradeClass
)
