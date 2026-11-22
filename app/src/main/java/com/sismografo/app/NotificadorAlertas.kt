package com.sismografo.app

import android.Manifest
import android.annotation.SuppressLint
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
 * Notificación cuando el equipo avisa que ejecutó el corte. El canal es
 * silencioso: el sonido y la vibración los maneja [AlarmaLocal] para respetar Ajustes.
 */
class NotificadorAlertas(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL, "Cortes preventivos", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando el equipo cierra el gas y corta la energía"
                enableVibration(false)
                setSound(null, null)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission") // se comprueba abajo
    fun alertar(hora: Long, manual: Boolean, pantallaCompleta: Boolean) {
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

        val titulo = if (manual) "Corte manual ejecutado" else "¡Alerta sísmica! Corte preventivo ejecutado"
        val texto = "Gas cerrado y energía cortada."
        val largo = if (manual) {
            "$texto\nRearma desde la app o con el botón del equipo cuando sea seguro."
        } else {
            "$texto\nAgáchate, cúbrete y sujétate. No enciendas fuego ni interruptores."
        }

        val builder = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(largo))
            .setWhen(hora)
            .setColor(0xFFDC2626.toInt())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
        if (pantallaCompleta) builder.setFullScreenIntent(abrirApp, true)

        NotificationManagerCompat.from(context).notify(ID_NOTIFICACION, builder.build())
    }

    fun cancelar() = NotificationManagerCompat.from(context).cancel(ID_NOTIFICACION)

    private companion object {
        const val CANAL = "cortes_preventivos"
        const val ID_NOTIFICACION = 1
    }
}
