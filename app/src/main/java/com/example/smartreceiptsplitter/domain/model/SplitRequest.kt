package com.example.smartreceiptsplitter.domain.model

sealed interface NetSplit {
    data object Even: NetSplit //Equal parts, leftover (cents) go to the first participants, one cent each
    data class Custom(val amounts: Map<Long, Long>): NetSplit //Exact net amount in cents per personId, must add up to - total - tax
}

sealed interface ShareAllocation {
    data object Proportional: ShareAllocation //in proportional to each person's net share (default)
    data object Even: ShareAllocation // equal parts regardless of net shares
    data class Manual(val amounts: Map<Long, Long>): ShareAllocation // exact amlunt in cents per personId (a missing person counts as ); must add up to the full tax / tip
}

/**
 * Input of the split calculation. All money values are Long in cents.
 *
 * @property totalAmount receipt total, tax included
 * @property taxAmount the part of [totalAmount] that is tax / VAT (0 if unknown)
 * @property tipAmount tip, paid on top of [totalAmount] (0 if none)
 */
data class SplitRequest(
    val totalAmount: Long,
    val taxAmount: Long = 0,
    val tipAmount: Long = 0,
    val participantIds: List<Long>,
    val groupId: Long? = null,
    val netSplit: NetSplit = NetSplit.Even,
    val taxAllocation: ShareAllocation = ShareAllocation.Proportional,
    val tipAllocation: ShareAllocation = ShareAllocation.Proportional
)