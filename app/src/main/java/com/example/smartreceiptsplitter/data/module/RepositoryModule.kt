package com.example.smartreceiptsplitter.data.module

import com.example.smartreceiptsplitter.data.ocr.MLKitTextRecognitionRepository
import com.example.smartreceiptsplitter.data.repository.PersonRepositoryImpl
import com.example.smartreceiptsplitter.data.repository.ReceiptRepositoryImpl
import com.example.smartreceiptsplitter.data.repository.ReceiptSplitRepositoryImpl
import com.example.smartreceiptsplitter.domain.repository.TextRecognitionRepository
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
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

    @Binds
    @Singleton
    abstract fun bindReceiptSplitRepository(
        impl: ReceiptSplitRepositoryImpl
    ) : ReceiptSplitRepository

    @Binds
    @Singleton
    abstract fun bindPersonRepository(
        impl: PersonRepositoryImpl
    ): PersonRepository

    @Binds
    @Singleton
    abstract fun bindTextRecognitionRepository(
        impl: MLKitTextRecognitionRepository
    ): TextRecognitionRepository

    //TODO: other repositories such as CategoryRepository also should be bind here
}