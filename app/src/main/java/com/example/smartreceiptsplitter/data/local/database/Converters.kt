package com.example.smartreceiptsplitter.data.local.database

import androidx.room.TypeConverter
import com.example.smartreceiptsplitter.data.local.enums.Categories

class Converters {
    @TypeConverter
    fun fromCategories(value: Categories): String = value.name
}