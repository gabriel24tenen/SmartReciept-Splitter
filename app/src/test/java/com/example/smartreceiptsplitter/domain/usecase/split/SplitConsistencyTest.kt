package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.SplitSummary

fun List<ReceiptSplit>.isOutdatedFor(receipt: Receipt): Boolean {
    if (isEmpty()) return false
    val tax = receipt.taxAmount ?: 0L
    val tip = receipt.tipAmount ?: 0L
    return sumOf { it.amountOwed } != receipt.totalAmount + tip ||
            sumOf { it.taxShare ?: 0L } != tax ||
            sumOf { it.tipShare ?: 0L } != tip
}

fun Receipt.withAmountsOf(splits: List<ReceiptSplit>): Receipt {
    val tip = splits.sumOf { it.tipShare ?: 0L }
    return copy(
        totalAmount = splits.sumOf { it.amountOwed } - tip,
        taxAmount = splits.sumOf { it.taxShare ?: 0L },
        tipAmount = tip
    )
}
/**
 * Split summary per receipt id, for the receipts that have a saved split. Receipts without a split are not in the map.
 * Splits of receipts that are not in [receipts] (e.g. filtered out of the list) are ignored.
 */
fun splitSummaries(receipts: List<Receipt>, splits: List<ReceiptSplit>): Map<Long, SplitSummary> {
    val byReceipt = splits.groupBy { it.receiptId }
    return receipts.mapNotNull { receipt ->
        val saved = byReceipt[receipt.id] ?: return@mapNotNull null
        receipt.id to SplitSummary(participants = saved.size, isOutdated = saved.isOutdatedFor(receipt))
    }.toMap()
}