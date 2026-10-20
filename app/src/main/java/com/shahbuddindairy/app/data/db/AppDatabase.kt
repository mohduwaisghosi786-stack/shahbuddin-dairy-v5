package com.shahbuddindairy.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shahbuddindairy.app.data.dao.AppSettingDao
import com.shahbuddindairy.app.data.dao.CustomerDao
import com.shahbuddindairy.app.data.dao.CustomerNoteDao
import com.shahbuddindairy.app.data.dao.DeliveryDao
import com.shahbuddindairy.app.data.dao.PaymentDao
import com.shahbuddindairy.app.data.dao.RouteDao
import com.shahbuddindairy.app.data.model.AppSettingEntity
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.data.model.RouteEntity

@Database(
    entities = [
        CustomerEntity::class,
        DeliveryEntity::class,
        PaymentEntity::class,
        AppSettingEntity::class,
        CustomerNoteEntity::class,
        RouteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun customerNoteDao(): CustomerNoteDao
    abstract fun routeDao(): RouteDao

    companion object {
        private const val DATABASE_NAME = "shahbuddin_dairy.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create routes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS routes (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        colorHex TEXT NOT NULL DEFAULT '#16A34A',
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // 2. Create customer_notes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS customer_notes (
                        id TEXT NOT NULL PRIMARY KEY,
                        customerId INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'General',
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        createdAt TEXT NOT NULL DEFAULT '',
                        updatedAt TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_notes_customerId ON customer_notes(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_notes_isPinned ON customer_notes(isPinned)")

                // 3. Add routeId column and index to customers table
                db.execSQL("ALTER TABLE customers ADD COLUMN routeId TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_routeId ON customers(routeId)")

                // 4. Seed default route
                db.execSQL("""
                    INSERT OR IGNORE INTO routes (id, name, description, colorHex, sortOrder, isActive, createdAt)
                    VALUES ('default', 'Main Route (मुख्य रूट)', 'Default delivery route', '#16A34A', 0, 1, '')
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN priority TEXT NOT NULL DEFAULT 'Normal'")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN reminderDateTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN mediaUris TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN audioUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN audioDurationMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customer_notes ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Strict: DO NOT USE fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
