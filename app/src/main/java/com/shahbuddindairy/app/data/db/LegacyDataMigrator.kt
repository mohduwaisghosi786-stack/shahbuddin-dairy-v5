package com.shahbuddindairy.app.data.db

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shahbuddindairy.app.data.model.AppSettingEntity
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import java.io.File
import java.io.InputStreamReader

object LegacyDataMigrator {
    private const val TAG = "LegacyDataMigrator"
    private const val SEED_ASSET = "seed_database_v4.json"

    data class MigrationResult(
        val success: Boolean,
        val message: String,
        val customersCount: Int = 0,
        val deliveriesCount: Int = 0,
        val paymentsCount: Int = 0
    )

    suspend fun checkAndMigrate(context: Context, database: AppDatabase): MigrationResult {
        val customerDao = database.customerDao()
        val deliveryDao = database.deliveryDao()
        val paymentDao = database.paymentDao()
        val settingDao = database.appSettingDao()

        val existingCustomerCount = customerDao.getCustomerCount()
        if (existingCustomerCount > 0) {
            Log.i(TAG, "Database already initialized with $existingCustomerCount customers. Skipping initial migration.")
            return MigrationResult(
                success = true,
                message = "Already initialized",
                customersCount = existingCustomerCount,
                deliveriesCount = deliveryDao.getDeliveryCount(),
                paymentsCount = paymentDao.getPaymentCount()
            )
        }

        Log.i(TAG, "Empty database detected. Starting SAFE migration: BACKUP -> VALIDATE -> MIGRATE -> VERIFY")

        val rawJson: String = try {
            context.assets.open(SEED_ASSET).use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read seed asset $SEED_ASSET", e)
            return MigrationResult(false, "Could not read legacy data asset: ${e.message}")
        }

        // STEP 1: SAFETY BACKUP OF RAW DATA
        try {
            val safetyFile = File(context.filesDir, "safety_backup_pre_migration.json")
            safetyFile.writeText(rawJson, Charsets.UTF_8)
            Log.i(TAG, "Safety snapshot saved to: ${safetyFile.absolutePath} (${safetyFile.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Warning: Failed to create safety backup file", e)
        }

        // STEP 2: VALIDATE LEGACY DATA
        val gson = Gson()
        val rootObj = try {
            gson.fromJson(rawJson, JsonObject::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid legacy JSON data", e)
            return MigrationResult(false, "Legacy data is corrupt JSON: ${e.message}")
        }

        val customersArray = rootObj.getAsJsonArray("customers")
            ?: return MigrationResult(false, "Validation failed: 'customers' array missing")
        val deliveriesArray = rootObj.getAsJsonArray("deliveries")
            ?: return MigrationResult(false, "Validation failed: 'deliveries' array missing")
        val paymentsArray = rootObj.getAsJsonArray("payments")
            ?: return MigrationResult(false, "Validation failed: 'payments' array missing")

        val customersList = mutableListOf<CustomerEntity>()
        for (i in 0 until customersArray.size()) {
            val c = customersArray.get(i).asJsonObject
            val id = c.get("id")?.asLong ?: (i + 1L)
            val name = c.get("name")?.asString ?: ""
            if (name.isBlank()) {
                return MigrationResult(false, "Validation failed: Customer at index $i has empty name")
            }
            customersList.add(
                CustomerEntity(
                    id = id,
                    name = name,
                    phone = c.get("phone")?.asString ?: "",
                    mohalla = c.get("mohalla")?.asString ?: "कटरा",
                    landmark = c.get("landmark")?.asString ?: "",
                    note = c.get("note")?.asString ?: "",
                    milkType = c.get("milk_type")?.asString ?: "भैंस",
                    defaultLiters = c.get("default_liters")?.asDouble ?: 1.0,
                    ratePerLiter = c.get("rate_per_liter")?.asDouble ?: 0.0,
                    billingType = if (c.get("billing_type")?.asString == "daily_cash") "daily_cash" else "monthly",
                    deliveryFrequency = if (c.get("delivery_frequency")?.asString == "alternate") "alternate" else "daily",
                    frequencyAnchorDate = c.get("frequency_anchor_date")?.asString ?: "",
                    isActive = if (c.get("is_active")?.asInt == 0) 0 else 1,
                    isArchived = if (c.get("is_archived")?.asInt == 1) 1 else 0,
                    routeOrder = c.get("route_order")?.asInt ?: (i + 1),
                    portionsJson = c.get("portions_json")?.asString ?: "[]",
                    createdAt = c.get("created_at")?.asString ?: ""
                )
            )
        }

        val deliveriesList = mutableListOf<DeliveryEntity>()
        for (i in 0 until deliveriesArray.size()) {
            val d = deliveriesArray.get(i).asJsonObject
            val id = d.get("id")?.asString ?: "d_$i"
            val customerId = d.get("customer_id")?.asLong ?: continue
            val date = d.get("date")?.asString ?: ""
            val cash = when {
                d.has("paid_cash") && !d.get("paid_cash").isJsonNull -> d.get("paid_cash").asDouble
                d.has("paid_amount") && !d.get("paid_amount").isJsonNull -> d.get("paid_amount").asDouble
                else -> 0.0
            }
            val online = if (d.has("paid_online") && !d.get("paid_online").isJsonNull) d.get("paid_online").asDouble else 0.0
            val totalPaid = if (d.has("total_paid") && !d.get("total_paid").isJsonNull) d.get("total_paid").asDouble else (cash + online)

            deliveriesList.add(
                DeliveryEntity(
                    id = id,
                    customerId = customerId,
                    date = date,
                    deliveryTime = d.get("delivery_time")?.asString ?: "",
                    milkType = d.get("milk_type")?.asString ?: "भैंस",
                    liters = d.get("liters")?.asDouble ?: 0.0,
                    rate = d.get("rate")?.asDouble ?: 0.0,
                    totalAmount = d.get("total_amount")?.asDouble ?: 0.0,
                    paidCash = cash,
                    paidOnline = online,
                    totalPaid = totalPaid,
                    status = if (d.get("status")?.asString == "skipped") "skipped" else "delivered",
                    portionsJson = d.get("portions_json")?.asString ?: "[]",
                    note = d.get("note")?.asString ?: "",
                    isVoid = d.get("is_void")?.asBoolean ?: false,
                    voidedAt = d.get("voided_at")?.takeIf { !it.isJsonNull }?.asString
                )
            )
        }

        val paymentsList = mutableListOf<PaymentEntity>()
        for (i in 0 until paymentsArray.size()) {
            val p = paymentsArray.get(i).asJsonObject
            val id = p.get("id")?.asString ?: "pay_$i"
            val customerId = p.get("customer_id")?.asLong ?: continue
            paymentsList.add(
                PaymentEntity(
                    id = id,
                    customerId = customerId,
                    date = p.get("date")?.asString ?: "",
                    amount = p.get("amount")?.asDouble ?: 0.0,
                    paymentMode = p.get("payment_mode")?.asString ?: "Cash",
                    paymentSubtype = p.get("payment_subtype")?.asString ?: "",
                    note = p.get("note")?.asString ?: "",
                    createdAt = p.get("created_at")?.asString ?: "",
                    isVoid = p.get("is_void")?.asBoolean ?: false,
                    voidedAt = p.get("voided_at")?.takeIf { !it.isJsonNull }?.asString
                )
            )
        }

        // STEP 3: MIGRATE IN ATOMIC ROOM TRANSACTION
        try {
            database.withTransaction {
                customerDao.insertCustomers(customersList)
                deliveryDao.insertDeliveries(deliveriesList)
                paymentDao.insertPayments(paymentsList)
                settingDao.setSetting(AppSettingEntity("migrated_at", System.currentTimeMillis().toString()))
                settingDao.setSetting(AppSettingEntity("migration_source", "legacy_v4"))
                settingDao.setSetting(AppSettingEntity("payee_name", "Shahbuddin Dairy"))
                settingDao.setSetting(AppSettingEntity("dairy_phone", "9897259461"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transaction failed during migration", e)
            return MigrationResult(false, "Migration transaction failed: ${e.message}")
        }

        // STEP 4: VERIFY POST-MIGRATION COUNTS
        val postCustCount = customerDao.getCustomerCount()
        val postDelivCount = deliveryDao.getDeliveryCount()
        val postPayCount = paymentDao.getPaymentCount()

        Log.i(TAG, "Post-migration verification: Customers=$postCustCount (expected ${customersList.size}), " +
                "Deliveries=$postDelivCount (expected ${deliveriesList.size}), " +
                "Payments=$postPayCount (expected ${paymentsList.size})")

        if (postCustCount != customersList.size || postDelivCount != deliveriesList.size || postPayCount != paymentsList.size) {
            val err = "Verification mismatch: Customers expected ${customersList.size} got $postCustCount, " +
                    "Deliveries expected ${deliveriesList.size} got $postDelivCount, " +
                    "Payments expected ${paymentsList.size} got $postPayCount"
            Log.e(TAG, err)
            return MigrationResult(false, err)
        }

        Log.i(TAG, "Migration completely verified! Data safety check PASSED.")
        return MigrationResult(
            success = true,
            message = "Successfully migrated and verified $postCustCount customers, $postDelivCount deliveries, $postPayCount payments",
            customersCount = postCustCount,
            deliveriesCount = postDelivCount,
            paymentsCount = postPayCount
        )
    }
}
