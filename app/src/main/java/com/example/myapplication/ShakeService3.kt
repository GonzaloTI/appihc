package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.myapplication.R

class ShakeService3 : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var isFaceDown = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var toneGen: ToneGenerator

    private val beepingRunnable = object : Runnable {
        override fun run() {
            if (isFaceDown) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()


        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "shake_service_channel"
        val channelName = "Shake Detection Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Servicio activo")
            .setContentText("Detectando si el teléfono está boca abajo.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val z = it.values[2]

            if (z < -9 && !isFaceDown) {
                // Se acaba de poner boca abajo
                isFaceDown = true
                handler.post(beepingRunnable)
            } else if (z > 0 && isFaceDown) {
                // Ya no está boca abajo
                isFaceDown = false
                handler.removeCallbacks(beepingRunnable)
            } else {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(beepingRunnable)
        toneGen.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
