package com.example.smartreceiptsplitter.domain.usecase.person

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import javax.inject.Inject

class GetOrCreateSelfPersonUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    suspend operator fun invoke(): Person = repository.getOrCreateSelf()
}