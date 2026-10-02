package com.shahbuddindairy.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shahbuddindairy.app.data.model.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC, createdAt DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date ASC, createdAt ASC")
    suspend fun getPaymentsForCustomerDirect(customerId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE date = :date")
    fun getPaymentsForDate(date: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE date = :date")
    suspend fun getPaymentsForDateDirect(date: String): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY date ASC, id ASC")
    suspend fun getAllPaymentsDirect(): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun getPaymentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)
}
