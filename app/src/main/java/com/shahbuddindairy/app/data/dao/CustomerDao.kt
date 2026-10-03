package com.shahbuddindairy.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shahbuddindairy.app.data.model.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY routeOrder ASC, id ASC")
    fun getAllActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY routeOrder ASC, id ASC")
    suspend fun getAllActiveCustomersDirect(): List<CustomerEntity>

    @Query("SELECT * FROM customers ORDER BY routeOrder ASC, id ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY routeOrder ASC, id ASC")
    suspend fun getAllCustomersDirect(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: Long): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int

    @Query("SELECT MAX(id) FROM customers")
    suspend fun getMaxCustomerId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET routeOrder = :order WHERE id = :customerId")
    suspend fun updateRouteOrder(customerId: Long, order: Int)

    @Query("UPDATE customers SET routeId = :routeId WHERE id = :customerId")
    suspend fun updateCustomerRoute(customerId: Long, routeId: String)
}

