package com.example.smartreceiptsplitter.data.module

import com.example.smartreceiptsplitter.data.repository.ReceiptRepositoryImpl
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(
        impl: ReceiptRepositoryImpl
    ): ReceiptRepository

    //TODO: other repositories such as PersonRepository, ReceiptSplitRepository, CategoryRepository also should be bind here
}