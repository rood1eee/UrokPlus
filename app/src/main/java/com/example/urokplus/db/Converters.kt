package com.example.urokplus.db

import androidx.room.TypeConverter
import com.example.urokplus.MessageType

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String {
        return value.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return try {
            MessageType.valueOf(value)
        } catch (e: Exception) {
            MessageType.TEXT
        }
    }
}
