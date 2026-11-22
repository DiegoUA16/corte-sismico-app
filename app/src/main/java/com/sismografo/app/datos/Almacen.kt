package com.sismografo.app.datos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Ajustes(
    val notificacion: Boolean = true,
    val vibracion: Boolean = true,
    val sonido: Boolean = true,
    val pantallaCompleta: Boolean = true,
    val reconectar: Boolean = true,
    /** Guarda en el registro los golpes que el filtro ignoró (solo con telemetría). */
    val registrarGolpes: Boolean = true,
)

/** Guarda ajustes, registro de eventos, último estado y último HC-06 en SharedPreferences. */
class Almacen(context: Context) {

    private val prefs = context.getSharedPreferences("corte_sismico", Context.MODE_PRIVATE)

    fun leerAjustes() = Ajustes(
        notificacion = prefs.getBoolean("notificacion", true),
        vibracion = prefs.getBoolean("vibracion", true),
        sonido = prefs.getBoolean("sonido", true),
        pantallaCompleta = prefs.getBoolean("pantalla_completa", true),
        reconectar = prefs.getBoolean("reconectar", true),
        registrarGolpes = prefs.getBoolean("registrar_golpes", true),
    )

    fun guardarAjustes(a: Ajustes) {
        prefs.edit()
            .putBoolean("notificacion", a.notificacion)
            .putBoolean("vibracion", a.vibracion)
            .putBoolean("sonido", a.sonido)
            .putBoolean("pantalla_completa", a.pantallaCompleta)
            .putBoolean("reconectar", a.reconectar)
            .putBoolean("registrar_golpes", a.registrarGolpes)
            .apply()
    }

    fun leerEventos(): List<EventoSistema> = runCatching {
        val arr = JSONArray(prefs.getString("eventos", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val tipo = runCatching { TipoEvento.valueOf(o.getString("t")) }.getOrNull() ?: return@mapNotNull null
            EventoSistema(tipo, o.getLong("h"), o.optString("d"))
        }
    }.getOrDefault(emptyList())

    fun guardarEventos(eventos: List<EventoSistema>) {
        val arr = JSONArray()
        eventos.forEach { arr.put(JSONObject().put("t", it.tipo.name).put("h", it.hora).put("d", it.detalle)) }
        prefs.edit().putString("eventos", arr.toString()).apply()
    }

    /** Último estado conocido del equipo, para mostrarlo al abrir la app sin conexión. */
    var ultimoEstado: EstadoSistema
        get() = runCatching { EstadoSistema.valueOf(prefs.getString("estado", null)!!) }
            .getOrDefault(EstadoSistema.DESCONOCIDO)
        set(v) = prefs.edit().putString("estado", v.name).apply()

    var estadoDesde: Long
        get() = prefs.getLong("estado_desde", 0L)
        set(v) = prefs.edit().putLong("estado_desde", v).apply()

    var ultimoDispositivo: String?
        get() = prefs.getString("ultimo_dispositivo", null)
        set(valor) = prefs.edit().putString("ultimo_dispositivo", valor).apply()
}
