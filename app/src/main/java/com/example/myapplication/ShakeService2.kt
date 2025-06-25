
// ShakeService2.kt

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
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.R

class ShakeService2 : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var shakeCount = 0
    private var lastShakeTime: Long = 0

    override fun onCreate() {
        super.onCreate()

        startForegroundServiceWithNotification()

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
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Detección de movimiento activa")
            .setContentText("El servicio está escuchando sacudidas.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(1, notification)
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
            val z = it.values[2]

            val now = System.currentTimeMillis()

            // Si el teléfono está boca abajo (z < -9)
            if (z < -9) {
                if (now - lastShakeTime > 800) {
                    lastShakeTime = now
                    shakeCount++
                    println("📱 Boca abajo detectado ($shakeCount/2)")
                }

                // Si se detectó 2 veces boca abajo
                if (shakeCount >= 2) {
                    shakeCount = 0
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    println("✅ Pitido emitido")
                }
            }

            // Si vuelve a posición normal (z > 0), resetea el contador si tarda demasiado
            if (z > 0 && now - lastShakeTime > 2000) {
                shakeCount = 0
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
