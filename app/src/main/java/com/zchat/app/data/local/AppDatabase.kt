package com.zchat.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zchat.app.data.model.*

@Database(
    entities = [
        User::class,
        Message::class,
        Call::class,
        Channel::class,
        ChannelMessage::class,
        Contact::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun callDao(): CallDao
    abstract fun channelDao(): ChannelDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goodok_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
