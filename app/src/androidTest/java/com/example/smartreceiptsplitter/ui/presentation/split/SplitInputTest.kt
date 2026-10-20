package com.example.smartreceiptsplitter.ui.presentation.split

import com.example.smartreceiptsplitter.data.local.enums.NetMode
import com.example.smartreceiptsplitter.data.local.enums.ShareMode
import com.example.smartreceiptsplitter.domain.model.NetSplit
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.ShareAllocation
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitRequest
import com.example.smartreceiptsplitter.domain.usecase.split.CalculateSplitUseCase
import com.example.smartreceiptsplitter.ui.util.formatMoneyInput
import com.example.smartreceiptsplitter.ui.util.parseMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SplitInputsTest {

    private val calculateUseCase = CalculateSplitUseCase()
    private val calculate: (SplitRequest) -> SplitCalculation = { calculateUseCase(it) }

    private fun receipt(total: Long = 1200, tax: Long? = 200) = Receipt(
        id = 1, merchantName = "Cafe", date = 0, totalAmount = total, taxAmount = tax, tipAmount = null,
        currency = "EUR", homeCurrencyAmount = null, exchangeRateUsed = null, categoryId = null,
        imagePath = null, isManualEntry = true, paidByPersonId = null
    )

    private fun splits(receipt: Receipt, inputs: SplitInputs, ids: List<Long>): List<ReceiptSplit> =
        (calculate(inputs.toRequest(receipt, ids)!!) as SplitCalculation.Success).splits

    // ---- parseMoney / formatMoneyInput

    @Test
    fun parseMoneyAcceptsDotAndComma() {
        assertEquals(1250L, parseMoney("12.50"))
        assertEquals(1250L, parseMoney("12,5"))
        assertEquals(1200L, parseMoney("12"))
        assertEquals(5L, parseMoney("0.05"))
        assertEquals(300L, parseMoney(" 3 "))
    }

    @Test
    fun parseMoneyTreatsBlankAsZero() {
        assertEquals(0L, parseMoney(""))
        assertEquals(0L, parseMoney("   "))
    }

    @Test
    fun parseMoneyRejectsInvalidText() {
        assertNull(parseMoney("abc"))
        assertNull(parseMoney("1.234"))
        assertNull(parseMoney("-1"))
        assertNull(parseMoney("12.5.0"))
    }

    @Test
    fun formatMoneyInputUsesDotAndTwoDecimals() {
        assertEquals("12.50", formatMoneyInput(1250))
        assertEquals("0.05", formatMoneyInput(5))
        assertEquals("0.00", formatMoneyInput(0))
    }

    // ---- toRequest

    @Test
    fun defaultInputsGiveEvenSplitWithProportionalShares() {
        val request = SplitInputs().toRequest(receipt(), listOf(1, 2))!!
        assertEquals(NetSplit.Even, request.netSplit)
        assertEquals(ShareAllocation.Proportional, request.taxAllocation)
        assertEquals(ShareAllocation.Proportional, request.tipAllocation)
        assertEquals(200L, request.taxAmount)
        assertEquals(0L, request.tipAmount)
    }

    @Test
    fun customAndManualInputsAreParsedIntoCents() {
        val inputs = SplitInputs(
            netMode = NetMode.CUSTOM,
            taxMode = ShareMode.MANUAL,
            tip = "3,00",
            netAmounts = mapOf(1L to "7.00", 2L to "3.00"),
            taxAmounts = mapOf(1L to "2.00")
        )
        val request = inputs.toRequest(receipt(), listOf(1, 2))!!
        assertEquals(NetSplit.Custom(mapOf(1L to 700L, 2L to 300L)), request.netSplit)
        // a person without text counts as 0
        assertEquals(ShareAllocation.Manual(mapOf(1L to 200L, 2L to 0L)), request.taxAllocation)
        assertEquals(300L, request.tipAmount)
    }

    @Test
    fun invalidTextGivesNoRequest() {
        assertNull(SplitInputs(tip = "abc").toRequest(receipt(), listOf(1, 2)))
        assertNull(SplitInputs(netMode = NetMode.CUSTOM, netAmounts = mapOf(1L to "1.234")).toRequest(receipt(), listOf(1, 2)))
        assertNull(SplitInputs(tipMode = ShareMode.MANUAL, tipAmounts = mapOf(2L to "x")).toRequest(receipt(), listOf(1, 2)))
    }

    @Test
    fun textOfPeopleWhoAreNotParticipantsIsIgnored() {
        val inputs = SplitInputs(netMode = NetMode.CUSTOM, netAmounts = mapOf(1L to "12.00", 9L to "garbage"))
        val request = inputs.toRequest(receipt(), listOf(1))!!
        assertEquals(NetSplit.Custom(mapOf(1L to 1200L)), request.netSplit)
    }

    // ---- inferInputs (reopening a saved split)

    @Test
    fun savedDefaultSplitComesBackAsDefaultSettings() {
        val receipt = receipt()
        val saved = splits(receipt, SplitInputs(), listOf(1, 2))
        assertEquals(SplitInputs(tip = "0.00"), inferInputs(receipt, saved, listOf(1, 2), calculate))
    }

    @Test
    fun savedCustomAmountsComeBackAsCustomWithTheSameAmounts() {
        val receipt = receipt()
        val inputs = SplitInputs(netMode = NetMode.CUSTOM, netAmounts = mapOf(1L to "7.00", 2L to "3.00"))
        val saved = splits(receipt, inputs, listOf(1, 2))

        val restored = inferInputs(receipt, saved, listOf(1, 2), calculate)

        assertEquals(NetMode.CUSTOM, restored.netMode)
        assertEquals(ShareMode.PROPORTIONAL, restored.taxMode)
        assertEquals(mapOf(1L to "7.00", 2L to "3.00"), restored.netAmounts)
        assertEquals(saved, splits(receipt, restored, listOf(1, 2)))
    }

    @Test
    fun savedManualTaxAndTipComeBackAsManualWithTheSameAmounts() {
        val receipt = receipt()
        val inputs = SplitInputs(
            taxMode = ShareMode.MANUAL,
            tipMode = ShareMode.MANUAL,
            tip = "4.00",
            taxAmounts = mapOf(1L to "2", 2L to "0"),
            tipAmounts = mapOf(1L to "1.00", 2L to "3.00")
        )
        val saved = splits(receipt, inputs, listOf(1, 2))

        val restored = inferInputs(receipt, saved, listOf(1, 2), calculate)

        assertEquals(NetMode.EVEN, restored.netMode)
        assertEquals(ShareMode.MANUAL, restored.taxMode)
        assertEquals(ShareMode.MANUAL, restored.tipMode)
        assertEquals("4.00", restored.tip)
        assertEquals(saved, splits(receipt, restored, listOf(1, 2)))
    }

    @Test
    fun savedEvenTaxAndTipComeBackAsEven() {
        val receipt = receipt()
        val inputs = SplitInputs(
            netMode = NetMode.CUSTOM,
            taxMode = ShareMode.EVEN,
            tipMode = ShareMode.EVEN,
            tip = "1.00",
            netAmounts = mapOf(1L to "9.00", 2L to "1.00")
        )
        val saved = splits(receipt, inputs, listOf(1, 2))

        val restored = inferInputs(receipt, saved, listOf(1, 2), calculate)

        assertEquals(ShareMode.EVEN, restored.taxMode)
        assertEquals(ShareMode.EVEN, restored.tipMode)
    }

    @Test
    fun splitThatNoLongerMatchesTheReceiptIsLoadedAsExactAmountsAndShowsTheDifference() {
        val saved = splits(receipt(total = 1200, tax = 200), SplitInputs(), listOf(1, 2))
        val changedReceipt = receipt(total = 1500, tax = 200) // receipt was edited after the split was saved

        val restored = inferInputs(changedReceipt, saved, listOf(1, 2), calculate)

        assertEquals(NetMode.CUSTOM, restored.netMode)
        // the saved amounts are kept, the calculation reports what is missing
        val result = calculate(restored.toRequest(changedReceipt, listOf(1, 2))!!)
        assertEquals(true, result is SplitCalculation.Invalid)
    }
}
