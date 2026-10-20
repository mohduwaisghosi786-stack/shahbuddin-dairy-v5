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
    val customerId: Long, // 0L for general/standalone notes, >0 for customer-linked notes
    val content: String,
    val category: String = "General", // "General", "Delivery", "Payment", "Preference", "Voice"
    val isPinned: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val title: String = "",
    val tags: String = "",
    val priority: String = "Normal", // "Low", "Normal", "High"
    val reminderDateTime: String = "",
    val mediaUris: String = "", // JSON list or comma-separated paths of attached photos
    val audioUri: String = "", // Path to voice note recording
    val audioDurationMs: Long = 0L,
    val isCompleted: Boolean = false
)
