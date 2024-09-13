package com.example.documentreader

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject

data class OCRResult(
    val id: String = "Data tidak tersedia",
    val name: String = "Data tidak tersedia",
    val birthdate: String = "Data tidak tersedia",
    val address: String = "Data tidak tersedia",
    val rtRw: String = "Data tidak tersedia",
    val province: String = "Data tidak tersedia",
    val city: String = "Data tidak tersedia",
    val kecamatan: String = "Data tidak tersedia",
    val kelurahan: String = "Data tidak tersedia"
)

fun parseOCRResult(resultJsonString: String): OCRResult {
    return try {
        // Parse outer JSON
        val outerJson = JSONObject(resultJsonString)
        // Extract inner JSON string
        val innerJsonString = outerJson.optString("result", "{}")
        // Parse inner JSON
        val jsonResult = JSONObject(innerJsonString)

        OCRResult(
            id = jsonResult.optString("id", "Data tidak tersedia"),
            name = jsonResult.optString("name", "Data tidak tersedia"),
            birthdate = jsonResult.optString("birthdate", "Data tidak tersedia"),
            address = jsonResult.optString("address", "Data tidak tersedia"),
            rtRw = jsonResult.optString("RT/RW", "Data tidak tersedia"),
            province = jsonResult.optString("province", "Data tidak tersedia"),
            city = jsonResult.optString("city", "Data tidak tersedia"),
            kecamatan = jsonResult.optString("kecamatan", "Data tidak tersedia"),
            kelurahan = jsonResult.optString("kelurahan", "Data tidak tersedia")
        )
    } catch (e: Exception) {
        Log.e("ResultScreen", "Error parsing JSON", e)
        OCRResult() // Return default values in case of error
    }
}

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ResultScreen(intent = intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ResultScreen(intent: Intent) {
    val isKtp = intent.getBooleanExtra("IS_KTP", true)
    val resultJsonString = if (isKtp) {
        intent.getStringExtra("RESULT_JSON_KTP") ?: "{}"
    } else {
        intent.getStringExtra("RESULT_JSON_PASPOR") ?: "{}"
    }
    val imageUri = intent.getStringExtra("IMAGE_URI")?.let { Uri.parse(it) }

    // Parse the JSON result
    val ocrResult = parseOCRResult(resultJsonString)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Result") }
            )
        },
        content = {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                // Menampilkan gambar jika ada URI gambar
                imageUri?.let { uri ->
                    val imageBitmap = BitmapFactory.decodeStream(
                        LocalContext.current.contentResolver.openInputStream(uri)
                    )
                    imageBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(top = 16.dp, bottom = 16.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .graphicsLayer(rotationZ = -90f)
                        )
                    } ?: Log.d("ResultScreen", "Failed to decode image from URI: $uri")
                } ?: Log.d("ResultScreen", "No image URI provided")

                // Menampilkan informasi hasil OCR di dalam Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("ID: ${ocrResult.id}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Name: ${ocrResult.name}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Birthdate: ${ocrResult.birthdate}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Address: ${ocrResult.address}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("RT/RW: ${ocrResult.rtRw}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Province: ${ocrResult.province}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("City: ${ocrResult.city}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kecamatan: ${ocrResult.kecamatan}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kelurahan: ${ocrResult.kelurahan}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol konfirmasi
                Button(
                    onClick = {
                        // Kirim data ke cloud atau lakukan tindakan lainnya
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Home", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    )
}
