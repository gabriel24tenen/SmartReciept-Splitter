package com.example.smartreceiptsplitter.domain.model

import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError

data class ValidatedReceipt(
    val merchantName: String,
    val date: Long,
    val totalAmount: Long,
    val taxAmount: Long?,
    val tipAmount: Long?,
    val currency: String
)

sealed interface ReceiptValidation {
    data class Valid(val receipt: ValidatedReceipt) : ReceiptValidation
    data class Invalid(val errors: Map<ReceiptField, ReceiptFieldError>) : ReceiptValidation
}

object ReceiptLimits {
    const val MAX_AMOUNT = 999_999_999L
    const val MAX_MERCHANT_LENGTH = 100
}
