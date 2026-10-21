package com.example.smartreceiptsplitter.ui.presentation.receiptsForm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.ReceiptValidation
import com.example.smartreceiptsplitter.domain.usecase.receipt.SaveReceiptUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.ValidateReceiptUseCase
import com.example.smartreceiptsplitter.ui.util.formatDateInput
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
    val errors: Map<ReceiptField, ReceiptFieldError> = emptyMap(),
    val isSaving: Boolean = false,
    val savedReceiptId: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiptFormViewModel @Inject constructor(
    private val validateReceipt: ValidateReceiptUseCase,
    private val saveReceipt: SaveReceiptUseCase
): ViewModel() {
    private val clock = Clock.systemDefaultZone()
    private val _state = MutableStateFlow(
        ReceiptFormUiState(inputs = ReceiptFormInputs(date = formatDateInput(clock.millis(), clock.zone)))
    )
    val state: StateFlow<ReceiptFormUiState> = _state.asStateFlow()

    private var submitted = false //after first Save, errors are just refreshed
    private val isManualEntry = true // TODO TASK 7.3

    fun onMerchantChange(text: String) = change { copy(merchantName = text) }
    fun onDateChange(text: String) = change { copy(date = text) }
    fun onTotalChange(text: String) = change { copy(total = text) }
    fun onTaxChange(text: String) = change { copy(tax = text) }
    fun onTipChange(text: String) = change { copy(tip = text) }
    fun onCurrencyChange(text: String) = change { copy(currency = text.filter { it.isLetter() }.take(3).uppercase()) }


    fun onSave() {
        if (_state.value.isSaving || _state.value.savedReceiptId != null) return
        submitted = true
        when(val result = _state.value.inputs.validate(validateReceipt, clock)) {
            is ReceiptValidation.Invalid -> _state.update { it.copy(errors = result.errors) }
            is ReceiptValidation.Valid -> {
                _state.update { it.copy(errors = emptyMap(), isSaving = true, errorMessage = null) }
                viewModelScope.launch {
                    runCatching { saveReceipt(result.receipt, isManualEntry) }.fold(onSuccess = { id -> _state.update { it.copy(isSaving = false, savedReceiptId = id) }},
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
}