package com.shahbuddindairy.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["routeOrder"]),
        Index(value = ["routeId"]),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val phone: String = "",
    val mohalla: String = "कटरा",
    val landmark: String = "",
    val note: String = "",
    val milkType: String = "भैंस",
    val defaultLiters: Double = 1.0,
    val ratePerLiter: Double = 0.0,
    val billingType: String = "monthly", // "daily_cash" or "monthly"
    val deliveryFrequency: String = "daily", // "daily" or "alternate"
    val frequencyAnchorDate: String = "",
    val isActive: Int = 1,
    val isArchived: Int = 0,
    val routeOrder: Int = 0,
    val routeId: String = "default",
    val portionsJson: String = "[]",
    val createdAt: String = ""
)
