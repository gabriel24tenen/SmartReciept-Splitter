package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import javax.inject.Inject

class DeleteReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository
){
    suspend operator fun invoke(receipt: Receipt) = repository.deleteReceipt(receipt)
}