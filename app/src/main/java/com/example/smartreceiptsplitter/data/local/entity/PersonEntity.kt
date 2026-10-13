package com.example.smartreceiptsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
    val avatarPath: String?,
    val isSelf: Boolean //tha app's own user is a normal row in DB with isSelf set as true
)