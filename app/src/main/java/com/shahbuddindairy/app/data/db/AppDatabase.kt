package com.shahbuddindairy.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shahbuddindairy.app.data.dao.AppSettingDao
import com.shahbuddindairy.app.data.dao.CustomerDao
import com.shahbuddindairy.app.data.dao.DeliveryDao
import com.shahbuddindairy.app.data.dao.PaymentDao
import com.shahbuddindairy.app.data.model.AppSettingEntity
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity

@Database(
    entities = [
        CustomerEntity::class,
        DeliveryEntity::class,
        PaymentEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        private const val DATABASE_NAME = "shahbuddin_dairy.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Strict: DO NOT USE fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
