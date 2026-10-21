package com.example.smartreceiptsplitter.ui.navigation

import android.net.Uri
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.ReceiptFormInputs

/*
Route constants for the app's NavHost. Kept as a flat object rather than a sealed
class with arguments until a route actually needs one — grows as more screens land
(receipts list/history for F06, OCR review for F02, split screen for F04, ...).
 */
object Routes {
    const val SCAN = "scan"

    //Receipt Form
    const val ARG_IMAGE_PATH = "imagePath"
    const val ARG_MERCHANT = "merchant"
    const val ARG_DATE = "date"
    const val ARG_TOTAL = "total"
    const val ARG_TAX = "tax"
    const val ARG_TIP = "tip"
    const val ARG_CURRENCY = "currency"
    val RECEIPT_FORM_ARGS = listOf(ARG_IMAGE_PATH, ARG_MERCHANT, ARG_DATE, ARG_TOTAL, ARG_TAX, ARG_TIP, ARG_CURRENCY)
    const val RECEIPT_FORM = "receipt-form$ARG_IMAGE_PATH={$ARG_IMAGE_PATH}&$ARG_MERCHANT={$ARG_MERCHANT&$ARG_DATE={$ARG_DATE}" +
            "&$ARG_TOTAL={$ARG_TOTAL}&$ARG_TAX={$ARG_TAX}&$$ARG_TIP={$ARG_TIP}&$ARG_CURRENCY={$ARG_CURRENCY}"



    const val ARG_RECEIPT_ID = "receiptId"
    const val SPLIT = "split/{$ARG_RECEIPT_ID}"
    // Route of the form for a receipt typed in from scratch, no camere needed
    fun receiptForm() = "receipt-form"
    fun receiptFormForScan(imagePath: String, inputs: ReceiptFormInputs = ReceiptFormInputs()): String {
        val args = listOf(
            ARG_IMAGE_PATH to imagePath,
            ARG_MERCHANT to inputs.merchantName,
            ARG_DATE to inputs.date,
            ARG_TOTAL to inputs.total,
            ARG_TAX to inputs.tax,
            ARG_TIP to inputs.tip,
            ARG_CURRENCY to inputs.currency
        ).filter { (_, value) -> value.isNotBlank() }
        return "receipt.form?" + args.joinToString("&") { (name, value) -> "$name=${Uri.encode(value)}"}
    }
    //Route of the split screen for one saved receipt
    fun split(receiptId: Long) = "split/$receiptId"
}