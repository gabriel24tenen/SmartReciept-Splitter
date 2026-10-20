package com.example.smartreceiptsplitter.ui.presentation.receiptform
import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import com.example.smartreceiptsplitter.domain.usecase.receipt.ValidateReceiptUseCase
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.ReceiptFormInputs
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.validate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class ReceiptFormInputsTest {
    private val validator = ValidateReceiptUseCase()

    // "today" is 20.10.2026 at noon in Zagreb
    private val zone = ZoneId.of("Europe/Zagreb")
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 20).atTime(12, 0).atZone(zone).toInstant(), zone)

    private fun startOfDay(year: Int, month: Int, day: Int) =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun inputs(
        merchantName: String = "Konzum",
        date: String = "19.10.2026",
        total: String = "12,50",
        tax: String = "2,50",
        tip: String = "",
        currency: String = "EUR"
    ) = ReceiptFormInputs(merchantName, date, total, tax, tip, currency)

    private fun validate(inputs: ReceiptFormInputs) = inputs.validate(validator, clock)

    private fun errors(inputs: ReceiptFormInputs): Map<ReceiptField, ReceiptFieldError> =
        (validate(inputs) as ReceiptValidation.Invalid).errors

    // ---- valid

    @Test
    fun typedTextIsConvertedToCentsAndTimestamp() {
        val result = validate(inputs(tip = "1", currency = "eur"))
        assertEquals(
            ReceiptValidation.Valid(
                ValidatedReceipt(
                    "Konzum",
                    startOfDay(2026, 10, 19),
                    1250,
                    250,
                    100,
                    "EUR"
                )
            ),
            result
        )
    }

    @Test
    fun blankTaxAndTipMeanNone() {
        val receipt = (validate(inputs(tax = "", tip = "  ")) as ReceiptValidation.Valid).receipt
        assertEquals(null, receipt.taxAmount)
        assertEquals(null, receipt.tipAmount)
    }

    @Test
    fun amountsAcceptDotAndComma() {
        val receipt = (validate(inputs(total = "12.5", tax = "0,25")) as ReceiptValidation.Valid).receipt
        assertEquals(1250L, receipt.totalAmount)
        assertEquals(25L, receipt.taxAmount)
    }

    @Test
    fun defaultCurrencyIsEur() {
        assertEquals("EUR", ReceiptFormInputs().currency)
    }

    // ---- required

    @Test
    fun emptyFormReportsRequiredMerchantDateAndTotal() {
        assertEquals(
            mapOf(
                ReceiptField.MERCHANT to ReceiptFieldError.REQUIRED,
                ReceiptField.DATE to ReceiptFieldError.REQUIRED,
                ReceiptField.TOTAL to ReceiptFieldError.REQUIRED
            ),
            errors(ReceiptFormInputs())
        )
    }

    // ---- unreadable text

    @Test
    fun unreadableAmountIsInvalidFormatNotRequired() {
        assertEquals(ReceiptFieldError.INVALID_FORMAT, errors(inputs(total = "12,5x"))[ReceiptField.TOTAL])
    }

    @Test
    fun amountsWithMoreThanTwoDecimalsAreInvalid() {
        assertEquals(ReceiptFieldError.INVALID_FORMAT, errors(inputs(total = "1.234"))[ReceiptField.TOTAL])
    }

    @Test
    fun negativeAmountTextIsInvalid() {
        assertEquals(ReceiptFieldError.INVALID_FORMAT, errors(inputs(tip = "-1"))[ReceiptField.TIP])
    }

    @Test
    fun unreadableTaxOrTipBlocksSavingEvenThoughItWouldOtherwiseBeNone() {
        assertEquals(mapOf(ReceiptField.TAX to ReceiptFieldError.INVALID_FORMAT), errors(inputs(tax = "abc")))
        assertEquals(mapOf(ReceiptField.TIP to ReceiptFieldError.INVALID_FORMAT), errors(inputs(tip = "1,2,3")))
    }

    @Test
    fun invalidDatesAreRejected() {
        for (text in listOf("31.02.2026", "20.13.2026", "20.10.26", "abc", "2026.10.20")) {
            assertEquals(text, ReceiptFieldError.INVALID_FORMAT, errors(inputs(date = text))[ReceiptField.DATE])
        }
    }

    @Test
    fun futureDateIsReportedForAReadableDate() {
        assertEquals(ReceiptFieldError.DATE_IN_FUTURE, errors(inputs(date = "21.10.2026"))[ReceiptField.DATE])
    }

    // ---- rules between fields

    @Test
    fun taxAboveTotalIsReported() {
        assertEquals(ReceiptFieldError.TAX_EXCEEDS_TOTAL, errors(inputs(total = "10", tax = "10,01"))[ReceiptField.TAX])
    }

    @Test
    fun unreadableTotalDoesNotCauseAFalseTaxError() {
        assertEquals(mapOf(ReceiptField.TOTAL to ReceiptFieldError.INVALID_FORMAT), errors(inputs(total = "abc", tax = "5")))
    }

    @Test
    fun unreadableAndInvalidFieldsAreReportedTogether() {
        assertEquals(
            mapOf(
                ReceiptField.MERCHANT to ReceiptFieldError.REQUIRED,
                ReceiptField.DATE to ReceiptFieldError.DATE_IN_FUTURE,
                ReceiptField.TOTAL to ReceiptFieldError.INVALID_FORMAT,
                ReceiptField.CURRENCY to ReceiptFieldError.INVALID_FORMAT
            ),
            errors(inputs(merchantName = " ", date = "21.10.2026", total = "abc", tax = "", currency = "XX"))
        )
    }
}