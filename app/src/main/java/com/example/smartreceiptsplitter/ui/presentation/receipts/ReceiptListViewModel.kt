package com.example.smartreceiptsplitter.ui.presentation.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptCurrenciesUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.GetReceiptListItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val getReceiptListItems: GetReceiptListItemsUseCase,
    private val getCurrencies: GetReceiptCurrenciesUseCase
): ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _state = MutableStateFlow(ReceiptListUiState())
    val state: StateFlow<ReceiptListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.map { it.toFilter(zone) }
                .distinctUntilChanged()
                .flatMapLatest { filter -> getReceiptListItems(filter) }
                .collect { items ->
                    _state.update { it.copy(receipts = items.receipts, splitSummaries = items.splitSummaries, isLoading = false) }
                }
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

    private fun change(transform: ReceiptListUiState.() -> ReceiptListUiState) {
        _state.update { it.transform().withDateErrors(zone) }
    }
}