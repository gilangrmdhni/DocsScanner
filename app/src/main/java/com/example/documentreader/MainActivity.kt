package com.example.documentreader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
            TopAppBar(
                title = { Text("Document Reader", fontSize = 20.sp) },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
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
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // QR Code Image
        val qrCodeImage: Painter = painterResource(id = R.drawable.qr_code) // Replace with your QR code image resource
        Image(
            painter = qrCodeImage,
            contentDescription = "QR Code",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Scanned",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .align(Alignment.CenterHorizontally) // Ini memastikan teks diatur ke tengah
        )

        ButtonWithIcon(
            text = "Scan KTP",
            icon = Icons.Filled.AccountBox,
            color = MaterialTheme.colorScheme.primary,
            onClick = { startScanActivity(context, "ID") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ButtonWithIcon(
            text = "Scan PASPOR",
            icon = Icons.Filled.AccountBox,
            color = MaterialTheme.colorScheme.secondary,
            onClick = { startScanActivity(context, "PASSPORT") }
        )
    }
}

@Composable
fun ButtonWithIcon(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(color.copy(alpha = 0.1f)),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.White, fontSize = 16.sp)
    }
}

private fun startScanActivity(context: Context, documentType: String) {
    val intent = Intent(context, ScanActivity::class.java).apply {
        putExtra("DOCUMENT_TYPE", documentType)
    }
    context.startActivity(intent)
}
