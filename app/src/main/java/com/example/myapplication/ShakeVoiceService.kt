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
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
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
    private var isProcessing = false  // Para saber si se está procesando la respuesta
    private lateinit var apiSender: ApiSender2

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

        apiSender = ApiSender2("http://192.168.220.92:5000") // IP de tu servidor Flask

        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)


        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "respuesta_final") {
                            if (isFaceDown && !isListening) {
                                handler.post {
                                    speechRecognizer.startListening(recognizerIntent)
                                    isListening = true
                                    isProcessing = false
                                }
                            }
                        }
                    }
                })
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                isProcessing = true
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.joinToString(" ") ?: ""

                // Paso 1: Decir "Enviando a la API" y esperar a que termine
                tts.speak("Realizando operacion", TextToSpeech.QUEUE_FLUSH, null, null)
                Thread {
                    while (tts.isSpeaking) {
                        Thread.sleep(100)
                    }

                    // Paso 2: Enviar a la API
                    val apiResponse = apiSender.sendPowerCommand(spokenText)

                    // 🛡Paso 3: Verificar si hubo error de conexión
                    if (apiResponse == null || apiResponse.contains("Error de conexión")) {
                        handler.post {
                            tts.speak("Error al conectar con el servidor", TextToSpeech.QUEUE_FLUSH, null, null)
                        }

                        return@Thread  //  Cortar este hilo, se reiniciará después
                    }

                    // Paso 4: Extraer y reproducir la respuesta
                    val respuesta = apiSender.extraerRespuestaTexto(apiResponse)

                    if (!respuesta.isNullOrBlank()) {
                        tts.speak(respuesta, TextToSpeech.QUEUE_FLUSH, null, "respuesta_final")
                    }else {
                        isProcessing = false   // si no hay respuesta reinicia igual
                    }
                }.start()

                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                Log.d("ShakeVoiceService", "Error en reconocimiento: $error")

                // Errores que indican que no hubo resultado o fallo temporal
                val reiniciables = listOf(
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_CLIENT
                )

                if (error in reiniciables && isFaceDown && !isProcessing) {
                    handler.postDelayed({
                        speechRecognizer.startListening(recognizerIntent)
                        isListening = true
                    }, 500)
                }
            }

            override fun onEndOfSpeech() { isListening = false }
            //override fun onError(error: Int) { isListening = false }
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
