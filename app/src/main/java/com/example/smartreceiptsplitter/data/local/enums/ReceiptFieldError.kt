package com.example.smartreceiptsplitter.data.local.enums

enum class ReceiptFieldError {
    REQUIRED,           // empty or missing
    INVALID_FORMAT,     // text is not a valid amount, date or currency code
    NOT_POSITIVE,       // total must be greater than zero
    NEGATIVE,           // tax or tip below zero
    TOO_LARGE,          // above ReceiptLimits.MAX_AMOUNT
    TOO_LONG,           // text above ReceiptLimits.MAX_MERCHANT_LENGTH
    TAX_EXCEEDS_TOTAL,
    DATE_IN_FUTURE
}