package com.example.smartreceiptsplitter.ui.presentation.split

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.data.local.enums.NetMode
import com.example.smartreceiptsplitter.data.local.enums.ShareMode
import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.usecase.person.AddPersonUseCase
import com.example.smartreceiptsplitter.domain.usecase.person.GetOrCreateSelfPersonUseCase
import com.example.smartreceiptsplitter.domain.usecase.person.GetPersonsUseCase
import com.example.smartreceiptsplitter.domain.usecase.person.RemovePersonResult
import com.example.smartreceiptsplitter.domain.usecase.person.RemovePersonUseCase
import com.example.smartreceiptsplitter.domain.usecase.person.RenamePersonUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptByIdUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.CalculateSplitUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.GetReceiptSplitsUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.SaveReceiptSplitsUseCase
import com.example.smartreceiptsplitter.domain.usecase.split.isOutdatedFor
import com.example.smartreceiptsplitter.domain.usecase.split.withAmountsOf
import com.example.smartreceiptsplitter.ui.navigation.Routes
import com.example.smartreceiptsplitter.ui.util.formatMoneyInput
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
    val inputs: SplitInputs = SplitInputs(),
    val calculation: SplitCalculation? = null, // result for the current selection, recomputed on every change
    val hasInvalidInput: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isSplitOutdated: Boolean = false, //the saved split was made for the other amounts (the receipt was updated after split)
    val editedPerson: Person? = null,
    val personMessage: String? = null
)

@HiltViewModel
class SplitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReceiptById: GetReceiptByIdUseCase,
    private val getReceiptSplits: GetReceiptSplitsUseCase,
    private val getPersons: GetPersonsUseCase,
    private val getOrCreateSelf: GetOrCreateSelfPersonUseCase,
    private val addPerson: AddPersonUseCase,
    private val renamePerson: RenamePersonUseCase,
    private val removePerson: RemovePersonUseCase,
    private val calculateSplit: CalculateSplitUseCase,
    private val saveSplits: SaveReceiptSplitsUseCase
): ViewModel() {
    private val receiptId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_RECEIPT_ID))
    private val _state = MutableStateFlow(SplitUiState())
    val state: StateFlow<SplitUiState> = _state.asStateFlow()

    private class Loaded(
        val receipt: Receipt?,
        val persons: List<Person>,
        val selectedIds: Set<Long>,
        val inputs: SplitInputs,
        val isOutdated: Boolean = false
    )

    init {
        viewModelScope.launch {
            runCatching {
                val self = getOrCreateSelf()
                val receipt = getReceiptById(receiptId)
                //editing existing split starting from the people already in it or just user
                val saved = getReceiptSplits(receiptId).first()
                val persons = getPersons().first()
                when {
                    receipt == null -> Loaded(null, persons,emptySet(), SplitInputs())
                    saved.isEmpty() -> Loaded(
                        receipt,
                        persons,
                        setOf(self.id),
                        SplitInputs(tip = receipt.tipAmount?.takeIf { it > 0 }?.let(::formatMoneyInput).orEmpty())
                    )
                    else -> {
                        val savedIds = saved.map { it.personId }.toSet()
                        val ordered = persons.map { it.id }.filter { it in savedIds }
                        if(saved.isOutdatedFor(receipt)) {
                            // modes - recognizing on the amounts the split was made for, then applied to the new ones - even/proportional split is recalculated right away, custom amounts have to be corrected
                            val modes = inferInputs(receipt.withAmountsOf(saved), saved, ordered) { calculateSplit(it)}
                            val tip = receipt.tipAmount?.takeIf { it > 0 }?.let(::formatMoneyInput).orEmpty()
                            Loaded(receipt,  persons,savedIds, modes.copy(tip = tip), isOutdated = true)
                        } else {
                            Loaded(receipt, persons, savedIds, inferInputs(receipt, saved, ordered) { calculateSplit(it) })
                        }
                    }
                }
            }.fold(
                onSuccess = { loaded ->
                    update { copy(isLoading = false, receipt = loaded.receipt, persons = loaded.persons, selectedIds = loaded.selectedIds, inputs = loaded.inputs) }
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

    fun onEditPerson(person: Person) = update { copy(editedPerson = null, personMessage = null) }
    fun onEditPersonDismiss() = update { copy(editedPerson = null, personMessage = null) }
    fun onRenamePerson(name: String) {
        val person = _state.value.editedPerson ?: return
        viewModelScope.launch {
            runCatching { renamePerson(person, name) }.fold(
                onSuccess = { renamed ->
                    update { if (renamed) copy(editedPerson = null, personMessage = null) else copy(personMessage = "Enter a name.") }
                },
                onFailure = { error -> update { copy(personMessage = error.message ?: "Could not rename the person.") } }
            )
        }
    }
    fun onRemovePerson() {
        val person = _state.value.editedPerson ?: return
        viewModelScope.launch {
            runCatching { removePerson(person) }.fold(
                onSuccess = { result ->
                    update {
                        when(result) {
                            RemovePersonResult.Removed -> copy(
                                editedPerson = null,
                                personMessage = null,
                                selectedIds = selectedIds - person.id,
                                inputs = inputs.copy(
                                    netAmounts = inputs.netAmounts - person.id,
                                    taxAmounts = inputs.taxAmounts - person.id,
                                    tipAmounts = inputs.tipAmounts - person.id
                                )
                            )
                            is RemovePersonResult.UsedInReceipts -> copy(
                                personMessage = "${person.name} is part of the split on ${result.count}" +
                                        (if (result.count == 1) "receipt" else "receipts") + " and can't be removed."
                            )
                            RemovePersonResult.IsSelf -> copy(personMessage = "Your own entry can't be removed.")
                        }
                    }
                },
                onFailure = { error -> update { copy(personMessage = error.message ?: "Could not remove the person.") } }
            )
        }
    }

    fun onNetModeChange(mode: NetMode) = update {
        val amounts = if (mode == NetMode.CUSTOM && inputs.netAmounts.isEmpty()) shareTexts { it.netAmount() } else inputs.netAmounts
        copy(inputs = inputs.copy(netMode = mode, netAmounts = amounts))
    }

    fun onTaxModeChange(mode: ShareMode) = update {
        val amounts = if (mode == ShareMode.MANUAL && inputs.taxAmounts.isEmpty()) shareTexts { it.taxShare ?: 0L } else inputs.taxAmounts
        copy(inputs = inputs.copy(taxMode = mode, taxAmounts = amounts))
    }

    fun onTipModeChange(mode: ShareMode) = update {
        val amounts = if (mode == ShareMode.MANUAL && inputs.tipAmounts.isEmpty()) shareTexts { it.tipShare ?: 0L } else inputs.tipAmounts
        copy(inputs = inputs.copy(tipMode = mode, tipAmounts = amounts))
    }

    fun onTipChange(text: String) = update { copy(inputs = inputs.copy(tip = text)) }

    fun onNetAmountChange(personId: Long, text: String) = update {
        copy(inputs = inputs.copy(netAmounts = inputs.netAmounts + (personId to text)))
    }

    fun onTaxAmountChange(personId: Long, text: String) = update {
        copy(inputs = inputs.copy(taxAmounts = inputs.taxAmounts + (personId to text)))
    }

    fun onTipAmountChange(personId: Long, text: String) = update {
        copy(inputs = inputs.copy(tipAmounts = inputs.tipAmounts + (personId to text)))
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

    private fun SplitUiState.shareTexts(amount: (ReceiptSplit) -> Long): Map<Long, String> =
        (calculation as? SplitCalculation.Success)?.splits.orEmpty().associate { it.personId to formatMoneyInput(amount(it)) }

    private fun SplitUiState.withCalculation(): SplitUiState {
        val receipt = receipt ?: return copy(calculation = null, hasInvalidInput = false)
        //persons are sorted by name
        val participantsIds = persons.map { it.id }.filter { it in selectedIds }
        val request = inputs.toRequest(receipt, participantsIds) ?: return copy(calculation = null, hasInvalidInput = true)
        return copy(calculation = calculateSplit(request), hasInvalidInput = false)
    }
}