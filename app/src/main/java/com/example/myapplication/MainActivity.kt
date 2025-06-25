package com.example.myapplication

import ShakeService2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var apiSender = ApiSender("http://192.168.0.10:5000") // IP de tu servidor Flask


        /*val serviceIntent = Intent(this, ShakeService3::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        */
        val serviceIntent = Intent(this, ShakeVoiceService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)



        val testTone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        testTone.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("IHC")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hellos $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("IHC")
    }
}