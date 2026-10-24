package com.example.smartreceiptsplitter.ui.presentation.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.usecase.receipt.DeleteReceiptUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptCurrenciesUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val getReceipts: GetReceiptsUseCase,
    private val getCurrencies: GetReceiptCurrenciesUseCase,
    private val deleteReceipt: DeleteReceiptUseCase
): ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _state = MutableStateFlow(ReceiptListUiState())
    val state: StateFlow<ReceiptListUiState> = _state.asStateFlow()
    /*private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()*/

    init {
        viewModelScope.launch {
            _state.map { it.toFilter(zone) }
                .distinctUntilChanged()
                .flatMapLatest { filter -> getReceipts(filter) }
                .collect { receipts ->  _state.update { it.copy(receipts = receipts, isLoading = false) } }
        }
        viewModelScope.launch {
            getCurrencies().collect { currencies ->
                _state.update { it.copy(currencies = currencies, currency = it.currency?.takeIf { selected -> selected in currencies }) }
            }
        }
    }

    fun onQueryChanged(text: String) = change { copy(query = text) }
    fun onDateFromChange(text: String) = change { copy(dateFrom = text) }
    fun onDateToChange(text: String) = change { copy(dateTo = text) }
    fun onCurrencyChange(currency: String?) = change { copy(currency = currency) }

    fun onClearFilters() = change { copy(query = "", dateFrom = "", dateTo = "", currency = null) }
    fun onDeleteRequest(receipt: Receipt) = _state.update { it.copy(pendingDelete = receipt, errorMessage = null) }
    fun onDeleteDismiss() = _state.update { it.copy(pendingDelete = null) }

    fun onDeleteConfirm() {
        val receipt = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            runCatching { deleteReceipt(receipt) }.onFailure { error ->
                _state.update { it.copy(errorMessage = error.message ?: "Could not delete the receipt.") }
            }
        }
    }
    private fun change(transform: ReceiptListUiState.() -> ReceiptListUiState) {
        _state.update { it.transform().withDateErrors(zone) }
    }
}