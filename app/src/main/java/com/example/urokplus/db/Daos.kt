package com.example.urokplus.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UrokDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity): Long

    @Query("SELECT * FROM profile WHERE id = :userId")
    suspend fun getProfile(userId: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessages(chatId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEventEntity>): List<Long>

    @Query("SELECT * FROM grade_events ORDER BY timestamp DESC")
    suspend fun getGrades(): List<GradeEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>): List<Long>

    @Query("SELECT * FROM assignments WHERE date = :date AND gradeClass = :gradeClass")
    suspend fun getAssignments(date: String, gradeClass: String): List<AssignmentEntity>
}
