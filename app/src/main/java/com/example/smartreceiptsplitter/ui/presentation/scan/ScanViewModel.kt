package com.example.smartreceiptsplitter.ui.presentation.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartreceiptsplitter.data.local.enums.CameraPermissionStatus
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.domain.usecase.ocr.RecognizeReceiptTextUseCase
import com.example.smartreceiptsplitter.domain.usecase.scan.CorrectPerspectiveUseCase
import com.example.smartreceiptsplitter.domain.usecase.scan.DetectDocumentEdgesUseCase
import com.example.smartreceiptsplitter.domain.usecase.scan.SaveCorrectedReceiptImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


sealed interface CaptureState {
    data object Idle : CaptureState //live preview, ready for the shutter
    data object Processing : CaptureState // capture just happened, detection + correction running
    data class Review(val correctedBitmap: Bitmap, val isConfident: Boolean) : CaptureState // corrected image ready for the user to accept or discard
    data object Saving : CaptureState // User confirmed, compressing + writing to disk
    //data class Saved(val file: File) : CaptureState //written to disk
    data class Recognizing(val file: File): CaptureState //image is on disk, on-device OCR is running
    data class Recognized(val file: File, val text: RecognizedText): CaptureState //OCR finished, raw text blocks are ready
    data class RecognitionFailed(val file: File, val message: String): CaptureState // OCR failed, image is still saved so the user can enter data by hand
    data class Error(val message: String) : CaptureState // camera, decode, processing or save error; retry possible
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val detectDocumentEdges: DetectDocumentEdgesUseCase,
    private val correctPerspective: CorrectPerspectiveUseCase,
    private val saveCorrectedImage : SaveCorrectedReceiptImageUseCase,
    private val recognizeReceiptText: RecognizeReceiptTextUseCase
) : ViewModel() {
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

    fun onPhotoCaptured(rawPhoto: Bitmap) {
        _captureState.value = CaptureState.Processing
        viewModelScope.launch {
            runCatching {
                val corners = detectDocumentEdges(rawPhoto)
                val corrected = correctPerspective(rawPhoto, corners)
                corrected to corners.isConfident
            }.fold(
                onSuccess = { (corrected, isConfident) ->
                    _captureState.value = CaptureState.Review(corrected, isConfident)
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

    fun onConfirm() {
        val reviewState = _captureState.value as? CaptureState.Review ?: return
        val corrected = reviewState.correctedBitmap
        _captureState.value = CaptureState.Saving
        viewModelScope.launch {
            val file = runCatching { saveCorrectedImage(corrected) }.getOrElse { error ->
                _captureState.value = CaptureState.Error(error.message ?: "Image saving failed.")
                return@launch
            }
            _captureState.value = CaptureState.Recognizing(file)
            runCatching { recognizeReceiptText(corrected) }.fold(
                onSuccess = { text -> _captureState.value = CaptureState.Recognized(file, text) },
                onFailure = { error -> _captureState.value = CaptureState.RecognitionFailed(file, error.message ?: "Text recognition failed.") }
            )
        }
    }

    // Scan result was handed over to the receipt form; back on this screen the camera is ready again
    fun onScanHandedOff() {
        _captureState.value = CaptureState.Idle
    }

    fun onRetake() {
        _captureState.value = CaptureState.Idle
    }

    fun onAlreadyGranted() {
        _permissionStatus.value = CameraPermissionStatus.GRANTED
    }
}