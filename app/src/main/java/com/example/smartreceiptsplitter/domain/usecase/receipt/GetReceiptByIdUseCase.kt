package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import javax.inject.Inject

class GetReceiptByIdUseCase @Inject constructor(
    private val repository: ReceiptRepository
){
    suspend operator fun invoke(id: Long): Receipt? = repository.getReceiptById(id)
}