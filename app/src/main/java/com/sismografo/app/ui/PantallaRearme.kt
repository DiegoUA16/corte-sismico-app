package com.sismografo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sismografo.app.UiState
import com.sismografo.app.datos.EstadoSistema
import com.sismografo.app.datos.Protocolo
import com.sismografo.app.ui.tema.Paleta
import kotlinx.coroutines.launch

private data class Verificacion(val icono: ImageVector, val titulo: String, val detalle: String)

private val verificaciones = listOf(
    Verificacion(Icons.Rounded.Vibration, "El movimiento sísmico terminó", "No se sienten réplicas ni vibraciones"),
    Verificacion(Icons.Rounded.Whatshot, "No hay olor a gas", "Revisa cocina, calentador y medidor"),
    Verificacion(Icons.Rounded.FlashOff, "No hay cables dañados, chispas ni humo", "Inspecciona tomas y tablero principal"),
    Verificacion(Icons.Rounded.People, "Todas las personas están a salvo", "Nadie está atrapado ni herido"),
)

/** Rearme seguro: lista de verificación + botón de mantener presionado que manda 'R'. */
@Composable
fun PantallaRearme(ui: UiState, onCerrar: () -> Unit, onRearmar: () -> Unit) {
    BackHandler(onBack = onCerrar)
    val marcadas = remember { mutableStateListOf(false, false, false, false) }
    val listo = marcadas.all { it }
    val enviando = ui.comandoPendiente == Protocolo.CMD_REARME
    val ahora by rememberAhora()

    // Si el equipo confirma (N!) se cierra solo.
    LaunchedEffect(ui.sistema) { if (ui.sistema == EstadoSistema.NORMAL) onCerrar() }

    Box(Modifier.fillMaxSize().background(Paleta.Fondo)) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCerrar, modifier = Modifier.clip(CircleShape).background(Paleta.Recesado)) {
                    Icon(Icons.Rounded.Close, "Cerrar")
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rearme seguro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Restablece gas y energía", style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave)
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Paleta.AlertaSuave)
                    .border(1.dp, Paleta.Alerta.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoCuadrado(Icons.Rounded.PowerSettingsNew, Paleta.Alerta, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (ui.sistemaDesde > 0) "Corte activo desde las ${hora(ui.sistemaDesde)}" else "Corte activo",
                        style = MaterialTheme.typography.titleSmall, color = Color(0xFF7F1D1D),
                    )
                    Text(
                        "Gas cerrado · Energía cortada" + if (ui.sistemaDesde > 0) " · ${duracionLarga(ahora - ui.sistemaDesde)}" else "",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF991B1B),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Antes de rearmar, confirma:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pildora("${marcadas.count { it }} de 4", if (listo) Paleta.Exito else Paleta.TintaSuave, punto = listo)
                }
                Spacer(Modifier.height(12.dp))
                verificaciones.forEachIndexed { i, v ->
                    FilaVerificacion(v, marcadas[i]) { marcadas[i] = !marcadas[i] }
                    if (i < verificaciones.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
            ) {
                Icon(Icons.Rounded.Memory, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Qué hace el equipo", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "La app envía «R». El Arduino desactiva el relé, regresa el servo a 0° (válvula abierta), " +
                            "apaga la sirena y recalibra el ADXL345 en reposo. No muevas el sensor durante la calibración.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            BotonMantener(
                habilitado = listo && ui.conectado && !enviando,
                enviando = enviando,
                texto = when {
                    !ui.conectado -> "Conéctate al equipo para rearmar"
                    enviando -> "Esperando confirmación del equipo…"
                    listo -> "Mantén presionado para rearmar"
                    else -> "Completa las 4 verificaciones"
                },
                onCompletado = onRearmar,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, null, tint = Paleta.TintaSuave, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "También puedes usar el botón físico de rearme (pin 2)",
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilaVerificacion(v: Verificacion, marcada: Boolean, onClick: () -> Unit) {
    val fondo by animateColorAsState(if (marcada) Color(0xFFE6F2F4) else Paleta.Recesado, label = "fondo")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fondo).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(v.icono, null, tint = if (marcada) MaterialTheme.colorScheme.primary else Paleta.TintaSuave)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(v.titulo, style = MaterialTheme.typography.titleSmall)
            Text(v.detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(if (marcada) MaterialTheme.colorScheme.primary else Color.White)
                .border(2.dp, if (marcada) MaterialTheme.colorScheme.primary else Paleta.Borde, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (marcada) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Botón píldora que hay que mantener presionado 1.5 s; la barra se llena mientras tanto. */
@Composable
private fun BotonMantener(habilitado: Boolean, enviando: Boolean, texto: String, onCompletado: () -> Unit) {
    val progreso = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    val primario = MaterialTheme.colorScheme.primary
    val base = if (habilitado || enviando) primario else Paleta.Borde

    Box(
        Modifier.fillMaxWidth().height(64.dp).clip(CircleShape)
            .background(if (habilitado || enviando) primario.copy(alpha = 0.18f) else Paleta.Recesado)
            .pointerInput(habilitado) {
                if (!habilitado) return@pointerInput
                detectTapGestures(onPress = {
                    val anim = scope.launch {
                        progreso.animateTo(1f, tween(1500, easing = LinearEasing))
                        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompletado()
                    }
                    tryAwaitRelease()
                    if (progreso.value < 1f) {
                        anim.cancel()
                        scope.launch { progreso.animateTo(0f, tween(250)) }
                    } else {
                        scope.launch { progreso.snapTo(0f) }
                    }
                })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(if (enviando) 1f else progreso.value).background(base))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val tinta = when {
                enviando || progreso.value > 0.5f -> Color.White
                habilitado -> primario
                else -> Paleta.TintaSuave
            }
            when {
                enviando -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                habilitado -> Icon(Icons.Rounded.Refresh, null, tint = tinta)
                else -> Icon(Icons.Rounded.Lock, null, tint = tinta)
            }
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.titleSmall, color = tinta)
        }
    }
}
