package com.example.smartreceipetsplitter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smartreceipetsplitter.data.local.dao.CategoryDao
import com.example.smartreceipetsplitter.data.local.dao.PersonDao
import com.example.smartreceipetsplitter.data.local.dao.ReceiptDao
import com.example.smartreceipetsplitter.data.local.dao.ReceiptItemDao
import com.example.smartreceipetsplitter.data.local.dao.ReceiptSplitDao
import com.example.smartreceipetsplitter.data.local.entity.AppSettingsEntity
import com.example.smartreceipetsplitter.data.local.entity.CategoryEntity
import com.example.smartreceipetsplitter.data.local.entity.ExchangeRateEntity
import com.example.smartreceipetsplitter.data.local.entity.GroupEntity
import com.example.smartreceipetsplitter.data.local.entity.GroupMemberCrossRefEntity
import com.example.smartreceipetsplitter.data.local.entity.ReceiptEntity
import com.example.smartreceipetsplitter.data.local.entity.ReceiptItemEntity
import com.example.smartreceipetsplitter.data.local.entity.ReceiptSplitEntity

@Database(
    entities=[
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        CategoryEntity::class,
        ReceiptSplitEntity::class,
        GroupEntity::class,
        GroupMemberCrossRefEntity::class,
        AppSettingsEntity::class,
        ExchangeRateEntity::class
             ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun receiptItemDao(): ReceiptItemDao
    abstract fun personDao(): PersonDao
    abstract fun receiptSplitDao(): ReceiptSplitDao
    abstract fun categoryDao(): CategoryDao

    //TODO: rest of DAOs for Group, AppSettings, ExchangeRates will come here later on with another versions
}