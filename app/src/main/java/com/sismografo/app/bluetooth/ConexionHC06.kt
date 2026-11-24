package com.sismografo.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class Emparejado(
    val device: BluetoothDevice,
    val nombre: String,
    val direccion: String,
    val esAudio: Boolean,
)

sealed interface EstadoConexion {
    data object Desconectado : EstadoConexion
    data class Conectando(val nombre: String) : EstadoConexion
    data class Conectado(val nombre: String) : EstadoConexion
    data class Error(val mensaje: String) : EstadoConexion
}

/**
 * Conexión serial (SPP / RFCOMM) con un módulo HC-06.
 * Los permisos de Bluetooth se piden en la UI antes de llamar a estos métodos.
 */
@SuppressLint("MissingPermission")
class ConexionHC06(private val manager: BluetoothManager?) {

    private val adapter: BluetoothAdapter? get() = manager?.adapter

    @Volatile
    private var socket: BluetoothSocket? = null

    fun dispositivosEmparejados(): List<BluetoothDevice> =
        adapter?.bondedDevices?.toList().orEmpty()
            .sortedByDescending { nombre(it).contains("HC", ignoreCase = true) }

    fun dispositivoPorDireccion(direccion: String): BluetoothDevice? =
        runCatching { adapter?.getRemoteDevice(direccion) }.getOrNull()

    fun nombre(device: BluetoothDevice): String = device.name ?: device.address

    /** Emparejados con datos listos para la UI (los audífonos se marcan como no compatibles). */
    fun emparejadosDetalle(): List<Emparejado> = dispositivosEmparejados().map {
        Emparejado(
            device = it,
            nombre = nombre(it),
            direccion = it.address,
            esAudio = it.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO,
        )
    }

    /** Abre el socket. Bloqueante: se ejecuta en Dispatchers.IO. */
    suspend fun conectar(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        val bt = adapter ?: throw IOException("Bluetooth no disponible")
        if (!bt.isEnabled) throw IOException("Bluetooth apagado")

        val habiaSocket = socket != null
        cerrar()
        if (habiaSocket) delay(PAUSA_LIBERACION_MS)

        // En Android 12+ cancelDiscovery() exige BLUETOOTH_SCAN, que no pedimos (no escaneamos):
        // antes lanzaba SecurityException y la conexión fallaba siempre.
        runCatching { if (bt.isDiscovering) bt.cancelDiscovery() }

        // Cada teléfono / HC-06 acepta una forma distinta de abrir el RFCOMM: se prueban en orden
        // con pausas suficientes para no saturar el único canal del HC-06 ni crear sockets fantasma.
        val intentos: List<Pair<Long, () -> BluetoothSocket>> = listOf(
            0L to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            600L to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            800L to {
                device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            },
        )
        var ultimoError: Exception? = null
        for ((esperaPrevia, crear) in intentos) {
            currentCoroutineContext().ensureActive()
            if (adapter?.isEnabled != true) throw IOException("Bluetooth apagado")
            if (esperaPrevia > 0L) delay(esperaPrevia)

            val s = try { crear() } catch (e: Exception) { ultimoError = e; continue }
            // Se guarda ANTES de connect() para que cerrar() siempre pueda abortar un
            // intento bloqueado y nunca quede un socket huérfano ocupando el HC-06.
            socket = s
            try {
                s.connect()
                currentCoroutineContext().ensureActive()
                if (!s.isConnected) throw IOException("Canal RFCOMM cerrado al conectar")
                return@withContext
            } catch (e: CancellationException) {
                if (socket === s) socket = null
                runCatching { s.close() }
                throw e
            } catch (e: Exception) {
                ultimoError = e
                if (socket === s) socket = null
                runCatching { s.close() }
            }
        }
        val detalle = ultimoError?.message?.take(60) ?: ultimoError?.javaClass?.simpleName ?: "error desconocido"
        throw IOException("Sin conexión con ${nombre(device)} ($detalle)", ultimoError)
    }

    /** Emite cada línea de texto que manda el Arduino. Termina cuando se cierra la conexión. */
    fun lineas(): Flow<String> = flow {
        val s = socket ?: throw IOException("No hay conexión abierta")
        val lector = s.inputStream.bufferedReader(Charsets.ISO_8859_1)
        while (true) {
            val linea = lector.readLine() ?: break
            emit(linea)
        }
    }.flowOn(Dispatchers.IO)

    /** Envía un comando al Arduino (por ejemplo "C", "R", "1".."4", "T" o "?"). */
    suspend fun enviar(texto: String) = withContext(Dispatchers.IO) {
        val s = socket ?: throw IOException("No hay conexión abierta")
        if (!s.isConnected) {
            cerrar()
            throw IOException("Conexión perdida")
        }
        try {
            s.outputStream.apply {
                write(texto.toByteArray(Charsets.US_ASCII))
                flush()
            }
        } catch (e: IOException) {
            // Si falla la escritura el socket está muerto: cerrarlo desbloquea lineas() para reconectar.
            cerrar()
            throw e
        }
    }

    fun cerrar() {
        val s = socket
        socket = null
        runCatching { s?.close() }
    }

    companion object {
        /** UUID estándar del perfil Serial Port (SPP), el que usa el HC-06. */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PAUSA_LIBERACION_MS = 500L
    }
}
