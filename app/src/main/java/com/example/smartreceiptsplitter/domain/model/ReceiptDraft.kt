package com.example.smartreceiptsplitter.domain.model

/**
 * Receipt data as the user entered or corrected it, already converted from text.
 * Both entry paths (manual entry and correcting OCR results) produce a draft and go through the same validation.
 * All money values are Long in cents. A null in a required field means "missing or unreadable".
 *
 * @property date receipt date as a timestamp (start of that day in the user's time zone)
 * @property taxAmount null if the receipt has no tax or it is unknown
 * @property tipAmount null if there is no tip
 */
data class ReceiptDraft(
    val merchantName: String = "",
    val date: Long? = null,
    val totalAmount: Long? = null,
    val taxAmount: Long? = null,
    val tipAmount: Long? = null,
    val currency: String
)
