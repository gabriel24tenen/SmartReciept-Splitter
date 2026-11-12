package com.example.smartreceiptsplitter.ui.presentation.receiptDetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.usecase.receipt.ReceiptDetail
import com.example.smartreceiptsplitter.ui.util.displayName
import com.example.smartreceiptsplitter.ui.util.formatDateInput
import com.example.smartreceiptsplitter.ui.util.formatMoney
import com.example.smartreceiptsplitter.ui.util.rememberReceiptImage

@Composable
fun ReceiptDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: ReceiptDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onEdit: (receiptId: Long) -> Unit = {},
    onSplit: (receiptId: Long) -> Unit = {},
    onDeleted: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isDeleted) {
        if(state.isDeleted) onDeleted()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    ) { padding ->
        val detail = state.detail
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            detail == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("Receipt not found.") }
            else -> DetailContent(
                detail = detail,
                isBusy = state.isDeleting,
                errorMessage = state.errorMessage,
                modifier = Modifier.padding(padding),
                onEdit = { onEdit(detail.receipt.id) },
                onSplit = { onSplit(detail.receipt.id) },
                onDelete = viewModel::onDeleteRequest
            )
        }
    }

    val receipt = state.detail?.receipt
    if (state.showDeleteConfirmation && receipt != null) {
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

@Composable
private fun DetailContent(
    detail: ReceiptDetail,
    isBusy: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit
) {
    val receipt = detail.receipt
    var showFullImage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(receipt.displayName(), style = MaterialTheme.typography.headlineSmall)
        Text(
            formatDateInput(receipt.date) + if (receipt.isManualEntry) " · entered by hand" else " · scanned",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AmountsCard(receipt)

        receipt.imagePath?.let { path ->
            ReceiptPhoto(path = path, onOpen = { showFullImage = true })
            if (showFullImage) FullScreenPhoto(path = path, onClose = { showFullImage = false })
        }

        SplitSection(detail = detail, onSplit = onSplit)

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onEdit,
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
            ) { Text("Edit") }
            TextButton(
                onClick = onDelete,
                enabled = !isBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) { Text("Delete") }
        }
    }
}
@Composable
private fun AmountsCard(receipt: Receipt) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AmountRow("Total", formatMoney(receipt.totalAmount, receipt.currency), emphasized = true)
            receipt.taxAmount?.let { AmountRow("of which tax / VAT", formatMoney(it, receipt.currency)) }
            receipt.tipAmount?.takeIf { it > 0 }?.let { tip ->
                AmountRow("Tip", formatMoney(tip, receipt.currency))
                HorizontalDivider()
                AmountRow("Paid with tip", formatMoney(receipt.totalAmount + tip, receipt.currency), emphasized = true)
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
@Composable
private fun SplitSection(detail: ReceiptDetail, onSplit: () -> Unit) {
    val currency = detail.receipt.currency
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Split", style = MaterialTheme.typography.titleMedium)

        if(detail.isSplitOutdated) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "The receipt was changed after this split was saved, so the split no longer adds up.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if(detail.shares.isEmpty()) {
            Text(
                "This receipt isn't split yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            detail.shares.forEach { share ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(share.personName)
                        Text(formatMoney(share.split.amountOwed, currency))
                    }
                    val parts = listOfNotNull(
                        share.split.taxShare?.let { "tax ${formatMoney(it, currency)}" },
                        share.split.tipShare?.let { "tip ${formatMoney(it, currency)}" }
                    )
                    if (parts.isNotEmpty()) {
                        Text(
                            parts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Button(onClick = onSplit, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    detail.shares.isEmpty() -> "Split receipt"
                    detail.isSplitOutdated -> "Update split"
                    else -> "Edit split"
                }
            )
        }
    }
}

@Composable
private fun ReceiptPhoto(path: String, onOpen: () -> Unit) {
    val image by rememberReceiptImage(path, maxDimension = THUMBNAIL_DIMENSION)
    image?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = "Receipt photo, tap to enlarge",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(220.dp).clickable(onClick = onOpen)
        )
    }
}

@Composable
private fun FullScreenPhoto(path: String, onClose: () -> Unit) {
    val image by rememberReceiptImage(path, maxDimension = FULL_DIMENSION)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            image?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Receipt photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: CircularProgressIndicator()
        }
    }
}

private const val THUMBNAIL_DIMENSION = 600
private const val FULL_DIMENSION = 2000