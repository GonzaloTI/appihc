package com.example.myapplication

import android.app.*
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import java.util.*

class ShakeVoiceService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var isFaceDown = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var toneGen: ToneGenerator
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech
    private var isListening = false

    private val beepingRunnable = object : Runnable {
        override fun run() {
            if (isFaceDown) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        var apiSender = ApiSender("http://192.168.0.10:5000") // IP de tu servidor Flask

        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.joinToString(" ") ?: ""

                // reproducir el texto capturado
                tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null)

                // Enviar a API
                apiSender.sendPowerCommand(spokenText)  // uso de la api para prender el foco

                isListening = false
            }

            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "shake_voice_channel"
        val channelName = "Shake Voice Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Servicio activo")
            .setContentText("Escuchando si está boca abajo y activando reconocimiento de voz.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val z = event?.values?.get(2) ?: return

        if (z < -9 && !isFaceDown) {
            isFaceDown = true
            handler.post(beepingRunnable)
            if (!isListening) {
                speechRecognizer.startListening(recognizerIntent)
                isListening = true
            }
        } else if (z > 0 && isFaceDown) {
            isFaceDown = false
            handler.removeCallbacks(beepingRunnable)
            if (isListening) {
                speechRecognizer.stopListening()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(beepingRunnable)
        toneGen.release()
        speechRecognizer.destroy()
        tts.shutdown()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
