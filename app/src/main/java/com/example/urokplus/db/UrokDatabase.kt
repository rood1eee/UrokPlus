package com.example.urokplus.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        MessageEntity::class,
        GradeEventEntity::class,
        AssignmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UrokDatabase : RoomDatabase() {
    abstract fun dao(): UrokDao

    companion object {
        @Volatile
        private var INSTANCE: UrokDatabase? = null

        fun getDatabase(context: Context): UrokDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UrokDatabase::class.java,
                    "urok_plus_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
