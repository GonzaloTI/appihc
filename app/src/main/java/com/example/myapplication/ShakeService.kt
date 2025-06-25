package com.example.myapplication

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
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var shakeCount = 0
    private var lastShakeTime: Long = 0

    override fun onCreate() {
        super.onCreate()

        // === Crear canal de notificación ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "shake_channel",
                "Shake Detection",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // === Crear notificación foreground ===
        val notification = NotificationCompat.Builder(this, "shake_channel")
            .setContentTitle("Detector de sacudidas activo")
            .setContentText("Esperando movimiento...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        // === Configurar sensor ===
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            if (gForce > 2.7) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 800) {
                    lastShakeTime = now
                    shakeCount++

                    if (shakeCount >= 3) {
                        shakeCount = 0
                        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
