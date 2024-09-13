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

// Data class for KTP result
data class OCRResultKTP(
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

// Data class for Passport result
data class OCRResultPassport(
    val fullName: String = "Data tidak tersedia",
    val documentNumber: String = "Data tidak tersedia",
    val birthDate: String = "Data tidak tersedia",
    val countryFullName: String = "Data tidak tersedia"
)

// Function to parse OCR result from JSON
fun parseOCRResult(resultJsonString: String, isKTP: Boolean): Any {
    return try {
        Log.d("parseOCRResult", "Parsing JSON: $resultJsonString")
        val outerJson = JSONObject(resultJsonString)
        val encodedInnerJsonString = outerJson.optString("result", "{}")
        val cleanedInnerJsonString = encodedInnerJsonString.replace("\\n", "").trim()
        Log.d("parseOCRResult", "Cleaned Inner JSON: $cleanedInnerJsonString")

        val innerJsonString = JSONObject(cleanedInnerJsonString)
        Log.d("parseOCRResult", "Decoded Inner JSON: $innerJsonString")

        if (isKTP) {
            OCRResultKTP(
                id = innerJsonString.optString("id", "Data tidak tersedia"),
                name = innerJsonString.optString("name", "Data tidak tersedia"),
                birthdate = innerJsonString.optString("birthdate", "Data tidak tersedia"),
                address = innerJsonString.optString("address", "Data tidak tersedia"),
                rtRw = innerJsonString.optString("RT/RW", "Data tidak tersedia"),
                province = innerJsonString.optString("province", "Data tidak tersedia"),
                city = innerJsonString.optString("city", "Data tidak tersedia"),
                kecamatan = innerJsonString.optString("kecamatan", "Data tidak tersedia"),
                kelurahan = innerJsonString.optString("kelurahan", "Data tidak tersedia")
            )
        } else {
            OCRResultPassport(
                fullName = innerJsonString.optString("fullName", "Data tidak tersedia"),
                documentNumber = innerJsonString.optString("documentNumber", "Data tidak tersedia"),
                birthDate = innerJsonString.optString("birthDate", "Data tidak tersedia"),
                countryFullName = innerJsonString.optString("countryFullName", "Data tidak tersedia")
            )
        }
    } catch (e: Exception) {
        Log.e("parseOCRResult", "Error parsing JSON", e)
        if (isKTP) OCRResultKTP() else OCRResultPassport()
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
    val resultJsonString = intent.getStringExtra("RESULT_JSON") ?: "{}" // Gunakan kunci yang sama
    val imageUri = intent.getStringExtra("IMAGE_URI")?.let { Uri.parse(it) }

    Log.d("ResultScreen", "Received JSON: $resultJsonString")

    val ocrResult = parseOCRResult(resultJsonString, isKtp)

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
                        if (isKtp) {
                            ocrResult as OCRResultKTP
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
                        } else {
                            ocrResult as OCRResultPassport
                            Text("Full Name: ${ocrResult.fullName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Document Number: ${ocrResult.documentNumber}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Birth Date: ${ocrResult.birthDate}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Country: ${ocrResult.countryFullName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Handle button click here, e.g., navigate back to home screen
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

