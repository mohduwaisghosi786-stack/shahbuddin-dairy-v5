package com.shahbuddindairy.app.data.repository

import com.shahbuddindairy.app.data.dao.AppSettingDao
import com.shahbuddindairy.app.data.dao.CustomerDao
import com.shahbuddindairy.app.data.dao.DeliveryDao
import com.shahbuddindairy.app.data.dao.PaymentDao
import com.shahbuddindairy.app.data.model.AppSettingEntity
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DairyRepository(
    private val customerDao: CustomerDao,
    private val deliveryDao: DeliveryDao,
    private val paymentDao: PaymentDao,
    private val settingDao: AppSettingDao
) {
    private val transactionMutex = Mutex()

    fun getAllActiveCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllActiveCustomers()

    suspend fun getAllActiveCustomersDirect(): List<CustomerEntity> = customerDao.getAllActiveCustomersDirect()

    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun getAllCustomersDirect(): List<CustomerEntity> = customerDao.getAllCustomersDirect()

    fun getCustomerById(id: Long): Flow<CustomerEntity?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): CustomerEntity? = customerDao.getCustomerByIdDirect(id)

    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntity>> = deliveryDao.getDeliveriesForDate(date)

    suspend fun getDeliveriesForDateDirect(date: String): List<DeliveryEntity> = deliveryDao.getDeliveriesForDateDirect(date)

    fun getDeliveriesForCustomer(customerId: Long): Flow<List<DeliveryEntity>> = deliveryDao.getDeliveriesForCustomer(customerId)

    suspend fun getDeliveriesForCustomerDirect(customerId: Long): List<DeliveryEntity> = deliveryDao.getDeliveriesForCustomerDirect(customerId)

    suspend fun getAllDeliveriesDirect(): List<DeliveryEntity> = deliveryDao.getAllDeliveriesDirect()

    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForCustomer(customerId)

    suspend fun getPaymentsForCustomerDirect(customerId: Long): List<PaymentEntity> = paymentDao.getPaymentsForCustomerDirect(customerId)

    fun getPaymentsForDate(date: String): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForDate(date)

    suspend fun getPaymentsForDateDirect(date: String): List<PaymentEntity> = paymentDao.getPaymentsForDateDirect(date)

    suspend fun getAllPaymentsDirect(): List<PaymentEntity> = paymentDao.getAllPaymentsDirect()

    suspend fun saveDelivery(delivery: DeliveryEntity) {
        transactionMutex.withLock {
            deliveryDao.insertDelivery(delivery)
        }
    }

    suspend fun savePayment(payment: PaymentEntity) {
        transactionMutex.withLock {
            paymentDao.insertPayment(payment)
        }
    }

    suspend fun voidDelivery(deliveryId: String) {
        transactionMutex.withLock {
            val all = deliveryDao.getAllDeliveriesDirect()
            val existing = all.find { it.id == deliveryId } ?: return@withLock
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val updated = existing.copy(isVoid = true, voidedAt = nowStr)
            deliveryDao.updateDelivery(updated)
        }
    }

    suspend fun voidPayment(paymentId: String) {
        transactionMutex.withLock {
            val all = paymentDao.getAllPaymentsDirect()
            val existing = all.find { it.id == paymentId } ?: return@withLock
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val updated = existing.copy(isVoid = true, voidedAt = nowStr)
            paymentDao.updatePayment(updated)
        }
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return transactionMutex.withLock {
            val finalCustomer = if (customer.id <= 0) {
                val maxId = customerDao.getMaxCustomerId() ?: 0L
                customer.copy(id = maxId + 1L)
            } else {
                customer
            }
            customerDao.insertCustomer(finalCustomer)
        }
    }

    suspend fun getNextCustomerId(): Long {
        return (customerDao.getMaxCustomerId() ?: 0L) + 1L
    }

    suspend fun getDeliveryForCustomerAndDate(customerId: Long, date: String): DeliveryEntity? {
        return deliveryDao.getDeliveryForCustomerAndDate(customerId, date)
    }

    suspend fun getSetting(key: String): String? = settingDao.getSetting(key)

    fun getSettingFlow(key: String): Flow<String?> = settingDao.getSettingFlow(key)

    suspend fun setSetting(key: String, value: String) {
        transactionMutex.withLock {
            settingDao.setSetting(AppSettingEntity(key, value))
        }
    }

    suspend fun getPbsPending(customerId: Long): Double {
        return getSetting("pbs_pending_$customerId")?.toDoubleOrNull() ?: 0.0
    }

    suspend fun setPbsPending(customerId: Long, amount: Double) {
        setSetting("pbs_pending_$customerId", amount.toString())
    }

    fun getAllPbsSettingsFlow(): Flow<List<AppSettingEntity>> = settingDao.getAllPbsSettingsFlow()

    suspend fun updateCustomerRouteOrders(orderList: List<Pair<Long, Int>>) {
        transactionMutex.withLock {
            for ((cId, order) in orderList) {
                customerDao.updateRouteOrder(cId, order)
            }
        }
    }
}

