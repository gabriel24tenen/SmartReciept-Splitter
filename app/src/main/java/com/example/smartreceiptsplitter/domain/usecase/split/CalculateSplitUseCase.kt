package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.data.local.enums.SplitPart
import com.example.smartreceiptsplitter.domain.model.NetSplit
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.ShareAllocation
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitError
import com.example.smartreceiptsplitter.domain.model.SplitRequest
import javax.inject.Inject

// Pure split logic
// amountOwed = net share + tax share + tip share
// net share = totalAmount - taxAmount
// tax share = taxAmount
// tip share = tipAmount
// amountOwed is always totalAmount + tipAmount
class CalculateSplitUseCase @Inject constructor() {

    operator fun invoke(request: SplitRequest): SplitCalculation = try {
        calculate(request)
    } catch (e: InvalidSplitException) {
        SplitCalculation.Invalid(e.error)
    }

    private fun calculate(request: SplitRequest): SplitCalculation.Success {
        val ids = request.participantIds
        if (ids.isEmpty()) throw InvalidSplitException(SplitError.NoParticipants)
        if (ids.distinct().size != ids.size) throw InvalidSplitException(SplitError.DuplicateParticipants)
        if (request.totalAmount < 0 || request.taxAmount < 0 || request.tipAmount < 0) {
            throw InvalidSplitException(SplitError.NegativeAmount)
        }
        if (request.taxAmount > request.totalAmount) throw InvalidSplitException(SplitError.TaxExceedsTotal)
        val netTotal = request.totalAmount - request.taxAmount

        val net = when (val split = request.netSplit) {
            NetSplit.Even -> distribute(netTotal, equalWeights(ids.size))
            is NetSplit.Custom -> manualAmounts(split.amounts, ids, netTotal, SplitPart.NET)
        }
        val tax = allocate(request.taxAllocation, request.taxAmount, ids, net, SplitPart.TAX)
        val tip = allocate(request.tipAllocation, request.tipAmount, ids, net, SplitPart.TIP)

        return SplitCalculation.Success(
            ids.indices.map { i ->
                ReceiptSplit(
                    personId = ids[i],
                    groupId = request.groupId,
                    amountOwed = net[i] + tax[i] + tip[i],
                    taxShare = tax[i].takeIf { request.taxAmount > 0 },
                    tipShare = tip[i].takeIf { request.tipAmount > 0 }
                )
            }
        )
    }

    private fun allocate( allocation: ShareAllocation, total: Long, ids: List<Long>, net: List<Long>, part: SplitPart ): List<Long> = when(allocation) {
        ShareAllocation.Proportional -> distribute(total, net)
        ShareAllocation.Even -> distribute(total, equalWeights(ids.size))
        is ShareAllocation.Manual -> manualAmounts(allocation.amounts, ids, total, part)
    }
    private fun manualAmounts( amounts: Map<Long, Long>, ids: List<Long>, expected: Long, part: SplitPart): List<Long> {
        val values = ids.map { amounts[it] ?: 0L }
        if (values.any { it < 0 }) throw InvalidSplitException(SplitError.NegativeAmount)
        val actual = values.sum()
        if (actual != expected) throw InvalidSplitException(SplitError.SumMismatch(part, expected, actual))
        return values
    }
    private fun equalWeights(count: Int): List<Long> = List(count) { 1L }
    // Divides total by weights using the largest remainder method, so the parts always add up to exactly total
    // Ties go to the earlier participant, if every weight is  the split is even
    private fun distribute(total: Long, weights: List<Long>): List<Long> {
        val weightSum = weights.sum()
        if(weightSum == 0L) return distribute(total, equalWeights(weights.size))

        val products = weights.map { Math.multiplyExact(total, it) }
        val parts = products.map { it/weightSum }.toMutableList()
        var leftover = total - parts.sum()

        val byReminder = products.indices.sortedWith( compareByDescending<Int> { products[it] % weightSum }.thenBy { it })
        for(i in byReminder) {
            if(leftover == 0L) break
            parts[i] += 1
            leftover--
        }
        return parts
    }
    private class InvalidSplitException(val error: SplitError): Exception()
}