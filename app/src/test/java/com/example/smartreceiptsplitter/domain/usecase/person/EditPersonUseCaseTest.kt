package com.example.smartreceiptsplitter.domain.usecase.person

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditPersonUseCaseTest {

    private class FakePersons : PersonRepository {
        val renamed = mutableListOf<Pair<Long, String>>()
        val deleted = mutableListOf<Long>()
        override fun getAllPerson(): Flow<List<Person>> = flowOf(emptyList())
        override suspend fun getOrCreateSelf(): Person = error("not used")
        override suspend fun addPerson(person: Person): Long = 0
        override suspend fun renamePerson(id: Long, name: String) { renamed += id to name }
        override suspend fun deletePerson(id: Long) { deleted += id }
    }

    // receipts each person is part of
    private class FakeSplits(private val receiptsByPerson: Map<Long, Int>) : ReceiptSplitRepository {
        override fun observeSplits(receiptId: Long): Flow<List<ReceiptSplit>> = flowOf(emptyList())
        override fun observeAllSplits(): Flow<List<ReceiptSplit>> = flowOf(emptyList())
        override suspend fun saveSplits(receiptId: Long, splits: List<ReceiptSplit>, receiptTipAmount: Long?) = Unit
        override suspend fun countSplitsOf(personId: Long): Int = receiptsByPerson[personId] ?: 0
    }

    private val me = Person(id = 1, name = "Me", isSelf = true)
    private val ana = Person(id = 2, name = "Anna")
    private val ivan = Person(id = 3, name = "Ivan")

    // ---- rename

    @Test
    fun personIsRenamedWithTheTrimmedName() {
        val persons = FakePersons()
        assertTrue(runBlocking { RenamePersonUseCase(persons)(ana, "  Ana ") })
        assertEquals(listOf(2L to "Ana"), persons.renamed)
    }

    @Test
    fun ownEntryCanBeRenamed() {
        val persons = FakePersons()
        assertTrue(runBlocking { RenamePersonUseCase(persons)(me, "Gabriel") })
        assertEquals(listOf(1L to "Gabriel"), persons.renamed)
    }

    @Test
    fun blankNameIsRejected() {
        val persons = FakePersons()
        assertFalse(runBlocking { RenamePersonUseCase(persons)(ana, "   ") })
        assertFalse(runBlocking { RenamePersonUseCase(persons)(me, "") })
        assertTrue(persons.renamed.isEmpty())
    }

    @Test
    fun sameNameChangesNothing() {
        val persons = FakePersons()
        assertTrue(runBlocking { RenamePersonUseCase(persons)(ana, "Anna ") })
        assertTrue(persons.renamed.isEmpty())
    }

    // ---- remove

    @Test
    fun personWithoutSplitsIsRemoved() {
        val persons = FakePersons()
        val result = runBlocking { RemovePersonUseCase(persons, FakeSplits(mapOf(2L to 3)))(ivan) }
        assertEquals(RemovePersonResult.Removed, result)
        assertEquals(listOf(3L), persons.deleted)
    }

    @Test
    fun personInSavedSplitsIsKeptAndTheReceiptsAreCounted() {
        val persons = FakePersons()
        val result = runBlocking { RemovePersonUseCase(persons, FakeSplits(mapOf(2L to 3)))(ana) }
        assertEquals(RemovePersonResult.UsedInReceipts(3), result)
        assertTrue(persons.deleted.isEmpty())
    }

    @Test
    fun ownEntryIsNeverRemoved() {
        val persons = FakePersons()
        val result = runBlocking { RemovePersonUseCase(persons, FakeSplits(emptyMap()))(me) }
        assertEquals(RemovePersonResult.IsSelf, result)
        assertTrue(persons.deleted.isEmpty())
    }
}

