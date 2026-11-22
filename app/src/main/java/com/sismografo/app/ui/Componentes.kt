package com.sismografo.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismografo.app.Muestra
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.Parametros
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

// ------------------------------------------------------------------ contenedores

/** Tarjeta blanca de 24dp con sombra suave, la base de todas las pantallas. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    color: Color = Paleta.Tarjeta,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = color,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun TituloTarjeta(
    titulo: String,
    icono: ImageVector? = null,
    subtitulo: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icono != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
            }
        }
        trailing()
    }
}

@Composable
fun EncabezadoPantalla(
    titulo: String,
    subtitulo: String,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave, maxLines = 1)
        }
        trailing()
    }
}

/** Icono dentro de un cuadrado redondeado de color suave (filas de eventos, pines…). */
@Composable
fun IconoCuadrado(icono: ImageVector, color: Color, tam: Dp = 44.dp) {
    Box(
        Modifier.size(tam).clip(RoundedCornerShape(tam * 0.3f)).background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icono, null, tint = color, modifier = Modifier.size(tam * 0.52f)) }
}

// ------------------------------------------------------------------ chips y píldoras

@Composable
fun Pildora(
    texto: String,
    color: Color,
    fondo: Color = color.copy(alpha = 0.12f),
    punto: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.clip(CircleShape).background(fondo).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

/** Punto que "late" para indicar algo en vivo. */
@Composable
fun PuntoPulsante(color: Color, tam: Dp = 8.dp) {
    val t = rememberInfiniteTransition(label = "pulso")
    val a by t.animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alfa",
    )
    Box(Modifier.size(tam).alpha(a).clip(CircleShape).background(color))
}

@Composable
fun ChipConexion(estado: EstadoConexion, demo: Boolean, onClick: () -> Unit) {
    val (texto, color) = when {
        demo && estado is EstadoConexion.Conectado -> "Demostración" to Color(0xFF7C3AED)
        estado is EstadoConexion.Conectado -> "${estado.nombre} · Conectado" to Paleta.Exito
        estado is EstadoConexion.Conectando -> "Conectando…" to Paleta.Ambar
        estado is EstadoConexion.Error -> "Reconectando" to Paleta.Ambar
        else -> "Sin conexión" to Paleta.TintaSuave
    }
    Row(
        Modifier.clip(CircleShape).background(Paleta.Recesado).clickable(onClick = onClick)
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (estado is EstadoConexion.Conectado) PuntoPulsante(color)
        else Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = Paleta.Tinta, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Icon(
            when {
                demo -> Icons.Rounded.Build
                estado is EstadoConexion.Desconectado -> Icons.Rounded.BluetoothDisabled
                else -> Icons.Rounded.Bluetooth
            },
            null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
        )
    }
}

/** Chip de filtro con estado seleccionado (relleno teal) o no (tonal). */
@Composable
fun ChipFiltro(texto: String, seleccionado: Boolean, puntoColor: Color? = null, onClick: () -> Unit) {
    val fondo = if (seleccionado) MaterialTheme.colorScheme.primary else Paleta.Recesado
    val tinta = if (seleccionado) Color.White else Paleta.Tinta
    Row(
        Modifier.clip(CircleShape).background(fondo).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (puntoColor != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(puntoColor))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = tinta)
    }
}

// ------------------------------------------------------------------ aceleración

/** Color según qué tan cerca está la aceleración del umbral de riesgo. */
fun colorAceleracion(ms2: Double?): Color = when {
    ms2 == null -> Paleta.TintaSuave
    ms2 >= Parametros.UMBRAL_MS2 -> Paleta.Alerta
    ms2 >= Parametros.UMBRAL_MS2 * 0.66 -> Paleta.Ambar
    else -> Paleta.Exito
}

/** Barra horizontal 0–8 m/s² con degradado y marca discontinua en el umbral. */
@Composable
fun BarraAceleracion(ms2: Double?, modifier: Modifier = Modifier) {
    val escala = Parametros.ESCALA_MS2.toFloat()
    val valor by animateFloatAsState(((ms2 ?: 0.0).toFloat() / escala).coerceIn(0f, 1f), tween(180), label = "acc")
    val umbral = (Parametros.UMBRAL_MS2 / Parametros.ESCALA_MS2).toFloat()
    val medidor = rememberTextMeasurer()
    val estilo = TextStyle(fontSize = 11.sp, color = Paleta.Alerta, fontWeight = FontWeight.SemiBold)
    val estiloEje = TextStyle(fontSize = 11.sp, color = Paleta.TintaSuave)

    Canvas(modifier.fillMaxWidth().height(54.dp)) {
        val alto = 12.dp.toPx()
        val y = 20.dp.toPx()
        val r = androidx.compose.ui.geometry.CornerRadius(alto / 2)
        drawRoundRect(Paleta.Recesado, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, alto), r)
        val pinceles = Brush.horizontalGradient(
            0f to Paleta.Exito, umbral * 0.66f to Paleta.Exito, umbral * 0.95f to Paleta.Ambar,
            umbral to Paleta.Alerta, 1f to Paleta.Alerta,
            startX = 0f, endX = size.width,
        )
        if (ms2 != null) {
            drawRoundRect(pinceles, Offset(0f, y), androidx.compose.ui.geometry.Size(max(alto, size.width * valor), alto), r)
            drawCircle(Color.White, alto * 0.95f, Offset(max(alto / 2, size.width * valor - alto / 2), y + alto / 2))
            drawCircle(colorAceleracion(ms2), alto * 0.95f, Offset(max(alto / 2, size.width * valor - alto / 2), y + alto / 2), style = Stroke(3.dp.toPx()))
        }
        val xu = size.width * umbral
        drawLine(
            Paleta.Alerta, Offset(xu, y - 6.dp.toPx()), Offset(xu, y + alto + 6.dp.toPx()), 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
        )
        val t = medidor.measure("Umbral ${Parametros.UMBRAL_MS2.f1()}", estilo)
        drawText(t, topLeft = Offset((xu - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), 0f))
        val t0 = medidor.measure("0", estiloEje)
        val t8 = medidor.measure("${Parametros.ESCALA_MS2.toInt()} m/s²", estiloEje)
        drawText(t0, topLeft = Offset(0f, y + alto + 6.dp.toPx()))
        drawText(t8, topLeft = Offset(size.width - t8.size.width, y + alto + 6.dp.toPx()))
    }
}

/** Gráfica de los últimos 30 s de aceleración con la línea del umbral. */
@Composable
fun Acelerograma(muestras: List<Muestra>, modifier: Modifier = Modifier, ventanaMs: Long = 30_000L) {
    val ahora by rememberAhora(250)
    val linea = MaterialTheme.colorScheme.primary
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 10.sp, color = Paleta.TintaSuave)
    val desde = ahora - ventanaMs
    val visibles = muestras.filter { it.hora >= desde }

    Box(modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado)) {
        Canvas(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 18.dp)) {
            val maxY = max(Parametros.UMBRAL_MS2.toFloat() * 1.6f, (visibles.maxOfOrNull { it.ms2 }?.toFloat() ?: 0f) * 1.1f)
            fun y(v: Float) = size.height * (1f - v / maxY)
            fun x(h: Long) = size.width * ((h - desde).toFloat() / ventanaMs)

            val yu = y(Parametros.UMBRAL_MS2.toFloat())
            drawLine(Paleta.Alerta, Offset(0f, yu), Offset(size.width, yu), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))

            listOf("-30 s" to 0f, "-15 s" to 0.5f, "Ahora" to 1f).forEach { (txt, f) ->
                val m = medidor.measure(txt, estiloEje)
                drawText(m, topLeft = Offset((size.width * f - m.size.width * f), size.height + 3.dp.toPx()))
            }

            if (visibles.size < 2) return@Canvas
            val trazo = Path().apply {
                moveTo(x(visibles.first().hora), y(visibles.first().ms2.toFloat()))
                visibles.drop(1).forEach { lineTo(x(it.hora), y(it.ms2.toFloat())) }
            }
            val relleno = Path().apply {
                addPath(trazo)
                lineTo(x(visibles.last().hora), size.height)
                lineTo(x(visibles.first().hora), size.height)
                close()
            }
            drawPath(relleno, Brush.verticalGradient(listOf(linea.copy(alpha = 0.20f), Color.Transparent)))
            drawPath(trazo, linea, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val ultimo = visibles.last()
            drawCircle(colorAceleracion(ultimo.ms2), 4.dp.toPx(), Offset(x(ultimo.hora), y(ultimo.ms2.toFloat())))
        }
    }
}

/** Cuatro círculos que se llenan con cada pico sostenido. */
@Composable
fun IndicadorPicos(picos: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(Parametros.PICOS_NECESARIOS) { i ->
            val lleno = i < picos
            val color by animateColorAsState(if (lleno) Paleta.Ambar else Color.Transparent, label = "pico")
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color)
                    .border(2.dp, if (lleno) Paleta.Ambar else Paleta.Borde, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${i + 1}", style = MaterialTheme.typography.titleMedium,
                    color = if (lleno) Color.White else Paleta.TintaSuave,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ filas

@Composable
fun FilaInterruptor(
    icono: ImageVector,
    titulo: String,
    subtitulo: String?,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onCambio(!activo) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = Paleta.TintaSuave, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Paleta.TintaSuave)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = activo, onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String, icono: ImageVector? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icono != null) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(valor, style = MaterialTheme.typography.titleSmall, color = Paleta.Tinta)
    }
}

// ------------------------------------------------------------------ tiempo

/** Hora actual que se refresca periódicamente (para contadores en vivo). */
@Composable
fun rememberAhora(cadaMs: Long = 1000): State<Long> = produceState(System.currentTimeMillis(), cadaMs) {
    while (true) {
        value = System.currentTimeMillis()
        delay(cadaMs)
    }
}

private val es = Locale.forLanguageTag("es")

fun hora(ms: Long): String = SimpleDateFormat("HH:mm", es).format(Date(ms))
fun horaSeg(ms: Long): String = SimpleDateFormat("HH:mm:ss", es).format(Date(ms))

/** "Hoy, 20 de noviembre" / "Ayer, 19 de noviembre" / "14 de noviembre" */
fun tituloDia(ms: Long): String {
    val fecha = SimpleDateFormat("d 'de' MMMM", es).format(Date(ms))
    val hoy = Calendar.getInstance()
    val dia = Calendar.getInstance().apply { timeInMillis = ms }
    fun mismo(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val ayer = (hoy.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        mismo(dia, hoy) -> "Hoy, $fecha"
        mismo(dia, ayer) -> "Ayer, $fecha"
        else -> fecha
    }
}

/** Tiempo transcurrido: "hace un momento", "hace 5 min", "hace 2 h", "hace 3 d". */
fun hace(ms: Long, ahora: Long): String {
    val s = (ahora - ms) / 1000
    return when {
        s < 60 -> "hace un momento"
        s < 3600 -> "hace ${s / 60} min"
        s < 86_400 -> "hace ${s / 3600} h"
        else -> "hace ${s / 86_400} d"
    }
}

/** "2 h 14 min", "5 min 03 s" */
fun duracionLarga(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s >= 3600 -> "${s / 3600} h ${(s % 3600) / 60} min"
        s >= 60 -> "${s / 60} min ${"%02d".format(s % 60)} s"
        else -> "$s s"
    }
}

fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}
