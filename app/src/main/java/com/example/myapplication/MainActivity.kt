
package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var serviceIntent: Intent

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            iniciarServicio()
        } else {
            println("❌ Permiso de micrófono denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceIntent = Intent(this, ShakeVoiceService::class.java)

        // Verifica permiso de micrófono
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarServicio()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Tonito inicial opcional
        val testTone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        testTone.startTone(ToneGenerator.TONE_PROP_BEEP, 500)

        // Composable UI
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppUI(
                        onStartService = { iniciarServicio() },
                        onStopService = { detenerServicio() }
                    )
                }
            }
        }
    }

    private fun iniciarServicio() {
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun detenerServicio() {
        stopService(serviceIntent)
    }
}

@Composable
fun AppUI(
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var isRunning by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = """
                📱 Instrucciones de uso:

                1. Coloca el dispositivo boca abajo.
                2. Sonará un pitido cada segundo.
                3. Habla el comando deseado.
                4. El dispositivo responderá por voz.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                if (isRunning) {
                    onStopService()
                } else {
                    onStartService()
                }
                isRunning = !isRunning
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color.Red else Color.Green,
                contentColor = Color.White
            ),
            modifier = Modifier
                .width(250.dp)
                .height(60.dp)
        ) {
            Text(
                text = if (isRunning) "Detener Servicio" else "Iniciar Servicio"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppUI() {
    MyApplicationTheme {
        AppUI(onStartService = {}, onStopService = {})
    }
}

