package com.example.urokplus.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.urokplus.MessageType

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val school: String,
    val grade: String,
    val avatarUrl: String?
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val chatId: String,
    val text: String,
    val isMe: Boolean,
    val type: MessageType,
    val timestamp: Long
)

@Entity(tableName = "grade_events")
data class GradeEventEntity(
    @PrimaryKey val id: Long,
    val studentName: String,
    val subject: String,
    val grade: String,
    val workType: String,
    val timestamp: Long,
    val isRead: Boolean
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String,
    val subject: String,
    val gradeClass: String,
    val date: String,
    val dueDate: String?,
    val attachmentUrl: String?,
    val teacherName: String?
)
