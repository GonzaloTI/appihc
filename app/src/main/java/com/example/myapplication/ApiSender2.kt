package com.example.myapplication

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class ApiSender2(private val baseUrl: String = "http://192.168.61.157:5000") {

    fun sendPowerCommand(spokenText: String): String? {
        val json = """{ "texto": "${spokenText.trim()}" }"""
        return sendPost("/power", json)
    }

    private fun sendPost(path: String, jsonBody: String): String? {
        return try {
            val url = URL("$baseUrl$path")
            val conn = url.openConnection() as HttpURLConnection
            //conn.connectTimeout = 9000  // 3 segundos para conectar
            //conn.readTimeout = 3000     // 3 segundos para leer
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                reader.readText()
            }

            println("✅ POST $path → Código: ${conn.responseCode}")
            conn.disconnect()
            response
        } catch (e: Exception) {
            println("❌ Error al enviar POST: $e")
            // Puedes retornar un JSON falso para que no se rompa el análisis
            """{ "respuesta": "Error de conexión con el servidor.", "comando": null, "color": null }"""
        }
    }

    fun extraerRespuestaTexto(jsonResponse: String?): String? {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            jsonObject.optString("respuesta", null)
        } catch (e: Exception) {
            null
        }
    }
}
