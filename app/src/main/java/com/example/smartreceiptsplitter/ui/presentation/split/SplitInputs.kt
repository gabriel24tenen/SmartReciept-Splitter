package com.example.smartreceiptsplitter.ui.presentation.split

import com.example.smartreceiptsplitter.data.local.enums.NetMode
import com.example.smartreceiptsplitter.data.local.enums.ShareMode
import com.example.smartreceiptsplitter.domain.model.NetSplit
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.ShareAllocation
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitRequest
import com.example.smartreceiptsplitter.ui.util.formatMoneyInput
import com.example.smartreceiptsplitter.ui.util.parseMoney

data class SplitInputs (
    val netMode: NetMode = NetMode.EVEN,
    val taxMode: ShareMode = ShareMode.PROPORTIONAL,
    val tipMode: ShareMode = ShareMode.PROPORTIONAL,
    val tip: String = "",
    val netAmounts: Map<Long, String> = emptyMap(),
    val taxAmounts: Map<Long, String> = emptyMap(),
    val tipAmounts: Map<Long, String> = emptyMap()
)

fun SplitInputs.toRequest(receipt: Receipt, participantIds: List<Long>): SplitRequest? {
    val tipCents = parseMoney(tip) ?: return null
    val netSplit = when(netMode) {
        NetMode.EVEN -> NetSplit.Even
        NetMode.CUSTOM -> NetSplit.Custom(parseAll(netAmounts, participantIds) ?: return null)
    }
    val taxAllocation = allocation(taxMode, taxAmounts, participantIds) ?: return null
    val tipAllocation = allocation(tipMode, tipAmounts, participantIds) ?: return null
    return SplitRequest(
        totalAmount = receipt.totalAmount,
        taxAmount = receipt.taxAmount ?: 0,
        tipAmount = tipCents,
        participantIds = participantIds,
        netSplit = netSplit,
        taxAllocation = taxAllocation,
        tipAllocation = tipAllocation
    )
}

private fun allocation(mode: ShareMode, amounts: Map<Long, String>, ids: List<Long>): ShareAllocation? =
    when(mode) {
        ShareMode.PROPORTIONAL -> ShareAllocation.Proportional
        ShareMode.EVEN -> ShareAllocation.Even
        ShareMode.MANUAL -> parseAll(amounts, ids)?.let { ShareAllocation.Manual(it) }
    }

private fun parseAll(texts: Map<Long, String>, ids: List<Long>): Map<Long, Long>? {
    val result = mutableMapOf<Long, Long>()
    for(id in ids) {
        result[id] = parseMoney(texts[id].orEmpty()) ?: return null
    }
    return result
}

fun inferInputs(
    receipt: Receipt,
    saved: List<ReceiptSplit>,
    participantIds: List<Long>,
    calculate: (SplitRequest) -> SplitCalculation
): SplitInputs {
    val exact = SplitInputs(
        netMode = NetMode.CUSTOM,
        taxMode = ShareMode.MANUAL,
        tipMode = ShareMode.MANUAL,
        tip = formatMoneyInput(saved.sumOf { it.tipShare ?: 0L }),
        netAmounts = saved.associate { it.personId to formatMoneyInput(it.netAmount()) },
        taxAmounts = saved.associate { it.personId to formatMoneyInput(it.taxShare ?: 0L) },
        tipAmounts = saved.associate { it.personId to formatMoneyInput(it.tipShare ?: 0L) }
    )
    for (netMode in NetMode.entries) {
        for(taxMode in ShareMode.entries) {
            for(tipMode in ShareMode.entries) {
                val candidate = exact.copy(
                    netMode = netMode,
                    taxMode = taxMode,
                    tipMode = tipMode,
                    netAmounts = if (netMode == NetMode.CUSTOM) exact.netAmounts else emptyMap(),
                    taxAmounts = if (taxMode == ShareMode.MANUAL) exact.taxAmounts else emptyMap(),
                    tipAmounts = if (tipMode == ShareMode.MANUAL) exact.tipAmounts else emptyMap()
                )
                val request = candidate.toRequest(receipt, participantIds) ?: continue
                val result = calculate(request) as? SplitCalculation.Success ?: continue
                if (result.splits.sameAmountsAs(saved)) return candidate
            }
        }
    }
    return exact
}

fun ReceiptSplit.netAmount(): Long = amountOwed - (taxShare ?: 0L) - (tipShare ?: 0L)

private fun List<ReceiptSplit>.sameAmountsAs(other: List<ReceiptSplit>): Boolean {
    if (size != other.size) return false
    val otherByPerson = other.associateBy { it.personId }
    return all { split ->
        val match = otherByPerson[split.personId] ?: return false
        split.amountOwed == match.amountOwed &&
                (split.taxShare ?: 0L) == (match.taxShare ?: 0L) &&
                (split.tipShare ?: 0L) == (match.tipShare ?: 0L)
    }
}