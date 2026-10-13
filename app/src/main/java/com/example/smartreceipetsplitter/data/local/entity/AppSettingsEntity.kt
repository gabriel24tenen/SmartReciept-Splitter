package com.example.smartreceipetsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val homeCurrency: String,
    val defaultVatRate: Double,
    val pinHash: String?
)