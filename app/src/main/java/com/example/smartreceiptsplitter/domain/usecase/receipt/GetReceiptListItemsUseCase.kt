package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptFilter
import com.example.smartreceiptsplitter.domain.model.SplitSummary
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import com.example.smartreceiptsplitter.domain.usecase.split.splitSummaries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetReceiptListItemsUseCase @Inject constructor(
    private val getReceipts: GetReceiptsUseCase,
    private val splits: ReceiptSplitRepository
) {
    operator fun invoke(filter: ReceiptFilter = ReceiptFilter()): Flow<ReceiptListItems> =
        combine(getReceipts(filter), splits.observeAllSplits()) { receipts, allSplits ->
            ReceiptListItems(receipts, splitSummaries(receipts, allSplits))
        }
}

data class ReceiptListItems(val receipts: List<Receipt>, val splitSummaries: Map<Long, SplitSummary>)