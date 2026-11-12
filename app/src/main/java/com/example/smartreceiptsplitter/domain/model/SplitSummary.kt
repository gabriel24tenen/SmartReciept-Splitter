package com.example.smartreceiptsplitter.domain.model

/*
* What the receipt list shows about a receipt's split
* participants - number of people in the saved split
* isOutdated the receipt was edited after the split was saved, so the split no longer adds up
* */
data class SplitSummary(val participants: Int, val isOutdated: Boolean)