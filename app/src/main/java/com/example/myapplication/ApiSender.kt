package com.example.myapplication
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiSender(private val baseUrl: String = "http://192.168.0.10:5000") {

    fun sendPowerCommand(spokenText: String) {
        val json = """{ "text": "${spokenText.trim()}" }"""
        sendPost("/power", json)
    }

    fun sendColorCommand(h: Int, s: Int, v: Int) {
        val json = """{ "h": $h, "s": $s, "v": $v }"""
        sendPost("/color", json)
    }

    private fun sendPost(path: String, jsonBody: String) {
        Thread {
            try {
                val url = URL("$baseUrl$path")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

                val code = conn.responseCode
                println("✅ POST $path → Código: $code")
                conn.disconnect()
            } catch (e: Exception) {
                println("❌ Error al enviar POST: $e")
            }
        }.start()
    }
}
