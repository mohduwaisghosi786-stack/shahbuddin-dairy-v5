package com.shahbuddindairy.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["isVoid"])
    ]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val customerId: Long,
    val date: String, // "YYYY-MM-DD"
    val amount: Double,
    val paymentMode: String = "Cash", // "Cash", "Online", "UPI", "Adjustment"
    val paymentSubtype: String = "",
    val note: String = "",
    val createdAt: String = "",
    val isVoid: Boolean = false,
    val voidedAt: String? = null
)
