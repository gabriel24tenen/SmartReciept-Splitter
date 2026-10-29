package com.example.smartreceiptsplitter.ui.presentation.scan

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.smartreceiptsplitter.data.local.enums.CameraPermissionStatus
import com.example.smartreceiptsplitter.domain.model.ParsedReceiptItem
import com.example.smartreceiptsplitter.domain.model.ReceiptExtraction
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.ReceiptFormInputs
import com.example.smartreceiptsplitter.ui.util.formatMoney
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun ScanScreen (
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
    onScanCompleted: (imagePath: String, inputs: ReceiptFormInputs) -> Unit = {_, _ ->},
    onManualEntry: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val captureState by viewModel.captureState.collectAsState()


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onAlreadyGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult( contract = ActivityResultContracts.RequestPermission() ) { isGranted ->
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context.findActivity(), android.Manifest.permission.CAMERA
        )
        viewModel.onPermissionResult(isGranted, shouldShowRationale)
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission( context, android.Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED

        if(alreadyGranted) {
            viewModel.onAlreadyGranted()
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    BackHandler(enabled = permissionStatus == CameraPermissionStatus.GRANTED && captureState != CaptureState.Idle) {
        when (captureState) {
            is CaptureState.Review,
            is CaptureState.Recognized,
            is CaptureState.RecognitionFailed,
            is CaptureState.Error -> viewModel.onRetake()

            CaptureState.Processing,
            CaptureState.Saving,
            is CaptureState.Recognizing,
            CaptureState.Idle -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when(permissionStatus) {
            CameraPermissionStatus.GRANTED -> {
                ScanCaptureFlow(
                    captureState = captureState,
                    onPhotoCaptured = viewModel::onPhotoCaptured,
                    onCaptureFailed = viewModel::onCaptureFailed,
                    onConfirm = viewModel::onConfirm,
                    onRetake = viewModel::onRetake,
                    onContinue = {
                        viewModel.continueToForm()?.let { (imagePath, inputs) -> onScanCompleted(imagePath, inputs) }
                    }
                )
            }

            CameraPermissionStatus.DENIED -> {
                PermissionRationale(
                    message = "The camera is required to scan receipts. Grant access so you can take photos of receipts.",
                    buttonLabel = "Allow camera access.",
                    onButtonClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA)}
                )
            }

            CameraPermissionStatus.PERMANENTLY_DENIED -> {
                PermissionRationale(
                    message = "Access to the camera is permanently denied. Enable it in the application settings so that it can scan accounts.",
                    buttonLabel = "Open settings.",
                    onButtonClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            CameraPermissionStatus.UNKNOWN -> {
                //Brief window before LaunchedEffect resolves; intentionally blank rather than flashing a rationale the user hasn't been asked for yet
            }
        }
        if(permissionStatus != CameraPermissionStatus.GRANTED || captureState == CaptureState.Idle) {
            FilledTonalButton(
                onClick = onOpenHistory,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)
            ) { Text("History") }
            FilledTonalButton(
                onClick = onManualEntry,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
            ) { Text("Enter manually") }
        }
    }
}
@Composable
private fun ScanCaptureFlow(
    captureState: CaptureState,
    onPhotoCaptured: (Bitmap) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview( modifier = Modifier.fillMaxSize(), onImageCaptureReady = { imageCapture = it } )

        when(captureState) {
            CaptureState.Idle -> {
                imageCapture?.let { controller ->
                    ShutterButton(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                        onClick = {
                            controller.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = try {
                                        image.toRotateBitmap()
                                    } catch (t: Throwable) {
                                        image.close()
                                        onCaptureFailed( t.message ?: "Image decoding failed.")
                                        return
                                    }
                                    image.close()
                                    onPhotoCaptured(bitmap)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    onCaptureFailed( exception.message ?: "Image shooting failed.")
                                }
                            })
                        }
                    )
                }
            }

            CaptureState.Processing -> LoadingOverlay("Processing image...")

            is CaptureState.Review -> ReviewOverlay( state = captureState, onConfirm = onConfirm, onRetake = onRetake )

            CaptureState.Saving -> LoadingOverlay("Saving...")

            is CaptureState.Recognizing -> LoadingOverlay("Reading receipt text...")

            is CaptureState.Recognized -> ExtractionReviewOverlay(
                text = captureState.text,
                extraction = captureState.extraction,
                onContinue = onContinue,
                onRetake = onRetake
            )

            is CaptureState.RecognitionFailed -> RecognitionFailedOverlay(
                message = captureState.message,
                onEnterManually = onContinue,
                onRetake = onRetake
            )

            is CaptureState.Error -> ErrorOverlay( message = captureState.message, onRetry = onRetake)
        }
    }
}
@Composable
private fun ShutterButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        color = Color.White
    ) { }
}

@Composable
private fun LoadingOverlay(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, color = Color.White)
        }
    }
}

@Composable
private fun ReviewOverlay(
    state: CaptureState.Review,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = state.correctedBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentScale = ContentScale.Fit
        )
        if(!state.isConfident) {
            Text(
                text = "Receipt's edges are not safely recognized - check if picture is readable.",
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onRetake) { Text("Repeat shoot.") }
            Button(onClick = onConfirm) { Text("Accept.") }
        }
    }
}
/* NOT NEEDED ANYMORE AFTER TASK 2.1
@Composable
private fun SavedOverlay(onScanAnother: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Receipt saved.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onScanAnother) { Text("Scan new receipt") }
        }
    }
}
*/

//new, instead of RecognizedTextOverlay
@Composable
private fun ExtractionReviewOverlay(
    text: RecognizedText,
    extraction: ReceiptExtraction,
    onContinue: () -> Unit,
    onRetake: () -> Unit
) {
    val currency = ReceiptFormInputs.DEFAULT_CURRENCY
    var showRawText by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Text("Check scanned receipt", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Fields that couldn't be read are left empty. You can correct everything on the next screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (text.isEmpty) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No text was recognized on this receipt. Retake the photo or continue and enter the data by hand.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ExtractedField("Total", extraction.totalAmount?.let { formatMoney(it, currency) })
                                ExtractedField("Date", extraction.date?.format(REVIEW_DATE_FORMAT))
                                ExtractedField("VAT / tax", extraction.taxAmount?.let { formatMoney(it, currency) })
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Items (${extraction.items.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        if (extraction.itemsMatchTotal == false) {
                            Text(
                                text = "Items add up to ${formatMoney(extraction.itemsTotal, currency)}, but the total is " +
                                        "${formatMoney(extraction.totalAmount ?: 0L, currency)}. Some items may be missing, misread or discounted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (extraction.items.isEmpty()) {
                            Text(
                                text = "No items recognized.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(extraction.items) { item -> ExtractedItemRow(item, currency) }
                    item {
                        RawTextSection(
                            fullText = text.fullText,
                            expanded = showRawText,
                            onToggle = { showRawText = !showRawText }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRetake) { Text("Retake") }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onContinue) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun RecognitionFailedOverlay(
    message: String,
    onEnterManually: () -> Unit,
    onRetake: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Text could not be read.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetake) { Text("Retake") }
                Button(onClick = onEnterManually) { Text("Enter manually") }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text (message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again.") }
    }
}

@Composable
private fun PermissionRationale(
    message: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onButtonClick) {
            Text(buttonLabel)
        }
    }
}

private fun ImageProxy.toRotateBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Unable to decode captured image.")
    val rotationDegrees = imageInfo.rotationDegrees
    if(rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun android.content.Context.findActivity(): android.app.Activity {
    var ctx = this
    while(ctx is android.content.ContextWrapper) {
        if(ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("Permission request requires an Activity context")
}

@Composable
private fun ExtractedField(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value ?: "Not found",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (value != null) FontWeight.SemiBold else FontWeight.Normal,
            fontStyle = if (value == null) FontStyle.Italic else FontStyle.Normal,
            color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExtractedItemRow(item: ParsedReceiptItem, currency: String){
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            if (item.unitPrice != null) {
                Text(
                    text = "${formatQuantity(item.quantity)} × ${formatMoney(item.unitPrice, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(formatMoney(item.totalPrice, currency), style = MaterialTheme.typography.bodyMedium)
    }
}

// Raw OCR text stays available: useful when a field is missing, and "Copy text" turns real receipts into parser tests
@Composable
private fun RawTextSection(fullText: String, expanded: Boolean, onToggle: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onToggle) { Text(if (expanded) "Hide recognized text" else "Show recognized text") }
            TextButton(onClick = {
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Receipt OCR", fullText))) }
            }) { Text("Copy text") }
        }
        if (expanded) {
            Text(
                text = fullText,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatQuantity(quantity: Double): String =
    if(quantity % 1.0 == 0.0) quantity.toLong().toString()
    else BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString().replace('.', ',')

private val REVIEW_DATE_FORMAT= DateTimeFormatter.ofPattern("dd.MM.yyyy")