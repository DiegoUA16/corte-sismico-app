package com.sismografo.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sismografo.app.datos.EventoSistema
import com.sismografo.app.datos.TipoEvento
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular

private enum class Filtro(val etiqueta: String, val acepta: (TipoEvento) -> Boolean) {
    TODOS("Todos", { true }),
    CORTES("Cortes", { it.esCorte }),
    GOLPES("Golpes ignorados", { it == TipoEvento.GOLPE_IGNORADO }),
    REARMES("Rearmes", { it == TipoEvento.REARME }),
    CONEXION("Conexión", { it == TipoEvento.CONEXION }),
}

@Composable
fun PantallaEventos(eventos: List<EventoSistema>, contentPadding: PaddingValues, onBorrar: () -> Unit) {
    val context = LocalContext.current
    var filtro by remember { mutableStateOf(Filtro.TODOS) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    val visibles = eventos.filter { filtro.acepta(it.tipo) }
    val porDia = visibles.groupBy { tituloDia(it.hora) }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            EncabezadoPantalla("Eventos", "Registro del sistema") {
                IconButton(onClick = { compartir(context, visibles) }, enabled = visibles.isNotEmpty()) {
                    Icon(Icons.Rounded.Share, "Compartir")
                }
                IconButton(onClick = { confirmarBorrado = true }, enabled = eventos.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteOutline, "Borrar registro")
                }
            }
        }
        item { Resumen(eventos) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Filtro.entries) { f ->
                    ChipFiltro(f.etiqueta, filtro == f) { filtro = f }
                }
            }
        }

        if (visibles.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconoCuadrado(Icons.Rounded.EventNote, Paleta.TintaSuave, 64.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Sin eventos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Aquí verás cada corte, rearme, golpe ignorado y conexión con el equipo.",
                        style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave, textAlign = TextAlign.Center,
                    )
                }
            }
        }

        porDia.forEach { (dia, lista) ->
            item(key = "h-$dia") {
                Text(
                    dia, Modifier.padding(top = 8.dp, start = 4.dp),
                    style = MaterialTheme.typography.labelLarge, color = Paleta.TintaSuave,
                )
            }
            item(key = "l-$dia-${lista.first().hora}") {
                Tarjeta(padding = 8.dp) {
                    lista.forEachIndexed { i, e ->
                        FilaEvento(e)
                        if (i < lista.lastIndex) HorizontalDivider(Modifier.padding(start = 64.dp), color = Paleta.Recesado)
                    }
                }
            }
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            containerColor = Color.White,
            title = { Text("¿Borrar todo el registro?") },
            text = { Text("Se eliminarán ${eventos.size} eventos guardados en este teléfono. No afecta al equipo.") },
            confirmButton = {
                Button(
                    onClick = { confirmarBorrado = false; onBorrar() },
                    colors = ButtonDefaults.buttonColors(containerColor = Paleta.Alerta),
                ) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Resumen(eventos: List<EventoSistema>) {
    val desde = System.currentTimeMillis() - 30L * 86_400_000
    val mes = eventos.filter { it.hora >= desde }
    Tarjeta {
        Text("Últimos 30 días", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metrica(mes.count { it.tipo.esCorte }, "cortes", null, Paleta.Alerta, Modifier.weight(1f))
            Metrica(mes.count { it.tipo == TipoEvento.GOLPE_IGNORADO }, "ignorados", "golpes filtrados", Paleta.Ambar, Modifier.weight(1f))
            Metrica(mes.count { it.tipo == TipoEvento.REARME }, "rearmes", null, Paleta.Exito, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Metrica(valor: Int, etiqueta: String, nota: String?, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.08f)).padding(12.dp)) {
        Text(
            valor.toString(), style = MaterialTheme.typography.headlineMedium.merge(Tabular),
            fontWeight = FontWeight.ExtraBold, color = color,
        )
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Paleta.Tinta)
        if (nota != null) Text(nota, style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave)
    }
}

@Composable
private fun FilaEvento(e: EventoSistema) {
    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconoCuadrado(iconoEvento(e.tipo), e.tipo.color, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.tipo.titulo, style = MaterialTheme.typography.titleSmall)
            Text(e.detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
        }
        Spacer(Modifier.width(8.dp))
        Text(horaSeg(e.hora), style = MaterialTheme.typography.labelLarge.merge(Tabular), color = Paleta.Tinta)
    }
}

private fun compartir(context: android.content.Context, eventos: List<EventoSistema>) {
    val texto = buildString {
        appendLine("Registro · Corte Sísmico")
        eventos.forEach { appendLine("${tituloDia(it.hora)} ${horaSeg(it.hora)} — ${it.tipo.titulo}: ${it.detalle}") }
    }
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
    context.startActivity(Intent.createChooser(intent, "Compartir registro"))
}
