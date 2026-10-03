package com.shahbuddindairy.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_notes",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["isPinned"])
    ]
)
data class CustomerNoteEntity(
    @PrimaryKey
    val id: String,
    val customerId: Long,
    val content: String,
    val category: String = "General", // "General", "Delivery", "Payment", "Preference"
    val isPinned: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)
