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
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.data.model.RouteEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val TAG = "BackupManager"
    private const val SAFETY_BACKUP_FILENAME = "safety_backup_before_restore.json"

    data class BackupStats(
        val customers: Int,
        val deliveries: Int,
        val payments: Int,
        val onlinePaymentsCount: Int,
        val routesCount: Int = 0,
        val notesCount: Int = 0,
        val mediaCount: Int = 0,
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
        val routes: List<RouteEntity> = emptyList(),
        val customerNotes: List<CustomerNoteEntity> = emptyList(),
        val settings: Map<String, String> = emptyMap(),
        val sourceUri: Uri? = null,
        val isZipArchive: Boolean = false,
        val mediaFilesCount: Int = 0
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
            val routes = database.routeDao().getAllRoutesDirect()
            val notes = database.customerNoteDao().getAllNotesDirect()
            val settingsList = settingDao.getAllSettings()
            val settingsMap = settingsList.associate { it.key to it.value }

            val notesMediaDir = File(context.filesDir, "notes_media")
            val notesAudioDir = File(context.filesDir, "notes_audio")
            val customSoundsDir = File(context.filesDir, "custom_sounds")

            val mediaFiles = mutableListOf<File>()
            if (notesMediaDir.exists()) notesMediaDir.listFiles()?.filter { it.isFile }?.let { mediaFiles.addAll(it) }
            if (notesAudioDir.exists()) notesAudioDir.listFiles()?.filter { it.isFile }?.let { mediaFiles.addAll(it) }
            if (customSoundsDir.exists()) customSoundsDir.listFiles()?.filter { it.isFile }?.let { mediaFiles.addAll(it) }

            val now = Date()
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(now)
            val fileSuffix = SimpleDateFormat("yyyy_MM_dd_HHmm", Locale.US).format(now)
            val zipFileName = "ShahbuddinDairy_Backup_$fileSuffix.zip"
            val jsonFileName = "ShahbuddinDairy_Backup_$fileSuffix.json"

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
                routesCount = routes.size,
                notesCount = notes.size,
                mediaCount = mediaFiles.size,
                totalLiters = (totalLit * 100.0).toInt() / 100.0,
                totalBill = (totalBill * 100.0).toInt() / 100.0,
                totalPaid = (totalPaid * 100.0).toInt() / 100.0,
                createdAt = isoDate
            )

            val payloadMap = linkedMapOf<String, Any>(
                "app" to "ShahbuddinDairy",
                "app_version" to "8.9.0",
                "backup_format" to 4,
                "schema" to 7,
                "created_at" to isoDate,
                "manifest" to mapOf(
                    "customers" to customers.size,
                    "deliveries" to deliveries.size,
                    "payments" to payments.size,
                    "online_payments" to onlinePaymentsCount,
                    "routes" to routes.size,
                    "notes" to notes.size,
                    "media_files" to mediaFiles.size,
                    "total_liters" to stats.totalLiters,
                    "total_bill" to stats.totalBill,
                    "total_paid" to stats.totalPaid
                ),
                "database" to mapOf(
                    "customers" to customers,
                    "deliveries" to deliveries,
                    "payments" to payments,
                    "routes" to routes,
                    "customer_notes" to notes
                ),
                "settings" to settingsMap
            )

            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(payloadMap)
            val checksum = sha256(jsonString)
            payloadMap["checksum"] = checksum
            val finalJson = gson.toJson(payloadMap)
            val fileBytes = finalJson.toByteArray(Charsets.UTF_8)

            // 1. Keep internal copies in filesDir/backups
            val internalBackupsDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val internalJsonFile = File(internalBackupsDir, jsonFileName)
            internalJsonFile.writeBytes(fileBytes)

            val internalZipFile = File(internalBackupsDir, zipFileName)
            ZipOutputStream(FileOutputStream(internalZipFile)).use { zos ->
                // Add backup.json
                val jsonEntry = ZipEntry("backup.json")
                zos.putNextEntry(jsonEntry)
                zos.write(fileBytes)
                zos.closeEntry()

                // Add all media files
                for (mf in mediaFiles) {
                    val relativeDir = when {
                        mf.parentFile?.name == "notes_media" -> "notes_media"
                        mf.parentFile?.name == "notes_audio" -> "notes_audio"
                        else -> "custom_sounds"
                    }
                    val entry = ZipEntry("$relativeDir/${mf.name}")
                    zos.putNextEntry(entry)
                    mf.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val zipBytes = internalZipFile.readBytes()
            var savedUri: Uri? = null
            var savedPath: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, zipFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { os ->
                        os.write(zipBytes)
                        os.flush()
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    savedUri = itemUri
                    savedPath = "Downloads/$zipFileName"
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, zipFileName)
                FileOutputStream(targetFile).use { fos ->
                    fos.write(zipBytes)
                    fos.flush()
                }
                savedPath = targetFile.absolutePath
                savedUri = Uri.fromFile(targetFile)
            }

            // Verify physical existence
            val verifiedLength = if (internalZipFile.exists()) internalZipFile.length() else 0L
            if (verifiedLength == 0L) {
                return BackupResult(false, "Failed to verify created backup file (0 bytes).")
            }

            // Update app settings metadata
            settingDao.setSetting(AppSettingEntity("last_backup_time", isoDate))
            val prevCount = settingDao.getSetting("backup_count")?.toIntOrNull() ?: 0
            settingDao.setSetting(AppSettingEntity("backup_count", (prevCount + 1).toString()))
            settingDao.setSetting(AppSettingEntity("last_backup_path", savedPath ?: internalZipFile.absolutePath))
            settingDao.setSetting(AppSettingEntity("last_backup_size", verifiedLength.toString()))

            Log.i(TAG, "Physical backup archive created successfully at $savedPath ($verifiedLength bytes, ${mediaFiles.size} media files)")
            BackupResult(
                success = true,
                message = "Backup archive created & verified: $zipFileName (${mediaFiles.size} media files)",
                uri = savedUri ?: Uri.fromFile(internalZipFile),
                filePath = savedPath ?: internalZipFile.absolutePath,
                fileName = zipFileName,
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
                val id = c.get("id")?.asLong ?: (i + 1L)
                val phone = c.get("phone")?.asString ?: ""
                val mohalla = c.get("mohalla")?.asString ?: "कटरा"
                val landmark = c.get("landmark")?.asString ?: ""
                val note = c.get("note")?.asString ?: ""
                val milkType = (c.get("milkType") ?: c.get("milk_type"))?.asString ?: "भैंस"
                val defaultLiters = (c.get("defaultLiters") ?: c.get("default_liters"))?.asDouble ?: 1.0
                val ratePerLiter = (c.get("ratePerLiter") ?: c.get("rate_per_liter"))?.asDouble ?: 0.0
                val billingTypeVal = (c.get("billingType") ?: c.get("billing_type"))?.asString
                val billingType = if (billingTypeVal == "daily_cash") "daily_cash" else "monthly"
                val delivFreqVal = (c.get("deliveryFrequency") ?: c.get("delivery_frequency"))?.asString
                val deliveryFrequency = if (delivFreqVal == "alternate") "alternate" else "daily"
                val freqAnchor = (c.get("frequencyAnchorDate") ?: c.get("frequency_anchor_date"))?.asString ?: ""
                val isActiveVal = (c.get("isActive") ?: c.get("is_active"))?.asInt
                val isActive = if (isActiveVal == 0) 0 else 1
                val isArchivedVal = (c.get("isArchived") ?: c.get("is_archived"))?.asInt
                val isArchived = if (isArchivedVal == 1) 1 else 0
                val routeId = (c.get("routeId") ?: c.get("route_id"))?.asString ?: "default"
                val routeOrder = (c.get("routeOrder") ?: c.get("route_order"))?.asInt ?: (i + 1)
                val portionsJson = (c.get("portionsJson") ?: c.get("portions_json"))?.asString ?: "[]"
                val createdAt = (c.get("createdAt") ?: c.get("created_at"))?.asString ?: ""

                customers.add(
                    CustomerEntity(
                        id = id,
                        name = name,
                        phone = phone,
                        mohalla = mohalla,
                        landmark = landmark,
                        note = note,
                        milkType = milkType,
                        defaultLiters = defaultLiters,
                        ratePerLiter = ratePerLiter,
                        billingType = billingType,
                        deliveryFrequency = deliveryFrequency,
                        frequencyAnchorDate = freqAnchor,
                        isActive = isActive,
                        isArchived = isArchived,
                        routeId = routeId,
                        routeOrder = routeOrder,
                        portionsJson = portionsJson,
                        createdAt = createdAt
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
                    val cid = (d.get("customerId") ?: d.get("customer_id"))?.asLong ?: continue
                    val cash = when {
                        d.has("paidCash") && !d.get("paidCash").isJsonNull -> d.get("paidCash").asDouble
                        d.has("paid_cash") && !d.get("paid_cash").isJsonNull -> d.get("paid_cash").asDouble
                        d.has("paid_amount") && !d.get("paid_amount").isJsonNull -> d.get("paid_amount").asDouble
                        else -> 0.0
                    }
                    val online = when {
                        d.has("paidOnline") && !d.get("paidOnline").isJsonNull -> d.get("paidOnline").asDouble
                        d.has("paid_online") && !d.get("paid_online").isJsonNull -> d.get("paid_online").asDouble
                        else -> 0.0
                    }
                    val tp = when {
                        d.has("totalPaid") && !d.get("totalPaid").isJsonNull -> d.get("totalPaid").asDouble
                        d.has("total_paid") && !d.get("total_paid").isJsonNull -> d.get("total_paid").asDouble
                        else -> (cash + online)
                    }
                    val isVoid = (d.get("isVoid") ?: d.get("is_void"))?.asBoolean ?: false
                    val status = if (d.get("status")?.asString == "skipped") "skipped" else "delivered"
                    val liters = d.get("liters")?.asDouble ?: 0.0
                    val totalAmt = (d.get("totalAmount") ?: d.get("total_amount"))?.asDouble ?: 0.0

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
                            deliveryTime = (d.get("deliveryTime") ?: d.get("delivery_time"))?.asString ?: "",
                            milkType = (d.get("milkType") ?: d.get("milk_type"))?.asString ?: "भैंस",
                            liters = liters,
                            rate = d.get("rate")?.asDouble ?: 0.0,
                            totalAmount = totalAmt,
                            paidCash = cash,
                            paidOnline = online,
                            totalPaid = tp,
                            status = status,
                            portionsJson = (d.get("portionsJson") ?: d.get("portions_json"))?.asString ?: "[]",
                            note = d.get("note")?.asString ?: "",
                            isVoid = isVoid,
                            voidedAt = (d.get("voidedAt") ?: d.get("voided_at"))?.takeIf { !it.isJsonNull }?.asString
                        )
                    )
                }
            }

            val payments = mutableListOf<PaymentEntity>()
            if (payArray != null) {
                for (i in 0 until payArray.size()) {
                    val p = payArray.get(i).asJsonObject
                    val cid = (p.get("customerId") ?: p.get("customer_id"))?.asLong ?: continue
                    val amt = p.get("amount")?.asDouble ?: 0.0
                    val mode = (p.get("paymentMode") ?: p.get("payment_mode"))?.asString ?: "Cash"
                    val isVoid = (p.get("isVoid") ?: p.get("is_void"))?.asBoolean ?: false

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
                            paymentSubtype = (p.get("paymentSubtype") ?: p.get("payment_subtype"))?.asString ?: "",
                            note = p.get("note")?.asString ?: "",
                            createdAt = (p.get("createdAt") ?: p.get("created_at"))?.asString ?: "",
                            isVoid = isVoid,
                            voidedAt = (p.get("voidedAt") ?: p.get("voided_at"))?.takeIf { !it.isJsonNull }?.asString
                        )
                    )
                }
            }

            val routes = mutableListOf<RouteEntity>()
            if (dbObj.has("routes") && !dbObj.get("routes").isJsonNull) {
                val routeArray = dbObj.getAsJsonArray("routes")
                for (i in 0 until routeArray.size()) {
                    val r = routeArray.get(i).asJsonObject
                    routes.add(
                        RouteEntity(
                            id = r.get("id")?.asString ?: "route_$i",
                            name = r.get("name")?.asString ?: "Route $i",
                            description = r.get("description")?.asString ?: "",
                            colorHex = r.get("color_hex")?.asString ?: "#16A34A",
                            sortOrder = r.get("sort_order")?.asInt ?: i,
                            isActive = r.get("is_active")?.asBoolean ?: true,
                            createdAt = r.get("created_at")?.asString ?: ""
                        )
                    )
                }
            }

            val customerNotes = mutableListOf<CustomerNoteEntity>()
            if (dbObj.has("customer_notes") && !dbObj.get("customer_notes").isJsonNull) {
                val notesArray = dbObj.getAsJsonArray("customer_notes")
                for (i in 0 until notesArray.size()) {
                    val n = notesArray.get(i).asJsonObject
                    val cid = when {
                        n.has("customerId") && !n.get("customerId").isJsonNull -> n.get("customerId").asLong
                        n.has("customer_id") && !n.get("customer_id").isJsonNull -> n.get("customer_id").asLong
                        else -> 0L
                    }
                    customerNotes.add(
                        CustomerNoteEntity(
                            id = n.get("id")?.asString ?: "note_$i",
                            customerId = cid,
                            content = n.get("content")?.asString ?: "",
                            category = n.get("category")?.asString ?: "General",
                            isPinned = when {
                                n.has("isPinned") && !n.get("isPinned").isJsonNull -> n.get("isPinned").asBoolean
                                n.has("is_pinned") && !n.get("is_pinned").isJsonNull -> n.get("is_pinned").asBoolean
                                else -> false
                            },
                            createdAt = (n.get("createdAt") ?: n.get("created_at"))?.asString ?: "",
                            updatedAt = (n.get("updatedAt") ?: n.get("updated_at"))?.asString ?: "",
                            title = n.get("title")?.asString ?: "",
                            tags = n.get("tags")?.asString ?: "",
                            priority = n.get("priority")?.asString ?: "Normal",
                            reminderDateTime = (n.get("reminderDateTime") ?: n.get("reminder_date_time"))?.asString ?: "",
                            mediaUris = (n.get("mediaUris") ?: n.get("media_uris"))?.asString ?: "",
                            audioUri = (n.get("audioUri") ?: n.get("audio_uri"))?.asString ?: "",
                            audioDurationMs = (n.get("audioDurationMs") ?: n.get("audio_duration_ms"))?.asLong ?: 0L,
                            isCompleted = when {
                                n.has("isCompleted") && !n.get("isCompleted").isJsonNull -> n.get("isCompleted").asBoolean
                                n.has("is_completed") && !n.get("is_completed").isJsonNull -> n.get("is_completed").asBoolean
                                else -> false
                            }
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
                routesCount = routes.size,
                notesCount = customerNotes.size,
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
                routes = routes,
                customerNotes = customerNotes,
                settings = settings
            )
        } catch (e: Exception) {
            ValidationResult(false, "Parsing failed: ${e.message}")
        }
    }

    suspend fun validateBackupFromUri(context: Context, uri: Uri): ValidationResult {
        return try {
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ValidationResult(false, "Could not open selected file")

            if (bytes.size < 4) {
                return ValidationResult(false, "File is too small to be a valid backup")
            }

            val isZip = bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

            if (isZip) {
                var jsonContent: String? = null
                var mediaCount = 0
                ZipInputStream(bytes.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "backup.json" || entry.name.endsWith(".json")) {
                            jsonContent = zis.bufferedReader(Charsets.UTF_8).readText()
                        } else if (entry.name.startsWith("notes_media/") || entry.name.startsWith("notes_audio/") || entry.name.startsWith("custom_sounds/")) {
                            if (!entry.isDirectory) mediaCount++
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                if (jsonContent.isNullOrBlank()) {
                    return ValidationResult(false, "No valid backup.json database record found inside ZIP archive")
                }

                val jsonValidation = validateBackupJson(jsonContent!!)
                if (!jsonValidation.isValid) {
                    return jsonValidation
                }

                return jsonValidation.copy(
                    sourceUri = uri,
                    isZipArchive = true,
                    mediaFilesCount = mediaCount,
                    stats = jsonValidation.stats?.copy(mediaCount = mediaCount)
                )
            } else {
                val rawJson = bytes.toString(Charsets.UTF_8)
                val jsonValidation = validateBackupJson(rawJson)
                return jsonValidation.copy(sourceUri = uri, isZipArchive = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating backup from URI", e)
            ValidationResult(false, "Validation failed: ${e.message}")
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
            val currRoutes = database.routeDao().getAllRoutesDirect()
            val currNotes = database.customerNoteDao().getAllNotesDirect()
            val currSettings = database.appSettingDao().getAllSettings().associate { it.key to it.value }

            val safetyPayload = mapOf(
                "snapshot_reason" to "pre_restore_safety",
                "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                "database" to mapOf(
                    "customers" to currCust,
                    "deliveries" to currDeliv,
                    "payments" to currPay,
                    "routes" to currRoutes,
                    "customer_notes" to currNotes
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

        // STEP 1.5: RESTORE MEDIA FILES IF ZIP ARCHIVE
        var restoredMediaCount = 0
        if (validationResult.isZipArchive && validationResult.sourceUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(validationResult.sourceUri)
                if (inputStream != null) {
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && (entry.name.startsWith("notes_media/") || entry.name.startsWith("notes_audio/") || entry.name.startsWith("custom_sounds/"))) {
                                val destFile = File(context.filesDir, entry.name)
                                destFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
                                destFile.outputStream().use { fos ->
                                    zis.copyTo(fos)
                                }
                                restoredMediaCount++
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                    Log.i(TAG, "Restored $restoredMediaCount media files from ZIP archive")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Warning: Could not extract media files: ${e.message}")
            }
        }

        // STEP 2: RESTORE IN ATOMIC ROOM TRANSACTION
        return try {
            database.withTransaction {
                database.customerDao().insertCustomers(validationResult.customers)
                database.deliveryDao().insertDeliveries(validationResult.deliveries)
                database.paymentDao().insertPayments(validationResult.payments)
                if (validationResult.routes.isNotEmpty()) {
                    database.routeDao().insertRoutes(validationResult.routes)
                }
                if (validationResult.customerNotes.isNotEmpty()) {
                    database.customerNoteDao().insertNotes(validationResult.customerNotes)
                }
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

            Log.i(TAG, "Post-restore verified: Customers=$postCust, Deliveries=$postDeliv, Payments=$postPay, Media=$restoredMediaCount")

            val mediaMsg = if (restoredMediaCount > 0) " & $restoredMediaCount media files" else ""
            RestoreResult(
                success = true,
                message = "Successfully restored $postCust customers, $postDeliv deliveries, $postPay payments$mediaMsg.",
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
                if (v.routes.isNotEmpty()) {
                    database.routeDao().insertRoutes(v.routes)
                }
                if (v.customerNotes.isNotEmpty()) {
                    database.customerNoteDao().insertNotes(v.customerNotes)
                }
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

    fun getLatestBackupFile(context: Context): File? {
        val internalBackupsDir = File(context.filesDir, "backups")
        if (!internalBackupsDir.exists()) return null
        return internalBackupsDir.listFiles()
            ?.filter { it.isFile && (it.extension == "zip" || it.extension == "json") && !it.name.contains("safety") }
            ?.maxByOrNull { it.lastModified() }
    }
}
