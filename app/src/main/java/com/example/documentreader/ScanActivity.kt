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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val API_URL_KTP = "https://uploadktp.panorasnap.com/uploadKtp"
private const val API_URL_PASPOR = "https://uploadpaspor.panorasnap.com/uploadPaspor"

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
                    val userId = "12345" // Ganti dengan ID pengguna yang sesuai
                    val (responseKtp, responsePaspor) = sendImageToApis(file, userId)

                    Log.d("ScanScreen", "API responses: KTP: $responseKtp, Paspor: $responsePaspor")

                    // Kirim data ke ResultActivity jika ada respons dari salah satu API
                    if (responseKtp != null || responsePaspor != null) {
                        val intent = Intent(context, ResultActivity::class.java).apply {
                            putExtra("IS_KTP", responseKtp != null)
                            putExtra("IMAGE_URI", imgUri.toString())
                            putExtra("RESULT_JSON_KTP", responseKtp ?: "{}")
                            putExtra("RESULT_JSON_PASPOR", responsePaspor ?: "{}")
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
                            .border(2.dp, MaterialTheme.colorScheme.primary)
                            .background(Color.Transparent)
                            .padding(8.dp)
                            .border(2.dp, Color.White)  // Extra border for contrast
                    )
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)  // Align button to bottom center
                        .padding(16.dp)
                        .size(80.dp)
                        .background(Color.Transparent, shape = CircleShape)
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
                        painter = painterResource(id = R.drawable.focus), // Ganti dengan ID gambar yang sesuai
                        contentDescription = "Capture",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    )

    apiResponse?.let { response ->
        Text(
            text = response,
            modifier = Modifier.padding(16.dp),
            color = Color.Black
        )
    }
}


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
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Set aspect ratio
                .build()

            val imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Set aspect ratio
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

private suspend fun sendImageToApis(file: File, userId: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()

    // Fungsi untuk mengirim gambar ke satu API
    fun sendToApi(apiUrl: String): String {
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

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "No response from server"
            if (!response.isSuccessful) {
                Log.e("SendImageToApi", "Failed with status code: ${response.code}")
                "Error: ${response.code} - $responseBody"
            } else {
                Log.d("SendImageToApi", "Response Body: $responseBody")
                responseBody
            }
        } catch (e: Exception) {
            Log.e("SendImageToApi", "Error: ${e.message}")
            "Error: ${e.message}"
        }
    }

    // Mengirim gambar ke kedua API
    val responseKtp = sendToApi(API_URL_KTP)
    val responsePaspor = sendToApi(API_URL_PASPOR)

    return@withContext Pair(responseKtp, responsePaspor)

}




