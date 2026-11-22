package com.sismografo.app.datos

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Parámetros del filtro tal como están en el sketch del Arduino
 * (arduino/corte_preventivo/corte_preventivo.ino). Si los cambias allá,
 * cámbialos aquí para que la app muestre lo mismo.
 */
object Parametros {
    const val UMBRAL_MS2 = 3.0          // umbralRiesgo
    const val PICOS_NECESARIOS = 4      // contadorPicos >= 4
    const val VENTANA_MS = 1500L        // tiempo máximo entre picos
    const val RANGO_G = 8               // ADXL345_RANGE_8_G
    const val ESCALA_MS2 = 8.0          // escala visual de la barra de aceleración
    const val GRAVEDAD = 9.80665
}

/** Estado físico de la instalación según el último mensaje del Arduino. */
enum class EstadoSistema { DESCONOCIDO, NORMAL, CORTE }

enum class TipoEvento(val titulo: String, val color: Color) {
    CORTE_AUTOMATICO("Corte automático", Color(0xFFDC2626)),
    CORTE_MANUAL("Corte manual", Color(0xFFB91C1C)),
    REARME("Sistema rearmado", Color(0xFF2E7D32)),
    GOLPE_IGNORADO("Golpe ignorado", Color(0xFFD97706)),
    CONEXION("Conexión", Color(0xFF0E5A6D)),
    ;

    val esCorte get() = this == CORTE_AUTOMATICO || this == CORTE_MANUAL
}

data class EventoSistema(
    val tipo: TipoEvento,
    val hora: Long,
    val detalle: String,
)

/**
 * Lo que puede llegar del Arduino por el HC-06, una línea por mensaje.
 *
 * Sketch original:
 *  - `S! ALERTA SISMO! CORTE EJECUTADO`  → gas cerrado, energía cortada, sirena
 *  - `N! SISTEMA CALIBRADO Y NORMAL`     → rearmado / recién encendido
 *
 * Variante con telemetría (opcional):
 *  - `A:0.42`   aceleración actual en m/s²
 *  - `G:4.10,2` pico sobre el umbral: magnitud y contador de picos (1..3)
 */
sealed interface Mensaje {
    data object Corte : Mensaje
    data object Normal : Mensaje
    data class Aceleracion(val ms2: Double) : Mensaje
    data class Pico(val ms2: Double, val contador: Int) : Mensaje
    data class Otro(val texto: String) : Mensaje
}

object Protocolo {
    const val CMD_CORTE = "C"
    const val CMD_REARME = "R"

    fun interpretar(linea: String): Mensaje? {
        val t = linea.trim()
        if (t.isEmpty()) return null
        return when {
            t.startsWith("S!") -> Mensaje.Corte
            t.startsWith("N!") -> Mensaje.Normal
            t.startsWith("A:") -> t.substring(2).trim().toDoubleOrNull()?.let { Mensaje.Aceleracion(it) }
            t.startsWith("G:") -> {
                val partes = t.substring(2).split(',')
                val m = partes.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return Mensaje.Otro(t)
                Mensaje.Pico(m, partes.getOrNull(1)?.trim()?.toIntOrNull() ?: 1)
            }
            else -> Mensaje.Otro(t)
        }
    }
}

fun Double.f1(): String = String.format(Locale.US, "%.1f", this)
fun Double.f2(): String = String.format(Locale.US, "%.2f", this)
fun Double.enG(): String = String.format(Locale.US, "%.2f g", this / Parametros.GRAVEDAD)
