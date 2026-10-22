package com.example.smartreceiptsplitter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smartreceiptsplitter.data.local.dao.CategoryDao
import com.example.smartreceiptsplitter.data.local.dao.PersonDao
import com.example.smartreceiptsplitter.data.local.dao.ReceiptDao
import com.example.smartreceiptsplitter.data.local.dao.ReceiptItemDao
import com.example.smartreceiptsplitter.data.local.dao.ReceiptSplitDao
import com.example.smartreceiptsplitter.data.local.entity.AppSettingsEntity
import com.example.smartreceiptsplitter.data.local.entity.CategoryEntity
import com.example.smartreceiptsplitter.data.local.entity.ExchangeRateEntity
import com.example.smartreceiptsplitter.data.local.entity.GroupEntity
import com.example.smartreceiptsplitter.data.local.entity.GroupMemberCrossRefEntity
import com.example.smartreceiptsplitter.data.local.entity.PersonEntity
import com.example.smartreceiptsplitter.data.local.entity.ReceiptEntity
import com.example.smartreceiptsplitter.data.local.entity.ReceiptItemEntity
import com.example.smartreceiptsplitter.data.local.entity.ReceiptSplitEntity

@Database(
    entities=[
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        CategoryEntity::class,
        ReceiptSplitEntity::class,
        GroupEntity::class,
        GroupMemberCrossRefEntity::class,
        AppSettingsEntity::class,
        ExchangeRateEntity::class,
        PersonEntity::class
             ],
    version = 2,
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