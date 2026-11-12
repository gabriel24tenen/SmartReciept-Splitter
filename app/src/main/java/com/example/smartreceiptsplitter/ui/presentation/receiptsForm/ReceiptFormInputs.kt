package com.example.smartreceiptsplitter.ui.presentation.receiptsForm

import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.ReceiptDraft
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.usecase.receipt.ValidateReceiptUseCase
import com.example.smartreceiptsplitter.ui.util.formatDateInput
import com.example.smartreceiptsplitter.ui.util.formatMoneyInput
import com.example.smartreceiptsplitter.ui.util.parseDate
import com.example.smartreceiptsplitter.ui.util.parseMoney
import java.time.Clock
import java.time.ZoneId

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

// Text for the form from typed data, e.g. an OCR result to correct. A field that was not found stays blank
// (the user has to fill it in), only the currency falls back to the default.
fun ReceiptDraft.toFormInputs(zone: ZoneId = ZoneId.systemDefault()) = ReceiptFormInputs(
    merchantName = merchantName,
    date = date?.let { formatDateInput(it, zone) }.orEmpty(),
    total = totalAmount?.let(::formatMoneyInput).orEmpty(),
    tax = taxAmount?.let(::formatMoneyInput).orEmpty(),
    tip = tipAmount?.let(::formatMoneyInput).orEmpty(),
    currency = currency.ifBlank { ReceiptFormInputs.DEFAULT_CURRENCY }
)

fun ReceiptFormInputs.hasChangesFrom(original: ReceiptFormInputs): Boolean = trimmed() != original.trimmed()

private fun ReceiptFormInputs.trimmed() = ReceiptFormInputs(
    merchantName = merchantName.trim(),
    date = date.trim(),
    total = total.trim(),
    tax = tax.trim(),
    tip = tip.trim(),
    currency = currency.trim()
)

fun ReceiptFormInputs.toSavedState(): Map<String, String> = mapOf(
    SAVED_MERCHANT to merchantName,
    SAVED_DATE to date,
    SAVED_TOTAL to total,
    SAVED_TAX to tax,
    SAVED_TIP to tip,
    SAVED_CURRENCY to currency
)

fun receiptFormInputsFromSavedState(read: (key: String) -> String?): ReceiptFormInputs? {
    return ReceiptFormInputs(
        merchantName = read(SAVED_MERCHANT) ?: return null,
        date = read(SAVED_DATE) ?: return null,
        total = read(SAVED_TOTAL) ?: return null,
        tax = read(SAVED_TAX) ?: return null,
        tip = read(SAVED_TIP) ?: return null,
        currency = read(SAVED_CURRENCY) ?: return null
    )
}

private const val SAVED_MERCHANT = "form_input_merchant"
private const val SAVED_DATE = "form_input_date"
private const val SAVED_TOTAL = "form_input_total"
private const val SAVED_TAX = "form_input_tax"
private const val SAVED_TIP = "form_input_tip"
private const val SAVED_CURRENCY = "form_input_currency"