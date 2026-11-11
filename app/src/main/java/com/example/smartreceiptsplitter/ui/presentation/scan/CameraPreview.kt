package com.example.smartreceiptsplitter.ui.presentation.scan

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.smartreceiptsplitter.domain.usecase.scan.ReceiptEdgeAnalyzer
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"
/*
* Camera preview with the shutter's ImageCapture and, while showEdgeOverlay is true, a live outline of the receipt.
* Preview and ImageCapture keep CameraX's default 4:3 and the analysis asks for 4:3 as well, so the analyzed frame
* shows the same area as the preview and the outline lines up. Don't give the preview another aspect ratio alone.
* The analyzer runs on its own thread and is removed when the overlay is off (review, OCR and error screens).
*/
@Composable
fun CameraPreview(
    modifier:Modifier = Modifier,
    showEdgeOverlay: Boolean = false,
    onImageCaptureReady: (ImageCapture) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember(context){ PreviewView(context) }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(ResolutionStrategy(ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    var edges by remember { mutableStateOf<LiveReceiptEdges?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle( lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalysis)
                onImageCaptureReady(imageCapture)
            }
            catch (e: Exception) {
                //Binding can potentially fail if camera is already in use by another app, for now it will only be logged so it's not silently swallowed
                Log.e(TAG, "Failed to bind camera preview", e)
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(analysisExecutor) {
        onDispose { analysisExecutor.shutdown() }
    }

    DisposableEffect(showEdgeOverlay, imageAnalysis) {
        edges = null
        if(showEdgeOverlay) {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            imageAnalysis.setAnalyzer(analysisExecutor, ReceiptEdgeAnalyzer { found -> mainExecutor.execute { edges = found } })
        }
        onDispose {
            imageAnalysis.clearAnalyzer()
            edges = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        if(showEdgeOverlay) edges?.let { ReceiptOutline(it) }
    }
}

@Composable
private fun ReceiptOutline(edges: LiveReceiptEdges) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = edges.pointsInView(size.width, size.height)
        val path = Path().apply {
            points.forEachIndexed { index, (x, y) -> if (index == 0) moveTo(x, y) else lineTo(x, y)  }
            close()
        }
        drawPath(path, color = color.copy(alpha = 0.15f))
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
        points.forEach { (x, y) -> drawCircle(color, radius = 6.dp.toPx(), center = Offset(x, y)) }
    }
}

private val ANALYSIS_SIZE = Size(640, 480)