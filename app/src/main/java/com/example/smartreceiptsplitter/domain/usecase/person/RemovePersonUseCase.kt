package com.example.smartreceiptsplitter.domain.usecase.person

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import javax.inject.Inject

// A person can be removed only while no saved split points to them as the
// history of who owed what and the RESTRICT FK would refuse the deletion anyway
// The user's own entry is never removed because the split screen would create a new "Me" right away

class RemovePersonUseCase @Inject constructor(
    private val persons: PersonRepository,
    private val splits: ReceiptSplitRepository
) {
    suspend operator fun invoke(person: Person): RemovePersonResult {
        if (person.isSelf) return RemovePersonResult.IsSelf
        val receipts = splits.countSplitsOf(person.id)
        if (receipts > 0) return RemovePersonResult.UsedInReceipts(receipts)
        persons.deletePerson(person.id)
        return RemovePersonResult.Removed
    }
}

sealed interface RemovePersonResult {
    data object Removed: RemovePersonResult
    data class UsedInReceipts(val count: Int): RemovePersonResult
    data object IsSelf: RemovePersonResult
}