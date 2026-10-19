package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.NetSplit
import com.example.smartreceiptsplitter.domain.model.ShareAllocation
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitError
import com.example.smartreceiptsplitter.data.local.enums.SplitPart
import com.example.smartreceiptsplitter.domain.model.SplitRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculateSplitUseCaseTest {
    private val calculate = CalculateSplitUseCase()

    private fun success(request: SplitRequest) : List<ReceiptSplit> =
        (calculate(request) as SplitCalculation.Success).splits

    private fun error(request: SplitRequest): SplitError =
        (calculate(request) as SplitCalculation.Invalid).error

    @Test
    fun evenSplitGivesLeftoverCentsToFirstParticipants() {
        val splits = success(SplitRequest(totalAmount = 1000, participantIds = listOf(1, 2, 3)))
        assertEquals(listOf(334L, 333L, 333L), splits.map { it.amountOwed })
    }

    @Test
    fun evenSplitWithTaxSplitsTaxEvenlyAndKeepsItInsideAmountOwed() {
        // 12.50 total, 2.50 of it is VAT, two people
        val splits = success(SplitRequest(totalAmount = 1250, taxAmount = 250, participantIds = listOf(1, 2)))
        assertEquals(listOf(625L, 625L), splits.map { it.amountOwed })
        assertEquals(listOf(125L, 125L), splits.map { it.taxShare })
        assertNull(splits[0].tipShare)
    }

    @Test
    fun singleParticipantOwesEverything() {
        val splits = success(SplitRequest(totalAmount = 999, taxAmount = 99, tipAmount = 100, participantIds = listOf(7)))
        assertEquals(1099L, splits.single().amountOwed)
    }

    @Test
    fun groupIdIsPassedToEverySplit() {
        val splits = success(SplitRequest(totalAmount = 100, participantIds = listOf(1, 2), groupId = 42))
        assertEquals(listOf<Long?>(42L, 42L), splits.map { it.groupId })
    }

    @Test
    fun customNetSplitUsesTheGivenAmounts() {
        val splits = success(
            SplitRequest(
                totalAmount = 2000,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 1500L, 2L to 500L))
            )
        )
        assertEquals(listOf(1500L, 500L), splits.map { it.amountOwed })
    }

    @Test
    fun customNetSplitMustAddUpToTotalMinusTax() {
        val result = error(
            SplitRequest(
                totalAmount = 2000,
                taxAmount = 400,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 1000L, 2L to 500L))
            )
        )
        assertEquals(SplitError.SumMismatch(SplitPart.NET, expected = 1600, actual = 1500), result)
    }

    @Test
    fun customNetSplitMissingPersonCountsAsZero() {
        val splits = success(
            SplitRequest(
                totalAmount = 1000,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 1000L))
            )
        )
        assertEquals(listOf(1000L, 0L), splits.map { it.amountOwed })
    }

    @Test
    fun negativeCustomAmountIsRejected() {
        val result = error(
            SplitRequest(
                totalAmount = 1000,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 1500L, 2L to -500L))
            )
        )
        assertEquals(SplitError.NegativeAmount, result)
    }

    @Test
    fun taxIsProportionalToNetShareByDefault() {
        // net 1000 split 700/300, tax 200 -> 140/60
        val splits = success(
            SplitRequest(
                totalAmount = 1200,
                taxAmount = 200,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 700L, 2L to 300L))
            )
        )
        assertEquals(listOf(140L, 60L), splits.map { it.taxShare })
        assertEquals(listOf(840L, 360L), splits.map { it.amountOwed })
    }

    @Test
    fun taxCanBeAssignedManuallyToOnePerson() {
        val splits = success(
            SplitRequest(
                totalAmount = 1200,
                taxAmount = 200,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 700L, 2L to 300L)),
                taxAllocation = ShareAllocation.Manual(mapOf(1L to 200L))
            )
        )
        assertEquals(listOf(200L, 0L), splits.map { it.taxShare })
        assertEquals(listOf(900L, 300L), splits.map { it.amountOwed })
    }

    @Test
    fun manualTaxMustAddUpToTaxAmount() {
        val result = error(
            SplitRequest(
                totalAmount = 1200,
                taxAmount = 200,
                participantIds = listOf(1, 2),
                taxAllocation = ShareAllocation.Manual(mapOf(1L to 50L, 2L to 100L))
            )
        )
        assertEquals(SplitError.SumMismatch(SplitPart.TAX, expected = 200, actual = 150), result)
    }

    @Test
    fun evenTaxAllocationIgnoresNetShares() {
        val splits = success(
            SplitRequest(
                totalAmount = 1200,
                taxAmount = 200,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 900L, 2L to 100L)),
                taxAllocation = ShareAllocation.Even
            )
        )
        assertEquals(listOf(100L, 100L), splits.map { it.taxShare })
    }

    @Test
    fun taxCannotExceedTotal() {
        assertEquals(
            SplitError.TaxExceedsTotal,
            error(SplitRequest(totalAmount = 100, taxAmount = 101, participantIds = listOf(1)))
        )
    }

    @Test
    fun tipIsProportionalToNetShareByDefaultAndAddedOnTopOfTotal() {
        val splits = success(
            SplitRequest(
                totalAmount = 1000,
                tipAmount = 300,
                participantIds = listOf(1, 2),
                netSplit = NetSplit.Custom(mapOf(1L to 700L, 2L to 300L))
            )
        )
        assertEquals(listOf(210L, 90L), splits.map { it.tipShare })
        assertEquals(listOf(910L, 390L), splits.map { it.amountOwed })
    }

    @Test
    fun tipCanBeAssignedManually() {
        val splits = success(
            SplitRequest(
                totalAmount = 1000,
                tipAmount = 300,
                participantIds = listOf(1, 2),
                tipAllocation = ShareAllocation.Manual(mapOf(2L to 300L))
            )
        )
        assertEquals(listOf(0L, 300L), splits.map { it.tipShare })
        assertEquals(listOf(500L, 800L), splits.map { it.amountOwed })
    }

    @Test
    fun manualTipMustAddUpToTipAmount() {
        val result = error(
            SplitRequest(
                totalAmount = 1000,
                tipAmount = 300,
                participantIds = listOf(1, 2),
                tipAllocation = ShareAllocation.Manual(mapOf(1L to 100L, 2L to 300L))
            )
        )
        assertEquals(SplitError.SumMismatch(SplitPart.TIP, expected = 300, actual = 400), result)
    }

    @Test
    fun proportionalFallsBackToEvenWhenAllNetSharesAreZero() {
        // everyone has a 0 net share, tip still has to be divided
        val splits = success(
            SplitRequest(
                totalAmount = 0,
                tipAmount = 100,
                participantIds = listOf(1, 2)
            )
        )
        assertEquals(listOf(50L, 50L), splits.map { it.tipShare })
    }

    // ---- invariants and validation

    @Test
    fun amountsAlwaysAddUpToTotalPlusTip() {
        for (people in 1..7) {
            for (total in listOf(1L, 99L, 1000L, 1237L, 99999L)) {
                for (tax in listOf(0L, 1L, total / 5)) {
                    for (tip in listOf(0L, 1L, 333L)) {
                        val ids = (1..people).map { it.toLong() }
                        val splits = success(
                            SplitRequest(
                                totalAmount = total,
                                taxAmount = tax,
                                tipAmount = tip,
                                participantIds = ids
                            )
                        )
                        assertEquals(total + tip, splits.sumOf { it.amountOwed })
                        assertEquals(tax, splits.sumOf { it.taxShare ?: 0L })
                        assertEquals(tip, splits.sumOf { it.tipShare ?: 0L })
                    }
                }
            }
        }
    }

    @Test
    fun noParticipantsIsRejected() {
        assertEquals(SplitError.NoParticipants, error(SplitRequest(totalAmount = 100, participantIds = emptyList())))
    }

    @Test
    fun duplicateParticipantsAreRejected() {
        assertEquals(
            SplitError.DuplicateParticipants,
            error(SplitRequest(totalAmount = 100, participantIds = listOf(1, 1)))
        )
    }

    @Test
    fun negativeTotalIsRejected() {
        assertEquals(SplitError.NegativeAmount, error(SplitRequest(totalAmount = -1, participantIds = listOf(1))))
    }

    @Test
    fun newSplitsArePendingWithoutPaidAt() {
        val split = success(SplitRequest(totalAmount = 100, participantIds = listOf(1))).single()
        assertEquals(com.example.smartreceiptsplitter.data.local.enums.Statuses.PENDING, split.status)
        assertNull(split.paidAt)
    }

}