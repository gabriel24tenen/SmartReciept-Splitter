package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptSplitsUseCase @Inject constructor(
    private val repository: ReceiptSplitRepository
) {
    operator fun invoke(receiptId: Long): Flow<List<ReceiptSplit>> = repository.observeSplits(receiptId)
}