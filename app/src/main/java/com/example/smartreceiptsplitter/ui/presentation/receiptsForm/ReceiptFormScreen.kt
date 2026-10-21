package com.example.smartreceiptsplitter.ui.presentation.receiptsForm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.smartreceiptsplitter.data.local.enums.ReceiptField
import com.example.smartreceiptsplitter.data.local.enums.ReceiptFieldError
import com.example.smartreceiptsplitter.domain.model.ReceiptLimits
import com.example.smartreceiptsplitter.ui.util.formatMoneyInput

@Composable
fun ReceiptFormScreen(
    modifier: Modifier = Modifier,
    viewModel: ReceiptFormViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onSaved: (Long) -> Unit = {} //receiving the id of the saved receipt
) {
    val state by viewModel.state.collectAsState()
    val inputs = state.inputs
    val errors = state.errors

    LaunchedEffect(state.savedReceiptId) {
        state.savedReceiptId?.let(onSaved)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(onClick = viewModel::onSave, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (state.isManualEntry) "New receipt" else "Check scanned receipt",
                style = MaterialTheme.typography.headlineSmall
            )
            if (!state.isManualEntry) {
                Text(
                    "These values were read from the photo and may be wrong. Correct anything that does not match the receipt.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FormField(
                label = "Merchant (optional)",
                value = inputs.merchantName,
                onValueChange = viewModel::onMerchantChange,
                error = errors[ReceiptField.MERCHANT],
                field = ReceiptField.MERCHANT,
                capitalization = KeyboardCapitalization.Words
            )
            FormField(
                label = "Date",
                value = inputs.date,
                onValueChange = viewModel::onDateChange,
                error = errors[ReceiptField.DATE],
                field = ReceiptField.DATE,
                hint = "dd.MM.yyyy"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(
                    label = "Total",
                    value = inputs.total,
                    onValueChange = viewModel::onTotalChange,
                    error = errors[ReceiptField.TOTAL],
                    field = ReceiptField.TOTAL,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                FormField(
                    label = "Currency",
                    value = inputs.currency,
                    onValueChange = viewModel::onCurrencyChange,
                    error = errors[ReceiptField.CURRENCY],
                    field = ReceiptField.CURRENCY,
                    capitalization = KeyboardCapitalization.Characters,
                    modifier = Modifier.width(120.dp)
                )
            }
            FormField(
                label = "Tax / VAT (optional)",
                value = inputs.tax,
                onValueChange = viewModel::onTaxChange,
                error = errors[ReceiptField.TAX],
                field = ReceiptField.TAX,
                keyboardType = KeyboardType.Decimal,
                hint = "The part of the total that is tax"
            )
            FormField(
                label = "Tip (optional)",
                value = inputs.tip,
                onValueChange = viewModel::onTipChange,
                error = errors[ReceiptField.TIP],
                field = ReceiptField.TIP,
                keyboardType = KeyboardType.Decimal,
                hint = "Paid on top of the total"
            )
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: ReceiptFieldError?,
    field: ReceiptField,
    modifier: Modifier = Modifier.fillMaxWidth(),
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    val supporting = error?.message(field) ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (supporting != null) { { Text(supporting) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        )
    )
}

private fun ReceiptFieldError.message(field: ReceiptField): String = when (this) {
    ReceiptFieldError.REQUIRED -> "Required"
    ReceiptFieldError.INVALID_FORMAT -> when (field) {
        ReceiptField.DATE -> "Use the format dd.MM.yyyy"
        ReceiptField.CURRENCY -> "Use a 3-letter currency code, e.g. EUR"
        else -> "Enter an amount with at most 2 decimals, e.g. 12.50"
    }
    ReceiptFieldError.NOT_POSITIVE -> "Must be greater than 0"
    ReceiptFieldError.NEGATIVE -> "Can't be negative"
    ReceiptFieldError.TOO_LARGE -> "Maximum is ${formatMoneyInput(ReceiptLimits.MAX_AMOUNT)}"
    ReceiptFieldError.TOO_LONG -> "At most ${ReceiptLimits.MAX_MERCHANT_LENGTH} characters"
    ReceiptFieldError.TAX_EXCEEDS_TOTAL -> "Tax can't be higher than the total"
    ReceiptFieldError.DATE_IN_FUTURE -> "Date can't be in the future"
}