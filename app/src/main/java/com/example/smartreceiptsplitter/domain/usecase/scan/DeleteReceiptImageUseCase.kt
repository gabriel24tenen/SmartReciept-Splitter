package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import javax.inject.Inject

class DeleteReceiptImageUseCase @Inject constructor(
    private val storage: ReceiptImageStorage
) {
    suspend operator fun invoke(path: String): Boolean = storage.delete(path)
}