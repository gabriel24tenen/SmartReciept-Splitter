package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class DeleteReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository,
    private val imageStorage: ReceiptImageStorage
){
    suspend operator fun invoke(receipt: Receipt) {
        repository.deleteReceipt(receipt)
        receipt.imagePath?.let { path ->
            try {
                imageStorage.delete(path)
            }
            catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                //the file stays behind, the receipt is deleted anyway
            }
        }
    }
}