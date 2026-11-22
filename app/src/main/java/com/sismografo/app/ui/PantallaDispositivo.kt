package com.sismografo.app.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sismografo.app.UiState
import com.sismografo.app.bluetooth.Emparejado
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular

@Composable
fun PantallaDispositivo(
    ui: UiState,
    contentPadding: PaddingValues,
    permisoBt: Boolean,
    btEncendido: Boolean,
    emparejados: List<Emparejado>,
    onPedirPermiso: () -> Unit,
    onEncenderBt: () -> Unit,
    onConectar: (BluetoothDevice) -> Unit,
    onDesconectar: () -> Unit,
    onRecalibrar: () -> Unit,
    onRefrescar: () -> Unit,
    onAbrirAjustesBt: () -> Unit,
) {
    val conectado = ui.estado is EstadoConexion.Conectado

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            EncabezadoPantalla("Dispositivo", "Conexión con el sismógrafo") {
                IconButton(onClick = onRefrescar) { Icon(Icons.Rounded.Refresh, "Actualizar lista") }
            }
        }

        item { TarjetaConexion(ui, onDesconectar, onRecalibrar) }

        if (conectado) item { Diagnostico(ui) }

        // Permisos / Bluetooth apagado
        when {
            !permisoBt -> item {
                AvisoAccion(
                    Icons.Rounded.Bluetooth, "Permite el acceso a Bluetooth",
                    "Lo necesitamos para hablar con el módulo HC-06.", "Permitir", onPedirPermiso,
                )
            }
            !btEncendido -> item {
                AvisoAccion(
                    Icons.Rounded.BluetoothDisabled, "El Bluetooth está apagado",
                    "Enciéndelo para ver tus dispositivos emparejados.", "Encender", onEncenderBt,
                )
            }
            else -> {
                item {
                    Row(Modifier.padding(top = 4.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Dispositivos emparejados", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Pildora(emparejados.size.toString(), Paleta.TintaSuave, punto = false)
                    }
                }
                items(emparejados, key = { it.direccion }) { e ->
                    FilaEmparejado(
                        e,
                        activo = ui.dispositivo?.direccion == e.direccion && ui.estado !is EstadoConexion.Desconectado,
                        onConectar = { onConectar(e.device) },
                    )
                }
            }
        }

        item {
            Tarjeta(color = MaterialTheme.colorScheme.primaryContainer) {
                Row {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "¿No aparece tu sismógrafo?", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Emparéjalo primero en los ajustes de Bluetooth del teléfono con el PIN 1234 o 0000.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onAbrirAjustesBt) {
                            Text("Abrir ajustes de Bluetooth")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaConexion(ui: UiState, onDesconectar: () -> Unit, onRecalibrar: () -> Unit) {
    val estado = ui.estado
    val ahora by rememberAhora()
    val conectado = estado is EstadoConexion.Conectado

    val fondo = Brush.linearGradient(listOf(Color(0xFFE9F6F1), Color.White, Color(0xFFE6F2F6)))
    Surface24(fondo) {
        Ilustracion(conectado)
        Spacer(Modifier.height(20.dp))

        val titulo = when (estado) {
            is EstadoConexion.Conectado -> "Conectado a ${estado.nombre}"
            is EstadoConexion.Conectando -> "Conectando con ${estado.nombre}…"
            is EstadoConexion.Error -> "Conexión perdida"
            EstadoConexion.Desconectado -> "Sin conexión"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(10.dp))
            when (estado) {
                is EstadoConexion.Conectado -> Pildora("En línea", Paleta.Exito)
                is EstadoConexion.Conectando -> Pildora("Enlazando", Color(0xFFD97706))
                is EstadoConexion.Error -> Pildora("Reintentando", Color(0xFFD97706))
                EstadoConexion.Desconectado -> {}
            }
        }
        Text(
            when {
                estado is EstadoConexion.Error -> estado.mensaje
                ui.dispositivo != null -> "${ui.dispositivo.direccion} · Bluetooth clásico (SPP)"
                else -> "Elige tu HC-06 en la lista de abajo para empezar."
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = Paleta.TintaSuave,
        )

        if (conectado) {
            Spacer(Modifier.height(16.dp))
            val recientes = ui.lecturas.count { it.hora >= ahora - 10_000 }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniDato(Icons.Rounded.Timer, "Tiempo", ui.conectadoDesde?.let { duracionLarga(ahora - it) } ?: "--", Modifier.weight(1f))
                MiniDato(Icons.Rounded.Speed, "Flujo", "%.1f lect/s".format(recientes / 10f), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onDesconectar, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFFBE3DD), contentColor = Color(0xFFB3261E),
                    ),
                ) {
                    Icon(Icons.Rounded.LinkOff, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Desconectar")
                }
                Button(onClick = onRecalibrar, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Tune, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Recalibrar")
                }
            }
        } else if (estado !is EstadoConexion.Desconectado) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onDesconectar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
    }
}

/** Teléfono ↔ sismógrafo unidos por una línea punteada animada cuando hay datos. */
@Composable
private fun Ilustracion(conectado: Boolean) {
    val t = rememberInfiniteTransition(label = "flujo")
    val fase by t.animateFloat(
        0f, 40f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "fase",
    )
    val color = if (conectado) MaterialTheme.colorScheme.primary else Paleta.Borde

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.7f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NodoIlustracion(Icons.Rounded.PhoneAndroid, "Este teléfono", Paleta.Recesado, Paleta.Tinta)
        Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(4.dp)) {
                drawLine(
                    color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 16f), if (conectado) -fase else 0f),
                )
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (conectado) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled, null,
                    tint = color, modifier = Modifier.size(20.dp),
                )
            }
        }
        NodoIlustracion(Icons.Rounded.Memory, "Sismógrafo", MaterialTheme.colorScheme.primary, Color.White)
    }
}

@Composable
private fun NodoIlustracion(icono: ImageVector, etiqueta: String, fondo: Color, tinta: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(fondo),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, null, tint = tinta, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Paleta.TintaSuave)
    }
}

@Composable
private fun MiniDato(icono: ImageVector, etiqueta: String, valor: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.8f)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Paleta.TintaSuave)
        }
        Spacer(Modifier.height(4.dp))
        Text(valor, style = MaterialTheme.typography.titleMedium.merge(Tabular), maxLines = 1)
    }
}

@Composable
private fun Diagnostico(ui: UiState) {
    val ahora by rememberAhora()
    Tarjeta {
        TituloTarjeta("Diagnóstico del enlace", Icons.Rounded.Code)
        Spacer(Modifier.height(14.dp))
        FilaDiagnostico("Lecturas recibidas", "%,d".format(ui.totalLecturas).replace(',', ' '))
        FilaDiagnostico("Lecturas en la última hora", ui.lecturas.size.toString())
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Última trama serie", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (ui.ultimaLinea != null) {
                    PuntoPulsante(Paleta.Exito, 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "hace ${((ahora - ui.ultimaLineaHora) / 1000).coerceAtLeast(0)} s",
                        style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Paleta.TintaSuave,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                ui.ultimaLinea ?: "Todavía no llega ninguna línea",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FilaDiagnostico(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleSmall.merge(Tabular))
    }
}

@Composable
private fun FilaEmparejado(e: Emparejado, activo: Boolean, onConectar: () -> Unit) {
    val compatible = !e.esAudio
    Tarjeta(padding = 14.dp, modifier = Modifier.alpha(if (compatible) 1f else 0.55f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (activo) MaterialTheme.colorScheme.primaryContainer else Paleta.Recesado),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (e.esAudio) Icons.Rounded.Headset else Icons.Rounded.Bluetooth, null,
                    tint = if (activo) MaterialTheme.colorScheme.primary else Paleta.TintaSuave,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    e.direccion, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Paleta.TintaSuave,
                )
                when {
                    activo -> Text("Conectado actualmente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    !compatible -> Text("No es un sismógrafo", style = MaterialTheme.typography.labelMedium, color = Paleta.Alerta)
                }
            }
            when {
                activo -> Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, "Conectado", tint = Color.White) }
                compatible -> Text(
                    "Conectar",
                    Modifier.clip(CircleShape).background(Paleta.Recesado).clickable(onClick = onConectar)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                else -> Pildora("Incompatible", Paleta.TintaSuave, punto = false)
            }
        }
    }
}

@Composable
private fun AvisoAccion(icono: ImageVector, titulo: String, texto: String, accion: String, onAccion: () -> Unit) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(texto, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAccion) { Text(accion) }
        }
    }
}

@Composable
private fun Surface24(fondo: Brush, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(24.dp), shadowElevation = 1.dp, color = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.background(fondo).padding(20.dp), content = content)
    }
}
