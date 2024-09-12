package com.example.documentreader

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


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
    val isKTP = intent.getBooleanExtra("IS_KTP", true)
    val ktpData = intent.getParcelableExtra<KTPData>("KTP_DATA")
    val passportData = intent.getParcelableExtra<PassportData>("PASSPORT_DATA")
    val imageUri = intent.getStringExtra("IMAGE_URI") // Ambil URI gambar

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Result") }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                // Menampilkan informasi sesuai dengan jenis dokumen (KTP atau Passport)
                if (isKTP) {
                    ktpData?.let {
                        Text("NIK: ${it.nik}", style = MaterialTheme.typography.bodyLarge)
                        Text("Nama: ${it.nama}", style = MaterialTheme.typography.bodyLarge)
                        Text("Tanggal Lahir: ${it.tanggalLahir}", style = MaterialTheme.typography.bodyLarge)
                        Text("Alamat Lengkap: ${it.alamatLengkap}", style = MaterialTheme.typography.bodyLarge)
                        Text("RT/RW: ${it.rtRw}", style = MaterialTheme.typography.bodyLarge)
                        Text("Provinsi: ${it.provinsi}", style = MaterialTheme.typography.bodyLarge)
                        Text("Kota/Kabupaten: ${it.kotaKabupaten}", style = MaterialTheme.typography.bodyLarge)
                        Text("Kecamatan: ${it.kecamatan}", style = MaterialTheme.typography.bodyLarge)
                        Text("Kelurahan: ${it.kelurahan}", style = MaterialTheme.typography.bodyLarge)
                    } ?: run {
                        Text("Data KTP tidak tersedia", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    passportData?.let {
                        Text("Nama: ${it.nama}", style = MaterialTheme.typography.bodyLarge)
                        Text("Tanggal Lahir: ${it.tanggalLahir}", style = MaterialTheme.typography.bodyLarge)
                        Text("Issued Date: ${it.issuedDate}", style = MaterialTheme.typography.bodyLarge)
                        Text("Expired Date: ${it.expiredDate}", style = MaterialTheme.typography.bodyLarge)
                        Text("Nomor Passport: ${it.nomorPassport}", style = MaterialTheme.typography.bodyLarge)
                        Text("Kode Negara: ${it.kodeNegara}", style = MaterialTheme.typography.bodyLarge)
                    } ?: run {
                        Text("Data Passport tidak tersedia", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Menampilkan gambar jika ada URI gambar
                imageUri?.let { uri ->
                    val imageBitmap = BitmapFactory.decodeStream(
                        LocalContext.current.contentResolver.openInputStream(Uri.parse(uri))
                    )
                    imageBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(top = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol konfirmasi untuk mengirim data ke cloud
                Button(
                    onClick = {
                        // Kirim data ke cloud
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Konfirmasi")
                }
            }
        }
    )
}




