package com.example.smartreceipetsplitter.data.local.database

import androidx.room.TypeConverter
import com.example.smartreceipetsplitter.data.local.enums.Categories

class Converters {
    @TypeConverter
    fun fromCategories(value: Categories): String = value.name
}