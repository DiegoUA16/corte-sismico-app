package com.sismografo.app.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sismografo.app.datos.Evento
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular
import java.util.Calendar

private enum class Periodo(val etiqueta: String, val dias: Int?) {
    TODOS("Todos", null), HOY("Hoy", 0), SEMANA("7 días", 7), MES("30 días", 30)
}

@Composable
fun PantallaHistorial(
    eventos: List<Evento>,
    contentPadding: PaddingValues,
    onBorrar: () -> Unit,
) {
    var periodo by rememberSaveable { mutableStateOf(Periodo.TODOS) }
    var minimo by rememberSaveable { mutableStateOf<Double?>(null) }
    var expandido by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val filtrados = remember(eventos, periodo, minimo) {
        val corte = periodo.dias?.let { inicioDeDia(-it) }
        eventos.filter { e -> (corte == null || e.hora >= corte) && (minimo == null || e.magnitud >= minimo!!) }
    }
    val porDia = remember(filtrados) { filtrados.groupBy { tituloDia(it.hora) } }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            EncabezadoPantalla("Historial", "${eventos.size} eventos registrados")
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Periodo.entries.forEach { p -> ChipFiltro(p.etiqueta, periodo == p) { periodo = p } }
                Box(Modifier.width(1.dp).height(24.dp).background(Paleta.Borde))
                ChipFiltro("≥ 4.0", minimo == 4.0, NivelSismo.LIGERO.color) { minimo = if (minimo == 4.0) null else 4.0 }
                ChipFiltro("≥ 5.0", minimo == 5.0, NivelSismo.MODERADO.color) { minimo = if (minimo == 5.0) null else 5.0 }
            }
        }
        item { Resumen(eventos) }

        if (filtrados.isEmpty()) {
            item { EstadoVacio(eventos.isEmpty()) }
        }

        porDia.forEach { (dia, delDia) ->
            item(key = "h$dia") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dia.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Pildora(
                        if (delDia.size == 1) "1 evento" else "${delDia.size} eventos",
                        MaterialTheme.colorScheme.primary, punto = false,
                    )
                }
            }
            items(delDia, key = { it.hora }) { e ->
                FilaEvento(e, expandido == e.hora) { expandido = if (expandido == e.hora) null else e.hora }
            }
        }

        if (eventos.isNotEmpty()) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { confirmarBorrado = true }) {
                        Icon(Icons.Rounded.DeleteOutline, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Borrar historial")
                    }
                }
            }
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar el historial?") },
            text = { Text("Se eliminarán los ${eventos.size} sismos registrados en este teléfono.") },
            confirmButton = {
                TextButton(onClick = { onBorrar(); confirmarBorrado = false }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Resumen(eventos: List<Evento>) {
    val ultimos30 = eventos.filter { it.hora >= inicioDeDia(-30) }
    // máximo de cada uno de los últimos 14 días (índice 13 = hoy)
    val porDia = (13 downTo 0).map { atras ->
        val ini = inicioDeDia(-atras)
        val fin = inicioDeDia(-atras + 1)
        eventos.filter { it.hora in ini until fin }.maxOfOrNull { it.magnitud }
    }

    Tarjeta {
        TituloTarjeta("Resumen de actividad", subtitulo = "Últimos 30 días")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metrica("Detectados", ultimos30.size.toString(), null, Modifier.weight(1f), unidad = null)
            val maximo = ultimos30.maxOfOrNull { it.magnitud }
            Metrica("Máx. magnitud", maximo?.f1() ?: "--", maximo?.let { NivelSismo.de(it).color }, Modifier.weight(1f))
            Metrica("Promedio", ultimos30.map { it.magnitud }.average().takeIf { !it.isNaN() }?.f1() ?: "--", null, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text("Distribución diaria (14 días)", style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            val hueco = 6.dp.toPx()
            val ancho = (size.width - hueco * 13) / 14
            val tope = maxOf(6.0, porDia.filterNotNull().maxOrNull() ?: 0.0)
            porDia.forEachIndexed { i, m ->
                val alto = if (m == null) 4.dp.toPx() else (size.height * (m / tope)).toFloat().coerceAtLeast(8.dp.toPx())
                drawRoundRect(
                    color = m?.let { NivelSismo.de(it).color } ?: Paleta.Borde,
                    topLeft = Offset(i * (ancho + hueco), size.height - alto),
                    size = Size(ancho, alto),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Hace 14 días", style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave, modifier = Modifier.weight(1f))
            Text("Hoy", style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave)
        }
    }
}

@Composable
private fun Metrica(etiqueta: String, valor: String, color: androidx.compose.ui.graphics.Color?, modifier: Modifier, unidad: String? = "Mw") {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado).padding(12.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                valor, style = MaterialTheme.typography.headlineSmall.merge(Tabular),
                fontWeight = FontWeight.Bold, color = color ?: Paleta.Tinta,
            )
            if (valor != "--" && unidad != null) {
                Text(" $unidad", style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun FilaEvento(evento: Evento, abierto: Boolean, onClick: () -> Unit) {
    val nivel = NivelSismo.de(evento.magnitud)
    val context = LocalContext.current

    Tarjeta(padding = 16.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InsigniaMagnitud(evento.magnitud)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(nivel.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${hora(evento.hora)} · Duración ${evento.duracionSeg.coerceAtLeast(1)} s",
                    style = MaterialTheme.typography.bodySmall.merge(Tabular), color = Paleta.TintaSuave,
                )
            }
            Icon(if (abierto) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Paleta.TintaSuave)
        }
        AnimatedVisibility(abierto) {
            Column(Modifier.padding(top = 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metrica("Magnitud máx.", evento.magnitud.f1(), nivel.color, Modifier.weight(1f))
                    Metrica("Hora exacta", horaSeg(evento.hora), null, Modifier.weight(1.3f), unidad = null)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(nivel.color.copy(alpha = 0.10f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(nivel.color))
                    Spacer(Modifier.width(10.dp))
                    Text("${nivel.nombre}: ${nivel.descripcion}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val texto = "Sismógrafo: ${nivel.titulo} de magnitud ${evento.magnitud.f1()} " +
                                "detectado el ${tituloDia(evento.hora)} a las ${horaSeg(evento.hora)} " +
                                "(duración ${evento.duracionSeg.coerceAtLeast(1)} s)."
                            val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
                            context.startActivity(Intent.createChooser(envio, "Compartir sismo"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir")
                    }
                    Button(onClick = onClick, modifier = Modifier.weight(1f)) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio(sinNada: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.EventNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(if (sinNada) "Sin sismos registrados" else "Nada con estos filtros", style = MaterialTheme.typography.titleMedium)
        Text(
            if (sinNada) "Aquí aparecerá cada sismo que supere tu umbral de alerta."
            else "Prueba con otro periodo o quita el filtro de magnitud.",
            style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave,
        )
    }
}

/** Medianoche de hoy + [desplazamiento] días. */
private fun inicioDeDia(desplazamiento: Int): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_YEAR, desplazamiento)
}.timeInMillis
