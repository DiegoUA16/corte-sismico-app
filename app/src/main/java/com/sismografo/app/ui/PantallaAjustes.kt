package com.sismografo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sismografo.app.BuildConfig
import com.sismografo.app.datos.Ajustes
import com.sismografo.app.datos.PerfilAlarma
import com.sismografo.app.ui.tema.Paleta

@Composable
fun PantallaAjustes(
    ajustes: Ajustes,
    modoDemo: Boolean,
    conectadoEquipo: Boolean,
    enCorte: Boolean,
    contentPadding: PaddingValues,
    onCambio: ((Ajustes) -> Ajustes) -> Unit,
    onElegirAlarma: (Int) -> Unit,
    onProbarAlarmaEquipo: () -> Unit,
    onProbarAlerta: () -> Unit,
    onIniciarDemo: () -> Unit,
    onSalirDemo: () -> Unit,
) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { EncabezadoPantalla("Ajustes", "Avisos y sirena del equipo") }

        item {
            Tarjeta {
                TituloTarjeta("Cuando el equipo corte", Icons.Rounded.NotificationsActive)
                Spacer(Modifier.height(8.dp))
                FilaInterruptor(Icons.Rounded.Notifications, "Notificación", "Aviso en el teléfono y reloj", ajustes.notificacion) { v ->
                    onCambio { it.copy(notificacion = v) }
                }
                HorizontalDivider(color = Paleta.Recesado)
                FilaInterruptor(Icons.Rounded.VolumeUp, "Sonido de alarma", "Usa el volumen de alarma del teléfono", ajustes.sonido) { v ->
                    onCambio { it.copy(sonido = v) }
                }
                HorizontalDivider(color = Paleta.Recesado)
                FilaInterruptor(Icons.Rounded.Vibration, "Vibración", "Patrón continuo durante 60 s", ajustes.vibracion) { v ->
                    onCambio { it.copy(vibracion = v) }
                }
                HorizontalDivider(color = Paleta.Recesado)
                FilaInterruptor(Icons.Rounded.Fullscreen, "Alerta a pantalla completa", "Con instrucciones de seguridad", ajustes.pantallaCompleta) { v ->
                    onCambio { it.copy(pantallaCompleta = v) }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onProbarAlerta, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Probar alerta en teléfono y reloj")
                }
            }
        }

        item {
            val puedeControlar = conectadoEquipo && !enCorte
            Tarjeta {
                TituloTarjeta(
                    "Sirena del equipo",
                    Icons.Rounded.GraphicEq,
                    if (conectadoEquipo) "Buzzer del Arduino · cambia el tono en vivo"
                    else "Conéctate al Arduino para cambiar o probar la sirena",
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerfilAlarma.entries.forEach { perfil ->
                        val seleccionado = ajustes.perfilAlarma == perfil.codigo
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (seleccionado) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else Paleta.Recesado)
                                .border(
                                    1.5.dp,
                                    if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable(enabled = !enCorte) { onElegirAlarma(perfil.codigo) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (seleccionado) MaterialTheme.colorScheme.primary else Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (seleccionado) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("${perfil.codigo}", style = MaterialTheme.typography.labelMedium, color = Paleta.TintaSuave)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(perfil.nombre, style = MaterialTheme.typography.titleSmall)
                                Text(perfil.detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onProbarAlarmaEquipo,
                    enabled = puedeControlar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.VolumeUp, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            enCorte -> "Sirena sonando en corte"
                            conectadoEquipo -> "Probar sonido en el equipo"
                            else -> "Conéctate para probar el buzzer"
                        },
                    )
                }
            }
        }

        item {
            Tarjeta {
                TituloTarjeta("Conexión y registro", Icons.Rounded.Autorenew)
                Spacer(Modifier.height(8.dp))
                FilaInterruptor(Icons.Rounded.Autorenew, "Reconectar solo", "Reintenta cada 3 s si se pierde el HC-06", ajustes.reconectar) { v ->
                    onCambio { it.copy(reconectar = v) }
                }
                HorizontalDivider(color = Paleta.Recesado)
                FilaInterruptor(Icons.Rounded.Vibration, "Registrar golpes ignorados", "Guarda los golpes descartados por el filtro", ajustes.registrarGolpes) { v ->
                    onCambio { it.copy(registrarGolpes = v) }
                }
            }
        }

        item {
            Tarjeta(color = Color(0xFFF3EEFF)) {
                TituloTarjeta("Modo demostración", Icons.Rounded.Build, "Para presentar la app sin el Arduino")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Simula el ADXL345 y el filtro de 4 picos dentro del teléfono.",
                    style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave,
                )
                Spacer(Modifier.height(12.dp))
                if (modoDemo) {
                    OutlinedButton(onClick = onSalirDemo, modifier = Modifier.fillMaxWidth()) { Text("Salir de la demostración") }
                } else {
                    Button(
                        onClick = onIniciarDemo, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    ) { Text("Iniciar demostración") }
                }
            }
        }

        item {
            Text(
                "Corte Sísmico ${BuildConfig.VERSION_NAME} · Comandos Bluetooth: C (corte), R (rearme), 1–4 (sirena) y T (prueba).",
                Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave,
            )
        }
    }
}
