package com.sismografo.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notificación cuando el equipo avisa que ejecutó el corte (o en prueba de alerta).
 *
 * Configurada para que smartwatches y pulseras (Wear OS, Xiaomi Smart Band vía Mi Fitness, etc.)
 * la reflejen sin filtrarla:
 * - Canal de alta importancia con vibración habilitada (no se marca como silencioso).
 * - `setLocalOnly(false)`, `VISIBILITY_PUBLIC` y `CATEGORY_EVENT`.
 * - El sonido de alarma en bucle del teléfono sigue en [AlarmaLocal] para respetar Ajustes.
 */
class NotificadorAlertas(private val context: Context) {

    @Volatile
    private var ultimoIdAlerta = ID_ALERTA_BASE

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL, "Alertas de corte sísmico", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso urgente en el teléfono y reloj cuando se corta el gas y la energía"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission") // se comprueba abajo
    fun alertar(hora: Long, manual: Boolean, pantallaCompleta: Boolean, esPrueba: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val abrirApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val titulo = when {
            esPrueba -> "Prueba de alerta · Corte Sísmico"
            manual -> "Corte manual ejecutado"
            else -> "¡Alerta sísmica! Corte ejecutado"
        }
        val texto = if (esPrueba) {
            "Prueba de aviso en teléfono y reloj. Gas y energía sin cambios."
        } else {
            "Gas cerrado y energía cortada."
        }
        val largo = when {
            esPrueba -> "$texto\nSi ves esto en tu smartwatch o Smart Band, las alertas ya están llegando bien."
            manual -> "$texto\nRearma desde la app o con el botón del equipo cuando sea seguro."
            else -> "$texto\nAgáchate, cúbrete y sujétate. No enciendas fuego ni interruptores."
        }

        // ID distinto por evento para que el reloj vuelva a vibrar ante cada nueva alerta.
        val id = ID_ALERTA_BASE + ((hora / 1000L) % 10_000L).toInt()
        val anterior = ultimoIdAlerta
        ultimoIdAlerta = id
        val manager = NotificationManagerCompat.from(context)
        if (anterior != id) manager.cancel(anterior)

        val builder = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(largo))
            .setWhen(hora)
            .setShowWhen(true)
            .setColor(0xFFDC2626.toInt())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocalOnly(false)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
        if (pantallaCompleta && !esPrueba) builder.setFullScreenIntent(abrirApp, true)

        manager.notify(id, builder.build())
    }

    fun cancelar() = NotificationManagerCompat.from(context).cancel(ultimoIdAlerta)

    private companion object {
        const val CANAL = "alertas_corte_reloj_v1"
        const val ID_ALERTA_BASE = 2000
    }
}
