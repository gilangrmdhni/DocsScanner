package com.example.documentreader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
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
            Log.d("ScanScreen", "Image captured: $imgUri")
            try {
                val resultText = processImage(context, imgUri)
                Log.d("ScanScreen", "OCR result: $resultText")
                val isKTP = determineIfKTP(resultText)
                val data = extractData(resultText, isKTP)

                val intent = Intent(context, ResultActivity::class.java).apply {
                    putExtra("IS_KTP", isKTP)
                    putExtra("IMAGE_URI", imgUri.toString())
                    if (isKTP) {
                        putExtra("KTP_DATA", data as KTPData)
                    } else {
                        putExtra("PASSPORT_DATA", data as PassportData)
                    }
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("ScanScreen", "Error processing image", e)
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
                            .size(width = 300.dp, height = 188.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary)
                            .background(Color.Transparent)
                            .padding(8.dp)
                            .border(2.dp, Color.White)  // Garis tambahan untuk kontras
                    )
                }

                Button(
                    onClick = {
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
    previewView: PreviewView,
    onImageCapture: (ImageCapture) -> Unit
) {
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            // Configure preview and image capture
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
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

private suspend fun processImage(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
    val bitmap = BitmapFactory.decodeStream(inputStream)

    // Langsung menggunakan bitmap tanpa preprocess
    val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
    val result = CompletableDeferred<String>()

    textRecognizer.process(image)
        .addOnSuccessListener { visionText ->
            val cleanedText = cleanOCRText(visionText.text)
            result.complete(cleanedText)
        }
        .addOnFailureListener { e ->
            Log.e("OCR", "Failed to recognize text", e)
            result.complete("Error: ${e.message}")
        }

    result.await()
}

private fun cleanOCRText(resultText: String): String {
    return resultText
        .replace(Regex("\\s+"), " ")  // Replace multiple spaces with single space
        .trim()  // Remove leading and trailing spaces
}

private fun determineIfKTP(resultText: String): Boolean {
    return resultText.contains("NIK") && resultText.contains("Alamat")
}

private fun extractData(resultText: String, isKTP: Boolean): Any {
    val nik = extractField(resultText, "NIK")
    val nama = extractField(resultText, "Nama")
    val tanggalLahir = extractField(resultText, "Tanggal Lahir")
    val alamatLengkap = extractField(resultText, "Alamat Lengkap")
    val rtRw = extractField(resultText, "RT/RW")
    val provinsi = extractField(resultText, "Provinsi")
    val kotaKabupaten = extractField(resultText, "Kota/Kabupaten")
    val kecamatan = extractField(resultText, "Kecamatan")
    val kelurahan = extractField(resultText, "Kelurahan")

    return if (isKTP) {
        KTPData(
            nik = nik,
            nama = nama,
            tanggalLahir = tanggalLahir,
            alamatLengkap = alamatLengkap,
            rtRw = rtRw,
            provinsi = provinsi,
            kotaKabupaten = kotaKabupaten,
            kecamatan = kecamatan,
            kelurahan = kelurahan
        )
    } else {
        PassportData(
            nama = nama,
            tanggalLahir = tanggalLahir,
            issuedDate = extractField(resultText, "Issued Date"),
            expiredDate = extractField(resultText, "Expired Date"),
            nomorPassport = extractField(resultText, "Nomor Passport"),
            kodeNegara = extractField(resultText, "Kode Negara")
        )
    }
}

private fun extractField(resultText: String, fieldName: String): String {
    val regex = Regex("$fieldName[:\\s]*(\\d+|.*)", RegexOption.IGNORE_CASE) // Sesuaikan pola regex
    val match = regex.find(resultText)
    return match?.groups?.get(1)?.value?.trim() ?: "Not found"
}
