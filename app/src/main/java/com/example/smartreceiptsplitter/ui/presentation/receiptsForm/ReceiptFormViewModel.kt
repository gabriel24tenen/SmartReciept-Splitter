package com.example.smartreceiptsplitter.ui.presentation.receiptsForm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptByIdUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.SaveReceiptUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.UpdateReceiptUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.ValidateReceiptUseCase
import com.example.smartreceiptsplitter.ui.navigation.Routes
import com.example.smartreceiptsplitter.ui.util.formatDateInput
import com.example.smartreceiptsplitter.ui.util.formatMoneyInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class ReceiptFormUiState(
    val inputs: ReceiptFormInputs = ReceiptFormInputs(),
    val isManualEntry: Boolean = true, // it will be false if form holds an OCR result of scanned receipt to correct
    val errors: Map<ReceiptField, ReceiptFieldError> = emptyMap(),
    val isSaving: Boolean = false,
    val savedReceiptId: Long? = null,
    val errorMessage: String? = null,
    //F06 editing a receipt that is already saved
    val isEditing: Boolean = false,
    val isLoading: Boolean = false, // receipt is being read
    val notFound: Boolean = false, // the receipt to edit doesn't exist
    val splitNeedsReview: Boolean = false // after an edit its saved split no longer matches the amounts
)

@HiltViewModel
class ReceiptFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val validateReceipt: ValidateReceiptUseCase,
    private val saveReceipt: SaveReceiptUseCase,
    private val getReceiptById: GetReceiptByIdUseCase,
    private val updateReceipt: UpdateReceiptUseCase
): ViewModel() {
    private val editingId: Long? = savedStateHandle.get<Long>(Routes.ARG_RECEIPT_ID)
    private var editedReceipt: Receipt? = null //stored receipt is loaded in init
    // Only a manual entry is stored with true value for manual entry, not ones that comes with its photo scanned
    private val imagePath: String? = savedStateHandle.get<String>(Routes.ARG_IMAGE_PATH)
    private val isManualEntry = imagePath == null
    private val clock = Clock.systemDefaultZone()
    private val _state = MutableStateFlow(
        ReceiptFormUiState(inputs = initialInputs(savedStateHandle), isManualEntry = isManualEntry, isEditing = editingId != null, isLoading =  editingId != null))
    val state: StateFlow<ReceiptFormUiState> = _state.asStateFlow()

    private var submitted = false //after first Save, errors are just refreshed

    init {
        if (editingId != null) loadReceiptToEdit(editingId)
    }

    fun onMerchantChange(text: String) = change { copy(merchantName = text) }
    fun onDateChange(text: String) = change { copy(date = text) }
    fun onTotalChange(text: String) = change { copy(total = text) }
    fun onTaxChange(text: String) = change { copy(tax = text) }
    fun onTipChange(text: String) = change { copy(tip = text) }
    fun onCurrencyChange(text: String) = change { copy(currency = text.filter { it.isLetter() }.take(3).uppercase()) }


    fun onSave() {
        if (_state.value.isSaving || _state.value.savedReceiptId != null) return
        if (editingId != null && editedReceipt == null) return
        submitted = true
        when(val result = _state.value.inputs.validate(validateReceipt, clock)) {
            is ReceiptValidation.Invalid -> _state.update { it.copy(errors = result.errors) }
            is ReceiptValidation.Valid -> {
                _state.update { it.copy(errors = emptyMap(), isSaving = true, errorMessage = null) }
                viewModelScope.launch {
                    val existing = editedReceipt
                    runCatching {
                        if (existing != null) {
                            val review = updateReceipt(existing, result.receipt)
                            existing.id to review
                        } else {
                            saveReceipt(result.receipt, isManualEntry, imagePath) to false
                        }
                    }.fold(
                        onSuccess = { (id, review) -> _state.update { it.copy(isSaving = false, savedReceiptId = id, splitNeedsReview = review) } },
                        onFailure = { error ->
                            _state.update { it.copy(isSaving = false, errorMessage = error.message ?: "Could not save the receipt.") }
                        })
                }
            }
        }
    }

    private fun change(transform: ReceiptFormInputs.() -> ReceiptFormInputs) {
        _state.update { state ->
            val inputs = state.inputs.transform()
            state.copy(inputs = inputs, errors = if (submitted) errorsOf(inputs) else state.errors)
        }
    }

    private fun errorsOf(inputs: ReceiptFormInputs): Map<ReceiptField, ReceiptFieldError> =
        (inputs.validate(validateReceipt, clock) as? ReceiptValidation.Invalid)?.errors.orEmpty()

    private fun loadReceiptToEdit(id: Long) {
        viewModelScope.launch {
            runCatching { getReceiptById(id) }.fold(
                onSuccess = { receipt ->
                    editedReceipt = receipt
                    _state.update {
                        if(receipt == null) it.copy(isLoading = false, notFound = true)
                        else it.copy(isLoading = false, inputs = receipt.toEditInputs(clock), isManualEntry = receipt.isManualEntry)
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, notFound = true, errorMessage = error.message ?: "Could not load the receipt.") }
                }
            )
        }
    }

    private fun Receipt.toEditInputs(clock: Clock) = ReceiptFormInputs(
        merchantName = merchantName.orEmpty(),
        date = formatDateInput(date, clock.zone),
        total = formatMoneyInput(totalAmount),
        tax = taxAmount?.let(::formatMoneyInput).orEmpty(),
        tip = tipAmount?.let(::formatMoneyInput).orEmpty(),
        currency = currency
    )
    // a new receipt starts with today's date, a scanned one starts with OCR read, if date was not found it will stay blank -> wrong default will not be saved
    private fun initialInputs(handle: SavedStateHandle): ReceiptFormInputs {
        if (editingId != null) return ReceiptFormInputs()
        if (isManualEntry) return ReceiptFormInputs(date = formatDateInput(clock.millis(), clock.zone))
        fun arg(name: String) = handle.get<String>(name).orEmpty()
        return ReceiptFormInputs(
            merchantName = arg(Routes.ARG_MERCHANT),
            date = arg(Routes.ARG_DATE),
            total = arg(Routes.ARG_TOTAL),
            tax = arg(Routes.ARG_TAX),
            tip = arg(Routes.ARG_TIP),
            currency = arg(Routes.ARG_CURRENCY).ifBlank { ReceiptFormInputs.DEFAULT_CURRENCY }
        )
    }
}