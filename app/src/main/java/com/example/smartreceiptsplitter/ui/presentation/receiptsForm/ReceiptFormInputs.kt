package com.example.smartreceiptsplitter.ui.presentation.receiptsForm

import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.ReceiptDraft
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.usecase.receipt.ValidateReceiptUseCase
import com.example.smartreceiptsplitter.ui.util.parseDate
import com.example.smartreceiptsplitter.ui.util.parseMoney
import java.time.Clock

data class ReceiptFormInputs(
    val merchantName: String = "",
    val date: String = "",
    val total: String = "",
    val tax: String = "",
    val tip: String = "",
    val currency: String = DEFAULT_CURRENCY
) {
    companion object {
        const val DEFAULT_CURRENCY = "EUR"
    }
}

fun ReceiptFormInputs.validate(
    validator: ValidateReceiptUseCase,
    clock: Clock = Clock.systemDefaultZone()
): ReceiptValidation {
    val formatErrors = linkedMapOf<ReceiptField, ReceiptFieldError>()

    fun amount(field: ReceiptField, text: String): Long? {
        if(text.isBlank()) return null
        return parseMoney(text) ?: run {
            formatErrors[field] = ReceiptFieldError.INVALID_FORMAT
            null
        }
    }

    val dateMillis = if(date.isBlank()) null else parseDate(date, clock.zone)
    if(date.isNotBlank() && dateMillis == null) formatErrors[ReceiptField.DATE] = ReceiptFieldError.INVALID_FORMAT

    val draft = ReceiptDraft(
        merchantName = merchantName,
        date = dateMillis,
        totalAmount = amount(ReceiptField.TOTAL, total),
        taxAmount = amount(ReceiptField.TAX, tax),
        tipAmount = amount(ReceiptField.TIP, tip),
        currency = currency
    )

    val result = validator(draft, clock)
    if (formatErrors.isEmpty()) return result

    val ruleErrors = (result as? ReceiptValidation.Invalid)?.errors.orEmpty()
    return ReceiptValidation.Invalid(ruleErrors + formatErrors)
}