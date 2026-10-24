package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateReceiptUseCase @Inject constructor(
    private val receipts: ReceiptRepository,
    private val splits: ReceiptSplitRepository
) {
    /*
    * @return true if the receipt has a saved split that no longer matches the new total, tax or tip so the caller can send the user to the split screen to review it
    *
    * */
    suspend operator fun invoke(existing: Receipt, edited: ValidatedReceipt): Boolean {
        val amountsChanged = existing.totalAmount != edited.totalAmount || existing.taxAmount != edited.taxAmount || existing.tipAmount != edited.tipAmount
        val conversionStale = existing.currency != edited.currency || existing.totalAmount != edited.totalAmount

        receipts.updateReceipt(
            existing.copy(
                merchantName = edited.merchantName,
                date = edited.date,
                totalAmount = edited.totalAmount,
                taxAmount = edited.taxAmount,
                tipAmount = edited.tipAmount,
                currency = edited.currency,
                homeCurrencyAmount = if (conversionStale) null else existing.homeCurrencyAmount,
                exchangeRateUsed = if (conversionStale) null else existing.exchangeRateUsed
            )
        )
        return amountsChanged && splits.observeSplits(existing.id).first().isNotEmpty()
    }
}