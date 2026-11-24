package com.sismografo.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.sismografo.app.bluetooth.Emparejado
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.Ajustes
import com.sismografo.app.datos.EstadoSistema
import com.sismografo.app.datos.EventoSistema
import kotlinx.coroutines.flow.StateFlow

data class InfoDispositivo(val nombre: String, val direccion: String)

data class Muestra(val hora: Long, val ms2: Double)

/** Secuencia de picos en curso (cuando llegan tramas `G:` desde el Arduino). */
data class Secuencia(val picos: Int, val maximo: Double, val ultimoPico: Long)

data class UiState(
    val estado: EstadoConexion = EstadoConexion.Desconectado,
    val btEncendido: Boolean = true,
    val modoDemo: Boolean = false,
    val dispositivo: InfoDispositivo? = null,
    val conectadoDesde: Long? = null,

    /** Estado de la instalación: válvula de gas, relé de energía y sirena. */
    val sistema: EstadoSistema = EstadoSistema.DESCONOCIDO,
    val sistemaDesde: Long = 0L,
    /** true si el estado lo confirmó el equipo en esta conexión (no es el guardado). */
    val sistemaConfirmado: Boolean = false,

    /** Telemetría opcional (`A:`). */
    val telemetria: Boolean = false,
    val muestras: List<Muestra> = emptyList(),
    val secuencia: Secuencia? = null,

    val ultimaLinea: String? = null,
    val ultimaLineaHora: Long = 0L,
    val lineasRecibidas: Int = 0,

    /** Registro, el más reciente primero. */
    val eventos: List<EventoSistema> = emptyList(),
    val ajustes: Ajustes = Ajustes(),

    /** Hora del corte que se está mostrando en la alerta a pantalla completa. */
    val alertaActiva: Long? = null,
    val alertaManual: Boolean = false,
    val alertaEsPrueba: Boolean = false,
    val alarmaSonando: Boolean = false,

    /** Comando enviado que espera confirmación ("C" o "R"). */
    val comandoPendiente: String? = null,
    val aviso: String? = null,
) {
    val conectado get() = estado is EstadoConexion.Conectado
    val aceleracion get() = muestras.lastOrNull()?.ms2
}

/**
 * ViewModel fino que expone el estado y acciones del [MonitorSismico] compartido.
 * No cierra la conexión en `onCleared()` para que el servicio en segundo plano siga vigilando.
 */
class SismografoViewModel(app: Application) : AndroidViewModel(app) {

    private val monitor: MonitorSismico = (app as CorteSismicoApp).monitor

    val ui: StateFlow<UiState> = monitor.ui
    val bluetoothDisponible: Boolean get() = monitor.bluetoothDisponible
    val bluetoothEncendido: Boolean get() = monitor.bluetoothEncendido

    fun emparejados(): List<Emparejado> = monitor.emparejados()
    fun autoconectar() = monitor.autoconectar()
    fun conectar(device: BluetoothDevice) = monitor.conectar(device)
    fun desconectar() = monitor.desconectar()
    fun iniciarDemo() = monitor.iniciarDemo()
    fun simularGolpe() = monitor.simularGolpe()
    fun simularSismo() = monitor.simularSismo()
    fun corteManual() = monitor.corteManual()
    fun rearmar() = monitor.rearmar()
    fun cambiarPerfilAlarma(codigo: Int) = monitor.cambiarPerfilAlarma(codigo)
    fun probarAlarmaEquipo() = monitor.probarAlarmaEquipo()
    fun descartarAviso() = monitor.descartarAviso()
    fun silenciar() = monitor.silenciar()
    fun estoyASalvo() = monitor.estoyASalvo()
    fun probarAlerta() = monitor.probarAlerta()
    fun borrarRegistro() = monitor.borrarRegistro()
    fun actualizarAjustes(cambio: (Ajustes) -> Ajustes) = monitor.actualizarAjustes(cambio)
}
