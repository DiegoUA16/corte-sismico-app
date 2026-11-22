package com.sismografo.app.datos

import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Réplica en Kotlin de la lógica de `vigilarSismo()` del sketch, para el
 * **modo demostración**: permite enseñar la app sin el Arduino a mano.
 * Emite exactamente las mismas líneas que el equipo real (variante con telemetría).
 *
 * Cada [paso] equivale a una vuelta del `loop()` (delay de 200 ms).
 */
class SimuladorArduino {

    private var enCorte = false
    private var contadorPicos = 0
    private var tiempoUltimoPico = 0L
    private var t = 0.0

    /** Ticks restantes de cada perturbación. */
    private var golpe = 0
    private var sismo = 0

    fun arrancar(): List<String> = listOf("N! SISTEMA CALIBRADO Y NORMAL")

    fun simularGolpe() { golpe = 1 }
    fun simularSismo() { sismo = 14 } // ~2.8 s de oscilación sostenida

    /** Comandos que la app manda por Bluetooth: 'C' y 'R'. */
    fun recibir(cmd: String): List<String> = when (cmd.trim().uppercase()) {
        Protocolo.CMD_CORTE -> cortar()
        Protocolo.CMD_REARME -> rearmar()
        else -> emptyList()
    }

    fun paso(ahora: Long): List<String> {
        t += 0.2
        // Ruido de fondo de un ADXL345 sobre una mesa (≈0.1–0.5 m/s² tras restar la base)
        var dx = Random.nextDouble(-0.18, 0.18)
        var dy = Random.nextDouble(-0.18, 0.18)
        var dz = Random.nextDouble(-0.25, 0.25) + 0.12 * sin(t * 1.7)
        if (golpe > 0) {
            golpe--
            dx += 3.4; dz += 2.1
        }
        if (sismo > 0) {
            sismo--
            val a = 4.0 + Random.nextDouble(0.0, 2.2)
            dx += a * sin(t * 9.0); dy += a * 0.6
        }
        val magnitud = sqrt(dx * dx + dy * dy + dz * dz)
        val salida = mutableListOf("A:" + magnitud.f2())
        if (enCorte) return salida

        if (magnitud > Parametros.UMBRAL_MS2) {
            contadorPicos = if (ahora - tiempoUltimoPico > Parametros.VENTANA_MS) 1 else contadorPicos + 1
            tiempoUltimoPico = ahora
            if (contadorPicos >= Parametros.PICOS_NECESARIOS) {
                salida += cortar()
            } else {
                salida += "G:${magnitud.f2()},$contadorPicos"
            }
        }
        return salida
    }

    private fun cortar(): List<String> {
        enCorte = true
        contadorPicos = 0
        golpe = 0
        return listOf("S! ALERTA SISMO! CORTE EJECUTADO")
    }

    private fun rearmar(): List<String> {
        enCorte = false
        contadorPicos = 0
        sismo = 0
        return listOf("N! SISTEMA CALIBRADO Y NORMAL")
    }
}
