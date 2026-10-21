package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import javax.inject.Inject

class SaveReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke (
        receipt: ValidatedReceipt,
        isManualEntry: Boolean,
        imagePath: String? = null
    ): Long = repository.addReceipt(
        Receipt(
            merchantName = receipt.merchantName,
            date = receipt.date,
            totalAmount = receipt.totalAmount,
            taxAmount = receipt.taxAmount,
            tipAmount = receipt.tipAmount,
            currency = receipt.currency,
            homeCurrencyAmount = null, // coming in F03
            exchangeRateUsed = null, // coming in F03
            categoryId = null, // coming in F09
            imagePath = imagePath,
            isManualEntry = isManualEntry,
            paidByPersonId = null // coming in F17
        )
    )
}