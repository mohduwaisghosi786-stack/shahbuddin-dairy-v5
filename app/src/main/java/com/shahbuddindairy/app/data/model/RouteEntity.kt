package com.shahbuddindairy.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes"
)
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val colorHex: String = "#16A34A",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: String = ""
)
