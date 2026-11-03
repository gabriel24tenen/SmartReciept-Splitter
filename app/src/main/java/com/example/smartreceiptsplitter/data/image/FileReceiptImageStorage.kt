package com.example.smartreceiptsplitter.data.image

import android.content.Context
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileReceiptImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
): ReceiptImageStorage {
    private val directory: File get() = File(context.filesDir, RECEIPTS_DIRECTORY)

    override suspend fun save(jpeg: ByteArray): String = withContext(Dispatchers.IO){
        val folder = directory.apply { mkdirs() }
        val file = File(folder, "receipt_${System.currentTimeMillis()}.jpg")
        file.writeBytes(jpeg)
        file.absolutePath
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        val isReceiptImage = file.canonicalFile.parentFile == directory.canonicalFile
        isReceiptImage && file.delete()
    }

    private companion object {
        const val RECEIPTS_DIRECTORY = "receipts"
    }
}