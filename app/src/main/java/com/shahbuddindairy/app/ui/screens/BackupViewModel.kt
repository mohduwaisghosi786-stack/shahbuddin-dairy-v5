package com.shahbuddindairy.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class BackupScreenUiState(
    val lastBackupTime: String = "Never",
    val backupCount: Int = 0,
    val lastBackupSize: Long = 0,
    val lastBackupPath: String = "",
    val totalCustomers: Int = 0,
    val totalDeliveries: Int = 0,
    val totalPayments: Int = 0,
    val totalOnlinePayments: Int = 0,
    val hasSafetySnapshot: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupResult: BackupManager.BackupResult? = null,
    val pendingRestoreValidation: BackupManager.ValidationResult? = null,
    val restoreResult: BackupManager.RestoreResult? = null,
    val payeeName: String = "Shahbuddin Dairy",
    val dairyPhone: String = "9897259461",
    val whatsappPhone: String = "9897259461",
    val upiId: String = "",
    val notifEnabled: Boolean = true,
    val notifPrivacy: String = "full",
    val isAppLockEnabled: Boolean = false
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DairyApplication
    private val database = app.database
    private val repository = app.repository

    private val _uiState = MutableStateFlow(BackupScreenUiState())
    val uiState: StateFlow<BackupScreenUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val custCount = database.customerDao().getCustomerCount()
            val delivCount = database.deliveryDao().getDeliveryCount()
            val payCount = database.paymentDao().getPaymentCount()

            var onlineCount = 0
            val delivs = database.deliveryDao().getAllDeliveriesDirect()
            val pays = database.paymentDao().getAllPaymentsDirect()
            for (d in delivs) if (!d.isVoid && d.paidOnline > 0) onlineCount++
            for (p in pays) if (!p.isVoid && p.paymentMode != "Cash") onlineCount++

            val lastTime = repository.getSetting("last_backup_time") ?: "Never"
            val count = repository.getSetting("backup_count")?.toIntOrNull() ?: 0
            val size = repository.getSetting("last_backup_size")?.toLongOrNull() ?: 0L
            val path = repository.getSetting("last_backup_path") ?: ""
            val payee = repository.getSetting("payee_name") ?: "Shahbuddin Dairy"
            val phone = repository.getSetting("dairy_phone") ?: "9897259461"
            val upi = repository.getSetting("upi_id") ?: ""
            val whatsapp = repository.getSetting("whatsapp_phone") ?: phone
            val notifEn = repository.getSetting("notif_enabled") != "false"
            val notifPriv = repository.getSetting("notif_privacy") ?: "full"

            val hasSafety = BackupManager.hasSafetySnapshot(app)
            val isLock = com.shahbuddindairy.app.security.AppLockManager.getInstance(app).isLockConfigured()

            _uiState.value = _uiState.value.copy(
                lastBackupTime = lastTime,
                backupCount = count,
                lastBackupSize = size,
                lastBackupPath = path,
                totalCustomers = custCount,
                totalDeliveries = delivCount,
                totalPayments = payCount,
                totalOnlinePayments = onlineCount,
                hasSafetySnapshot = hasSafety,
                payeeName = payee,
                dairyPhone = phone,
                whatsappPhone = whatsapp,
                upiId = upi,
                notifEnabled = notifEn,
                notifPrivacy = notifPriv,
                isAppLockEnabled = isLock
            )
        }
    }

    fun setNotifEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            repository.setSetting("notif_enabled", enabled.toString())
            _uiState.value = _uiState.value.copy(notifEnabled = enabled)
            if (enabled) {
                com.shahbuddindairy.app.notification.DailyTaskNotificationManager.updateNotification(context)
            } else {
                com.shahbuddindairy.app.notification.DailyTaskNotificationManager.cancelNotification(context)
            }
        }
    }

    fun setNotifPrivacy(privacy: String, context: Context) {
        viewModelScope.launch {
            repository.setSetting("notif_privacy", privacy)
            _uiState.value = _uiState.value.copy(notifPrivacy = privacy)
            com.shahbuddindairy.app.notification.DailyTaskNotificationManager.updateNotification(context)
        }
    }

    fun backupDataNow(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true, backupResult = null)
            val result = withContext(Dispatchers.IO) {
                BackupManager.createBackup(context, database)
            }
            _uiState.value = _uiState.value.copy(isBackingUp = false, backupResult = result)
            refreshStatus()
        }
    }

    fun shareBackupToCloud(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            var backupFile = withContext(Dispatchers.IO) {
                BackupManager.getLatestBackupFile(context)
            }
            if (backupFile == null) {
                val res = withContext(Dispatchers.IO) {
                    BackupManager.createBackup(context, database)
                }
                _uiState.value = _uiState.value.copy(backupResult = res)
                backupFile = withContext(Dispatchers.IO) {
                    BackupManager.getLatestBackupFile(context)
                }
            }
            _uiState.value = _uiState.value.copy(isBackingUp = false)

            if (backupFile != null && backupFile.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        backupFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Shahbuddin Dairy Backup - ${backupFile.name}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Save Backup to Google Drive / Cloud")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "No backup file found to share", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onFileSelectedForRestore(context: Context, uri: Uri) {
        viewModelScope.launch {
            val rawJson = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        InputStreamReader(stream, Charsets.UTF_8).readText()
                    } ?: ""
                } catch (e: Exception) {
                    ""
                }
            }

            if (rawJson.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    pendingRestoreValidation = BackupManager.ValidationResult(false, "Could not read selected file")
                )
                return@launch
            }

            val validation = withContext(Dispatchers.IO) {
                BackupManager.validateBackupJson(rawJson)
            }
            _uiState.value = _uiState.value.copy(pendingRestoreValidation = validation)
        }
    }

    fun dismissRestoreDialog() {
        _uiState.value = _uiState.value.copy(pendingRestoreValidation = null)
    }

    fun executeRestore(context: Context) {
        val validation = _uiState.value.pendingRestoreValidation ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val res = withContext(Dispatchers.IO) {
                BackupManager.executeRestore(context, database, validation)
            }
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                pendingRestoreValidation = null,
                restoreResult = res
            )
            refreshStatus()
        }
    }

    fun revertSafetyBackup(context: Context) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                BackupManager.revertSafetySnapshot(context, database)
            }
            _uiState.value = _uiState.value.copy(restoreResult = res)
            refreshStatus()
        }
    }

    fun clearBackupResult() {
        _uiState.value = _uiState.value.copy(backupResult = null)
    }

    fun clearRestoreResult() {
        _uiState.value = _uiState.value.copy(restoreResult = null)
    }

    fun saveSettings(payee: String, phone: String, upi: String) {
        saveUpiSettings(payee, phone, upi, phone)
    }

    fun saveUpiSettings(payee: String, phone: String, upi: String, whatsapp: String) {
        viewModelScope.launch {
            repository.setSetting("payee_name", payee)
            repository.setSetting("dairy_phone", phone)
            repository.setSetting("upi_id", upi)
            repository.setSetting("whatsapp_phone", whatsapp)
            refreshStatus()
        }
    }

    fun setAppLock(enabled: Boolean, pin: String? = null, currentPin: String? = null): Boolean {
        val appLockManager = com.shahbuddindairy.app.security.AppLockManager.getInstance(app)
        val success = if (enabled && pin != null) {
            appLockManager.setPin(pin)
        } else if (!enabled && currentPin != null) {
            appLockManager.disableLock(currentPin)
        } else {
            false
        }
        refreshStatus()
        return success
    }

    fun changeAppLockPin(currentPin: String, newPin: String): Boolean {
        val appLockManager = com.shahbuddindairy.app.security.AppLockManager.getInstance(app)
        val success = appLockManager.changePin(currentPin, newPin)
        refreshStatus()
        return success
    }
}
