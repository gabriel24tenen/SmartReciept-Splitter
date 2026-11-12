package com.example.smartreceiptsplitter.domain.usecase.person

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import javax.inject.Inject

class RenamePersonUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    suspend operator fun invoke(person: Person, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed != person.name) repository.renamePerson(person.id, trimmed)
        return true
    }
}