package com.shahbuddindairy.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shahbuddindairy.app.data.model.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries WHERE date = :date")
    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE date = :date")
    suspend fun getDeliveriesForDateDirect(date: String): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY date DESC, id DESC")
    fun getDeliveriesForCustomer(customerId: Long): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY date ASC, id ASC")
    suspend fun getDeliveriesForCustomerDirect(customerId: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND date = :date LIMIT 1")
    suspend fun getDeliveryForCustomerAndDate(customerId: Long, date: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries ORDER BY date ASC, id ASC")
    suspend fun getAllDeliveriesDirect(): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE isVoid = 0 ORDER BY date DESC")
    fun getAllActiveDeliveries(): Flow<List<DeliveryEntity>>

    @Query("SELECT COUNT(*) FROM deliveries")
    suspend fun getDeliveryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<DeliveryEntity>)

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)
}
