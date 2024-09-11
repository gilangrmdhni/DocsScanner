package com.example.documentreader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocumentReaderApp()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentReaderApp() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Document Reader") })
        },
        content = {
            MainScreen()
        }
    )
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            // Navigasi ke ScanActivity dengan jenis dokumen KTP
            startScanActivity(context, "ID")
        }) {
            Text("Scan KTP")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Navigasi ke ScanActivity dengan jenis dokumen PASPOR
            startScanActivity(context, "PASSPORT")
        }) {
            Text("Scan PASPOR")
        }
    }
}

private fun startScanActivity(context: Context, documentType: String) {
    val intent = Intent(context, ScanActivity::class.java).apply {
        putExtra("DOCUMENT_TYPE", documentType)
    }
    context.startActivity(intent)
}
