package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.NetSplit
import com.example.smartreceiptsplitter.domain.model.ReceiptDraft
import com.example.smartreceiptsplitter.domain.model.ReceiptLimits
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitRequest
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import com.example.smartreceiptsplitter.domain.usecase.split.CalculateSplitUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class ValidateReceiptUseCaseTest {
    private val validate = ValidateReceiptUseCase()

    // "today" is 20.10.2026 at noon in Zagreb
    private val zone = ZoneId.of("Europe/Zagreb")
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 20).atTime(12, 0).atZone(zone).toInstant(), zone)

    private fun startOfDay(year: Int, month: Int, day: Int) =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun draft(
        merchantName: String = "Konzum",
        date: Long? = startOfDay(2026, 10, 19),
        totalAmount: Long? = 1250,
        taxAmount: Long? = 250,
        tipAmount: Long? = null,
        currency: String = "EUR"
    ) = ReceiptDraft(merchantName, date, totalAmount, taxAmount, tipAmount, currency)

    private fun errors(draft: ReceiptDraft): Map<ReceiptField, ReceiptFieldError> =
        (validate(draft, clock) as ReceiptValidation.Invalid).errors

    private fun error(draft: ReceiptDraft, field: ReceiptField): ReceiptFieldError? = errors(draft)[field]

    private fun valid(draft: ReceiptDraft): ValidatedReceipt =
        (validate(draft, clock) as ReceiptValidation.Valid).receipt

    // ---- valid

    @Test
    fun validDraftIsAccepted() {
        assertEquals(
            ValidatedReceipt("Konzum", startOfDay(2026, 10, 19), 1250, 250, null, "EUR"),
            valid(draft())
        )
    }

    @Test
    fun validReceiptIsTrimmedAndCurrencyUpperCased() {
        val receipt = valid(draft(merchantName = "  Konzum d.o.o.  ", currency = " eur "))
        assertEquals("Konzum d.o.o.", receipt.merchantName)
        assertEquals("EUR", receipt.currency)
    }

    @Test
    fun taxAndTipAreOptional() {
        val receipt = valid(draft(taxAmount = null, tipAmount = null))
        assertEquals(null, receipt.taxAmount)
        assertEquals(null, receipt.tipAmount)
    }

    // ---- merchant

    @Test
    fun blankMerchantIsAcceptedAndBecomesNull() {
        assertEquals(ReceiptFieldError.REQUIRED, error(draft(merchantName = ""), ReceiptField.MERCHANT))
        assertEquals(ReceiptFieldError.REQUIRED, error(draft(merchantName = "   "), ReceiptField.MERCHANT))
    }

    @Test
    fun merchantLongerThanLimitIsRejected() {
        val limit = ReceiptLimits.MAX_MERCHANT_LENGTH
        valid(draft(merchantName = "a".repeat(limit)))
        assertEquals(ReceiptFieldError.TOO_LONG, error(draft(merchantName = "a".repeat(limit + 1)), ReceiptField.MERCHANT))
    }

    // ---- date

    @Test
    fun missingDateIsRequired() {
        assertEquals(ReceiptFieldError.REQUIRED, error(draft(date = null), ReceiptField.DATE))
    }

    @Test
    fun todayIsAcceptedWhateverTheTimeOfDay() {
        valid(draft(date = startOfDay(2026, 10, 20)))
        valid(draft(date = LocalDate.of(2026, 10, 20).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()))
    }

    @Test
    fun tomorrowIsInTheFuture() {
        assertEquals(ReceiptFieldError.DATE_IN_FUTURE, error(draft(date = startOfDay(2026, 10, 21)), ReceiptField.DATE))
    }

    @Test
    fun oldDatesAreAccepted() {
        valid(draft(date = startOfDay(2019, 1, 1)))
    }

    // ---- total

    @Test
    fun missingTotalIsRequired() {
        assertEquals(ReceiptFieldError.REQUIRED, error(draft(totalAmount = null), ReceiptField.TOTAL))
    }

    @Test
    fun zeroAndNegativeTotalAreNotPositive() {
        assertEquals(ReceiptFieldError.NOT_POSITIVE, error(draft(totalAmount = 0, taxAmount = null), ReceiptField.TOTAL))
        assertEquals(ReceiptFieldError.NOT_POSITIVE, error(draft(totalAmount = -1, taxAmount = null), ReceiptField.TOTAL))
    }

    @Test
    fun totalAboveMaximumIsTooLarge() {
        valid(draft(totalAmount = ReceiptLimits.MAX_AMOUNT, taxAmount = null))
        assertEquals(
            ReceiptFieldError.TOO_LARGE,
            error(draft(totalAmount = ReceiptLimits.MAX_AMOUNT + 1, taxAmount = null), ReceiptField.TOTAL)
        )
    }

    // ---- tax

    @Test
    fun zeroTaxIsAccepted() {
        assertEquals(0L, valid(draft(taxAmount = 0)).taxAmount)
    }

    @Test
    fun negativeTaxIsRejected() {
        assertEquals(ReceiptFieldError.NEGATIVE, error(draft(taxAmount = -1), ReceiptField.TAX))
    }

    @Test
    fun taxEqualToTotalIsAcceptedButAboveIsNot() {
        valid(draft(totalAmount = 1250, taxAmount = 1250))
        assertEquals(ReceiptFieldError.TAX_EXCEEDS_TOTAL, error(draft(totalAmount = 1250, taxAmount = 1251), ReceiptField.TAX))
    }

    @Test
    fun taxAboveMaximumIsTooLargeNotExceedingTotal() {
        val max = ReceiptLimits.MAX_AMOUNT
        assertEquals(ReceiptFieldError.TOO_LARGE, error(draft(totalAmount = max, taxAmount = max + 1), ReceiptField.TAX))
    }

    @Test
    fun taxIsNotComparedWithAnUnusableTotal() {
        assertEquals(mapOf(ReceiptField.TOTAL to ReceiptFieldError.REQUIRED), errors(draft(totalAmount = null, taxAmount = 500)))
        assertEquals(mapOf(ReceiptField.TOTAL to ReceiptFieldError.NOT_POSITIVE), errors(draft(totalAmount = 0, taxAmount = 500)))
    }

    // ---- tip

    @Test
    fun negativeTipIsRejected() {
        assertEquals(ReceiptFieldError.NEGATIVE, error(draft(tipAmount = -1), ReceiptField.TIP))
    }

    @Test
    fun tipAboveMaximumIsTooLarge() {
        valid(draft(tipAmount = ReceiptLimits.MAX_AMOUNT))
        assertEquals(ReceiptFieldError.TOO_LARGE, error(draft(tipAmount = ReceiptLimits.MAX_AMOUNT + 1), ReceiptField.TIP))
    }

    @Test
    fun tipMayBeLargerThanTotal() {
        // tip is paid on top of the total, unlike tax
        assertEquals(5000L, valid(draft(totalAmount = 1000, taxAmount = null, tipAmount = 5000)).tipAmount)
    }

    // ---- currency

    @Test
    fun blankCurrencyIsRequired() {
        assertEquals(ReceiptFieldError.REQUIRED, error(draft(currency = ""), ReceiptField.CURRENCY))
        assertEquals(ReceiptFieldError.REQUIRED, error(draft(currency = "  "), ReceiptField.CURRENCY))
    }

    @Test
    fun currencyMustBeARealIsoCode() {
        for (code in listOf("EU", "EURO", "E1R", "ABC")) {
            assertEquals(code, ReceiptFieldError.INVALID_FORMAT, error(draft(currency = code), ReceiptField.CURRENCY))
        }
        for (code in listOf("EUR", "usd", "HRK", "GBP", "CHF")) {
            valid(draft(currency = code))
        }
    }

    // ---- everything together

    @Test
    fun everyInvalidFieldIsReportedAtOnce() {
        val result = errors(
            ReceiptDraft(
                merchantName = "a".repeat(ReceiptLimits.MAX_MERCHANT_LENGTH + 1),
                date = startOfDay(2026, 12, 1),
                totalAmount = 100,
                taxAmount = 200,
                tipAmount = -5,
                currency = "XX"
            )
        )
        assertEquals(
            mapOf(
                ReceiptField.MERCHANT to ReceiptFieldError.TOO_LONG,
                ReceiptField.DATE to ReceiptFieldError.DATE_IN_FUTURE,
                ReceiptField.TAX to ReceiptFieldError.TAX_EXCEEDS_TOTAL,
                ReceiptField.TIP to ReceiptFieldError.NEGATIVE,
                ReceiptField.CURRENCY to ReceiptFieldError.INVALID_FORMAT
            ),
            result
        )
    }

    @Test
    fun emptyDraftReportsOnlyTheRequiredFields() {
        assertEquals(
            mapOf(
                ReceiptField.DATE to ReceiptFieldError.REQUIRED,
                ReceiptField.TOTAL to ReceiptFieldError.REQUIRED,
                ReceiptField.CURRENCY to ReceiptFieldError.REQUIRED
            ),
            errors(ReceiptDraft(currency = "EUR"))
        )
    }

    // ---- the amount limit protects the split calculation

    @Test
    fun largestAcceptedAmountsCanStillBeSplit() {
        val max = ReceiptLimits.MAX_AMOUNT
        // custom net split gives one person the whole net amount, so tax / tip are multiplied by the largest possible weight
        val request = SplitRequest(
            totalAmount = max,
            taxAmount = 0,
            tipAmount = max,
            participantIds = listOf(1, 2, 3),
            netSplit = NetSplit.Custom(mapOf(1L to max))
        )
        assertTrue(CalculateSplitUseCase()(request) is SplitCalculation.Success)
    }
}