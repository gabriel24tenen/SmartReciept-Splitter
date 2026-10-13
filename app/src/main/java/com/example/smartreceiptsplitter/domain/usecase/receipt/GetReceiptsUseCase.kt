package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    operator fun invoke(): Flow<List<Receipt>> = repository.getAllReceipts()
}