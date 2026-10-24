package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptFilter
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    operator fun invoke(filter: ReceiptFilter = ReceiptFilter()): Flow<List<Receipt>> = repository.getReceipts(filter.dateFrom, filter.dateToExclusive, filter.currency)
        .map { receipts ->
            if(filter.merchantQuery.isBlank()) receipts
            else receipts.filter { matchesMerchant(it.merchantName, filter.merchantQuery) }
        }
}