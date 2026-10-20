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

    @Query("SELECT * FROM customer_notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<CustomerNoteEntity>>

    @Query("SELECT * FROM customer_notes WHERE customerId = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getGeneralNotes(): Flow<List<CustomerNoteEntity>>

    @Query("SELECT * FROM customer_notes WHERE customerId > 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getCustomerNotes(): Flow<List<CustomerNoteEntity>>

    @Query("SELECT * FROM customer_notes WHERE id = :id")
    fun getNoteById(id: String): Flow<CustomerNoteEntity?>

    @Query("SELECT * FROM customer_notes WHERE id = :id")
    suspend fun getNoteByIdDirect(id: String): CustomerNoteEntity?

    @Query("SELECT * FROM customer_notes WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY isPinned DESC, createdAt DESC")
    fun searchNotes(query: String): Flow<List<CustomerNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CustomerNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<CustomerNoteEntity>)

    @Update
    suspend fun updateNote(note: CustomerNoteEntity)

    @Query("DELETE FROM customer_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
