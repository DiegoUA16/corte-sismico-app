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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Whatshot
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismografo.app.datos.Parametros
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular

/** Vista de emergencia a pantalla completa cuando el equipo avisa `S!` (corte ejecutado). */
@Composable
fun PantallaAlerta(
    hora: Long,
    manual: Boolean,
    esPrueba: Boolean,
    alarmaSonando: Boolean,
    onEstoyASalvo: () -> Unit,
    onSilenciar: () -> Unit,
) {
    BackHandler(onBack = onEstoyASalvo)
    val context = LocalContext.current
    val ahora by rememberAhora()

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B), Color(0xFF7F1D1D)))),
    ) {
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
                    when {
                        esPrueba -> "PRUEBA DE ALERTA"
                        manual -> "CORTE MANUAL · EJECUTADO"
                        else -> "ALERTA SÍSMICA · CORTE EJECUTADO"
                    },
                    color = Color.White, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${horaSeg(hora)}  ·  hace ${mmss(ahora - hora)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge.merge(Tabular), letterSpacing = 1.sp,
            )

            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color(0xFFDC2626), modifier = Modifier.size(42.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (manual) "Corte manual ejecutado" else "Corte preventivo ejecutado",
                color = Color.White, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (manual) "Se ordenó el corte desde la app"
                else "Se detectaron oscilaciones sostenidas sobre el umbral de riesgo",
                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(20.dp)) {
                Text("Estado de la instalación", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                FilaInstalacion(Icons.Rounded.Whatshot, "Válvula de gas", "Cerrada")
                FilaInstalacion(Icons.Rounded.FlashOff, "Energía eléctrica", "Cortada")
                FilaInstalacion(Icons.Rounded.NotificationsActive, "Sirena del equipo", "Activa")
            }

            if (!manual) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalHospital, null, tint = Color(0xFFDC2626))
                        Spacer(Modifier.width(10.dp))
                        Text("Qué hacer ahora", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Paso(Icons.Rounded.Accessibility, "1. Agáchate", "Al piso", Modifier.weight(1f))
                        Paso(Icons.Rounded.Shield, "2. Cúbrete", "Bajo mesa firme", Modifier.weight(1f))
                        Paso(Icons.Rounded.PanTool, "3. Sujétate", "Hasta que pare", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Cuando pase el movimiento:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Consejo(Icons.Rounded.SmokeFree, "No enciendas fuego, velas ni interruptores")
                    Consejo(Icons.Rounded.Whatshot, "Revisa si hay olor a gas antes de rearmar")
                    Consejo(Icons.Rounded.DirectionsRun, "Evacúa con calma si ves daños en la estructura")
                }
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
                    disabledContainerColor = Color.White.copy(alpha = 0.10f), disabledContentColor = Color.White.copy(alpha = 0.6f),
                )
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:123"))) },
                    modifier = Modifier.weight(1f).height(52.dp), colors = translucido,
                ) {
                    Icon(Icons.Rounded.Call, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Emergencias 123", maxLines = 1)
                }
                Button(
                    onClick = onSilenciar, enabled = alarmaSonando,
                    modifier = Modifier.weight(1f).height(52.dp), colors = translucido,
                ) {
                    Icon(Icons.Rounded.VolumeOff, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (alarmaSonando) "Silenciar" else "Silenciada", maxLines = 1)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "La app silencia solo el teléfono. La sirena del equipo se apaga al rearmar, " +
                    "desde Monitor o con el botón físico, cuando todo sea seguro.",
                color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            )
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
        val centro = center.copy(y = size.height * 0.2f)
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
private fun FilaInstalacion(icono: ImageVector, nombre: String, estado: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconoCuadrado(icono, Paleta.Alerta, 40.dp)
        Spacer(Modifier.width(12.dp))
        Text(nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Pildora(estado, Paleta.Alerta)
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
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Consejo(icono: ImageVector, texto: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = Paleta.TintaSuave, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = Paleta.Tinta)
    }
}
