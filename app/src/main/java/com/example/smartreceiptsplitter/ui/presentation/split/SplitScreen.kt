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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.smartreceiptsplitter.data.local.enums.NetMode
import com.example.smartreceiptsplitter.data.local.enums.ShareMode
import com.example.smartreceiptsplitter.data.local.enums.SplitPart
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.SplitCalculation
import com.example.smartreceiptsplitter.domain.model.SplitError
import com.example.smartreceiptsplitter.ui.util.formatMoney
import com.example.smartreceiptsplitter.ui.util.parseMoney

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
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = viewModel::onSave,
                    enabled = state.calculation is SplitCalculation.Success && !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) { Text("Save split") }
            }
        }
    ) { padding ->
        val receipt = state.receipt
        when {
            state.isLoading -> Box(Modifier
                .fillMaxSize()
                .padding(padding), Alignment.Center) { CircularProgressIndicator() }
            receipt == null -> Box(Modifier
                .fillMaxSize()
                .padding(padding), Alignment.Center) { Text(state.errorMessage ?: "Receipt not found.") }
            else -> SplitContent(
                state = state,
                receipt = receipt,
                padding = padding,
                actions = SplitActions(
                    onTogglePerson = viewModel::onTogglePerson,
                    onAddPerson = { showAddPerson = true },
                    onNetModeChange = viewModel::onNetModeChange,
                    onTaxModeChange = viewModel::onTaxModeChange,
                    onTipModeChange = viewModel::onTipModeChange,
                    onTipChange = viewModel::onTipChange,
                    onNetAmountChange = viewModel::onNetAmountChange,
                    onTaxAmountChange = viewModel::onTaxAmountChange,
                    onTipAmountChange = viewModel::onTipAmountChange
                )
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

private class SplitActions(
    val onTogglePerson: (Long) -> Unit,
    val onAddPerson: () -> Unit,
    val onNetModeChange: (NetMode) -> Unit,
    val onTaxModeChange: (ShareMode) -> Unit,
    val onTipModeChange: (ShareMode) -> Unit,
    val onTipChange: (String) -> Unit,
    val onNetAmountChange: (Long, String) -> Unit,
    val onTaxAmountChange: (Long, String) -> Unit,
    val onTipAmountChange: (Long, String) -> Unit
)

@Composable
private fun SplitContent(
    state: SplitUiState,
    receipt: Receipt,
    padding: PaddingValues,
    actions: SplitActions
) {
    val personById = state.persons.associateBy { it.id }
    val selectedPersons = state.persons.filter { it.id in state.selectedIds }
    val currency = receipt.currency
    val inputs = state.inputs
    val shareModes = listOf(
        ShareMode.PROPORTIONAL to "Proportional",
        ShareMode.EVEN to "Even",
        ShareMode.MANUAL to "Manual"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") {
            Column {
                Text(receipt.merchantName ?: "Receipt", style = MaterialTheme.typography.headlineSmall)
                Text("Total: ${formatMoney(receipt.totalAmount, currency)}")
                receipt.taxAmount?.let { Text("Tax: ${formatMoney(it, currency)}") }
            }
        }
        item(key = "participants.title") { Text("Participants", style = MaterialTheme.typography.titleMedium) }
        items(state.persons, key = { "person-${it.id}" }) { person ->
            val selected = person.id in state.selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = selected,
                        role = Role.Checkbox,
                        onValueChange = { actions.onTogglePerson(person.id) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(person.name)
            }
        }
        item(key = "add-person") { OutlinedButton(onClick = actions.onAddPerson)  { Text("Add person") } }

        item(key = "net-title") { SectionTitle("Amounts per person") }
        item(key = "net-mode") {
            ModeSelector(
                options = listOf(NetMode.EVEN to "Even", NetMode.CUSTOM to "Custom"),
                selected = inputs.netMode,
                onSelect = actions.onNetModeChange
            )
        }
        if(inputs.netMode == NetMode.CUSTOM) {
            items(selectedPersons, key = { "net-${it.id}" }) { person ->
                AmountField(person.name, inputs.netAmounts[person.id].orEmpty(), currency) {
                    actions.onNetAmountChange(person.id, it)
                }
            }
        }

        if ((receipt.taxAmount ?: 0L) > 0L) {
            item(key = "tax-title") { SectionTitle("Tax share") }
            item(key = "tax-mode") {
                ModeSelector(options = shareModes, selected = inputs.taxMode, onSelect = actions.onTaxModeChange)
            }
            if (inputs.taxMode == ShareMode.MANUAL) {
                items(selectedPersons, key = { "tax-${it.id}" }) { person ->
                    AmountField(person.name, inputs.taxAmounts[person.id].orEmpty(), currency) {
                        actions.onTaxAmountChange(person.id, it)
                    }
                }
            }
        }

        item(key = "tip-title") { SectionTitle("Tip (paid on top of the total)") }
        item(key = "tip-total") { AmountField("Tip amount", inputs.tip, currency, actions.onTipChange) }
        item(key = "tip-mode") {
            ModeSelector(options = shareModes, selected = inputs.tipMode, onSelect = actions.onTipModeChange)
        }
        if (inputs.tipMode == ShareMode.MANUAL) {
            items(selectedPersons, key = { "tip-${it.id}" }) { person ->
                AmountField(person.name, inputs.tipAmounts[person.id].orEmpty(), currency) {
                    actions.onTipAmountChange(person.id, it)
                }
            }
        }


        item(key = "preview-title") { SectionTitle("Split preview") }
        if(state.hasInvalidInput) {
            item(key = "preview-invalid") {
                Text("Enter amounts like 12.50 (at most two decimals.", color = MaterialTheme.colorScheme.error)
            }
        }
        when (val calculation = state.calculation) {
            is SplitCalculation.Success -> items(calculation.splits, key = { it.personId}) { split ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(personById[split.personId]?.name.orEmpty())
                        Text(formatMoney(split.amountOwed, currency))
                    }
                    val details = listOfNotNull(
                        split.taxShare?.let { "tax ${formatMoney(it, currency)}" },
                        split.tipShare?.let { "tip ${formatMoney(it, currency)}" }
                    )
                    if(details.isNotEmpty()) {
                        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            is SplitCalculation.Invalid -> item(key="preview-error") {
                Text(calculation.error.message(calculation.error.message(currency)), color = MaterialTheme.colorScheme.error)
            }
            null -> Unit
        }

        state.errorMessage?.let { message ->
            item(key = "save-error") { Text(message, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun <T> ModeSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Spacer(Modifier.width(4.dp))
                Text(label)
            }
        }
    }

}

@Composable
private fun AmountField(
    label: String,
    value: String,
    currency: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(currency) },
        singleLine = true,
        isError = parseMoney(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
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
private fun SplitError.message(currency: String): String = when(this) {
    SplitError.NoParticipants -> "Select at least one person."
    SplitError.DuplicateParticipants -> "A person can only be selected once."
    SplitError.NegativeAmount -> "Amounts cannot be negative."
    SplitError.TaxExceedsTotal -> "The tax amount is larger than the receipt total. Correct the receipt first."
    is SplitError.SumMismatch -> {

        val what = when (part) {
            SplitPart.NET -> "Amounts per person"
            SplitPart.TAX -> "Tax shares"
            SplitPart.TIP -> "Tip shares"
        }
        val difference = expected - actual
        if (difference > 0) "$what: ${formatMoney(difference, currency)} still to assign."
        else "$what: ${formatMoney(-difference, currency)} over the total."
    }
}