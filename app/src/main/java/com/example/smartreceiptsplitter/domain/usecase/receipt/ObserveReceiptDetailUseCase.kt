package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import com.example.smartreceiptsplitter.domain.usecase.split.isOutdatedFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveReceiptDetailUseCase @Inject constructor(
    private val receipts: ReceiptRepository,
    private val splits: ReceiptSplitRepository,
    private val persons: PersonRepository
) {
    operator fun invoke(receiptId: Long): Flow<ReceiptDetail?> =
        combine(
            receipts.observeReceipt(receiptId),
            splits.observeSplits(receiptId),
            persons.getAllPerson()
        ) {
            receipt, saved, people ->
            receipt?.let { receiptDetailOf(it, saved, people)}
        }
}

internal fun receiptDetailOf(receipt: Receipt, splits: List<ReceiptSplit>, people: List<Person>): ReceiptDetail{
    val names = people.associate { it.id to it.name }
    val shares = splits
        .map { split -> ReceiptDetail.Share(personName = names[split.personId].orEmpty(), split = split) }
        .sortedBy { it.personName.lowercase() }
    return ReceiptDetail(receipt = receipt, shares = shares, isSplitOutdated = splits.isOutdatedFor(receipt))
}

data class ReceiptDetail(
    val receipt: Receipt,
    val shares: List<Share>, // one entry per person in the saved split, empty if the receipt isn't split
    val isSplitOutdated: Boolean  // if the receipt was edited after it was saved
) {
    data class Share(val personName: String, val split: ReceiptSplit)
}