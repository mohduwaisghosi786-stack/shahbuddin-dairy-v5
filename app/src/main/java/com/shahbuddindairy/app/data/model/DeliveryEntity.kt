package com.shahbuddindairy.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["customerId", "date"]),
        Index(value = ["isVoid"])
    ]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: String,
    val customerId: Long,
    val date: String, // "YYYY-MM-DD"
    val deliveryTime: String = "",
    val milkType: String = "भैंस",
    val liters: Double = 0.0,
    val rate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidCash: Double = 0.0,
    val paidOnline: Double = 0.0,
    val totalPaid: Double = 0.0,
    val status: String = "delivered", // "delivered" or "skipped"
    val portionsJson: String = "[]",
    val note: String = "",
    val isVoid: Boolean = false,
    val voidedAt: String? = null
)
