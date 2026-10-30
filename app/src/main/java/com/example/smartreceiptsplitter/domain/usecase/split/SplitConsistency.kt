package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit

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