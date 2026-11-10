package com.example.smartreceiptsplitter.ui.presentation.scan

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
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
import com.example.smartreceiptsplitter.domain.model.DocumentCorner
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import com.example.smartreceiptsplitter.domain.model.ParsedReceiptItem
import com.example.smartreceiptsplitter.domain.model.ReceiptExtraction
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.ReceiptFormInputs
import com.example.smartreceiptsplitter.ui.util.formatMoney
import com.example.smartreceiptsplitter.ui.util.formatQuantity
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
    onScanCompleted: (imagePath: String, inputs: ReceiptFormInputs) -> Unit = { _, _ -> },
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
                val granted = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onAlreadyGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                context.findActivity(), android.Manifest.permission.CAMERA
            )
            viewModel.onPermissionResult(isGranted, shouldShowRationale)
        }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        when {
            alreadyGranted -> viewModel.onAlreadyGranted()
            //only asking the first time, after that a refusal the rationale screen has its own button
            // coming from History or the form doesn't open the system dialog again
            viewModel.permissionStatus.value == CameraPermissionStatus.UNKNOWN -> permissionLauncher.launch(
                android.Manifest.permission.CAMERA
            )
        }
    }

    BackHandler(enabled = permissionStatus == CameraPermissionStatus.GRANTED && captureState != CaptureState.Idle) {
        when (captureState) {
            is CaptureState.Review, is CaptureState.Recognized, is CaptureState.RecognitionFailed, is CaptureState.Error -> viewModel.onRetake()

            CaptureState.Processing, CaptureState.Correcting, is CaptureState.Recognizing, CaptureState.Idle -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (permissionStatus) {
            CameraPermissionStatus.GRANTED -> {
                ScanCaptureFlow(
                    captureState = captureState,
                    onCaptureStarted = viewModel::onCaptureStarted,
                    onPhotoCaptured = viewModel::onPhotoCaptured,
                    onCaptureFailed = viewModel::onCaptureFailed,
                    onConfirm = viewModel::onConfirm,
                    onRetake = viewModel::onRetake,
                    onCornerMoved = viewModel::onCornerMoved,
                    onResetCorners = viewModel::onResetCorners,
                    onContinue = {
                        viewModel.continueToForm()
                            ?.let { (imagePath, inputs) -> onScanCompleted(imagePath, inputs) }
                    })
            }

            CameraPermissionStatus.DENIED -> {
                PermissionRationale(
                    message = "The camera is required to scan receipts. Grant access so you can take photos of receipts.",
                    buttonLabel = "Allow camera access",
                    onButtonClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) })
            }

            CameraPermissionStatus.PERMANENTLY_DENIED -> {
                PermissionRationale(
                    message = "Access to the camera is permanently denied. Enable it in the application settings so that it can scan receipts.",
                    buttonLabel = "Open settings",
                    onButtonClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    })
            }

            CameraPermissionStatus.UNKNOWN -> {
                //Brief window before LaunchedEffect resolves; intentionally blank rather than flashing a rationale the user hasn't been asked for yet
            }
        }
        if (permissionStatus != CameraPermissionStatus.GRANTED || captureState == CaptureState.Idle) {
            FilledTonalButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) { Text("History") }
            FilledTonalButton(
                onClick = onManualEntry,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) { Text("Enter manually") }
        }
    }
}

@Composable
private fun ScanCaptureFlow(
    captureState: CaptureState,
    onCaptureStarted: () -> Unit,
    onPhotoCaptured: (CapturedPhoto) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onCornerMoved: (DocumentCorner, NormalizedPoint) -> Unit,
    onResetCorners: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraActive = captureState == CaptureState.Idle || captureState == CaptureState.Processing
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraActive) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                showEdgeOverlay = captureState == CaptureState.Idle,
                onImageCaptureReady = { imageCapture = it })
        }
        when (captureState) {
            CaptureState.Idle -> {
                imageCapture?.let { controller ->
                    ShutterButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp),
                        onClick = {
                            controller.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val photo = try {
                                            image.toCapturedPhoto()
                                        } catch (t: Throwable) {
                                            onCaptureFailed(t.message ?: "Image reading failed.")
                                            return
                                        } finally {
                                            image.close()
                                        }
                                        onPhotoCaptured(photo)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        onCaptureFailed(
                                            exception.message ?: "Image shooting failed."
                                        )
                                    }
                                })
                        })
                }
            }

            CaptureState.Processing -> LoadingOverlay("Processing image...")

            is CaptureState.Review -> ReviewOverlay(
                state = captureState,
                onCornerMoved = onCornerMoved,
                onResetCorners = onResetCorners,
                onConfirm = onConfirm,
                onRetake = onRetake
            )

            CaptureState.Correcting -> LoadingOverlay("Cropping receipt...")

            is CaptureState.Recognizing -> LoadingOverlay("Reading receipt text...")

            is CaptureState.Recognized -> ExtractionReviewOverlay(
                text = captureState.text,
                extraction = captureState.extraction,
                onContinue = onContinue,
                onRetake = onRetake
            )

            is CaptureState.RecognitionFailed -> RecognitionFailedOverlay(
                message = captureState.message, onEnterManually = onContinue, onRetake = onRetake
            )

            is CaptureState.Error -> ErrorOverlay(
                message = captureState.message, onRetry = onRetake
            )
        }
    }
}

@Composable
private fun ShutterButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.size(72.dp), shape = CircleShape, color = Color.White
    ) { }
}

@Composable
private fun LoadingOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
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
    onCornerMoved: (DocumentCorner, NormalizedPoint) -> Unit,
    onResetCorners: () -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CropEditor(
            preview = state.preview,
            corners = state.corners,
            onCornerMoved = onCornerMoved,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Text(
            text = when {
                !state.canUseCorners -> "The corners don't outline a receipt. Move them to the receipt's four corners."
                !state.isConfident -> "Receipt's edges were not found. Drag the corners to the receipt's edges."
                else -> "Drag the corners if the outline doesn't match the receipt."
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.canUseCorners) Color.White else MaterialTheme.colorScheme.error
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onRetake) { Text("Retake") }
            TextButton(
                onClick = onResetCorners, enabled = state.corners != state.detected
            ) { Text("Reset") }
            Button(onClick = onConfirm, enabled = state.canUseCorners) { Text("Use photo") }
        }
    }
}


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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text("Scan result", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Fields that couldn't be read are left empty. You can correct everything on the next screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (text.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No text was recognized on this receipt. Retake the photo or continue and enter the data by hand.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ExtractedField("Merchant", extraction.merchantName)
                                ExtractedField(
                                    "Total",
                                    extraction.totalAmount?.let { formatMoney(it, currency) })
                                ExtractedField("Date", extraction.date?.format(REVIEW_DATE_FORMAT))
                                ExtractedField(
                                    "VAT / tax",
                                    extraction.taxAmount?.let { formatMoney(it, currency) })
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
                                text = "Items add up to ${
                                    formatMoney(
                                        extraction.itemsTotal, currency
                                    )
                                }, but the total is " + "${
                                    formatMoney(
                                        extraction.totalAmount ?: 0L, currency
                                    )
                                }. Some items may be missing, misread or discounted.",
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
                            onToggle = { showRawText = !showRawText })
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
    message: String, onEnterManually: () -> Unit, onRetake: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(message, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun PermissionRationale(
    message: String, buttonLabel: String, onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onButtonClick) {
            Text(buttonLabel)
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
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
private fun ExtractedItemRow(item: ParsedReceiptItem, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            if (item.unitPrice != null) {
                Text(
                    text = "${formatQuantity(item.quantity)} × ${
                        formatMoney(
                            item.unitPrice, currency
                        )
                    }",
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onToggle) { Text(if (expanded) "Hide recognized text" else "Show recognized text") }
            TextButton(onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                "Receipt OCR", fullText
                            )
                        )
                    )
                }
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

private val REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")