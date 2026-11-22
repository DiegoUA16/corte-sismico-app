package com.sismografo.app.datos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Ajustes(
    val umbral: Float = 4.0f,
    val notificacion: Boolean = true,
    val vibracion: Boolean = true,
    val sonido: Boolean = true,
    val pantallaCompleta: Boolean = true,
    val reconectar: Boolean = true,
)

/** Guarda ajustes, historial y último HC-06 usado en SharedPreferences. */
class Almacen(context: Context) {

    private val prefs = context.getSharedPreferences("sismografo", Context.MODE_PRIVATE)

    fun leerAjustes() = Ajustes(
        umbral = prefs.getFloat("umbral", 4.0f),
        notificacion = prefs.getBoolean("notificacion", true),
        vibracion = prefs.getBoolean("vibracion", true),
        sonido = prefs.getBoolean("sonido", true),
        pantallaCompleta = prefs.getBoolean("pantalla_completa", true),
        reconectar = prefs.getBoolean("reconectar", true),
    )

    fun guardarAjustes(a: Ajustes) {
        prefs.edit()
            .putFloat("umbral", a.umbral)
            .putBoolean("notificacion", a.notificacion)
            .putBoolean("vibracion", a.vibracion)
            .putBoolean("sonido", a.sonido)
            .putBoolean("pantalla_completa", a.pantallaCompleta)
            .putBoolean("reconectar", a.reconectar)
            .apply()
    }

    fun leerEventos(): List<Evento> = runCatching {
        val arr = JSONArray(prefs.getString("eventos", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Evento(o.getDouble("m"), o.getLong("h"), o.optInt("d", 0))
        }
    }.getOrDefault(emptyList())

    fun guardarEventos(eventos: List<Evento>) {
        val arr = JSONArray()
        eventos.forEach { arr.put(JSONObject().put("m", it.magnitud).put("h", it.hora).put("d", it.duracionSeg)) }
        prefs.edit().putString("eventos", arr.toString()).apply()
    }

    var ultimoDispositivo: String?
        get() = prefs.getString("ultimo_dispositivo", null)
        set(valor) = prefs.edit().putString("ultimo_dispositivo", valor).apply()
}
