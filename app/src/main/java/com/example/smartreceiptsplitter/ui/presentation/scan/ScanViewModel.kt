package com.example.smartreceiptsplitter.ui.presentation.scan

import android.graphics.Camera
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class CameraPermissionStatus {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

@HiltViewModel
class ScanViewModel @Inject constructor() : ViewModel() {
    private val _permissionStatus = MutableStateFlow(CameraPermissionStatus.UNKNOWN)
    val permissionStatus: StateFlow<CameraPermissionStatus> = _permissionStatus.asStateFlow()

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

    fun onAlreadyGranted() {
        _permissionStatus.value = CameraPermissionStatus.GRANTED
    }
}