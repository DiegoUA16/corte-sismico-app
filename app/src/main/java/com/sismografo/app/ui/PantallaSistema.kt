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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Whatshot
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
import com.sismografo.app.datos.EstadoSistema
import com.sismografo.app.datos.Parametros
import com.sismografo.app.datos.enG
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular

@Composable
fun PantallaSistema(
    ui: UiState,
    contentPadding: PaddingValues,
    permisoBt: Boolean,
    btEncendido: Boolean,
    emparejados: List<Emparejado>,
    onPedirPermiso: () -> Unit,
    onEncenderBt: () -> Unit,
    onConectar: (BluetoothDevice) -> Unit,
    onDesconectar: () -> Unit,
    onIniciarDemo: () -> Unit,
    onRefrescar: () -> Unit,
    onAbrirAjustesBt: () -> Unit,
) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            EncabezadoPantalla("Sistema", "Equipo y conexión") {
                IconButton(onClick = onRefrescar) { Icon(Icons.Rounded.Refresh, "Actualizar lista") }
            }
        }

        item { TarjetaConexion(ui, onDesconectar) }

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
                    val esActual = !ui.modoDemo && ui.dispositivo?.direccion == e.direccion && ui.estado !is EstadoConexion.Desconectado
                    FilaEmparejado(
                        e = e,
                        estadoActual = if (esActual) ui.estado else null,
                        onConectar = { onConectar(e.device) },
                    )
                }
            }
        }

        if (!ui.modoDemo) {
            item {
                Tarjeta(color = Color(0xFFF3EEFF), padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCuadrado(Icons.Rounded.Build, Color(0xFF7C3AED), 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("¿Sin el equipo a mano?", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "El modo demostración simula el Arduino con la misma lógica de detección.",
                                style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onIniciarDemo,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        ) { Text("Probar") }
                    }
                }
            }
        }

        item { ComoFunciona() }
        item { ParametrosFiltro() }
        item { HardwarePines() }
        if (ui.conectado) item { Diagnostico(ui) }

        item {
            Tarjeta(color = MaterialTheme.colorScheme.primaryContainer) {
                Row {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "¿No aparece tu HC-06?", style = MaterialTheme.typography.titleSmall,
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
private fun TarjetaConexion(ui: UiState, onDesconectar: () -> Unit) {
    val estado = ui.estado
    val ahora by rememberAhora()
    val conectado = estado is EstadoConexion.Conectado

    val fondo = Brush.linearGradient(listOf(Color(0xFFE9F6F1), Color.White, Color(0xFFE6F2F6)))
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(24.dp), shadowElevation = 1.dp, color = Color.White, modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.background(fondo).padding(20.dp)) {
            Ilustracion(conectado, ui.modoDemo)
            Spacer(Modifier.height(20.dp))

            val titulo = when (estado) {
                is EstadoConexion.Conectado -> if (ui.modoDemo) "Modo demostración" else "Conectado a ${estado.nombre}"
                is EstadoConexion.Conectando -> "Conectando con ${estado.nombre}…"
                is EstadoConexion.Error -> "Conexión perdida"
                EstadoConexion.Desconectado -> "Sin conexión"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                when (estado) {
                    is EstadoConexion.Conectado -> Pildora(if (ui.modoDemo) "Simulado" else "En línea", if (ui.modoDemo) Color(0xFF7C3AED) else Paleta.Exito)
                    is EstadoConexion.Conectando -> Pildora("Enlazando", Paleta.Ambar)
                    is EstadoConexion.Error -> Pildora("Reintentando", Paleta.Ambar)
                    EstadoConexion.Desconectado -> {}
                }
            }
            Text(
                when {
                    estado is EstadoConexion.Error -> estado.mensaje
                    ui.dispositivo != null && estado !is EstadoConexion.Desconectado ->
                        if (ui.modoDemo) ui.dispositivo.direccion else "${ui.dispositivo.direccion} · Bluetooth clásico (SPP) · 9600"
                    else -> "Elige tu HC-06 en la lista de abajo para empezar."
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = if (ui.modoDemo) FontFamily.Default else FontFamily.Monospace),
                color = Paleta.TintaSuave,
            )

            if (conectado) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniDato(Icons.Rounded.Timer, "Tiempo", ui.conectadoDesde?.let { duracionLarga(ahora - it) } ?: "--", Modifier.weight(1f))
                    MiniDato(
                        Icons.Rounded.VerifiedUser,
                        "Estado",
                        when (ui.sistema) {
                            EstadoSistema.NORMAL -> "Normal"
                            EstadoSistema.CORTE -> "Corte activo"
                            EstadoSistema.DESCONOCIDO -> "Sin confirmar"
                        },
                        Modifier.weight(1f),
                    )
                }
            }
            if (estado !is EstadoConexion.Desconectado) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onDesconectar, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFBE3DD), contentColor = Color(0xFFB3261E)),
                ) {
                    Icon(Icons.Rounded.LinkOff, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.modoDemo) "Salir de la demostración" else if (conectado) "Desconectar" else "Cancelar")
                }
            }
        }
    }
}

/** Teléfono ↔ Arduino unidos por una línea punteada animada cuando hay enlace. */
@Composable
private fun Ilustracion(conectado: Boolean, demo: Boolean) {
    val t = rememberInfiniteTransition(label = "flujo")
    val fase by t.animateFloat(0f, 40f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "fase")
    val color = if (conectado) MaterialTheme.colorScheme.primary else Paleta.Borde

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.7f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Nodo(Icons.Rounded.PhoneAndroid, "Este teléfono", Paleta.Recesado, Paleta.Tinta)
        Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(4.dp)) {
                drawLine(
                    color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 16f), if (conectado) -fase else 0f),
                )
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(
                    when { demo -> Icons.Rounded.Build; conectado -> Icons.Rounded.Bluetooth; else -> Icons.Rounded.BluetoothDisabled },
                    null, tint = color, modifier = Modifier.size(20.dp),
                )
            }
        }
        Nodo(Icons.Rounded.Memory, if (demo) "Simulador" else "Arduino", MaterialTheme.colorScheme.primary, Color.White)
    }
}

@Composable
private fun Nodo(icono: ImageVector, etiqueta: String, fondo: Color, tinta: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(fondo), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = tinta, modifier = Modifier.size(30.dp))
        }
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

// ------------------------------------------------------------------ información técnica

@Composable
private fun ComoFunciona() {
    Tarjeta {
        TituloTarjeta("Cómo funciona", Icons.Rounded.Lightbulb)
        Spacer(Modifier.height(14.dp))
        val pasos = listOf(
            Triple(Icons.Rounded.ShowChart, "Mide", "El ADXL345 lee la aceleración en X, Y y Z unas 20 veces por segundo y le resta la posición de reposo."),
            Triple(Icons.Rounded.FilterList, "Filtra", "Solo cuenta picos sobre ${Parametros.UMBRAL_MS2.f1()} m/s². Si pasan más de ${Parametros.VENTANA_MS / 1000.0} s entre picos, vuelve a empezar."),
            Triple(Icons.Rounded.FlashOff, "Corta", "Con ${Parametros.PICOS_NECESARIOS} picos seguidos: servo cierra el gas, relé corta la energía, suena la sirena."),
            Triple(Icons.Rounded.Bluetooth, "Avisa", "Envía «S!» por el HC-06 y la app lanza la alerta. El rearme manual envía «N!»."),
        )
        pasos.forEachIndexed { i, (icono, titulo, texto) ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconoCuadrado(icono, MaterialTheme.colorScheme.primary, 40.dp)
                    if (i < pasos.lastIndex) {
                        Icon(Icons.Rounded.ArrowDownward, null, tint = Paleta.Borde, modifier = Modifier.padding(vertical = 2.dp).size(16.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(top = 2.dp)) {
                    Text("${i + 1}. $titulo", style = MaterialTheme.typography.titleSmall)
                    Text(texto, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
                }
            }
        }
    }
}

@Composable
private fun ParametrosFiltro() {
    Tarjeta {
        TituloTarjeta("Parámetros del filtro", Icons.Rounded.Tune, "Definidos en el sketch del Arduino")
        Spacer(Modifier.height(10.dp))
        FilaDato("Umbral de riesgo", "${Parametros.UMBRAL_MS2.f1()} m/s² (${Parametros.UMBRAL_MS2.enG()})")
        FilaDato("Picos necesarios", "${Parametros.PICOS_NECESARIOS}")
        FilaDato("Tiempo máximo entre picos", "${Parametros.VENTANA_MS / 1000.0} s")
        FilaDato("Lectura del sensor", "cada ~50 ms")
        FilaDato("Rango del ADXL345", "±${Parametros.RANGO_G} g")
    }
}

@Composable
private fun HardwarePines() {
    Tarjeta {
        TituloTarjeta("Hardware y pines", Icons.Rounded.Memory)
        Spacer(Modifier.height(10.dp))
        FilaDato("Acelerómetro ADXL345", "I²C 0x53 · A4/A5", Icons.Rounded.ShowChart)
        FilaDato("Bluetooth HC-06", "RX 10 · TX 11", Icons.Rounded.Bluetooth)
        FilaDato("Servo válvula de gas", "Pin 6", Icons.Rounded.Whatshot)
        FilaDato("Relé de energía", "Pin 7", Icons.Rounded.FlashOn)
        FilaDato("Buzzer (sirena)", "Pin 8", Icons.Rounded.NotificationsActive)
        FilaDato("LED verde / rojo", "Pin 9 / 12", Icons.Rounded.Lightbulb)
        FilaDato("Botón de rearme", "Pin 2", Icons.Rounded.TouchApp)
    }
}

@Composable
private fun Diagnostico(ui: UiState) {
    val ahora by rememberAhora()
    Tarjeta {
        TituloTarjeta("Diagnóstico del enlace", Icons.Rounded.Code)
        Spacer(Modifier.height(10.dp))
        FilaDato("Líneas recibidas", "%,d".format(ui.lineasRecibidas).replace(',', ' '))
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Último mensaje", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (ui.ultimaLinea != null) {
                    Text(
                        hace(ui.ultimaLineaHora, ahora),
                        style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Paleta.TintaSuave,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                ui.ultimaLinea ?: "Todavía no llega ningún mensaje",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FilaEmparejado(e: Emparejado, estadoActual: EstadoConexion?, onConectar: () -> Unit) {
    val compatible = !e.esAudio
    val conectado = estadoActual is EstadoConexion.Conectado
    val enProceso = estadoActual != null && !conectado
    Tarjeta(padding = 14.dp, modifier = Modifier.alpha(if (compatible) 1f else 0.55f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (conectado) MaterialTheme.colorScheme.primaryContainer else Paleta.Recesado),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (e.esAudio) Icons.Rounded.Headset else Icons.Rounded.Bluetooth, null,
                    tint = when {
                        conectado -> MaterialTheme.colorScheme.primary
                        enProceso -> Paleta.Ambar
                        else -> Paleta.TintaSuave
                    },
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(e.direccion, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = Paleta.TintaSuave)
                when {
                    conectado -> Text("Conectado actualmente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    enProceso -> Text(
                        if (estadoActual is EstadoConexion.Conectando) "Conectando…" else "Reintentando…",
                        style = MaterialTheme.typography.labelMedium, color = Paleta.Ambar,
                    )
                    !compatible -> Text("Es un dispositivo de audio", style = MaterialTheme.typography.labelMedium, color = Paleta.Alerta)
                }
            }
            when {
                conectado -> Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, "Conectado", tint = Color.White) }
                enProceso -> Pildora(
                    if (estadoActual is EstadoConexion.Conectando) "Enlazando" else "Reintentando",
                    Paleta.Ambar, punto = false,
                )
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
