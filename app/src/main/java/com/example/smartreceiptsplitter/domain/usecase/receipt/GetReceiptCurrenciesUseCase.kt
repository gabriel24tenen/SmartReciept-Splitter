package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptCurrenciesUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.getCurrencies()
}