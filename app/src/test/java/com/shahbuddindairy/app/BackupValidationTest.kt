package com.shahbuddindairy.app

import com.shahbuddindairy.app.backup.BackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupValidationTest {

    @Test
    fun testSeedDatabaseValidation() {
        val seedFile = File("src/main/assets/seed_database_v4.json")
        assertTrue("Seed database file must exist at ${seedFile.absolutePath}", seedFile.exists())

        val rawJson = seedFile.readText(Charsets.UTF_8)
        val result = BackupManager.validateBackupJson(rawJson)

        assertTrue("Validation of seed database should succeed: ${result.error}", result.isValid)
        assertTrue("Customers list must not be empty", result.customers.isNotEmpty())
        assertTrue("Deliveries list must not be empty", result.deliveries.isNotEmpty())
        assertTrue("Payments list must not be empty", result.payments.isNotEmpty())

        val stats = result.stats
        assertTrue("Stats must not be null", stats != null)
        println("Verified seed data: ${stats?.customers} customers, ${stats?.deliveries} deliveries, ${stats?.payments} payments")
    }
}
