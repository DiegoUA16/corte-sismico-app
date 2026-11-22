package com.sismografo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sismografo.app.UiState
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.Evento
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular

@Composable
fun PantallaMonitor(
    ui: UiState,
    contentPadding: PaddingValues,
    onIrADispositivo: () -> Unit,
    onIrAHistorial: () -> Unit,
) {
    var ventana by rememberSaveable { mutableIntStateOf(0) }
    val ventanas = listOf(60, 600, 3600)
    val ahora by rememberAhora()
    val nivel = ui.ultima?.let { NivelSismo.de(it.magnitud) }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            EncabezadoPantalla("Sismógrafo", "Monitoreo en tiempo real") {
                ChipConexion(ui.estado, onIrADispositivo)
            }
        }

        if (ui.estado !is EstadoConexion.Conectado) {
            item { AvisoSinConexion(ui.estado, onIrADispositivo) }
        }

        // Medidor principal
        item {
            Tarjeta(padding = 20.dp) {
                MedidorMagnitud(ui.ultima?.magnitud, ui.ajustes.umbral)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (nivel != null) Pildora("${nivel.nombre} · ${nivel.descripcion}", nivel.color)
                    else Pildora("Sin datos todavía", Paleta.TintaSuave)
                }
            }
        }

        // Estadísticas rápidas
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Estadistica("Pico hoy", if (ui.totalLecturas > 0) ui.picoHoy.f1() else "--", Modifier.weight(1f))
                Estadistica("Lecturas", "%,d".format(ui.totalLecturas).replace(',', ' '), Modifier.weight(1f))
                Estadistica(
                    "Última alerta",
                    ui.eventos.firstOrNull()?.let { haceCorto(it.hora, ahora) } ?: "—",
                    Modifier.weight(1f),
                )
            }
        }

        // Actividad en vivo
        item {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Actividad en vivo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Segmentado(listOf("1 min", "10 min", "1 h"), ventana) { ventana = it }
                }
                Spacer(Modifier.height(16.dp))
                Sismograma(ui.lecturas, ui.ajustes.umbral, ventanas[ventana])
            }
        }

        // Escala de Richter
        item {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Escala de Richter", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (nivel != null) Pildora("Actual: ${nivel.nombre}", nivel.color, punto = false)
                }
                Spacer(Modifier.height(16.dp))
                BarraRichter(nivel)
            }
        }

        // Alertas recientes
        item {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alertas recientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onIrAHistorial) { Text("Ver historial") }
                }
                Spacer(Modifier.height(8.dp))
                if (ui.eventos.isEmpty()) {
                    Text(
                        "No se han detectado sismos por encima del umbral (${ui.ajustes.umbral.f1()}).",
                        style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ui.eventos.take(3).forEach { FilaAlertaReciente(it, onIrAHistorial) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoSinConexion(estado: EstadoConexion, onConectar: () -> Unit) {
    Tarjeta(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BluetoothSearching, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (estado) {
                        is EstadoConexion.Conectando -> "Conectando con ${estado.nombre}…"
                        is EstadoConexion.Error -> estado.mensaje
                        else -> "Conecta tu sismógrafo"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Las alertas solo funcionan mientras el HC-06 está conectado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (estado is EstadoConexion.Desconectado) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = onConectar) { Text("Conectar") }
            }
        }
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Tarjeta(modifier = modifier, padding = 16.dp) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Text(
            valor, style = MaterialTheme.typography.headlineSmall.merge(Tabular),
            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
        )
    }
}

@Composable
private fun FilaAlertaReciente(evento: Evento, onClick: () -> Unit) {
    val nivel = NivelSismo.de(evento.magnitud)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InsigniaMagnitud(evento.magnitud)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(nivel.titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                "${tituloDia(evento.hora)} · ${hora(evento.hora)}",
                style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Paleta.TintaSuave)
    }
}
