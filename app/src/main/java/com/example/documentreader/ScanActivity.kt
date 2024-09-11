package com.example.documentreader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

class ScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanScreen()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val outputDirectory = context.cacheDir
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                startCamera(cameraProviderFuture, lifecycleOwner, context) { capture ->
                    imageCapture = capture
                }
            } else {
                Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera(cameraProviderFuture, lifecycleOwner, context) { capture ->
                imageCapture = capture
            }
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Menggunakan LaunchedEffect untuk menjalankan OCR setelah gambar diambil
    imageUri?.let { imgUri ->
        LaunchedEffect(imgUri) {
            processImage(context, imgUri) { result ->
                // Handle OCR result here (e.g., display it)
                Toast.makeText(context, "OCR Result: $result", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Scan Document") })
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        val preview = Preview.Builder().build()
                        var imageCapture = ImageCapture.Builder().build()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                                imageCapture = imageCapture // Set the imageCapture here
                            } catch (e: Exception) {
                                Log.e("CameraPreview", "Error starting camera", e)
                                Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
                            }
                        }, ContextCompat.getMainExecutor(context))
                        previewView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                Button(
                    onClick = {
                        imageCapture?.let { capture ->
                            takePhoto(
                                capture,
                                outputDirectory,
                                cameraExecutor,
                                context
                            ) { uri ->
                                imageUri = uri // Update imageUri state after capturing
                            }
                        } ?: run {
                            Toast.makeText(context, "Image capture not initialized", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Text("Capture")
                }
            }
        }
    )
}

private fun startCamera(
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    lifecycleOwner: LifecycleOwner,
    context: Context,
    onImageCapture: (ImageCapture) -> Unit
) {
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            Log.d("CameraPreview", "Binding to lifecycle")
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            Log.d("CameraPreview", "Camera bound to lifecycle")
            onImageCapture(imageCapture)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error: ${e.message}")
            Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun takePhoto(
    imageCapture: ImageCapture,
    outputDirectory: File,
    cameraExecutor: ExecutorService,
    context: Context,
    onImageCaptured: (Uri) -> Unit
) {
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Error taking photo", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private suspend fun processImage(context: Context, imageUri: Uri, onResult: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        // Create a TextRecognizer instance with options
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        if (bitmap != null) {
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    onResult(resultText)
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Failed to recognize text", e)
                    onResult("Error: ${e.message}")
                }
        } else {
            Log.e("OCR", "Failed to decode bitmap")
            onResult("Error: Failed to decode image")
        }
    }
}
