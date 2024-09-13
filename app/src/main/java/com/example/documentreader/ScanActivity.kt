package com.example.documentreader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Tasks
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.material3.*

// Constants
private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val API_URL_KTP = "https://uploadktp.panorasnap.com/uploadKtp"
private const val API_URL_PASPOR = "https://uploadpaspor.panorasnap.com/uploadPaspor"

// ScanActivity class
class ScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanScreen()
        }
    }
}

// ScanScreen Composable function
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var apiResponse by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val outputDirectory = context.cacheDir
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                Log.d("ScanScreen", "Permission granted")
            } else {
                Log.e("ScanScreen", "Permission denied")
                Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("ScanScreen", "Permission already granted, initializing camera")
        } else {
            Log.d("ScanScreen", "Requesting camera permission")
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    imageUri?.let { imgUri ->
        LaunchedEffect(imgUri) {
            coroutineScope.launch {
                isProcessing = true
                try {
                    val file = File(context.cacheDir, "temp_image.jpg")
                    context.contentResolver.openInputStream(imgUri)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    val documentType = detectDocumentType(imgUri, context)
                    val userId = "12345" // Replace with the actual user ID

                    val apiUrl = when (documentType) {
                        "KTP" -> API_URL_KTP
                        "PASPOR" -> API_URL_PASPOR
                        else -> null
                    }

                    val response = apiUrl?.let { sendImageToApi(it, file, userId) }

                    Log.d("ScanScreen", "API response: $response")

                    // Send data to ResultActivity if there's a response from the API
                    if (response != null) {
                        val intent = Intent(context, ResultActivity::class.java).apply {
                            putExtra("IS_KTP", documentType == "KTP")
                            putExtra("IMAGE_URI", imgUri.toString())
                            putExtra("RESULT_JSON", response) // Pastikan kunci yang digunakan konsisten
                        }
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Tidak ada respons dari API", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ScanScreen", "Error processing image", e)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
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
                        startCamera(cameraProviderFuture, lifecycleOwner, previewView) { capture ->
                            imageCapture = capture
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3 / 5f) // Ensure aspect ratio matches camera preview
                            .border(2.dp, Color(0xFF8AB4F8)) // Soft blue border
                            .background(Color.Transparent)
                            .padding(8.dp)
                            .border(2.dp, Color.White)  // Extra border for contrast
                            .shadow(8.dp) // Add shadow for better visibility
                    )
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp), // Larger indicator
                            color = Color(0xFF8AB4F8) // Soft blue color for progress indicator
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)  // Align button to bottom center
                        .padding(16.dp)
                        .size(60.dp)
                        .background(Color(0xFF8AB4F8), shape = CircleShape) // Soft blue background
                        .clickable {
                            if (!isProcessing) {
                                isProcessing = true
                                coroutineScope.launch {
                                    imageCapture?.let { capture ->
                                        takePhoto(
                                            capture,
                                            outputDirectory,
                                            cameraExecutor,
                                            context
                                        ) { uri ->
                                            Log.d("ScanScreen", "Photo taken successfully: $uri")
                                            imageUri = uri
                                        }
                                    } ?: run {
                                        Log.e("ScanScreen", "Image capture not initialized")
                                        Toast.makeText(context, "Image capture not initialized", Toast.LENGTH_SHORT).show()
                                        isProcessing = false
                                    }
                                }
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.record), // Replace with Kotlin icon
                        contentDescription = "Capture",
                        modifier = Modifier.size(60.dp)
                        .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    )
}

// Start camera function
private fun startCamera(
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onImageCapture: (ImageCapture) -> Unit
) {
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Optimize for speed
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
            onImageCapture(imageCapture)
            Log.d("CameraPreview", "Camera initialized successfully")
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error starting camera", e)
            Toast.makeText(previewView.context, "Error starting camera", Toast.LENGTH_SHORT).show()
        }
    }, ContextCompat.getMainExecutor(previewView.context))
}

// Take photo function
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

// Create HTTP client function
private fun createHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Adjust as needed
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)   // Adjust as needed
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // Adjust as needed
        .build()
}

// Send image to API function
private suspend fun sendImageToApi(apiUrl: String, file: File, userId: String): String? = withContext(Dispatchers.IO) {
    val client = createHttpClient()
    val fileBody = file.asRequestBody("image/jpeg".toMediaType())
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("user_id", userId)
        .addFormDataPart("image", file.name, fileBody)
        .build()

    val request = Request.Builder()
        .url(apiUrl)
        .post(requestBody)
        .build()

    return@withContext try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        Log.d("SendImageToApi", "Status Code: ${response.code}")
        Log.d("SendImageToApi", "Response Body: $responseBody")
        if (!response.isSuccessful) {
            Log.e("SendImageToApi", "Failed with status code: ${response.code}")
            "Error: ${response.code} - ${response.message}"
        } else {
            responseBody ?: "No response from server"
        }
    } catch (e: Exception) {
        Log.e("SendImageToApi", "Error: ${e.message}")
        "Error: ${e.message}"
    }
}

// Detect document type function
private suspend fun detectDocumentType(imageUri: Uri, context: Context): String = withContext(Dispatchers.IO) {
    val image = InputImage.fromFilePath(context, imageUri)
    val options = TextRecognizerOptions.Builder().build()
    val recognizer = TextRecognition.getClient(options)

    return@withContext try {
        val result = Tasks.await(recognizer.process(image))
        val detectedText = result.text

        // Log the detected text for debugging
        Log.d("DetectDocumentType", "Detected text: $detectedText")

        when {
            detectedText.contains("NIK", ignoreCase = true) -> "KTP"
            detectedText.contains("PASPOR", ignoreCase = true) -> "PASPOR"
            else -> "UNKNOWN"
        }
    } catch (e: Exception) {
        Log.e("DetectDocumentType", "Error detecting text", e)
        "UNKNOWN"
    }
}
