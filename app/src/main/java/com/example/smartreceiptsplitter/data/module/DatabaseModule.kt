package com.example.smartreceiptsplitter.data.module

import android.content.Context
import androidx.room.Room
import com.example.smartreceiptsplitter.data.local.dao.CategoryDao
import com.example.smartreceiptsplitter.data.local.dao.PersonDao
import com.example.smartreceiptsplitter.data.local.dao.ReceiptDao
import com.example.smartreceiptsplitter.data.local.dao.ReceiptItemDao
import com.example.smartreceiptsplitter.data.local.dao.ReceiptSplitDao
import com.example.smartreceiptsplitter.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "smart_receipt.db")
    //TODO: replace with real Migration objects before the first release
    //destructive fallback is only safee while schema version stays at 1
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideReceiptDao(db: AppDatabase): ReceiptDao = db.receiptDao()
    @Provides
    fun provideReceiptItemDao(db: AppDatabase): ReceiptItemDao = db.receiptItemDao()

    @Provides
    fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()

    @Provides
    fun provideReceiptSplitDao(db: AppDatabase): ReceiptSplitDao = db.receiptSplitDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
}