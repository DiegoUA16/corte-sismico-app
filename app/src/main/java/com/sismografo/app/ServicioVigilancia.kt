package com.sismografo.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.EstadoSistema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano (Foreground Service) que mantiene viva la conexión
 * Bluetooth con el HC-06 incluso si la pantalla se apaga o la app pasa a segundo plano.
 *
 * Su notificación es fija, silenciosa y `localOnly = true` para no saturar ni hacer
 * vibrar smartwatches o pulseras (al reloj solo le llegan las alertas de corte).
 */
class ServicioVigilancia : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observador: Job? = null

    private val monitor: MonitorSismico
        get() = (application as CorteSismicoApp).monitor

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DETENER) {
            monitor.desconectar()
            detenerServicio()
            return START_NOT_STICKY
        }

        if (!iniciarForegroundSeguro(construirNotificacion(monitor.ui.value))) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Si Android reinició el servicio tras liberar memoria, retoma el último HC-06.
        if (intent == null) {
            monitor.autoconectar()
        }

        if (observador == null) {
            observador = scope.launch {
                monitor.ui
                    .map { ResumenEstado(it.estado, it.sistema, it.dispositivo?.nombre, it.modoDemo) }
                    .distinctUntilChanged()
                    .collect { resumen ->
                        if (resumen.modoDemo || resumen.estado is EstadoConexion.Desconectado) {
                            detenerServicio()
                        } else {
                            actualizarNotificacion(monitor.ui.value)
                        }
                    }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observador?.cancel()
        observador = null
        scope.cancel()
        super.onDestroy()
    }

    private fun detenerServicio() {
        observador?.cancel()
        observador = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun iniciarForegroundSeguro(notificacion: Notification): Boolean {
        val tienePermisoBt = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        if (!tienePermisoBt) return false

        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
        return runCatching {
            ServiceCompat.startForeground(this, ID_NOTIFICACION, notificacion, tipo)
            true
        }.getOrDefault(false)
    }

    private fun actualizarNotificacion(ui: UiState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching {
            NotificationManagerCompat.from(this).notify(ID_NOTIFICACION, construirNotificacion(ui))
        }
    }

    private fun construirNotificacion(ui: UiState): Notification {
        val abrirApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val detener = PendingIntent.getService(
            this, 1,
            Intent(this, ServicioVigilancia::class.java).setAction(ACTION_DETENER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val nombre = ui.dispositivo?.nombre ?: "HC-06"
        val (titulo, texto) = when (val e = ui.estado) {
            is EstadoConexion.Conectado -> when (ui.sistema) {
                EstadoSistema.CORTE -> "Corte activo · $nombre" to "Gas cerrado y energía cortada"
                EstadoSistema.NORMAL -> "Vigilando · $nombre" to "Conectado en segundo plano · Sin vibraciones peligrosas"
                EstadoSistema.DESCONOCIDO -> "Conectado a $nombre" to "Vigilancia activa · Esperando confirmación de estado"
            }
            is EstadoConexion.Conectando -> "Conectando con ${e.nombre}…" to "Enlazando por Bluetooth (SPP)"
            is EstadoConexion.Error -> "Vigilancia en espera · $nombre" to e.mensaje
            EstadoConexion.Desconectado -> "Corte Sísmico" to "Servicio inactivo"
        }

        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setColor(if (ui.sistema == EstadoSistema.CORTE) 0xFFDC2626.toInt() else 0xFF0E5A6D.toInt())
            .setOngoing(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(abrirApp)
            .addAction(0, "Detener", detener)
            .build()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL, "Vigilancia activa (segundo plano)", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la conexión Bluetooth activa con el equipo en segundo plano"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private data class ResumenEstado(
        val estado: EstadoConexion,
        val sistema: EstadoSistema,
        val nombre: String?,
        val modoDemo: Boolean,
    )

    companion object {
        private const val CANAL = "vigilancia_activa_v1"
        private const val ID_NOTIFICACION = 1001
        private const val ACTION_DETENER = "com.sismografo.app.ACCION_DETENER_VIGILANCIA"

        fun iniciar(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, ServicioVigilancia::class.java))
            }
        }

        fun detener(context: Context) {
            runCatching {
                context.stopService(Intent(context, ServicioVigilancia::class.java))
            }
        }
    }
}
