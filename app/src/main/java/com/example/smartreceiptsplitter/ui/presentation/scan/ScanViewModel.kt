package com.example.smartreceiptsplitter.ui.presentation.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.data.local.enums.CameraPermissionStatus
import com.example.smartreceiptsplitter.domain.model.DocumentCorner
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import com.example.smartreceiptsplitter.domain.model.ReceiptExtraction
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.domain.usecase.ocr.ExtractReceiptDataUseCase
import com.example.smartreceiptsplitter.domain.usecase.ocr.RecognizeReceiptTextUseCase
import com.example.smartreceiptsplitter.domain.usecase.scan.DeleteReceiptImageUseCase
import com.example.smartreceiptsplitter.domain.usecase.scan.DetectReceiptUseCase
import com.example.smartreceiptsplitter.domain.usecase.scan.ProcessCapturedReceiptUseCase
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.ReceiptFormInputs
import com.example.smartreceiptsplitter.ui.presentation.receiptsForm.toFormInputs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import javax.inject.Inject


sealed interface CaptureState {
    data object Idle : CaptureState //live preview, ready for the shutter
    data object Processing : CaptureState // capture just happened, detection + correction running
    // corrected image ready for the user to accept or discard
    data class Review(val photo: CapturedPhoto, val preview: Bitmap, val detected: DocumentCorners, val corners: DocumentCorners) : CaptureState {
        val isConfident get() = detected.isConfident
        val canUseCorners get() = corners.isUsableCrop()
    }
    data object Correcting : CaptureState // "Use photo" cropping to the corners and saving the image
    // data object Saving : CaptureState // User confirmed, compressing + writing to disk -- removed after refactoring
    data class Recognizing(val file: File): CaptureState //image is on disk, on-device OCR is running
    data class Recognized(val file: File, val text: RecognizedText, val extraction: ReceiptExtraction): CaptureState //OCR finished, raw text blocks are ready
    data class RecognitionFailed(val file: File, val message: String): CaptureState // OCR failed, image is still saved so the user can enter data by hand
    data class Error(val message: String) : CaptureState // camera, decode, processing or save error; retry possible
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val detectReceipt: DetectReceiptUseCase,
    private val processCapturedReceiptUseCase: ProcessCapturedReceiptUseCase,
    private val recognizeReceiptText: RecognizeReceiptTextUseCase,
    private val extractReceiptData: ExtractReceiptDataUseCase,
    private val deleteReceiptImage: DeleteReceiptImageUseCase
) : ViewModel() {
    private val clock = Clock.systemDefaultZone()
    private val _permissionStatus = MutableStateFlow(CameraPermissionStatus.UNKNOWN)
    val permissionStatus: StateFlow<CameraPermissionStatus> = _permissionStatus.asStateFlow()

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private var hasRequestedPermission = false

    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        /*Compose permission launcher callback with OS result and ActivityCompat for show request permission rationale for camera*/
        _permissionStatus.value = when {
            isGranted -> CameraPermissionStatus.GRANTED
            hasRequestedPermission && !shouldShowRationale -> CameraPermissionStatus.PERMANENTLY_DENIED //only reachable after at least one prior request when the system no longer offers a rationale (false value) (don't ask again, denied on a device)
            else -> CameraPermissionStatus.DENIED
        }
        hasRequestedPermission = true
    }

    fun onCaptureStarted() {
        if (_captureState.value == CaptureState.Idle) _captureState.value = CaptureState.Processing
    }

    fun onPhotoCaptured(photo: CapturedPhoto) {
        _captureState.value = CaptureState.Processing
        viewModelScope.launch {
            runCatching {
                val detected = detectReceipt(photo.jpeg, photo.rotationDegrees)
                val preview = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(detected.previewJpeg, 0, detected.previewJpeg.size)
                } ?: error("Unable to read the captured image.")
                CaptureState.Review(photo, preview, detected = detected.corners, corners = detected.corners)
            }.fold(
                onSuccess = {  review ->
                    _captureState.value = review
                },
                onFailure = { error ->
                    _captureState.value = CaptureState.Error(error.message ?: "Image processing failed.")
                }
            )
        }
    }

    fun onCaptureFailed(message: String) {
        _captureState.value = CaptureState.Error(message)
    }

    fun onCornerMoved(corner: DocumentCorner, point: NormalizedPoint) {
        val review = _captureState.value as? CaptureState.Review ?: return
        _captureState.value = review.copy(corners = review.corners.with(corner, point))
    }

    fun onResetCorners() {
        val review = _captureState.value as? CaptureState.Review ?: return
        _captureState.value = review.copy(corners = review.detected)
    }

    fun onConfirm() {
        val review = _captureState.value as? CaptureState.Review ?: return
        if (!review.canUseCorners) return
        _captureState.value = CaptureState.Correcting
        viewModelScope.launch {
            runCatching {
                processCapturedReceiptUseCase(review.photo.jpeg, review.photo.rotationDegrees, review.corners)
            }.fold(
                onSuccess = { path -> recognize(File(path)) },
                onFailure = { error -> _captureState.value = CaptureState.Error(error.message ?: "Saving the image failed.") }
            )
        }
    }

    private fun recognize(file: File) {
        _captureState.value = CaptureState.Recognizing(file)
        viewModelScope.launch {
            runCatching {
                val text = recognizeReceiptText(file.path)
                text to withContext(Dispatchers.Default) { extractReceiptData(text, clock) }
            }.fold(
                onSuccess = { (text, extraction) -> _captureState.value = CaptureState.Recognized(file, text, extraction)},
                onFailure = { error -> _captureState.value = CaptureState.RecognitionFailed(file, error.message ?: "Text recognition failed.") }
            )
        }
    }

    // Scan result was handed over to the receipt form; back on this screen the camera is ready again
    fun continueToForm(): Pair<String, ReceiptFormInputs>? {
        val (file, extraction) = when (val state = _captureState.value) {
            is CaptureState.Recognized -> state.file to state.extraction
            is CaptureState.RecognitionFailed -> state.file to null
            else -> return null
        }
        _captureState.value = CaptureState.Idle
        val inputs = extraction
            ?.toDraft(clock.zone, ReceiptFormInputs.DEFAULT_CURRENCY)
            ?.toFormInputs(clock.zone)
            ?: ReceiptFormInputs()
        return file.absolutePath to inputs
    }

    fun onRetake() {
        // the review has only the photo in memory, a file exists from the "Use photo" on
        val unusedImage = when (val state = _captureState.value) {
            is CaptureState.Recognized -> state.file
            is CaptureState.RecognitionFailed -> state.file
            else -> null
        }
        _captureState.value = CaptureState.Idle
        unusedImage?.let { file -> viewModelScope.launch { deleteReceiptImage(file.path) } }
    }

    fun onAlreadyGranted() {
        _permissionStatus.value = CameraPermissionStatus.GRANTED
    }
}