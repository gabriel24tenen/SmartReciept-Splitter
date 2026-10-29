package com.example.smartreceiptsplitter.domain.usecase.scan

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DeleteReceiptImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(file: File): Boolean = withContext(Dispatchers.IO) {
        val receiptsDirectory = File(context.filesDir, RECEIPTS_DIRECTORY).canonicalFile
        val isReceiptImage = file.canonicalFile.parentFile == receiptsDirectory
        isReceiptImage && file.delete()
    }

    private companion object {
        const val RECEIPTS_DIRECTORY = "receipts"
    }
}