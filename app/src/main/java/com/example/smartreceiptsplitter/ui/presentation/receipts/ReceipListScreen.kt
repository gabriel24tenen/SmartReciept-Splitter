package com.example.smartreceiptsplitter.ui.presentation.receipts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.ui.util.formatDateInput
import com.example.smartreceiptsplitter.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    modifier: Modifier = Modifier,
    viewModel: ReceiptListViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenReceipt: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var shownFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Receipts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by merchant")},
                singleLine = true,
                trailingIcon = if (state.query.isNotEmpty()) {
                    { TextButton(onClick = { viewModel.onQueryChanged("") }) { Text("Clear") } }
                 } else null
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { shownFilters = !shownFilters }) {
                    Text(if (state.activeFilterCount > 0) "Filters (${state.activeFilterCount})" else "Filters")
                }
                Spacer(modifier = Modifier.weight(1f))
                if(state.hasActiveFilters) {
                    TextButton(onClick = viewModel::onClearFilters) { Text("Clear all") }
                }
            }

            if(shownFilters) {
                FilterPanel(
                    state = state,
                    onDateFromChange = viewModel::onDateFromChange,
                    onDateToChange = viewModel::onDateToChange,
                    onCurrencyChange = viewModel::onCurrencyChange
                )
            }

            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.receipts.isEmpty() -> EmptyState(
                    hasFilters = state.hasActiveFilters,
                    onClearFilters = viewModel::onClearFilters,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.receipts, key = { it.id }) { receipt ->
                        ReceiptRow(
                            receipt = receipt,
                            onOpen = { onOpenReceipt(receipt.id) },
                            onDelete = { viewModel.onDeleteRequest(receipt) }
                        )
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { receipt ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete receipt?") },
            text = {
                Text(
                    "${receipt.displayName()} from ${formatDateInput(receipt.date)} " +
                            "(${formatMoney(receipt.totalAmount, receipt.currency)}) will be deleted permanently, " +
                            "together with its split. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanel(
    state: ReceiptListUiState,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    onCurrencyChange: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField(
                label = "From",
                value = state.dateFrom,
                onValueChange = onDateFromChange,
                error = if (state.dateFromInvalid) "Use the format dd.MM.yyyy" else null,
                modifier = Modifier.weight(1f)
            )
            DateField(
                label = "To",
                value = state.dateTo,
                onValueChange = onDateToChange,
                error = when {
                    state.dateToInvalid -> "Use the format dd.MM.yyyy"
                    state.dateRangeReversed -> "Before the start date"
                    else -> null
                },
                modifier = Modifier.weight(1f)
            )
        }
        if(state.currencies.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.currency == null,
                    onClick = { onCurrencyChange(null) },
                    label = { Text("All currencies") }
                )
                state.currencies.forEach { code ->
                    FilterChip(
                        selected = state.currency == code,
                        onClick = { onCurrencyChange(code) },
                        label = { Text(code) }
                    )
                }
            }
        }
    }
}


@Composable
private fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        isError = error != null,
        supportingText = { Text(error ?: "dd.MM.yyyy") },
        singleLine = true
    )
}

@Composable
private fun ReceiptRow(
    receipt: Receipt,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
          Column(modifier = Modifier.weight(1f)) {
              Text(
                  receipt.displayName(),
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
              )
              Text(formatDateInput(receipt.date), style = MaterialTheme.typography.bodyMedium)
          }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(receipt.totalAmount, receipt.currency),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun EmptyState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(hasFilters) {
            Text("No receipts match your search.", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onClearFilters) { Text("Clear filters") }
        } else {
            Text("No saved receipts yet.", style = MaterialTheme.typography.bodyLarge)
            Text("Scan or enter a receipt and it will show up here.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun Receipt.displayName(): String = merchantName?.takeIf { it.isNotBlank() } ?: "Unknown merchant"