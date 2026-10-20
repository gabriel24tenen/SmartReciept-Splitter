package com.example.smartreceiptsplitter.domain.model

import com.example.smartreceiptsplitter.data.local.enums.SplitPart

sealed interface SplitError {
    data object NoParticipants: SplitError
    data object DuplicateParticipants: SplitError
    data object NegativeAmount: SplitError
    data object TaxExceedsTotal: SplitError

    //   Manually entered amounts of part do not add up
    //   Difference = Expected - Actual
    data class SumMismatch(val part: SplitPart, val expected: Long, val actual: Long): SplitError
}

sealed interface SplitCalculation {
    // One receipt split per participant, in the order of participants Ids from Split Request
    data class Success(val splits: List<ReceiptSplit>): SplitCalculation
    data class Invalid(val error: SplitError): SplitCalculation
}