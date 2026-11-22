package com.sismografo.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismografo.app.bluetooth.ConexionHC06
import com.sismografo.app.bluetooth.Emparejado
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.Ajustes
import com.sismografo.app.datos.Almacen
import com.sismografo.app.datos.Evento
import com.sismografo.app.datos.Lectura
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.ProtocoloSismografo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class InfoDispositivo(val nombre: String, val direccion: String)

data class UiState(
    val estado: EstadoConexion = EstadoConexion.Desconectado,
    val dispositivo: InfoDispositivo? = null,
    val conectadoDesde: Long? = null,
    val ultima: Lectura? = null,
    val ultimaLinea: String? = null,
    val ultimaLineaHora: Long = 0L,
    /** Lecturas de la última hora, para la gráfica en vivo. */
    val lecturas: List<Lectura> = emptyList(),
    val totalLecturas: Int = 0,
    val picoHoy: Double = 0.0,
    /** Historial de sismos, el más reciente primero. */
    val eventos: List<Evento> = emptyList(),
    val ajustes: Ajustes = Ajustes(),
    val alertaActiva: Evento? = null,
    val alertaEsPrueba: Boolean = false,
    val alarmaSonando: Boolean = false,
)

class SismografoViewModel(app: Application) : AndroidViewModel(app) {

    private val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
    private val conexion = ConexionHC06(adapter)
    private val notificador = NotificadorAlertas(app)
    private val alarma = AlarmaLocal(app)
    private val almacen = Almacen(app)

    private val _ui = MutableStateFlow(
        UiState(eventos = almacen.leerEventos(), ajustes = almacen.leerAjustes())
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var trabajo: Job? = null
    private var apagarAlarma: Job? = null
    private var episodio: Evento? = null
    private var diaPico = diaDe(System.currentTimeMillis())

    val bluetoothDisponible: Boolean get() = adapter != null
    val bluetoothEncendido: Boolean get() = adapter?.isEnabled == true

    fun emparejados(): List<Emparejado> =
        runCatching { conexion.emparejadosDetalle() }.getOrDefault(emptyList())

    // ---------------------------------------------------------------- conexión

    /** Reconecta al último HC-06 usado (si sigue emparejado). */
    fun autoconectar() {
        if (_ui.value.estado != EstadoConexion.Desconectado || !bluetoothEncendido) return
        val direccion = almacen.ultimoDispositivo ?: return
        val emparejado = emparejados().firstOrNull { it.direccion == direccion } ?: return
        conectar(emparejado.device)
    }

    fun conectar(device: BluetoothDevice) {
        trabajo?.cancel()
        val info = InfoDispositivo(conexion.nombre(device), device.address)
        almacen.ultimoDispositivo = device.address

        trabajo = viewModelScope.launch {
            _ui.update { it.copy(dispositivo = info) }
            while (isActive) {
                _ui.update { it.copy(estado = EstadoConexion.Conectando(info.nombre), conectadoDesde = null) }
                val motivo = try {
                    conexion.conectar(device)
                    _ui.update {
                        it.copy(
                            estado = EstadoConexion.Conectado(info.nombre),
                            conectadoDesde = System.currentTimeMillis(),
                        )
                    }
                    conexion.lineas().collect { procesar(it) }
                    "El sismógrafo cerró la conexión"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "Sin conexión con ${info.nombre}"
                } finally {
                    conexion.cerrar()
                }

                // Si el usuario pulsó "Desconectar", el job ya está cancelado.
                if (!isActive) break
                if (!_ui.value.ajustes.reconectar) {
                    _ui.update { it.copy(estado = EstadoConexion.Error(motivo), conectadoDesde = null) }
                    break
                }
                _ui.update { it.copy(estado = EstadoConexion.Error("$motivo · reintentando…"), conectadoDesde = null) }
                delay(ESPERA_RECONEXION_MS)
            }
        }
    }

    fun desconectar() {
        trabajo?.cancel()
        conexion.cerrar() // desbloquea el readLine()
        _ui.update { it.copy(estado = EstadoConexion.Desconectado, conectadoDesde = null) }
    }

    fun recalibrar() {
        viewModelScope.launch { runCatching { conexion.enviar("CAL\n") } }
    }

    // ---------------------------------------------------------------- datos

    private fun procesar(linea: String) {
        val ahora = System.currentTimeMillis()
        _ui.update { it.copy(ultimaLinea = linea.trim(), ultimaLineaHora = ahora) }

        val lectura = ProtocoloSismografo.interpretar(linea) ?: return
        val dia = diaDe(lectura.hora)
        val mismoDia = dia == diaPico
        diaPico = dia
        _ui.update { s ->
            s.copy(
                ultima = lectura,
                lecturas = (s.lecturas + lectura).takeLast(MAX_LECTURAS),
                totalLecturas = s.totalLecturas + 1,
                picoHoy = if (mismoDia) maxOf(s.picoHoy, lectura.magnitud) else lectura.magnitud,
            )
        }
        evaluarAlerta(lectura)
    }

    /**
     * Un "episodio" empieza cuando una lectura supera el umbral y termina cuando
     * baja. Se avisa al empezar y otra vez solo si sube de nivel en la escala.
     */
    private fun evaluarAlerta(lectura: Lectura) {
        val s = _ui.value
        val supera = lectura.alertaDelSensor || lectura.magnitud >= s.ajustes.umbral
        val actual = episodio
        if (!supera) {
            episodio = null
            return
        }

        if (actual == null) {
            val nuevo = Evento(lectura.magnitud, lectura.hora)
            episodio = nuevo
            guardarEventos(listOf(nuevo) + s.eventos)
            _ui.update { it.copy(alertaActiva = nuevo, alertaEsPrueba = false) }
            dispararAvisos(nuevo)
        } else {
            val actualizado = actual.copy(
                magnitud = maxOf(actual.magnitud, lectura.magnitud),
                duracionSeg = ((lectura.hora - actual.hora) / 1000).toInt(),
            )
            episodio = actualizado
            guardarEventos(s.eventos.map { if (it.hora == actual.hora) actualizado else it })
            val subioNivel = NivelSismo.de(actualizado.magnitud) != NivelSismo.de(actual.magnitud)
            _ui.update {
                // Si el usuario ya cerró la alerta, solo se vuelve a mostrar si el sismo empeora.
                it.copy(alertaActiva = if (it.alertaActiva == null && !subioNivel) null else actualizado)
            }
            if (subioNivel) dispararAvisos(actualizado)
        }
    }

    private fun dispararAvisos(evento: Evento) {
        val a = _ui.value.ajustes
        if (a.notificacion) notificador.alertar(evento, a.pantallaCompleta)
        sonarAlarma(a)
    }

    private fun sonarAlarma(a: Ajustes) {
        if (!a.sonido && !a.vibracion) return
        alarma.iniciar(a.sonido, a.vibracion)
        _ui.update { it.copy(alarmaSonando = true) }
        apagarAlarma?.cancel()
        apagarAlarma = viewModelScope.launch {
            delay(DURACION_ALARMA_MS)
            silenciar()
        }
    }

    private fun guardarEventos(eventos: List<Evento>) {
        val recortados = eventos.take(MAX_EVENTOS)
        _ui.update { it.copy(eventos = recortados) }
        almacen.guardarEventos(recortados)
    }

    // ---------------------------------------------------------------- acciones de la UI

    fun silenciar() {
        apagarAlarma?.cancel()
        alarma.detener()
        _ui.update { it.copy(alarmaSonando = false) }
    }

    fun estoyASalvo() {
        silenciar()
        notificador.cancelar()
        _ui.update { it.copy(alertaActiva = null, alertaEsPrueba = false) }
    }

    fun probarAlerta() {
        val a = _ui.value.ajustes
        val prueba = Evento(magnitud = a.umbral + 0.6, hora = System.currentTimeMillis())
        _ui.update { it.copy(alertaActiva = prueba, alertaEsPrueba = true) }
        sonarAlarma(a)
    }

    fun actualizarAjustes(cambio: (Ajustes) -> Ajustes) {
        val nuevos = cambio(_ui.value.ajustes)
        almacen.guardarAjustes(nuevos)
        _ui.update { it.copy(ajustes = nuevos) }
    }

    fun borrarHistorial() = guardarEventos(emptyList())

    override fun onCleared() {
        conexion.cerrar()
        alarma.detener()
    }

    private fun diaDe(ms: Long): Int = Calendar.getInstance().run {
        timeInMillis = ms
        get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
    }

    private companion object {
        const val MAX_LECTURAS = 3600 // 1 h a 1 lectura/s
        const val MAX_EVENTOS = 200
        const val ESPERA_RECONEXION_MS = 3_000L
        const val DURACION_ALARMA_MS = 60_000L
    }
}
