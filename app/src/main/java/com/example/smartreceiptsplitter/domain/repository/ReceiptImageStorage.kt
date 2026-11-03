package com.example.smartreceiptsplitter.domain.repository

interface ReceiptImageStorage {
    suspend fun save(jpeg: ByteArray): String // saves a corrected receipt image (JPEG bytes) and returns its path
    suspend fun delete(path: String): Boolean // deletes a receipt image; false if the file doesn't exist or is not a receipt image of this app
}