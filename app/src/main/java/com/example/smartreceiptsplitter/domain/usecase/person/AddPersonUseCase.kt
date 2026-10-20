package com.example.smartreceiptsplitter.domain.usecase.person

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import javax.inject.Inject

class AddPersonUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    // returns the id of the new person, if name is blank it's null
    suspend operator fun invoke(name: String): Long? {
        val trimmed = name.trim()
        if(trimmed.isEmpty()) return null
        return repository.addPerson(Person(name = trimmed))
    }
}