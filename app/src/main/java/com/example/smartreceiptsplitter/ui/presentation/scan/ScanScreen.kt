package com.example.smartreceiptsplitter.ui.presentation.scan

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Camera
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.security.Permission
import java.util.jar.Manifest

@Composable
fun ScanScreen (
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    //
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

    Box(modifier = modifier.fillMaxSize()) {
        when(permissionStatus) {
            CameraPermissionStatus.GRANTED -> {
                CameraPreview(modifier = Modifier.fillMaxSize())
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

private fun android.content.Context.findActivity(): android.app.Activity {
    var ctx = this
    while(ctx is android.content.ContextWrapper) {
        if(ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("Permission request requires an Activity context")
}