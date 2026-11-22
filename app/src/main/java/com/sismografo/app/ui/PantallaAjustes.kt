package com.sismografo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismografo.app.datos.Ajustes
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular
import kotlin.math.roundToInt

@Composable
fun PantallaAjustes(
    ajustes: Ajustes,
    contentPadding: PaddingValues,
    onCambio: ((Ajustes) -> Ajustes) -> Unit,
    onProbarAlerta: () -> Unit,
) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { EncabezadoPantalla("Ajustes", "Preferencias y alertas") }

        item { TarjetaUmbral(ajustes.umbral) { v -> onCambio { it.copy(umbral = v) } } }

        item {
            Tarjeta {
                TituloTarjeta("Cómo te avisamos", Icons.Rounded.VolumeUp, "Respuesta visual, táctil y sonora")
                Spacer(Modifier.height(8.dp))
                FilaInterruptor(
                    Icons.Rounded.NotificationsActive, "Notificación", "Aviso aunque estés en otra app",
                    ajustes.notificacion,
                ) { v -> onCambio { it.copy(notificacion = v) } }
                FilaInterruptor(
                    Icons.Rounded.Vibration, "Vibración", "Patrón continuo hasta que confirmes",
                    ajustes.vibracion,
                ) { v -> onCambio { it.copy(vibracion = v) } }
                FilaInterruptor(
                    Icons.Rounded.VolumeUp, "Sonido de alarma", "Usa el tono de alarma del teléfono",
                    ajustes.sonido,
                ) { v -> onCambio { it.copy(sonido = v) } }
                FilaInterruptor(
                    Icons.Rounded.Fullscreen, "Pantalla completa", "Abre la vista de emergencia automáticamente",
                    ajustes.pantallaCompleta,
                ) { v -> onCambio { it.copy(pantallaCompleta = v) } }
            }
        }

        item {
            Tarjeta {
                TituloTarjeta("Monitoreo", Icons.Rounded.SettingsInputAntenna, "Enlace con el HC-06")
                Spacer(Modifier.height(8.dp))
                FilaInterruptor(
                    Icons.Rounded.Sync, "Reconectar automáticamente", "Reintenta cada 3 s si se pierde la señal",
                    ajustes.reconectar,
                ) { v -> onCambio { it.copy(reconectar = v) } }
            }
        }

        item {
            FilledTonalButton(onClick = onProbarAlerta, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Rounded.NotificationsActive, null)
                Spacer(Modifier.width(10.dp))
                Text("Probar alerta")
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sismógrafo · Versión 1.0", style = MaterialTheme.typography.labelMedium, color = Paleta.TintaSuave)
                Text(
                    "Proyecto Electiva Profesional · HC-06",
                    style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave,
                )
            }
        }
    }
}

@Composable
private fun TarjetaUmbral(umbral: Float, onCambio: (Float) -> Unit) {
    val nivel = NivelSismo.de(umbral.toDouble())
    Tarjeta {
        TituloTarjeta("Umbral de alerta", Icons.Rounded.Tune, "A partir de qué magnitud te avisamos")
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                umbral.f1(), style = MaterialTheme.typography.displayMedium.merge(Tabular),
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                " Mw", style = MaterialTheme.typography.titleMedium, color = Paleta.TintaSuave,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Pildora(nivel.nombre, nivel.color, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(8.dp))

        // Pista con degradado de la escala de Richter detrás de un Slider transparente
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(10.dp).clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NivelSismo.MENOR.color, NivelSismo.LIGERO.color, NivelSismo.MODERADO.color,
                                NivelSismo.FUERTE.color, NivelSismo.MAYOR.color, NivelSismo.GRAN.color,
                            )
                        )
                    ),
            )
            Slider(
                value = umbral,
                onValueChange = { onCambio((it * 2).roundToInt() / 2f) }, // pasos de 0.5
                valueRange = 2f..8f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            (2..8).forEach { v ->
                val n = NivelSismo.de(v.toDouble())
                val activo = n == nivel
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$v.0", style = MaterialTheme.typography.labelMedium.merge(Tabular),
                        color = if (activo) n.color else Paleta.Tinta,
                    )
                    Text(n.corto, fontSize = 10.sp, color = if (activo) n.color else Paleta.TintaSuave, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado).padding(14.dp),
        ) {
            Icon(Icons.Rounded.Info, null, tint = Paleta.TintaSuave, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Recibirás una alerta cuando la magnitud estimada alcance ${umbral.f1()} o más. " +
                    "Si el Arduino envía ALERTA, se avisa siempre.",
                style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave,
            )
        }
    }
}
