package com.example.smartreceipetsplitter.data.local.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCategories(value: Categories): String = value.name
}