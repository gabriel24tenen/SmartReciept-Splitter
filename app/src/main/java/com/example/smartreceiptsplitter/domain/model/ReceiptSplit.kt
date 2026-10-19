package com.example.smartreceiptsplitter.domain.model

import com.example.smartreceiptsplitter.data.local.enums.Statuses

// One share of receipt. All money values are Long in the smallest currency unit in cents
// amountOwed contains the tax and tip share, so taxShare&tipShare are informational
// receipted stays 0 until the split is saved, repository will assign the real id
data class ReceiptSplit (
    val id: Long = 0,
    val receiptId: Long = 0,
    val personId: Long,
    val groupId: Long? = null,
    val amountOwed: Long,
    val taxShare: Long? = null,
    val tipShare: Long? = null,
    val status: Statuses = Statuses.PENDING,
    val paidAt: Long? = null
)