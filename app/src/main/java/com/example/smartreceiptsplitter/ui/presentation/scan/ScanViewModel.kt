package com.example.smartreceiptsplitter.ui.presentation.scan

import android.graphics.Bitmap
import android.graphics.Camera
import android.os.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class CameraPermissionStatus {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

sealed interface CaptureState {
    data object Idle : CaptureState //live preview, ready for the shutter
    data object Processing : CaptureState // capture just happened, detection + correction running
    data class Review(val correctedBitmap: Bitmap, val isConfident: Boolean) : CaptureState // corrected image ready for the user to accept or discard
    data object Saving : CaptureState // User confirmed, compresing + writing to disk
    data class Saved(val file: File) : CaptureState //written to disk
    data class Error(val message: String) : CaptureState // camera, decode, processing or save error; retry possible
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val detectDocumentEdges: DetectDocumentEdgesUseCase,
    private val correctPerspective: CorrectPerspectiveUseCase,
    private val saveCorrectedImage : SaveCorrectedReceiptImageUseCase
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
        _captureState.value = CaptureState.Saving
        viewModelScope.launch {
            runCatching { saveCorrectedImage(reviewState.correctedBitmap) }.fold(
                onSuccess = { file -> _captureState.value = CaptureState.Saved(file) },
                onFailure = { error ->
                    _captureState.value = CaptureState.Error(error.message ?: "Image saving failed.")
                }
            )
        }
    }

    fun onRetake() {
        _captureState.value = CaptureState.Idle
    }

    fun onAlreadyGranted() {
        _permissionStatus.value = CameraPermissionStatus.GRANTED
    }
}