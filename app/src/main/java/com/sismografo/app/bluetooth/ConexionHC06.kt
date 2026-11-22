package com.sismografo.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
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
class ConexionHC06(private val adapter: BluetoothAdapter?) {

    private var socket: BluetoothSocket? = null

    fun dispositivosEmparejados(): List<BluetoothDevice> =
        adapter?.bondedDevices?.toList().orEmpty()
            .sortedByDescending { nombre(it).contains("HC", ignoreCase = true) }

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
        cerrar()
        // En Android 12+ cancelDiscovery() exige BLUETOOTH_SCAN, que no pedimos (no escaneamos):
        // antes lanzaba SecurityException y la conexión fallaba siempre.
        runCatching { if (adapter?.isDiscovering == true) adapter.cancelDiscovery() }

        // Cada teléfono / HC-06 acepta una forma distinta de abrir el RFCOMM: se prueban en orden.
        val intentos: List<() -> BluetoothSocket> = listOf(
            { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            {
                device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            },
        )
        var ultimoError: Exception? = null
        for (crear in intentos) {
            val s = try { crear() } catch (e: Exception) { ultimoError = e; continue }
            try {
                s.connect()
                socket = s
                return@withContext
            } catch (e: Exception) {
                ultimoError = e
                runCatching { s.close() }
            }
        }
        throw IOException("No se pudo abrir el canal con ${nombre(device)}", ultimoError)
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

    /** Envía un comando al Arduino (por ejemplo "CAL\n" para recalibrar). */
    suspend fun enviar(texto: String) = withContext(Dispatchers.IO) {
        socket?.outputStream?.apply {
            write(texto.toByteArray(Charsets.US_ASCII))
            flush()
        }
    }

    fun cerrar() {
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        /** UUID estándar del perfil Serial Port (SPP), el que usa el HC-06. */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
