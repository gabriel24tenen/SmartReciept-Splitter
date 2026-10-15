package com.example.smartreceiptsplitter.domain.usecase.scan

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class SaveCorrectedReceiptImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke (bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(directory, "receipt_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)}
        file
    }

    private companion object {
        const val JPEG_QUALITY = 85
    }
}