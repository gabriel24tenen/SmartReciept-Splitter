package com.example.smartreceiptsplitter.ui.presentation.split

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitRequest
import com.example.smartreceiptsplitter.domain.usecase.person.AddPersonUseCase
import com.example.smartreceiptsplitter.domain.usecase.person.GetOrCreateSelfPersonUseCase
import com.example.smartreceiptsplitter.domain.usecase.person.GetPersonsUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptByIdUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.CalculateSplitUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.GetReceiptSplitsUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.SaveReceiptSplitsUseCase
import com.example.smartreceiptsplitter.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitUiState(
    val isLoading: Boolean = true,
    val receipt: Receipt? = null,
    val persons: List<Person> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val calculation: SplitCalculation? = null, // result for the current selection, recomputed on every change
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SplitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReceiptById: GetReceiptByIdUseCase,
    private val getReceiptSplits: GetReceiptSplitsUseCase,
    private val getPersons: GetPersonsUseCase,
    private val getOrCreateSelf: GetOrCreateSelfPersonUseCase,
    private val addPerson: AddPersonUseCase,
    private val calculateSplit: CalculateSplitUseCase,
    private val saveSplits: SaveReceiptSplitsUseCase
): ViewModel() {
    private val receiptId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_RECEIPT_ID))
    private val _state = MutableStateFlow(SplitUiState())
    val state: StateFlow<SplitUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val self = getOrCreateSelf()
                val receipt = getReceiptById(receiptId)
                //editing existing split starting from the people already in it or just user
                val existing = getReceiptSplits(receiptId).first()
                receipt to existing.map { it.personId }.toSet().ifEmpty { setOf(self.id) }
            }.fold(
                onSuccess = { (receipt, selected) ->
                    update { copy(isLoading = false, receipt = receipt, selectedIds = selected) }
                },
                onFailure = { error ->
                    update { copy(isLoading = false, errorMessage = error.message ?: "Could not load the receipt.") }
                }
            )
        }
        viewModelScope.launch {
            getPersons().collect { persons -> update { copy(persons = persons) } }
        }
    }

    fun onTogglePerson(personId: Long) = update {
        copy(selectedIds = if (personId in selectedIds) selectedIds - personId else selectedIds + personId)
    }

    fun onAddPerson(name: String) {
        viewModelScope.launch {
            addPerson(name)?.let { id -> update { copy(selectedIds = selectedIds + id) } }
        }
    }

    fun onSave() {
        val calculation = _state.value.calculation as? SplitCalculation.Success ?: return
        if(_state.value.isSaving) return
        update { copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { saveSplits(receiptId, calculation.splits) }.fold(
                onSuccess = { update { copy(isSaving = false, isSaved = true) } },
                onFailure = { error ->
                    update { copy(isSaving = false, errorMessage = error.message ?: "Could not save the split.") }
                }
            )
        }
    }

    private fun update(transform: SplitUiState.() -> SplitUiState) {
        _state.update { it.transform().withCalculation() }
    }

    private fun SplitUiState.withCalculation(): SplitUiState {
        val receipt = receipt ?: return copy(calculation = null)
        //persons are sorted by name
        val participantsIds = persons.map { it.id }.filter { it in selectedIds }
        return copy(
            calculation = calculateSplit(
                SplitRequest(
                    totalAmount = receipt.totalAmount,
                    taxAmount = receipt.taxAmount ?: 0,
                    tipAmount = receipt.tipAmount ?: 0,
                    participantsIds
                )
            )
        )
    }
}