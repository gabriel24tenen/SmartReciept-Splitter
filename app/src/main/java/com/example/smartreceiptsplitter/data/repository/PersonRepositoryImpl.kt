package com.example.smartreceiptsplitter.data.repository

import com.example.smartreceiptsplitter.data.local.dao.PersonDao
import com.example.smartreceiptsplitter.data.local.entity.PersonEntity
import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class PersonRepositoryImpl @Inject constructor(
    private val personDao: PersonDao
): PersonRepository {

    //singleton repostiroy, this guards against creating two "self" rows
    private val selfMutex = Mutex()

    override fun getAllPerson(): Flow<List<Person>> =
        personDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getOrCreateSelf(): Person = selfMutex.withLock {
        personDao.getSelf()?.toDomain() ?: run {
            val self = Person(name = SELF_NAME, isSelf = true)
            self.copy(id = personDao.insert(self.toEntity()))
        }
    }

    override suspend fun addPerson(person: Person): Long =
        personDao.insert(person.toEntity())

    private fun PersonEntity.toDomain() = Person(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        email = email,
        avatarPath = avatarPath,
        isSelf = isSelf
    )

    private fun Person.toEntity() = PersonEntity(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        email = email,
        avatarPath = avatarPath,
        isSelf = isSelf
    )

    private companion object {
        const val SELF_NAME = "Me"
    }
}