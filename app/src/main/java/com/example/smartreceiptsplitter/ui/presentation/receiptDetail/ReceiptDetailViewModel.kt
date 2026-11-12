package com.example.smartreceiptsplitter.ui.presentation.receiptDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.domain.usecase.receipt.DeleteReceiptUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.ObserveReceiptDetailUseCase
import com.example.smartreceiptsplitter.domain.usecase.receipt.ReceiptDetail
import com.example.smartreceiptsplitter.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptDetailUiState(
    val isLoading: Boolean = true,
    val detail: ReceiptDetail? = null,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeReceiptDetail: ObserveReceiptDetailUseCase,
    private val deleteReceipt: DeleteReceiptUseCase
): ViewModel() {
    private val receiptId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_RECEIPT_ID))
    private val _state = MutableStateFlow(ReceiptDetailUiState())
    val state: StateFlow<ReceiptDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeReceiptDetail(receiptId).collect { detail ->
                _state.update { state ->
                    if (state.isDeleting || state.isDeleted) state
                    else state.copy(isLoading = false, detail = detail)
                }
            }
        }
    }

    fun onDeleteRequest() = _state.update { it.copy(showDeleteConfirmation = true, errorMessage = null) }
    fun onDeleteDismiss() = _state.update { it.copy(showDeleteConfirmation = false) }

    fun onDeleteConfirm() {
        val receipt = _state.value.detail?.receipt ?: return
        if (_state.value.isDeleting) return
        _state.update { it.copy(showDeleteConfirmation = false, isDeleting = true) }
        viewModelScope.launch {
            runCatching { deleteReceipt(receipt) }.fold(
                onSuccess = { _state.update { it.copy(isDeleting = false, isDeleted = true) } },
                onFailure = { error ->
                    _state.update { it.copy(isDeleting = false, errorMessage = error.message ?: "Could not delete the receipt.") }
                }
            )
        }
    }
}