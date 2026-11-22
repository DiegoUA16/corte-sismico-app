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
import com.sismografo.app.datos.Evento
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.f1

/**
 * Notificación de alerta. El canal es silencioso: el sonido y la vibración los
 * maneja [AlarmaLocal] para respetar los interruptores de Ajustes.
 */
class NotificadorAlertas(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL, "Alertas sísmicas", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando el sismógrafo detecta un sismo"
                enableVibration(false)
                setSound(null, null)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission") // se comprueba abajo
    fun alertar(evento: Evento, pantallaCompleta: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val nivel = NivelSismo.de(evento.magnitud)
        val abrirApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("¡Alerta sísmica! Magnitud ${evento.magnitud.f1()}")
            .setContentText("${nivel.nombre} · ${nivel.descripcion}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${nivel.nombre} · ${nivel.descripcion}\nAgáchate, cúbrete y sujétate."
                )
            )
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
        const val CANAL = "alertas_sismo_v2"
        const val ID_NOTIFICACION = 1
    }
}
