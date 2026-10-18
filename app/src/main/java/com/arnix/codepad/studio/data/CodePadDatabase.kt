package com.arnix.codepad.studio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CodeProjectEntity::class, CodeRevisionSnapshot::class],
    version = 9,
    exportSchema = false
)
abstract class CodePadDatabase : RoomDatabase() {
    abstract fun codeProjectDao(): CodeProjectDao

    companion object {
        @Volatile
        private var INSTANCE: CodePadDatabase? = null

        fun getInstance(context: Context): CodePadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodePadDatabase::class.java,
                    "codepad_studio.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
