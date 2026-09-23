package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Farmer
import com.example.data.model.NotificationItem
import com.example.data.model.TransportRequest
import com.example.data.model.TransporterResponse

@Database(
    entities = [Farmer::class, TransportRequest::class, NotificationItem::class, TransporterResponse::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun farmerDao(): FarmerDao
    abstract fun requestDao(): RequestDao
    abstract fun notificationDao(): NotificationDao
    abstract fun transporterResponseDao(): TransporterResponseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rurallink_farmer_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
