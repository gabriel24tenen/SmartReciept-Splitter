package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import javax.inject.Inject

class SaveReceiptSplitsUseCase @Inject constructor(
    private val repository: ReceiptSplitRepository
) {
    suspend operator fun invoke(receiptId: Long, splits: List<ReceiptSplit>) =
        repository.saveSplits(receiptId, splits)
}