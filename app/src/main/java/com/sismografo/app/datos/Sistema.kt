package com.sismografo.app.datos

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Parámetros del filtro tal como están en el sketch del Arduino
 * (arduino/corte_preventivo/corte_preventivo.ino). Si los cambias allá,
 * cámbialos aquí para que la app muestre lo mismo.
 */
object Parametros {
    const val UMBRAL_MS2 = 1.6          // umbralSismo (≈ 0.16g - Mercalli VI)
    const val PICOS_NECESARIOS = 4      // contadorPicos >= 4
    const val VENTANA_MS = 1500L        // tiempo máximo entre picos
    const val RANGO_G = 2               // ADXL345_RANGE_2_G
    const val ESCALA_MS2 = 4.0          // escala visual de la barra de aceleración
    const val GRAVEDAD = 9.80665
}

/** Perfiles de sirena configurables en vivo en el Arduino (comandos '1'..'4'). */
enum class PerfilAlarma(val codigo: Int, val nombre: String, val detalle: String) {
    BITONO(1, "Bitono clásico", "Sirena alterna de dos tonos"),
    BARRIDO(2, "Barrido electrónico", "Tono ascendente continuo"),
    PULSO(3, "Pulso industrial", "3 pitidos agudos intermitentes"),
    DOBLE_RAPIDO(4, "Doble tono rápido", "Alerta sísmica rápida"),
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
    const val CMD_ESTADO = "?"
    const val CMD_PROBAR_ALARMA = "T"

    fun interpretar(linea: String): Mensaje? {
        val t = linea.trim { it.isWhitespace() || it.isISOControl() }
        if (t.isEmpty()) return null
        // S!/N! se buscan en cualquier parte de la línea y antes que A:/G:, para que un byte
        // sucio al inicio de la trama del HC-06 nunca impida detectar un corte o un rearme.
        return when {
            t.contains("S!") -> Mensaje.Corte
            t.contains("N!") -> Mensaje.Normal
            t.contains("G:") -> {
                val partes = t.substringAfter("G:").split(',')
                val m = partes.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return Mensaje.Otro(t)
                val c = partes.getOrNull(1)?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 1
                Mensaje.Pico(m, c)
            }
            t.startsWith("A:") -> t.substring(2).trim().replace(',', '.').toDoubleOrNull()?.let { Mensaje.Aceleracion(it) }
            else -> Mensaje.Otro(t)
        }
    }
}

fun Double.f1(): String = String.format(Locale.US, "%.1f", this)
fun Double.f2(): String = String.format(Locale.US, "%.2f", this)
fun Double.enG(): String = String.format(Locale.US, "%.2f g", this / Parametros.GRAVEDAD)
