package com.example.smartreceiptsplitter.ui.presentation.scan

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.ViewModelStoreOwner
import java.io.File
import java.security.Permission
import java.util.jar.Manifest

@Composable
fun ScanScreen (
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
    onReceiptScanned: (File) -> Unit = {}
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

    LaunchedEffect(captureState) {
        val saved = captureState as? CaptureState.Saved ?: return@LaunchedEffect
        onReceiptScanned(saved.file)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when(permissionStatus) {
            CameraPermissionStatus.GRANTED -> {
                ScanCaptureFlow(
                    captureState = captureState,
                    onPhotoCaptured = viewModel::onPhotoCaptured,
                    onCaptureFailed = viewModel::onCaptureFailed,
                    onConfirm = viewModel::onConfirm,
                    onRetake = viewModel::onRetake
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
    }
}
@Composable
private fun ScanCaptureFlow(
    captureState: CaptureState,
    onPhotoCaptured: (Bitmap) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
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

            is CaptureState.Saved -> SavedOverlay(onScanAnother = onRetake)

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