package com.example.urokplus

import android.content.Context
import android.util.Log
import com.example.urokplus.api.*
import com.example.urokplus.db.ProfileEntity
import com.example.urokplus.db.UrokDatabase
import com.example.urokplus.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.time.LocalDate
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.example.urokplus.util.isRussianWeekendOrHoliday
import com.example.urokplus.SchoolEvent

private const val PREFS_NAME = "urok_plus_prefs"
private const val KEY_USER_ID = "user_id"
private const val KEY_LOGIN = "user_login"
private const val TAG = "AuthRepository"

class AuthRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val api = ApiClient.api
    private val db = UrokDatabase.getDatabase(context).dao()

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)
    fun getUserLogin(): String = prefs.getString(KEY_LOGIN, "") ?: ""

    private fun setUserId(id: Long, login: String) {
        prefs.edit().putLong(KEY_USER_ID, id).putString(KEY_LOGIN, login).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // --- ПРОФИЛЬ ---
    suspend fun getProfile(specificUserId: Long? = null): UserProfile = withContext(Dispatchers.IO) {
        val targetId = specificUserId ?: getUserId()
        if (targetId == -1L) return@withContext UserProfile()

        try {
            val res = api.getProfile(targetId)
            if (res.isSuccessful && res.body() != null) {
                val profile = res.body()!!.toUserProfile()
                if (specificUserId == null) {
                    db.saveProfile(ProfileEntity(targetId, profile.name, profile.school, profile.grade, profile.avatarUrl))
                }
                return@withContext profile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile", e)
        }

        val local = db.getProfile(targetId)
        local?.let { UserProfile(it.name, it.school, it.grade, it.avatarUrl) } ?: UserProfile(name = "Пользователь #$targetId")
    }

    suspend fun saveProfile(p: UserProfile) = withContext(Dispatchers.IO) {
        val currentId = getUserId()
        if (currentId == -1L) return@withContext
        try {
            val existingAvatar = db.getProfile(currentId)?.avatarUrl
            val remoteAvatarUrl = resolveAvatarForServer(p.avatarUrl, currentId)
            // Если аплоад нового аватара не удался, не затираем уже сохраненный URL null-ом.
            val finalAvatar = when {
                !remoteAvatarUrl.isNullOrBlank() -> remoteAvatarUrl
                p.avatarUrl?.startsWith("http") == true -> p.avatarUrl
                else -> existingAvatar
            }
            val finalProfile = p.copy(avatarUrl = finalAvatar)
            db.saveProfile(ProfileEntity(currentId, finalProfile.name, finalProfile.school, finalProfile.grade, finalProfile.avatarUrl))
            api.saveProfile(currentId, ProfileDto(finalProfile.name, finalProfile.school, finalProfile.grade, finalProfile.avatarUrl))
        } catch (e: Exception) { Log.e(TAG, "Error saving profile", e) }
    }

    // --- ПОИСК ---
    suspend fun searchUsers(query: String): Resource<List<UserDto>> = withContext(Dispatchers.IO) {
        try {
            val res = api.searchUsers(query, getUserId())
            if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
            else Resource.Error("Ошибка: ${res.code()}")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    // --- ЧАТЫ ---
    suspend fun getActiveChats(): Resource<List<ChatDto>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getChats(getUserId())
            if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
            else Resource.Error("Ошибка чатов: ${res.code()}")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun getMessages(chatId: String): Resource<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMessages(chatId, getUserId())
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!.map { it.toMessage() })
            } else Resource.Error("Ошибка сообщений: ${res.code()}")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun sendMessage(m: Message): Boolean = withContext(Dispatchers.IO) {
        try {
            val finalText = when (m.type) {
                MessageType.TEXT -> m.text
                else -> uploadMessageFile(m.text, m.type) ?: return@withContext false
            }
            val res = api.sendMessage(
                m.chatId,
                getUserId(),
                SendMessageBody(text = finalText, type = m.type.name, timestamp = m.timestamp, replyToId = m.replyToId)
            )
            res.isSuccessful
        } catch (e: Exception) { false }
    }

    suspend fun updateMessage(chatId: String, messageId: Long, newText: String, type: MessageType = MessageType.TEXT): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val finalText = when (type) {
                    MessageType.TEXT -> newText
                    else -> uploadMessageFile(newText, type) ?: return@withContext false
                }
                val res = api.updateMessage(chatId, messageId, getUserId(), UpdateMessageBody(finalText, type.name))
                res.isSuccessful
            } catch (e: Exception) { false }
        }

    suspend fun deleteMessage(chatId: String, messageId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            api.deleteMessage(chatId, messageId, getUserId()).isSuccessful
        } catch (e: Exception) { false }
    }

    suspend fun markChatRead(chatId: String, lastReadMessageId: Long) = withContext(Dispatchers.IO) {
        try {
            api.markChatRead(chatId, getUserId(), MarkChatReadBody(lastReadMessageId)).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTypingUserId(chatId: String): Long? = withContext(Dispatchers.IO) {
        try {
            val res = api.getTyping(chatId, getUserId())
            if (res.isSuccessful && res.body() != null) res.body()!!.typingUserId else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendTypingPulse(chatId: String) = withContext(Dispatchers.IO) {
        try {
            api.sendTyping(chatId, getUserId())
        } catch (_: Exception) { }
    }

    private suspend fun uploadMessageFile(localPath: String, type: MessageType = MessageType.IMAGE): String? {
        return try {
            val file = File(localPath)
            val mime = when (type) {
                MessageType.VOICE -> "audio/*"
                else -> "image/*"
            }
            val requestBody = file.asRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val res = api.uploadFile(part)
            if (res.isSuccessful) res.body()?.url else null
        } catch (e: Exception) { null }
    }

    suspend fun getParentChildren(): Resource<List<StudentItemDto>> = withContext(Dispatchers.IO) {
        try {
            val uid = getUserId()
            if (uid < 1) return@withContext Resource.Success(emptyList())
            val res = api.getParentChildren(uid)
            if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
            else Resource.Error("Ошибка")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun setAssignmentStatus(assignmentId: Long, status: AssignmentProgress): Boolean = withContext(Dispatchers.IO) {
        try {
            api.setAssignmentStatus(assignmentId, getUserId(), AssignmentStatusBody(status.name)).isSuccessful
        } catch (e: Exception) { false }
    }

    private suspend fun resolveAvatarForServer(avatarUrl: String?, userId: Long): String? {
        if (avatarUrl.isNullOrBlank()) return null
        if (avatarUrl.startsWith("http")) return avatarUrl
        val file = File(avatarUrl)
        if (!file.exists()) return null
        return try {
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("avatar", file.name, requestBody)
            val res = api.uploadAvatar(userId, part)
            if (res.isSuccessful) res.body()?.avatarUrl else null
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading avatar", e)
            null
        }
    }

    // --- РЕЙТИНГ ---
    suspend fun getRating(): Resource<List<RatingItem>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getRating()
            if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
            else Resource.Error("Ошибка рейтинга")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    // --- ОЦЕНКИ ---
    suspend fun getGradeEvents(forStudentUserId: Long? = null): Resource<List<GradeEvent>> = withContext(Dispatchers.IO) {
        val uid = getUserId()
        if (uid < 1) return@withContext Resource.Success(emptyList())
        try {
            val res = api.getGradeEvents(uid, forStudentUserId)
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!.map { it.toGradeEvent() })
            } else Resource.Error("Ошибка")
        } catch (e: Exception) {
            Resource.Error("Нет сети")
        }
    }

    suspend fun addGrade(event: GradeEvent) = withContext(Dispatchers.IO) {
        try {
            api.addGrade(AddGradeBody(event.studentName, event.subject, event.grade, event.workType, event.timestamp))
        } catch (e: Exception) { Log.e(TAG, "Error adding grade", e) }
    }

    // --- ЗАДАНИЯ ---
    suspend fun getAssignments(date: LocalDate, gradeClass: String): Resource<List<Assignment>> = withContext(Dispatchers.IO) {
        if (gradeClass.isBlank()) return@withContext Resource.Success(emptyList())
        val uid = getUserId().let { if (it > 0) it else 0L }
        try {
            val res = api.getAssignments(date.toString(), gradeClass, uid)
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!.map { it.toAssignment() })
            } else Resource.Error("Ошибка")
        } catch (e: Exception) {
            Resource.Error("Нет сети")
        }
    }

    suspend fun createAssignment(
        date: LocalDate,
        gradeClass: String,
        subject: String,
        title: String,
        description: String,
        attachmentUrl: String?,
        attachmentName: String?,
        teacherName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = api.createAssignment(
                CreateAssignmentBody(title, description, subject, gradeClass, date.toString(), null, attachmentUrl, attachmentName, teacherName)
            )
            if (!res.isSuccessful) {
                Log.e(TAG, "createAssignment HTTP ${res.code()}: ${res.errorBody()?.string()}")
            }
            res.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error creating assignment", e)
            false
        }
    }

    suspend fun uploadAssignmentFile(file: File): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val res = api.uploadFile(part)
            if (res.isSuccessful && res.body() != null) {
                val b = res.body()!!
                Pair(b.url, b.filename ?: file.name)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "uploadAssignmentFile", e)
            null
        }
    }

    suspend fun getSchoolEvents(gradeClass: String?): Resource<List<SchoolEvent>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getSchoolEvents(gradeClass)
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!.map { it.toSchoolEvent() })
            } else Resource.Error("Ошибка событий")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun getClasses(): Resource<List<String>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getClasses()
            if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
            else Resource.Error("Ошибка")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun getSubjectsForClass(gradeClass: String): Resource<List<String>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getSubjects(gradeClass)
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!.map { it.name })
            } else Resource.Error("Ошибка")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun getStudentsForClass(gradeClass: String): Resource<List<StudentItemDto>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getStudents(gradeClass)
            if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
            else Resource.Error("Ошибка")
        } catch (e: Exception) { Resource.Error("Нет сети") }
    }

    suspend fun getLessons(date: LocalDate, gradeClass: String): Resource<List<Lesson>> = withContext(Dispatchers.IO) {
        if (gradeClass.isBlank() || isRussianWeekendOrHoliday(date)) {
            return@withContext Resource.Success(emptyList())
        }
        try {
            val res = api.getLessons(date.toString(), gradeClass)
            if (res.isSuccessful && res.body() != null) {
                Resource.Success(res.body()!!.map { it.toLesson() })
            } else Resource.Error("Ошибка расписания: ${res.code()}")
        } catch (e: Exception) {
            Resource.Error("Нет сети")
        }
    }

    // --- АВТОРИЗАЦИЯ ---
    suspend fun login(login: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting login to URL: ${BuildConfig.API_BASE_URL}")
            val res = api.login(LoginRequest(login.trim(), password))
            if (res.isSuccessful) {
                val body = res.body()
                if (body?.success == true && body.user != null) {
                    setUserId(body.user.id, body.user.login)
                    return@withContext AuthResult.Success(UserRole.fromString(body.user.role) ?: UserRole.STUDENT)
                } else {
                    return@withContext AuthResult.Error(body?.error ?: "Неверный логин или пароль")
                }
            } else {
                val code = res.code()
                if (code in listOf(502, 503, 504)) {
                    Log.w(
                        TAG,
                        "Gateway $code: запустите «node index.js» в server/ (порт 8080). Туннель tuna должен вести на этот порт. " +
                            "Эмулятор без туннеля: local.properties → api.base.url=http://10.0.2.2:8080. Проверка: GET …/api/health → {\"ok\":true}"
                    )
                }
                val msg = when (code) {
                    502, 503, 504 ->
                        "Сервер недоступен ($code). Запустите API: в папке server — node index.js"
                    else -> "Сервер ответил ошибкой: $code"
                }
                return@withContext AuthResult.Error(msg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            AuthResult.Error("Нет связи с сервером. Проверьте интернет и адрес API в local.properties (api.base.url).")
        }
    }

    suspend fun register(login: String, password: String, confirm: String, role: UserRole): AuthResult = withContext(Dispatchers.IO) {
        try {
            val res = api.register(RegisterRequest(login.trim(), password, confirm, role.name))
            if (res.isSuccessful && res.body()?.success == true) AuthResult.Success(role)
            else AuthResult.Error(res.body()?.error ?: "Ошибка регистрации")
        } catch (e: Exception) { AuthResult.Error("Нет связи") }
    }

    // --- ADMIN ---
    suspend fun adminRegister(
        login: String,
        password: String,
        confirm: String,
        role: UserRole,
        name: String,
        school: String,
        grade: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = getUserId()
            if (uid < 0) return@withContext Resource.Error("Не авторизован")

            val res = api.adminRegister(
                uid,
                AdminRegisterRequest(
                    login = login.trim(),
                    password = password,
                    confirm = confirm,
                    role = role.name,
                    name = name.trim(),
                    school = school.trim(),
                    grade = grade.trim()
                )
            )

            if (res.isSuccessful && res.body()?.success == true) {
                Resource.Success(Unit)
            } else {
                Resource.Error(res.body()?.error ?: "Ошибка создания аккаунта")
            }
        } catch (e: Exception) {
            Log.e(TAG, "adminRegister error", e)
            Resource.Error("Нет сети")
        }
    }
}
