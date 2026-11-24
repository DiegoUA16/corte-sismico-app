package com.sismografo.app

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.sismografo.app.bluetooth.ConexionHC06
import com.sismografo.app.bluetooth.Emparejado
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.Ajustes
import com.sismografo.app.datos.Almacen
import com.sismografo.app.datos.EstadoSistema
import com.sismografo.app.datos.EventoSistema
import com.sismografo.app.datos.Mensaje
import com.sismografo.app.datos.Parametros
import com.sismografo.app.datos.Protocolo
import com.sismografo.app.datos.SimuladorArduino
import com.sismografo.app.datos.TipoEvento
import com.sismografo.app.datos.f1
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Motor único de vigilancia sísmica a nivel de proceso ([CorteSismicoApp]).
 *
 * Vive fuera de `MainActivity` para que [ServicioVigilancia] pueda mantener la conexión
 * Bluetooth activa en segundo plano hasta que el usuario pulse "Desconectar" o "Detener".
 */
class MonitorSismico(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val btManager = app.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = btManager?.adapter
    private val conexion = ConexionHC06(btManager)
    private val notificador = NotificadorAlertas(app)
    private val alarma = AlarmaLocal(app)
    private val almacen = Almacen(app)

    private val _ui = MutableStateFlow(
        UiState(
            btEncendido = adapter?.isEnabled == true,
            eventos = almacen.leerEventos(),
            ajustes = almacen.leerAjustes(),
            sistema = almacen.ultimoEstado,
            sistemaDesde = almacen.estadoDesde,
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var trabajo: Job? = null
    private var apagarAlarma: Job? = null
    private var esperaComando: Job? = null
    private var simulador: SimuladorArduino? = null

    private val receptorBt = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                    _ui.update { it.copy(btEncendido = false) }
                    conexion.cerrar()
                }
                BluetoothAdapter.STATE_ON -> {
                    _ui.update { it.copy(btEncendido = true) }
                    val s = _ui.value
                    if (!s.modoDemo && s.ajustes.reconectar && trabajo?.isActive != true) {
                        scope.launch {
                            delay(ESPERA_BT_LISTO_MS)
                            autoconectar()
                        }
                    }
                }
            }
        }
    }

    val bluetoothDisponible: Boolean get() = adapter != null
    val bluetoothEncendido: Boolean get() = adapter?.isEnabled == true

    init {
        runCatching {
            ContextCompat.registerReceiver(
                app,
                receptorBt,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        // Cierra las secuencias de picos que no llegaron a 4: son golpes ignorados.
        scope.launch {
            while (isActive) {
                delay(250)
                val sec = _ui.value.secuencia ?: continue
                if (System.currentTimeMillis() - sec.ultimoPico > Parametros.VENTANA_MS) {
                    _ui.update { it.copy(secuencia = null) }
                    if (_ui.value.ajustes.registrarGolpes && _ui.value.sistema != EstadoSistema.CORTE) {
                        registrar(
                            TipoEvento.GOLPE_IGNORADO, sec.ultimoPico,
                            "${sec.maximo.f1()} m/s² · ${sec.picos} de ${Parametros.PICOS_NECESARIOS} picos",
                        )
                    }
                }
            }
        }
    }

    fun emparejados(): List<Emparejado> =
        runCatching { conexion.emparejadosDetalle() }.getOrDefault(emptyList())

    // ---------------------------------------------------------------- conexión Bluetooth

    /** Reconecta al último HC-06 usado (si sigue emparejado). */
    fun autoconectar() {
        val s = _ui.value
        val listo = s.estado is EstadoConexion.Desconectado || s.estado is EstadoConexion.Error
        if (!listo || trabajo?.isActive == true || s.modoDemo || !bluetoothEncendido) return
        val direccion = almacen.ultimoDispositivo ?: return
        val emparejado = emparejados().firstOrNull { it.direccion == direccion } ?: return
        conectar(emparejado.device)
    }

    fun conectar(device: BluetoothDevice) {
        val habiaTrabajo = trabajo?.isActive == true
        detenerTrabajo()
        val info = InfoDispositivo(conexion.nombre(device), device.address)
        almacen.ultimoDispositivo = device.address

        _ui.update {
            it.copy(
                dispositivo = info,
                modoDemo = false,
                btEncendido = bluetoothEncendido,
                estado = EstadoConexion.Conectando(info.nombre),
                conectadoDesde = null,
            )
        }

        trabajo = scope.launch {
            if (habiaTrabajo) delay(500)
            while (isActive) {
                if (!bluetoothEncendido) {
                    _ui.update { it.copy(estado = EstadoConexion.Error("Bluetooth apagado · esperando…"), conectadoDesde = null) }
                    if (!_ui.value.ajustes.reconectar) {
                        ServicioVigilancia.detener(app)
                        break
                    }
                    _ui.first { it.btEncendido }
                    delay(ESPERA_BT_LISTO_MS)
                    if (!isActive) break
                }

                _ui.update { it.copy(estado = EstadoConexion.Conectando(info.nombre), conectadoDesde = null) }
                var llegoAConectar = false
                val motivo = try {
                    val actual = conexion.dispositivoPorDireccion(info.direccion) ?: device
                    conexion.conectar(actual)
                    llegoAConectar = true
                    alConectar(info)
                    ServicioVigilancia.iniciar(app)
                    // Consulta de estado ('?'): los sketches actualizados responden S! o N!;
                    // se reintenta una vez si el estado aún no se ha confirmado.
                    val consulta = launch {
                        delay(400)
                        runCatching { conexion.enviar(Protocolo.CMD_ESTADO) }
                        delay(1_200)
                        if (!_ui.value.sistemaConfirmado) {
                            runCatching { conexion.enviar(Protocolo.CMD_ESTADO) }
                        }
                    }
                    try {
                        conexion.lineas().collect { procesar(it) }
                    } finally {
                        consulta.cancel()
                    }
                    "El equipo cerró la conexión"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!bluetoothEncendido) "Bluetooth apagado"
                    else e.message?.takeIf { it.isNotBlank() } ?: "Sin conexión con ${info.nombre}"
                } finally {
                    conexion.cerrar()
                }

                if (!isActive) break
                if (llegoAConectar) {
                    registrar(TipoEvento.CONEXION, System.currentTimeMillis(), "Se perdió el enlace con ${info.nombre}")
                }
                _ui.update { it.copy(sistemaConfirmado = false, telemetria = false, muestras = emptyList(), secuencia = null) }
                if (!_ui.value.ajustes.reconectar) {
                    _ui.update { it.copy(estado = EstadoConexion.Error(motivo), conectadoDesde = null) }
                    ServicioVigilancia.detener(app)
                    break
                }
                if (!bluetoothEncendido) continue
                _ui.update { it.copy(estado = EstadoConexion.Error("$motivo · reintentando…"), conectadoDesde = null) }
                delay(ESPERA_RECONEXION_MS)
            }
        }
    }

    private fun alConectar(info: InfoDispositivo) {
        _ui.update {
            it.copy(
                estado = EstadoConexion.Conectado(info.nombre),
                conectadoDesde = System.currentTimeMillis(),
                sistemaConfirmado = false,
            )
        }
        registrar(TipoEvento.CONEXION, System.currentTimeMillis(), "Conectado a ${info.nombre}")
    }

    fun desconectar() {
        detenerTrabajo()
        ServicioVigilancia.detener(app)
        _ui.update {
            it.copy(
                estado = EstadoConexion.Desconectado, conectadoDesde = null, modoDemo = false,
                sistemaConfirmado = false, telemetria = false, muestras = emptyList(), secuencia = null,
                comandoPendiente = null,
            )
        }
    }

    private fun detenerTrabajo() {
        trabajo?.cancel()
        trabajo = null
        esperaComando?.cancel()
        simulador = null
        conexion.cerrar() // desbloquea el readLine()
    }

    // ---------------------------------------------------------------- modo demostración

    fun iniciarDemo() {
        detenerTrabajo()
        ServicioVigilancia.detener(app)
        val sim = SimuladorArduino()
        simulador = sim
        val info = InfoDispositivo("Simulador", "Modo demostración · sin Bluetooth")
        trabajo = scope.launch {
            _ui.update { it.copy(modoDemo = true, dispositivo = info, comandoPendiente = null) }
            alConectar(info)
            sim.arrancar().forEach { procesar(it) }
            while (isActive) {
                delay(200)
                sim.paso(System.currentTimeMillis()).forEach { procesar(it) }
            }
        }
    }

    fun simularGolpe() { simulador?.simularGolpe() }
    fun simularSismo() { simulador?.simularSismo() }

    // ---------------------------------------------------------------- mensajes del Arduino

    private fun procesar(linea: String) {
        val ahora = System.currentTimeMillis()
        val texto = linea.trim()
        _ui.update {
            if (texto.startsWith("A:")) it.copy(lineasRecibidas = it.lineasRecibidas + 1)
            else it.copy(ultimaLinea = texto, ultimaLineaHora = ahora, lineasRecibidas = it.lineasRecibidas + 1)
        }

        when (val m = Protocolo.interpretar(texto) ?: return) {
            Mensaje.Corte -> alCorte(ahora)
            Mensaje.Normal -> alNormal(ahora)
            is Mensaje.Aceleracion -> _ui.update { s ->
                s.copy(
                    telemetria = true,
                    muestras = (s.muestras + Muestra(ahora, m.ms2)).takeLast(MAX_MUESTRAS),
                )
            }
            is Mensaje.Pico -> _ui.update { s ->
                if (s.sistema == EstadoSistema.CORTE) s
                else s.copy(
                    secuencia = Secuencia(m.contador, maxOf(s.secuencia?.maximo ?: 0.0, m.ms2), ahora),
                )
            }
            is Mensaje.Otro -> Unit
        }
    }

    private fun alCorte(ahora: Long) {
        val s = _ui.value
        val manual = s.comandoPendiente == Protocolo.CMD_CORTE
        terminarComando()
        val yaEstaba = s.sistema == EstadoSistema.CORTE && s.sistemaConfirmado
        cambiarSistema(EstadoSistema.CORTE, if (yaEstaba) s.sistemaDesde else ahora)
        _ui.update { it.copy(secuencia = null) }
        if (yaEstaba) return

        if (manual) {
            registrar(TipoEvento.CORTE_MANUAL, ahora, "Enviado desde la app · gas cerrado y energía cortada")
        } else {
            registrar(TipoEvento.CORTE_AUTOMATICO, ahora, "Oscilaciones sostenidas · gas cerrado y energía cortada")
        }
        _ui.update { it.copy(alertaActiva = ahora, alertaManual = manual, alertaEsPrueba = false) }
        dispararAvisos(ahora, manual)
    }

    private fun alNormal(ahora: Long) {
        val s = _ui.value
        val desdeApp = s.comandoPendiente == Protocolo.CMD_REARME
        terminarComando()
        val veniaDeCorte = s.sistema == EstadoSistema.CORTE
        val desde = if (s.sistema == EstadoSistema.NORMAL && s.sistemaDesde > 0) s.sistemaDesde else ahora
        cambiarSistema(EstadoSistema.NORMAL, desde)
        if (veniaDeCorte) {
            registrar(
                TipoEvento.REARME, ahora,
                if (desdeApp) "Desde la app · válvula abierta y energía restablecida"
                else "Botón físico del equipo · sensor recalibrado",
            )
            // El rearme apaga la sirena del equipo: aquí también se apaga la alarma.
            silenciar()
            notificador.cancelar()
            _ui.update { it.copy(alertaActiva = null, alertaEsPrueba = false) }
        }
    }

    private fun cambiarSistema(nuevo: EstadoSistema, desde: Long) {
        almacen.ultimoEstado = nuevo
        almacen.estadoDesde = desde
        _ui.update { it.copy(sistema = nuevo, sistemaDesde = desde, sistemaConfirmado = true) }
    }

    // ---------------------------------------------------------------- comandos al Arduino

    /** Manda 'C': el Arduino ejecuta `ejecutarCorteEmergencia()` y responde `S!`. */
    fun corteManual() = enviarComando(Protocolo.CMD_CORTE, "El equipo no confirmó el corte. Revisa la conexión.")

    /** Manda 'R': el Arduino ejecuta `rearmarSistema()`, recalibra y responde `N!`. */
    fun rearmar() = enviarComando(Protocolo.CMD_REARME, "El equipo no confirmó el rearme. Usa el botón físico.")

    /** Manda '1'..'4': cambia el perfil de sirena en el Arduino (el equipo reproduce 2 ciclos de prueba). */
    fun cambiarPerfilAlarma(codigo: Int) {
        val valido = codigo.coerceIn(1, 4)
        actualizarAjustes { it.copy(perfilAlarma = valido) }
        val s = _ui.value
        if (s.conectado && !s.modoDemo && s.sistema != EstadoSistema.CORTE) {
            scope.launch { runCatching { conexion.enviar(valido.toString()) } }
        }
    }

    /** Manda 'T': reproduce 2 ciclos de prueba del buzzer del equipo sin cortar gas ni energía. */
    fun probarAlarmaEquipo() {
        val s = _ui.value
        if (s.conectado && !s.modoDemo && s.sistema != EstadoSistema.CORTE) {
            scope.launch { runCatching { conexion.enviar(Protocolo.CMD_PROBAR_ALARMA) } }
        }
    }

    private fun enviarComando(cmd: String, error: String) {
        if (!_ui.value.conectado) {
            _ui.update { it.copy(aviso = "Conéctate al equipo para enviar comandos.") }
            return
        }
        _ui.update { it.copy(comandoPendiente = cmd, aviso = null) }
        esperaComando?.cancel()
        esperaComando = scope.launch {
            val sim = simulador
            if (sim != null) {
                delay(if (cmd == Protocolo.CMD_REARME) 900 else 300) // como el delay(400) + calibración
                val respuesta = sim.recibir(cmd)
                respuesta.forEach { procesar(it) }
                return@launch
            }
            var enviadoAlgunaVez = false
            repeat(REINTENTOS_COMANDO) { intento ->
                if (_ui.value.comandoPendiente != cmd || !_ui.value.conectado) return@launch
                val ok = runCatching { conexion.enviar(cmd) }.isSuccess
                if (ok) {
                    enviadoAlgunaVez = true
                } else if (!enviadoAlgunaVez && intento == 0) {
                    _ui.update { it.copy(comandoPendiente = null, aviso = "No se pudo enviar el comando.") }
                    return@launch
                }
                delay(ESPERA_REINTENTO_COMANDO_MS)
            }
            if (_ui.value.comandoPendiente == cmd) _ui.update { it.copy(comandoPendiente = null, aviso = error) }
        }
    }

    private fun terminarComando() {
        esperaComando?.cancel()
        _ui.update { it.copy(comandoPendiente = null) }
    }

    fun descartarAviso() = _ui.update { it.copy(aviso = null) }

    // ---------------------------------------------------------------- avisos en el teléfono y reloj

    private fun dispararAvisos(hora: Long, manual: Boolean) {
        val a = _ui.value.ajustes
        if (a.notificacion) notificador.alertar(hora, manual, a.pantallaCompleta)
        sonarAlarma(a)
    }

    private fun sonarAlarma(a: Ajustes) {
        if (!a.sonido && !a.vibracion) return
        alarma.iniciar(a.sonido, a.vibracion)
        _ui.update { it.copy(alarmaSonando = true) }
        apagarAlarma?.cancel()
        apagarAlarma = scope.launch {
            delay(DURACION_ALARMA_MS)
            silenciar()
        }
    }

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
        val ahora = System.currentTimeMillis()
        val a = _ui.value.ajustes
        _ui.update { it.copy(alertaActiva = ahora, alertaManual = false, alertaEsPrueba = true) }
        if (a.notificacion) notificador.alertar(ahora, manual = false, pantallaCompleta = false, esPrueba = true)
        sonarAlarma(a)
    }

    // ---------------------------------------------------------------- registro y ajustes

    private fun registrar(tipo: TipoEvento, hora: Long, detalle: String) {
        val nuevos = (listOf(EventoSistema(tipo, hora, detalle)) + _ui.value.eventos)
            .sortedByDescending { it.hora }
            .take(MAX_EVENTOS)
        _ui.update { it.copy(eventos = nuevos) }
        almacen.guardarEventos(nuevos)
    }

    fun borrarRegistro() {
        _ui.update { it.copy(eventos = emptyList()) }
        almacen.guardarEventos(emptyList())
    }

    fun actualizarAjustes(cambio: (Ajustes) -> Ajustes) {
        val nuevos = cambio(_ui.value.ajustes)
        almacen.guardarAjustes(nuevos)
        _ui.update { it.copy(ajustes = nuevos) }
    }

    private companion object {
        const val MAX_MUESTRAS = 150 // 30 s a 5 muestras/s (delay(200) del sketch)
        const val MAX_EVENTOS = 300
        const val ESPERA_RECONEXION_MS = 4_000L
        const val ESPERA_BT_LISTO_MS = 3_000L
        const val REINTENTOS_COMANDO = 3
        const val ESPERA_REINTENTO_COMANDO_MS = 2_000L
        const val DURACION_ALARMA_MS = 60_000L
    }
}
