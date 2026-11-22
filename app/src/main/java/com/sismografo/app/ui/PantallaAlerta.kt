package com.sismografo.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismografo.app.datos.Evento
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Tabular

/** Vista de emergencia a pantalla completa mientras hay una alerta activa. */
@Composable
fun PantallaAlerta(
    evento: Evento,
    esPrueba: Boolean,
    nombreSensor: String?,
    alarmaSonando: Boolean,
    onEstoyASalvo: () -> Unit,
    onSilenciar: () -> Unit,
) {
    BackHandler(onBack = onEstoyASalvo)
    val context = LocalContext.current
    val ahora by rememberAhora()
    val nivel = NivelSismo.de(evento.magnitud)
    val arriba = if (nivel.ordinal <= NivelSismo.LIGERO.ordinal) Color(0xFFEA7A0C) else Color(0xFFEA580C)
    val abajo = lerp(nivel.color, Color(0xFF7F1D1D), 0.35f)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(arriba, abajo)))) {
        Ondas(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PuntoPulsante(Color.White, 10.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (esPrueba) "PRUEBA DE ALERTA" else "ALERTA SÍSMICA · EN CURSO",
                    color = Color.White, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "HORA DEL EVENTO  ${horaSeg(evento.hora)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge.merge(Tabular), letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    evento.magnitud.f1(), color = Color.White, fontSize = 112.sp, lineHeight = 112.sp,
                    fontWeight = FontWeight.ExtraBold, style = Tabular,
                )
                Text(
                    "Mw", color = Color.White.copy(alpha = 0.85f), fontSize = 32.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 18.dp, start = 6.dp),
                )
            }
            Text("Magnitud estimada", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Warning, null, tint = nivel.color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${nivel.nombre} · ${nivel.descripcion}", color = nivel.color,
                    style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatoTranslucido(Icons.Rounded.Bluetooth, "SENSOR", nombreSensor ?: "HC-06", Modifier.weight(1f))
                DatoTranslucido(Icons.Rounded.Timer, "DURACIÓN", mmss(ahora - evento.hora), Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalHospital, null, tint = Color(0xFFDC2626))
                    Spacer(Modifier.width(10.dp))
                    Text("¿Qué hacer ahora?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Paso(Icons.Rounded.Accessibility, "1. Agáchate", "Al piso", Modifier.weight(1f))
                    Paso(Icons.Rounded.Shield, "2. Cúbrete", "Bajo mesa firme", Modifier.weight(1f))
                    Paso(Icons.Rounded.PanTool, "3. Sujétate", "Hasta que pare", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Aléjate de ventanas, estanterías y objetos que puedan caer. No uses ascensores.",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF7E6)).padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7C4A03),
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onEstoyASalvo,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFDC2626)),
            ) {
                Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.width(10.dp))
                Text("Estoy a salvo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val translucido = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White,
                )
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:123"))) },
                    modifier = Modifier.weight(1f).height(52.dp), colors = translucido,
                ) {
                    Icon(Icons.Rounded.Call, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Emergencias 123")
                }
                Button(
                    onClick = onSilenciar, enabled = alarmaSonando,
                    modifier = Modifier.weight(1f).height(52.dp), colors = translucido,
                ) {
                    Icon(Icons.Rounded.VolumeOff, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (alarmaSonando) "Silenciar" else "Silenciada")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Anillos concéntricos que se expanden, como ondas sísmicas. */
@Composable
private fun Ondas(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "ondas")
    val p by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "p",
    )
    Canvas(modifier) {
        val centro = center.copy(y = size.height * 0.24f)
        val maximo = size.maxDimension * 0.75f
        repeat(4) { i ->
            val f = (p + i / 4f) % 1f
            drawCircle(
                Color.White.copy(alpha = 0.16f * (1f - f)),
                radius = maximo * f, center = centro, style = Stroke(2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun DatoTranslucido(icono: ImageVector, etiqueta: String, valor: String, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.16f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(etiqueta, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            Text(valor, color = Color.White, style = MaterialTheme.typography.titleMedium.merge(Tabular), maxLines = 1)
        }
    }
}

@Composable
private fun Paso(icono: ImageVector, titulo: String, detalle: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFF4F3F1)).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFDE2E2)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, null, tint = Color(0xFFDC2626)) }
        Spacer(Modifier.height(8.dp))
        Text(titulo, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF526066), textAlign = TextAlign.Center)
    }
}
