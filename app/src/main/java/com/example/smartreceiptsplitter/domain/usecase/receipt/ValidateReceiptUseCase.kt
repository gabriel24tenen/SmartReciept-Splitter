package com.example.smartreceiptsplitter.domain.usecase.receipt

import android.icu.util.Currency
import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.ReceiptDraft
import com.example.smartreceiptsplitter.domain.model.ReceiptLimits
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

// Pure validation rules for a receipt entered by hand or corrected after OCR (F07)
// merchant: required, at most MAX_MERCHANT_LENGTH characters
// date: required, not after today
// total: required, greater than 0, at most MAX_AMOUNT
// tax: optional, 0..MAX_AMOUNT and not above the total (same rule as the split calculation)
// tip: optional, 0..MAX_AMOUNT
// currency: required, an ISO 4217 code (EUR, USD, ...)
// Every field is checked, so the form can show all errors at once

class ValidateReceiptUseCase @Inject constructor(){
    operator fun invoke(draft: ReceiptDraft, clock: Clock = Clock.systemDefaultZone()): ReceiptValidation {
        val errors = linkedMapOf<ReceiptField, ReceiptFieldError>()
        val merchant = draft.merchantName.trim().takeIf { it.isNotEmpty() }
        if (merchant != null && merchant.length > ReceiptLimits.MAX_MERCHANT_LENGTH) {
            errors[ReceiptField.MERCHANT] = ReceiptFieldError.TOO_LONG
        }

        val date = draft.date
        if(date == null) {
            errors[ReceiptField.DATE] = ReceiptFieldError.REQUIRED
        } else if (localDate(date, clock).isAfter(LocalDate.now(clock))) {
            errors[ReceiptField.DATE] = ReceiptFieldError.DATE_IN_FUTURE
        }

        val total = draft.totalAmount
        when {
            total == null -> errors[ReceiptField.TOTAL] = ReceiptFieldError.REQUIRED
            total <= 0 -> errors[ReceiptField.TOTAL] = ReceiptFieldError.NOT_POSITIVE
            total > ReceiptLimits.MAX_AMOUNT -> errors[ReceiptField.TOTAL] = ReceiptFieldError.TOO_LARGE
        }

        val tax = draft.taxAmount
        when {
            tax == null -> Unit
            tax < 0 -> errors[ReceiptField.TAX] = ReceiptFieldError.NEGATIVE
            tax > ReceiptLimits.MAX_AMOUNT -> errors[ReceiptField.TAX] = ReceiptFieldError.TOO_LARGE
            // compared only with a usable total, otherwise the total's own error is the one to fix first
            total != null && ReceiptField.TOTAL !in errors && tax > total ->
                errors[ReceiptField.TAX] = ReceiptFieldError.TAX_EXCEEDS_TOTAL
        }

        val tip = draft.tipAmount
        when {
            tip == null -> Unit
            tip < 0 -> errors[ReceiptField.TIP] = ReceiptFieldError.NEGATIVE
            tip > ReceiptLimits.MAX_AMOUNT -> errors[ReceiptField.TIP] = ReceiptFieldError.TOO_LARGE
        }

        val currency = draft.currency.trim().uppercase()
        when {
            currency.isEmpty() -> errors[ReceiptField.CURRENCY] = ReceiptFieldError.REQUIRED
            !isIsoCurrency(currency) -> errors[ReceiptField.CURRENCY] = ReceiptFieldError.INVALID_FORMAT
        }

        if(errors.isNotEmpty() || date == null || total == null) return ReceiptValidation.Invalid(errors)

        return ReceiptValidation.Valid(
            ValidatedReceipt(
                merchantName = merchant ?: "(empty)",
                date = date,
                totalAmount = total,
                taxAmount = tax,
                tipAmount = tip,
                currency = currency
            )
        )
    }

    private fun localDate(timestamp: Long, clock: Clock): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(clock.zone).toLocalDate()

    private fun isIsoCurrency(code: String): Boolean =
        code.length == 3 && runCatching { Currency.getInstance(code) }.isSuccess
}