package com.shahbuddindairy.app.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.shahbuddindairy.app.data.db.AppDatabase
import com.shahbuddindairy.app.data.model.AppSettingEntity
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {
    private const val TAG = "BackupManager"
    private const val SAFETY_BACKUP_FILENAME = "safety_backup_before_restore.json"

    data class BackupStats(
        val customers: Int,
        val deliveries: Int,
        val payments: Int,
        val onlinePaymentsCount: Int,
        val totalLiters: Double,
        val totalBill: Double,
        val totalPaid: Double,
        val createdAt: String
    )

    data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null,
        val stats: BackupStats? = null,
        val customers: List<CustomerEntity> = emptyList(),
        val deliveries: List<DeliveryEntity> = emptyList(),
        val payments: List<PaymentEntity> = emptyList(),
        val settings: Map<String, String> = emptyMap()
    )

    data class BackupResult(
        val success: Boolean,
        val message: String,
        val uri: Uri? = null,
        val filePath: String? = null,
        val fileName: String? = null,
        val sizeBytes: Long = 0,
        val timestamp: String = "",
        val stats: BackupStats? = null
    )

    data class RestoreResult(
        val success: Boolean,
        val message: String,
        val restoredStats: BackupStats? = null
    )

    private fun sha256(str: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(str.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun createBackup(context: Context, database: AppDatabase): BackupResult {
        return try {
            val customerDao = database.customerDao()
            val deliveryDao = database.deliveryDao()
            val paymentDao = database.paymentDao()
            val settingDao = database.appSettingDao()

            val customers = customerDao.getAllCustomersDirect()
            val deliveries = deliveryDao.getAllDeliveriesDirect()
            val payments = paymentDao.getAllPaymentsDirect()
            val settingsList = settingDao.getAllSettings()
            val settingsMap = settingsList.associate { it.key to it.value }

            val now = Date()
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(now)
            val fileSuffix = SimpleDateFormat("yyyy_MM_dd_HHmm", Locale.US).format(now)
            val fileName = "ShahbuddinDairy_Backup_$fileSuffix.json"

            var totalLit = 0.0
            var totalBill = 0.0
            var totalPaid = 0.0
            var onlinePaymentsCount = 0

            for (d in deliveries) {
                if (!d.isVoid) {
                    if (d.status == "delivered") {
                        totalLit += d.liters
                        totalBill += d.totalAmount
                    }
                    totalPaid += d.totalPaid
                    if (d.paidOnline > 0) onlinePaymentsCount++
                }
            }

            for (p in payments) {
                if (!p.isVoid) {
                    totalPaid += p.amount
                    if (p.paymentMode != "Cash") onlinePaymentsCount++
                }
            }

            val stats = BackupStats(
                customers = customers.size,
                deliveries = deliveries.size,
                payments = payments.size,
                onlinePaymentsCount = onlinePaymentsCount,
                totalLiters = (totalLit * 100.0).toInt() / 100.0,
                totalBill = (totalBill * 100.0).toInt() / 100.0,
                totalPaid = (totalPaid * 100.0).toInt() / 100.0,
                createdAt = isoDate
            )

            val payloadMap = linkedMapOf<String, Any>(
                "app" to "ShahbuddinDairy",
                "app_version" to "5.1.0",
                "backup_format" to 2,
                "schema" to 5,
                "created_at" to isoDate,
                "manifest" to mapOf(
                    "customers" to customers.size,
                    "deliveries" to deliveries.size,
                    "payments" to payments.size,
                    "online_payments" to onlinePaymentsCount,
                    "total_liters" to stats.totalLiters,
                    "total_bill" to stats.totalBill,
                    "total_paid" to stats.totalPaid
                ),
                "database" to mapOf(
                    "customers" to customers,
                    "deliveries" to deliveries,
                    "payments" to payments
                ),
                "settings" to settingsMap
            )

            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(payloadMap)
            val checksum = sha256(jsonString)
            payloadMap["checksum"] = checksum
            val finalJson = gson.toJson(payloadMap)
            val fileBytes = finalJson.toByteArray(Charsets.UTF_8)

            var savedUri: Uri? = null
            var savedPath: String? = null

            // Also keep an internal copy in filesDir/backups
            val internalBackupsDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val internalFile = File(internalBackupsDir, fileName)
            internalFile.writeBytes(fileBytes)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { os ->
                        os.write(fileBytes)
                        os.flush()
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    savedUri = itemUri
                    savedPath = "Downloads/$fileName"
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { fos ->
                    fos.write(fileBytes)
                    fos.flush()
                }
                savedPath = targetFile.absolutePath
                savedUri = Uri.fromFile(targetFile)
            }

            // Verify physical existence
            val verifiedLength = if (internalFile.exists()) internalFile.length() else 0L
            if (verifiedLength == 0L) {
                return BackupResult(false, "Failed to verify created backup file (0 bytes).")
            }

            // Update app settings metadata
            settingDao.setSetting(AppSettingEntity("last_backup_time", isoDate))
            val prevCount = settingDao.getSetting("backup_count")?.toIntOrNull() ?: 0
            settingDao.setSetting(AppSettingEntity("backup_count", (prevCount + 1).toString()))
            settingDao.setSetting(AppSettingEntity("last_backup_path", savedPath ?: internalFile.absolutePath))
            settingDao.setSetting(AppSettingEntity("last_backup_size", verifiedLength.toString()))

            Log.i(TAG, "Physical backup created successfully at $savedPath ($verifiedLength bytes)")
            BackupResult(
                success = true,
                message = "Backup created & verified: $fileName",
                uri = savedUri ?: Uri.fromFile(internalFile),
                filePath = savedPath ?: internalFile.absolutePath,
                fileName = fileName,
                sizeBytes = verifiedLength,
                timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(now),
                stats = stats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Backup creation error", e)
            BackupResult(false, "Error creating backup: ${e.message}")
        }
    }

    fun validateBackupJson(rawJson: String): ValidationResult {
        return try {
            val gson = Gson()
            val root = gson.fromJson(rawJson, JsonObject::class.java)
                ?: return ValidationResult(false, "Empty or invalid JSON")

            val dbObj = if (root.has("database")) root.getAsJsonObject("database") else root
            val custArray = if (dbObj.has("customers")) dbObj.getAsJsonArray("customers") else null
                ?: return ValidationResult(false, "Missing 'customers' in backup")

            val delivArray = if (dbObj.has("deliveries")) dbObj.getAsJsonArray("deliveries") else null
            val payArray = if (dbObj.has("payments")) dbObj.getAsJsonArray("payments") else null

            val customers = mutableListOf<CustomerEntity>()
            for (i in 0 until custArray.size()) {
                val c = custArray.get(i).asJsonObject
                val name = c.get("name")?.asString ?: ""
                if (name.isBlank()) {
                    return ValidationResult(false, "Invalid customer entry at index $i (blank name)")
                }
                customers.add(
                    CustomerEntity(
                        id = c.get("id")?.asLong ?: (i + 1L),
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

            val deliveries = mutableListOf<DeliveryEntity>()
            var totalLiters = 0.0
            var totalBill = 0.0
            var totalPaid = 0.0
            var onlineCount = 0

            if (delivArray != null) {
                for (i in 0 until delivArray.size()) {
                    val d = delivArray.get(i).asJsonObject
                    val cid = d.get("customer_id")?.asLong ?: continue
                    val cash = when {
                        d.has("paid_cash") && !d.get("paid_cash").isJsonNull -> d.get("paid_cash").asDouble
                        d.has("paid_amount") && !d.get("paid_amount").isJsonNull -> d.get("paid_amount").asDouble
                        else -> 0.0
                    }
                    val online = if (d.has("paid_online") && !d.get("paid_online").isJsonNull) d.get("paid_online").asDouble else 0.0
                    val tp = if (d.has("total_paid") && !d.get("total_paid").isJsonNull) d.get("total_paid").asDouble else (cash + online)
                    val isVoid = d.get("is_void")?.asBoolean ?: false
                    val status = if (d.get("status")?.asString == "skipped") "skipped" else "delivered"
                    val liters = d.get("liters")?.asDouble ?: 0.0
                    val totalAmt = d.get("total_amount")?.asDouble ?: 0.0

                    if (!isVoid) {
                        if (status == "delivered") {
                            totalLiters += liters
                            totalBill += totalAmt
                        }
                        totalPaid += tp
                        if (online > 0) onlineCount++
                    }

                    deliveries.add(
                        DeliveryEntity(
                            id = d.get("id")?.asString ?: "d_$i",
                            customerId = cid,
                            date = d.get("date")?.asString ?: "",
                            deliveryTime = d.get("delivery_time")?.asString ?: "",
                            milkType = d.get("milk_type")?.asString ?: "भैंस",
                            liters = liters,
                            rate = d.get("rate")?.asDouble ?: 0.0,
                            totalAmount = totalAmt,
                            paidCash = cash,
                            paidOnline = online,
                            totalPaid = tp,
                            status = status,
                            portionsJson = d.get("portions_json")?.asString ?: "[]",
                            note = d.get("note")?.asString ?: "",
                            isVoid = isVoid,
                            voidedAt = d.get("voided_at")?.takeIf { !it.isJsonNull }?.asString
                        )
                    )
                }
            }

            val payments = mutableListOf<PaymentEntity>()
            if (payArray != null) {
                for (i in 0 until payArray.size()) {
                    val p = payArray.get(i).asJsonObject
                    val cid = p.get("customer_id")?.asLong ?: continue
                    val amt = p.get("amount")?.asDouble ?: 0.0
                    val mode = p.get("payment_mode")?.asString ?: "Cash"
                    val isVoid = p.get("is_void")?.asBoolean ?: false

                    if (!isVoid) {
                        totalPaid += amt
                        if (mode != "Cash") onlineCount++
                    }

                    payments.add(
                        PaymentEntity(
                            id = p.get("id")?.asString ?: "pay_$i",
                            customerId = cid,
                            date = p.get("date")?.asString ?: "",
                            amount = amt,
                            paymentMode = mode,
                            paymentSubtype = p.get("payment_subtype")?.asString ?: "",
                            note = p.get("note")?.asString ?: "",
                            createdAt = p.get("created_at")?.asString ?: "",
                            isVoid = isVoid,
                            voidedAt = p.get("voided_at")?.takeIf { !it.isJsonNull }?.asString
                        )
                    )
                }
            }

            val createdAt = root.get("created_at")?.asString ?: ""
            val stats = BackupStats(
                customers = customers.size,
                deliveries = deliveries.size,
                payments = payments.size,
                onlinePaymentsCount = onlineCount,
                totalLiters = (totalLiters * 100.0).toInt() / 100.0,
                totalBill = (totalBill * 100.0).toInt() / 100.0,
                totalPaid = (totalPaid * 100.0).toInt() / 100.0,
                createdAt = createdAt
            )

            val settings = mutableMapOf<String, String>()
            if (root.has("settings")) {
                val sObj = root.getAsJsonObject("settings")
                for (key in sObj.keySet()) {
                    val v = sObj.get(key)
                    if (v.isJsonPrimitive) settings[key] = v.asString
                }
            }

            ValidationResult(
                isValid = true,
                stats = stats,
                customers = customers,
                deliveries = deliveries,
                payments = payments,
                settings = settings
            )
        } catch (e: Exception) {
            ValidationResult(false, "Parsing failed: ${e.message}")
        }
    }

    suspend fun executeRestore(
        context: Context,
        database: AppDatabase,
        validationResult: ValidationResult
    ): RestoreResult {
        if (!validationResult.isValid || validationResult.stats == null) {
            return RestoreResult(false, "Cannot restore invalid backup: ${validationResult.error}")
        }

        // STEP 1: CREATE AUTOMATIC SAFETY BACKUP OF CURRENT DATA BEFORE RESTORE
        val safetyFile = File(context.filesDir, SAFETY_BACKUP_FILENAME)
        try {
            val currCust = database.customerDao().getAllCustomersDirect()
            val currDeliv = database.deliveryDao().getAllDeliveriesDirect()
            val currPay = database.paymentDao().getAllPaymentsDirect()
            val currSettings = database.appSettingDao().getAllSettings().associate { it.key to it.value }

            val safetyPayload = mapOf(
                "snapshot_reason" to "pre_restore_safety",
                "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                "database" to mapOf(
                    "customers" to currCust,
                    "deliveries" to currDeliv,
                    "payments" to currPay
                ),
                "settings" to currSettings
            )
            val gson = GsonBuilder().setPrettyPrinting().create()
            safetyFile.writeText(gson.toJson(safetyPayload), Charsets.UTF_8)
            Log.i(TAG, "Pre-restore safety snapshot created at ${safetyFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create pre-restore safety snapshot", e)
            return RestoreResult(false, "Could not create safety snapshot before restore: ${e.message}")
        }

        // STEP 2: RESTORE IN ATOMIC ROOM TRANSACTION
        return try {
            database.withTransaction {
                database.customerDao().insertCustomers(validationResult.customers)
                database.deliveryDao().insertDeliveries(validationResult.deliveries)
                database.paymentDao().insertPayments(validationResult.payments)
                for ((k, v) in validationResult.settings) {
                    database.appSettingDao().setSetting(AppSettingEntity(k, v))
                }
                database.appSettingDao().setSetting(
                    AppSettingEntity("last_restored_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                )
            }

            // STEP 3: VERIFY RESTORED COUNTS
            val postCust = database.customerDao().getCustomerCount()
            val postDeliv = database.deliveryDao().getDeliveryCount()
            val postPay = database.paymentDao().getPaymentCount()

            Log.i(TAG, "Post-restore verified: Customers=$postCust, Deliveries=$postDeliv, Payments=$postPay")

            RestoreResult(
                success = true,
                message = "Successfully restored $postCust customers, $postDeliv deliveries, $postPay payments.",
                restoredStats = validationResult.stats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Restore transaction failed. Reverting to safety snapshot.", e)
            // Rollback using safety snapshot
            revertSafetySnapshot(context, database)
            RestoreResult(false, "Restore failed and was safely rolled back: ${e.message}")
        }
    }

    suspend fun revertSafetySnapshot(context: Context, database: AppDatabase): RestoreResult {
        val safetyFile = File(context.filesDir, SAFETY_BACKUP_FILENAME)
        if (!safetyFile.exists()) {
            return RestoreResult(false, "No safety snapshot found to revert to.")
        }
        return try {
            val json = safetyFile.readText(Charsets.UTF_8)
            val v = validateBackupJson(json)
            if (!v.isValid) {
                return RestoreResult(false, "Safety snapshot is corrupted: ${v.error}")
            }
            database.withTransaction {
                database.customerDao().insertCustomers(v.customers)
                database.deliveryDao().insertDeliveries(v.deliveries)
                database.paymentDao().insertPayments(v.payments)
            }
            RestoreResult(true, "Successfully reverted to pre-restore safety snapshot ✓")
        } catch (e: Exception) {
            RestoreResult(false, "Failed to revert safety snapshot: ${e.message}")
        }
    }

    fun hasSafetySnapshot(context: Context): Boolean {
        val f = File(context.filesDir, SAFETY_BACKUP_FILENAME)
        return f.exists() && f.length() > 0
    }
}
