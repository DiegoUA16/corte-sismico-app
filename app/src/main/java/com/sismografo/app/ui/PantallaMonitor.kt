package com.sismografo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismografo.app.UiState
import com.sismografo.app.datos.EstadoSistema
import com.sismografo.app.datos.Parametros
import com.sismografo.app.datos.Protocolo
import com.sismografo.app.datos.TipoEvento
import com.sismografo.app.datos.enG
import com.sismografo.app.datos.f1
import com.sismografo.app.datos.f2
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular

@Composable
fun PantallaMonitor(
    ui: UiState,
    contentPadding: PaddingValues,
    onIrASistema: () -> Unit,
    onIrAEventos: () -> Unit,
    onCorteManual: () -> Unit,
    onAbrirRearme: () -> Unit,
    onSimularGolpe: () -> Unit,
    onSimularSismo: () -> Unit,
    onDescartarAviso: () -> Unit,
) {
    var confirmarCorte by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            EncabezadoPantalla("Corte Sísmico", "Protección preventiva") {
                ChipConexion(ui.estado, ui.modoDemo, onIrASistema)
            }
        }

        ui.aviso?.let { aviso ->
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFFFF4E5)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ReportProblem, null, tint = Paleta.Ambar)
                    Spacer(Modifier.width(10.dp))
                    Text(aviso, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7C4A03))
                    Icon(
                        Icons.Rounded.Close, "Cerrar", tint = Color(0xFF7C4A03),
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onDescartarAviso).padding(4.dp),
                    )
                }
            }
        }

        item { TarjetaEstado(ui, onIrASistema) }
        item { Actuadores(ui.sistema, ui.sistemaConfirmado) }

        item {
            Acciones(
                ui = ui,
                onCorte = { confirmarCorte = true },
                onRearme = onAbrirRearme,
            )
        }

        if (ui.modoDemo) item { TarjetaDemo(onSimularGolpe, onSimularSismo) }

        item { TarjetaAceleracion(ui) }
        item { TarjetaFiltro(ui) }
        item { ActividadReciente(ui, onIrAEventos) }
    }

    if (confirmarCorte) {
        AlertDialog(
            onDismissRequest = { confirmarCorte = false },
            containerColor = Color.White,
            icon = { Icon(Icons.Rounded.PowerSettingsNew, null, tint = Paleta.Alerta) },
            title = { Text("¿Ejecutar corte manual?") },
            text = {
                Text(
                    "El equipo cerrará la válvula de gas, cortará la energía y encenderá la sirena. " +
                        "Para restablecer tendrás que hacer el rearme seguro.",
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmarCorte = false; onCorteManual() },
                    colors = ButtonDefaults.buttonColors(containerColor = Paleta.Alerta),
                ) { Text("Cortar ahora") }
            },
            dismissButton = { TextButton(onClick = { confirmarCorte = false }) { Text("Cancelar") } },
        )
    }
}

// ------------------------------------------------------------------ estado principal

@Composable
private fun TarjetaEstado(ui: UiState, onIrASistema: () -> Unit) {
    val ahora by rememberAhora()
    val sistema = ui.sistema
    val color = when (sistema) {
        EstadoSistema.NORMAL -> Paleta.Exito
        EstadoSistema.CORTE -> Paleta.Alerta
        EstadoSistema.DESCONOCIDO -> Paleta.TintaSuave
    }
    val fondoArriba by animateColorAsState(
        when (sistema) {
            EstadoSistema.NORMAL -> Color(0xFFE8F5EC)
            EstadoSistema.CORTE -> Color(0xFFFDE4E4)
            EstadoSistema.DESCONOCIDO -> Paleta.Recesado
        }, tween(500), label = "fondo",
    )

    val (titulo, subtitulo) = when {
        sistema == EstadoSistema.CORTE -> "Corte activo" to "Gas cerrado y energía cortada"
        sistema == EstadoSistema.NORMAL && ui.conectado -> "Sistema vigilando" to "Sin vibraciones peligrosas"
        sistema == EstadoSistema.NORMAL -> "Último estado: normal" to "Conéctate para vigilar en vivo"
        ui.conectado -> "Esperando al equipo" to "Pulsa reset en el Arduino o rearma para recibir el estado"
        else -> "Sin datos del equipo" to "Conecta el HC-06 para empezar"
    }
    val pie = when {
        ui.sistemaDesde <= 0 -> null
        sistema == EstadoSistema.CORTE -> "Desde las ${hora(ui.sistemaDesde)} · ${duracionLarga(ahora - ui.sistemaDesde)}"
        sistema == EstadoSistema.NORMAL -> "Armado ${hace(ui.sistemaDesde, ahora)}"
        else -> null
    }

    Tarjeta(padding = 0.dp) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(fondoArriba, Color.White)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EscudoAnimado(sistema, color, vivo = ui.conectado)
            Spacer(Modifier.height(16.dp))
            Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Paleta.Tinta)
            Text(subtitulo, style = MaterialTheme.typography.bodyLarge, color = Paleta.TintaSuave, textAlign = TextAlign.Center)
            if (pie != null) {
                Spacer(Modifier.height(10.dp))
                Pildora(pie, color, punto = sistema == EstadoSistema.CORTE)
            }
            if (!ui.conectado) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onIrASistema, modifier = Modifier.height(48.dp)) {
                    Icon(Icons.Rounded.Bluetooth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Conectar equipo")
                }
            } else if (!ui.sistemaConfirmado && sistema != EstadoSistema.DESCONOCIDO) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Estado guardado: el equipo lo confirma con su próximo mensaje.",
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Escudo con halo; en vigilancia emite ondas suaves, en corte late en rojo. */
@Composable
private fun EscudoAnimado(sistema: EstadoSistema, color: Color, vivo: Boolean) {
    val t = rememberInfiniteTransition(label = "escudo")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (sistema == EstadoSistema.CORTE) 1100 else 2600, easing = LinearEasing), RepeatMode.Restart),
        label = "onda",
    )
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        if (vivo && sistema != EstadoSistema.DESCONOCIDO) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(2) { i ->
                    val f = (p + i * 0.5f) % 1f
                    drawCircle(color.copy(alpha = 0.28f * (1f - f)), radius = size.minDimension / 2 * (0.55f + 0.45f * f), style = Stroke(2.dp.toPx()))
                }
            }
        }
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when (sistema) {
                        EstadoSistema.NORMAL -> Icons.Rounded.VerifiedUser
                        EstadoSistema.CORTE -> Icons.Rounded.PowerSettingsNew
                        EstadoSistema.DESCONOCIDO -> Icons.Rounded.HelpOutline
                    },
                    null, tint = Color.White, modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ actuadores

@Composable
private fun Actuadores(sistema: EstadoSistema, confirmado: Boolean) {
    val corte = sistema == EstadoSistema.CORTE
    val desconocido = sistema == EstadoSistema.DESCONOCIDO
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Actuador(
            Icons.Rounded.Whatshot, "Gas",
            when { desconocido -> "—"; corte -> "Cerrada"; else -> "Abierta" },
            when { desconocido -> Paleta.TintaSuave; corte -> Paleta.Alerta; else -> Paleta.Exito },
            "Servo · pin 6", Modifier.weight(1f),
        )
        Actuador(
            if (corte) Icons.Rounded.FlashOff else Icons.Rounded.FlashOn, "Energía",
            when { desconocido -> "—"; corte -> "Cortada"; else -> "Activa" },
            when { desconocido -> Paleta.TintaSuave; corte -> Paleta.Alerta; else -> Paleta.Exito },
            "Relé · pin 7", Modifier.weight(1f),
        )
        Actuador(
            if (corte) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, "Sirena",
            when { desconocido -> "—"; corte -> "Sonando"; else -> "Apagada" },
            if (corte) Paleta.Alerta else Paleta.TintaSuave,
            "Buzzer · pin 8", Modifier.weight(1f),
        )
    }
}

@Composable
private fun Actuador(icono: ImageVector, nombre: String, estado: String, color: Color, pin: String, modifier: Modifier) {
    Tarjeta(modifier, padding = 14.dp) {
        IconoCuadrado(icono, color, 40.dp)
        Spacer(Modifier.height(10.dp))
        Text(nombre, style = MaterialTheme.typography.labelLarge, color = Paleta.Tinta, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Pildora(estado, color, punto = estado != "—")
        Spacer(Modifier.height(6.dp))
        Text(pin, style = MaterialTheme.typography.labelSmall, color = Paleta.TintaSuave, maxLines = 1)
    }
}

// ------------------------------------------------------------------ acciones

@Composable
private fun Acciones(ui: UiState, onCorte: () -> Unit, onRearme: () -> Unit) {
    val corte = ui.sistema == EstadoSistema.CORTE
    val pendiente = ui.comandoPendiente
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onCorte,
            enabled = ui.conectado && !corte && pendiente == null,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Paleta.Alerta),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp, if (ui.conectado && !corte) Paleta.Alerta else Paleta.Borde,
            ),
        ) {
            if (pendiente == Protocolo.CMD_CORTE) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Paleta.Alerta, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.PowerSettingsNew, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Corte manual")
        }
        Button(
            onClick = onRearme,
            // DESCONOCIDO: el Arduino ya estaba encendido al conectar; rearmar le hace responder N!.
            enabled = ui.conectado && (corte || ui.sistema == EstadoSistema.DESCONOCIDO) && pendiente == null,
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            if (pendiente == Protocolo.CMD_REARME) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Rearmar")
        }
    }
}

@Composable
private fun TarjetaDemo(onGolpe: () -> Unit, onSismo: () -> Unit) {
    Tarjeta(color = Color(0xFFF3EEFF)) {
        TituloTarjeta("Modo demostración", Icons.Rounded.Build, "Simula el ADXL345 con la misma lógica del sketch")
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onGolpe, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Vibration, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Golpe")
            }
            Button(
                onClick = onSismo, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            ) {
                Icon(Icons.Rounded.ShowChart, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sismo")
            }
        }
    }
}

// ------------------------------------------------------------------ aceleración

@Composable
private fun TarjetaAceleracion(ui: UiState) {
    Tarjeta {
        TituloTarjeta("Aceleración en vivo", Icons.Rounded.ShowChart, "ADXL345 · magnitud del vector respecto al reposo") {
            if (ui.telemetria && ui.conectado) {
                PuntoPulsante(Paleta.Exito)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (ui.telemetria && ui.conectado) {
            val a = ui.aceleracion
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    a?.f2() ?: "--",
                    style = MaterialTheme.typography.displaySmall.merge(Tabular),
                    fontWeight = FontWeight.ExtraBold, color = colorAceleracion(a),
                )
                Text(" m/s²", style = MaterialTheme.typography.titleMedium, color = Paleta.TintaSuave, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                if (a != null) Pildora(a.enG(), Paleta.TintaSuave, punto = false)
            }
            Spacer(Modifier.height(8.dp))
            BarraAceleracion(a)
            Spacer(Modifier.height(8.dp))
            Acelerograma(ui.muestras)
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado).padding(16.dp),
            ) {
                Text(
                    "Sin telemetría", style = MaterialTheme.typography.titleSmall, color = Paleta.Tinta,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "El sketch base solo avisa del corte y del rearme. Para ver la aceleración en vivo, " +
                        "carga la variante corte_preventivo_telemetria o activa el modo demostración en Ajustes.",
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave,
                )
            }
        }
    }
}

@Composable
private fun TarjetaFiltro(ui: UiState) {
    val picos = ui.secuencia?.picos ?: 0
    Tarjeta {
        TituloTarjeta("Filtro anti falsas alarmas", Icons.Rounded.FilterList) {
            Pildora("$picos/${Parametros.PICOS_NECESARIOS}", if (picos > 0) Paleta.Ambar else Paleta.TintaSuave, punto = picos > 0)
        }
        Spacer(Modifier.height(16.dp))
        IndicadorPicos(picos)
        Spacer(Modifier.height(14.dp))
        Text(
            "Se necesitan ${Parametros.PICOS_NECESARIOS} picos sobre ${Parametros.UMBRAL_MS2.f1()} m/s² con menos de " +
                "${Parametros.VENTANA_MS / 1000.0} s entre ellos. Un golpe aislado se ignora.",
            style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave,
        )
        AnimatedVisibility(ui.secuencia != null) {
            Text(
                "Pico máximo de esta secuencia: ${ui.secuencia?.maximo?.f1() ?: "--"} m/s²",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge, color = Paleta.Ambar,
            )
        }
    }
}

// ------------------------------------------------------------------ actividad

@Composable
private fun ActividadReciente(ui: UiState, onVerTodo: () -> Unit) {
    val ahora by rememberAhora()
    val recientes = ui.eventos.filter { it.tipo != TipoEvento.CONEXION }.take(3)
    Tarjeta {
        TituloTarjeta("Actividad reciente") {
            Row(
                Modifier.clip(CircleShape).clickable(onClick = onVerTodo).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ver registro", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (recientes.isEmpty()) {
            Text(
                "Todavía no hay cortes, rearmes ni golpes registrados.",
                Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave,
            )
        }
        recientes.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconoCuadrado(iconoEvento(e.tipo), e.tipo.color, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.tipo.titulo, style = MaterialTheme.typography.titleSmall)
                    Text(e.detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(hora(e.hora), style = MaterialTheme.typography.labelLarge.merge(Tabular))
                    Text(hace(e.hora, ahora).removePrefix("hace "), fontSize = 11.sp, color = Paleta.TintaSuave)
                }
            }
        }
    }
}

fun iconoEvento(tipo: TipoEvento): ImageVector = when (tipo) {
    TipoEvento.CORTE_AUTOMATICO -> Icons.Rounded.FlashOff
    TipoEvento.CORTE_MANUAL -> Icons.Rounded.PowerSettingsNew
    TipoEvento.REARME -> Icons.Rounded.Shield
    TipoEvento.GOLPE_IGNORADO -> Icons.Rounded.Vibration
    TipoEvento.CONEXION -> Icons.Rounded.Bluetooth
}
