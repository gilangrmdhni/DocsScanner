package com.example.documentreader

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OCR Result") })
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("OCR Result", modifier = Modifier.padding(16.dp))

                if (isKTP) {
                    ktpData?.let {
                        // Display KTP data
                        Text("NIK: ${it.nik}")
                        Text("Nama: ${it.nama}")
                        Text("Tanggal Lahir: ${it.tanggalLahir}")
                        Text("Alamat Lengkap: ${it.alamatLengkap}")
                        Text("RT/RW: ${it.rtRw}")
                        Text("Provinsi: ${it.provinsi}")
                        Text("Kota/Kabupaten: ${it.kotaKabupaten}")
                        Text("Kecamatan: ${it.kecamatan}")
                        Text("Kelurahan: ${it.kelurahan}")
                    }
                } else {
                    passportData?.let {
                        // Display Passport data
                        Text("Nama: ${it.nama}")
                        Text("Tanggal Lahir: ${it.tanggalLahir}")
                        Text("Issued Date: ${it.issuedDate}")
                        Text("Expired Date: ${it.expiredDate}")
                        Text("Nomor Passport: ${it.nomorPassport}")
                        Text("Kode Negara: ${it.kodeNegara}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // Kirim data ke cloud
                }) {
                    Text("Confirm")
                }
            }
        }
    )
}
