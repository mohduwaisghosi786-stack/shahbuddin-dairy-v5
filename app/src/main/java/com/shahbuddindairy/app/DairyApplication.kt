package com.shahbuddindairy.app

import android.app.Application
import com.shahbuddindairy.app.data.db.AppDatabase
import com.shahbuddindairy.app.data.db.LegacyDataMigrator
import com.shahbuddindairy.app.data.repository.DairyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DairyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        DairyRepository(
            database.customerDao(),
            database.deliveryDao(),
            database.paymentDao(),
            database.appSettingDao(),
            database.customerNoteDao(),
            database.routeDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Execute safe initial migration if needed
        applicationScope.launch {
            LegacyDataMigrator.checkAndMigrate(this@DairyApplication, database)
        }
    }
}
