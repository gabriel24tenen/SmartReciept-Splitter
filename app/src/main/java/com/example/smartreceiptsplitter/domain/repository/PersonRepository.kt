package com.example.smartreceiptsplitter.domain.repository

import com.example.smartreceiptsplitter.domain.model.Person
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun getAllPerson(): Flow<List<Person>>
    suspend fun getOrCreateSelf(): Person //creates user on first use, later returns the person that it represents
    suspend fun addPerson(person: Person): Long
}