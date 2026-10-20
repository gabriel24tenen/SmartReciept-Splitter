package com.example.smartreceiptsplitter.ui.presentation.split


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitError
import com.example.smartreceiptsplitter.ui.util.formatMoney

@Composable
fun SplitScreen(
    modifier: Modifier = Modifier,
    viewModel: SplitViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
){
    val state by viewModel.state.collectAsState()
    var showAddPerson by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if(state.isSaved) onSaved()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = viewModel::onSave,
                    enabled = state.calculation is SplitCalculation.Success && !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) { Text("Save split.") }
            }
        }
    ) { padding ->
        val receipt = state.receipt
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            receipt == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(state.errorMessage ?: "Receipt not found.") }
            else -> SplitContent(
                state = state,
                receipt = receipt,
                padding = padding,
                onTogglePerson = viewModel::onTogglePerson,
                onAddPerson = { showAddPerson = true }
            )
        }

        if(showAddPerson) {
            AddPersonDialog(
                onDismiss = { showAddPerson = false },
                onConfirm = { name ->
                    viewModel.onAddPerson(name)
                    showAddPerson = false
                }
            )
        }
    }
}

@Composable
private fun SplitContent(
    state: SplitUiState,
    receipt: Receipt,
    padding: PaddingValues,
    onTogglePerson: (Long) -> Unit,
    onAddPerson: () -> Unit
) {
    val personById = state.persons.associateBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Text(receipt.merchantName, style = MaterialTheme.typography.headlineSmall)
                Text("Total: ${formatMoney(receipt.totalAmount, receipt.currency)}")
                receipt.taxAmount?.let { Text("Tax: ${formatMoney(it, receipt.currency)}") }
            }
        }
        item { Text("Participants", style = MaterialTheme.typography.titleMedium) }
        items(state.persons, key = { it.id }) { person ->
            val selected = person.id in state.selectedIds
            Row(
                modifier = Modifier.fillMaxSize().toggleable(value = selected, role = Role.Checkbox, onValueChange = { onTogglePerson(person.id)}).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(person.name)
            }
        }
        item { OutlinedButton(onClick = onAddPerson)  { Text("Add person") } }
        item { Text("Split preview (even)", style = MaterialTheme.typography.titleMedium)}
        when (val calculation = state.calculation) {
            is SplitCalculation.Success -> items(calculation.splits, key = { it.personId}) { split ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(personById[split.personId]?.name.orEmpty())
                    Text(formatMoney(split.amountOwed, receipt.currency))
                }
            }
            is SplitCalculation.Invalid -> item {
                Text(calculation.error.message(), color = MaterialTheme.colorScheme.error)
            }
            null -> Unit
        }

        state.errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
    }
}
@Composable
private fun AddPersonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add person") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it},
                label = { Text("Name")},
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }

    )
}
private fun SplitError.message(): String = when(this) {
    SplitError.NoParticipants -> "Select at least one person."
    SplitError.DuplicateParticipants -> "A person can only be selected once."
    SplitError.NegativeAmount -> "Amounts cannot be negative."
    SplitError.TaxExceedsTotal -> "The tax amount is larger than the receipt total. Correct the receipt first."
    is SplitError.SumMismatch -> "The amounts do not add up."
}