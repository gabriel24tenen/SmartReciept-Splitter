package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.ReceiptExtraction
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import java.time.Clock
import javax.inject.Inject

class ExtractReceiptDataUseCase @Inject constructor(
    private val parseTotal: ParseTotalAmountUseCase,
    private val parseDate : ParseReceiptDateUseCase,
    private val parseItems: ParseLineItemsUseCase,
    private val parseTax: ParseTaxAmountUseCase
) {
    operator fun invoke(text: RecognizedText, clock: Clock = Clock.systemDefaultZone()): ReceiptExtraction {
        if(text.isEmpty) return ReceiptExtraction.EMPTY
        val total = parseTotal(text)
        return ReceiptExtraction(
            totalAmount = total,
            date = parseDate(text, clock),
            taxAmount = parseTax(text)?.takeIf { tax -> total == null || tax < total },
            items = parseItems(text)
        )
    }
}