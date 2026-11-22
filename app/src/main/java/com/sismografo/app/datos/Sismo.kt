package com.sismografo.app.datos

import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Una medición recibida del sismógrafo. */
data class Lectura(
    val magnitud: Double,
    /** true si el propio Arduino marcó la lectura como alerta ("ALERTA:x.x"). */
    val alertaDelSensor: Boolean,
    val hora: Long = System.currentTimeMillis(),
)

/**
 * Un sismo registrado: empieza cuando una lectura supera el umbral y termina
 * cuando vuelve a bajar. [hora] sirve también como identificador.
 */
data class Evento(
    val magnitud: Double,
    val hora: Long,
    val duracionSeg: Int = 0,
)

/** Clasificación según la escala de Richter, con los colores semánticos del diseño. */
enum class NivelSismo(
    val desde: Double,
    val nombre: String,
    val corto: String,
    val titulo: String,
    val color: Color,
    val descripcion: String,
) {
    MICRO(0.0, "Micro", "Micro", "Microsismo", Color(0xFF6F8F7A), "No se percibe"),
    MENOR(2.0, "Menor", "Menor", "Sismo menor", Color(0xFF2E7D32), "Lo perciben pocas personas"),
    LIGERO(4.0, "Ligero", "Ligero", "Sismo ligero", Color(0xFFD97706), "Se siente; los objetos se mueven"),
    MODERADO(5.0, "Moderado", "Mod.", "Sismo moderado", Color(0xFFEA580C), "Daños leves en construcciones débiles"),
    FUERTE(6.0, "Fuerte", "Fuerte", "Sismo fuerte", Color(0xFFDC2626), "Daños en zonas pobladas"),
    MAYOR(7.0, "Mayor", "Mayor", "Sismo mayor", Color(0xFFB91C1C), "Daños graves en áreas extensas"),
    GRAN(8.0, "Gran terremoto", "Gran", "Gran terremoto", Color(0xFF7C3AED), "Destrucción severa");

    companion object {
        fun de(magnitud: Double): NivelSismo = entries.lastOrNull { magnitud >= it.desde } ?: MICRO
    }
}

/**
 * Formato que manda el Arduino, una línea por mensaje terminada en '\n':
 *   MAG:3.42      -> lectura normal
 *   ALERTA:5.10   -> el Arduino detectó un sismo
 *   3.42          -> también se acepta solo el número
 */
object ProtocoloSismografo {
    fun interpretar(linea: String): Lectura? {
        val texto = linea.trim().uppercase(Locale.ROOT)
        if (texto.isEmpty()) return null

        val (esAlerta, valor) = when {
            texto.startsWith("ALERTA:") -> true to texto.substringAfter(':')
            texto.startsWith("MAG:") -> false to texto.substringAfter(':')
            else -> false to texto
        }
        val magnitud = valor.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (magnitud < 0.0 || magnitud > 10.0) return null
        return Lectura(magnitud, esAlerta)
    }
}

/** 1 decimal con punto, como en el diseño: 4.0, 5.2 */
fun Double.f1(): String = String.format(Locale.US, "%.1f", this)
fun Float.f1(): String = String.format(Locale.US, "%.1f", this)
