package com.shahbuddindairy.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerNoteDao {
    @Query("SELECT * FROM customer_notes WHERE customerId = :customerId ORDER BY isPinned DESC, createdAt DESC")
    fun getNotesForCustomer(customerId: Long): Flow<List<CustomerNoteEntity>>

    @Query("SELECT * FROM customer_notes WHERE customerId = :customerId ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getNotesForCustomerDirect(customerId: Long): List<CustomerNoteEntity>

    @Query("SELECT * FROM customer_notes WHERE isPinned = 1")
    fun getAllPinnedNotes(): Flow<List<CustomerNoteEntity>>

    @Query("SELECT * FROM customer_notes WHERE isPinned = 1")
    suspend fun getAllPinnedNotesDirect(): List<CustomerNoteEntity>

    @Query("SELECT * FROM customer_notes")
    suspend fun getAllNotesDirect(): List<CustomerNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CustomerNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<CustomerNoteEntity>)

    @Update
    suspend fun updateNote(note: CustomerNoteEntity)

    @Query("DELETE FROM customer_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
