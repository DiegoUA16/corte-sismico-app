package com.sismografo.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
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
import androidx.compose.ui.geometry.Size
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
import com.sismografo.app.bluetooth.EstadoConexion
import com.sismografo.app.datos.Lectura
import com.sismografo.app.datos.NivelSismo
import com.sismografo.app.datos.f1
import com.sismografo.app.ui.tema.Paleta
import com.sismografo.app.ui.tema.Tabular
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

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
        modifier.clip(CircleShape).background(fondo).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1)
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
fun ChipConexion(estado: EstadoConexion, onClick: () -> Unit) {
    val (texto, color) = when (estado) {
        is EstadoConexion.Conectado -> "${estado.nombre} · Conectado" to Paleta.Exito
        is EstadoConexion.Conectando -> "Conectando…" to Color(0xFFD97706)
        is EstadoConexion.Error -> "Reconectando" to Color(0xFFD97706)
        EstadoConexion.Desconectado -> "Sin conexión" to Paleta.TintaSuave
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
            if (estado is EstadoConexion.Desconectado) Icons.Rounded.BluetoothDisabled else Icons.Rounded.Bluetooth,
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

/** Grupo de opciones tipo "1 min / 10 min / 1 h". */
@Composable
fun Segmentado(opciones: List<String>, seleccion: Int, onSeleccion: (Int) -> Unit) {
    Row(Modifier.clip(CircleShape).background(Paleta.Recesado).padding(3.dp)) {
        opciones.forEachIndexed { i, op ->
            val activo = i == seleccion
            Box(
                Modifier.clip(CircleShape)
                    .background(if (activo) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSeleccion(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    op, style = MaterialTheme.typography.labelMedium,
                    color = if (activo) Color.White else Paleta.TintaSuave,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ magnitud

@Composable
fun InsigniaMagnitud(magnitud: Double, tam: Dp = 56.dp) {
    val nivel = NivelSismo.de(magnitud)
    Column(
        Modifier.size(tam).clip(RoundedCornerShape(16.dp)).background(nivel.color.copy(alpha = 0.13f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            magnitud.f1(),
            style = MaterialTheme.typography.titleLarge.merge(Tabular),
            fontWeight = FontWeight.ExtraBold, color = nivel.color,
        )
        Text("Mw", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = nivel.color.copy(alpha = 0.8f))
    }
}

/**
 * Medidor semicircular 0–9 segmentado por la escala de Richter.
 * El valor actual se marca con un punto sobre el arco (no con una aguja,
 * para no tapar el número), y el umbral con una marca discontinua.
 */
@Composable
fun MedidorMagnitud(magnitud: Double?, umbral: Float, modifier: Modifier = Modifier) {
    val animado by animateFloatAsState((magnitud ?: 0.0).toFloat(), tween(600), label = "mag")
    val medidor = rememberTextMeasurer()
    val nivel = magnitud?.let { NivelSismo.de(it) }
    val estiloEtiqueta = TextStyle(fontSize = 12.sp, color = Paleta.TintaSuave)
    val estiloUmbral = TextStyle(fontSize = 12.sp, color = Paleta.Alerta, fontWeight = FontWeight.Bold)

    Box(modifier.fillMaxWidth().aspectRatio(1.55f)) {
        Canvas(Modifier.fillMaxSize()) {
            val grosor = 18.dp.toPx()
            val arriba = 26.dp.toPx()
            val r = minOf(size.width / 2f - grosor, size.height - arriba - 14.dp.toPx())
            val c = Offset(size.width / 2f, arriba + r)
            val caja = Size(2 * r, 2 * r)
            val esquina = Offset(c.x - r, c.y - r)
            fun angulo(m: Float) = 180f + (m.coerceIn(0f, 9f) / 9f) * 180f
            fun punto(m: Float, radio: Float): Offset {
                val a = Math.toRadians(angulo(m).toDouble())
                return Offset(c.x + radio * cos(a).toFloat(), c.y + radio * sin(a).toFloat())
            }

            // pista de fondo
            drawArc(Paleta.Recesado, 180f, 180f, false, esquina, caja, style = Stroke(grosor, cap = StrokeCap.Round))
            // segmentos por nivel
            val niveles = NivelSismo.entries
            niveles.forEachIndexed { i, n ->
                val desde = n.desde.toFloat()
                val hasta = if (i + 1 < niveles.size) niveles[i + 1].desde.toFloat() else 9f
                val activo = nivel != null && animado >= desde
                drawArc(
                    color = n.color.copy(alpha = if (activo) 1f else 0.28f),
                    startAngle = angulo(desde) + 0.8f,
                    sweepAngle = angulo(hasta) - angulo(desde) - 1.6f,
                    useCenter = false, topLeft = esquina, size = caja,
                    style = Stroke(grosor, cap = StrokeCap.Butt),
                )
            }
            // marca del umbral
            drawLine(
                Paleta.Alerta, punto(umbral, r - grosor), punto(umbral, r + grosor),
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
            )
            val txtU = medidor.measure(umbral.f1(), estiloUmbral)
            val pU = punto(umbral, r + grosor + 10.dp.toPx())
            drawText(txtU, topLeft = Offset(pU.x - txtU.size.width / 2f, pU.y - txtU.size.height / 2f))

            // etiquetas 0 y 9
            val t0 = medidor.measure("0", estiloEtiqueta)
            val t9 = medidor.measure("9", estiloEtiqueta)
            drawText(t0, topLeft = Offset(c.x - r - t0.size.width / 2f, c.y + grosor / 2f + 4.dp.toPx()))
            drawText(t9, topLeft = Offset(c.x + r - t9.size.width / 2f, c.y + grosor / 2f + 4.dp.toPx()))

            // valor actual
            if (nivel != null) {
                val p = punto(animado, r)
                drawCircle(Color.White, grosor * 0.75f, p)
                drawCircle(nivel.color, grosor * 0.75f, p, style = Stroke(4.dp.toPx()))
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                magnitud?.f1() ?: "--",
                style = MaterialTheme.typography.displayLarge.merge(Tabular),
                fontWeight = FontWeight.ExtraBold,
                color = nivel?.color ?: Paleta.TintaSuave,
            )
            Text("Magnitud estimada", style = MaterialTheme.typography.bodyMedium, color = Paleta.TintaSuave)
        }
    }
}

/** Barra con los 7 niveles de Richter y el actual resaltado. */
@Composable
fun BarraRichter(actual: NivelSismo?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NivelSismo.entries.forEach { n ->
            val activo = n == actual
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(if (activo) 12.dp else 8.dp).clip(CircleShape)
                        .background(n.color.copy(alpha = if (activo) 1f else 0.35f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    n.corto, fontSize = 11.sp, maxLines = 1,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                    color = if (activo) n.color else Paleta.TintaSuave,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ gráfica

/**
 * Gráfica de magnitud en el tiempo. Agrupa las lecturas en ~90 columnas
 * (tomando el máximo de cada una) para que la línea se vea suave en cualquier ventana.
 */
@Composable
fun Sismograma(lecturas: List<Lectura>, umbral: Float, ventanaSeg: Int, modifier: Modifier = Modifier) {
    val ahora by rememberAhora()
    val linea = MaterialTheme.colorScheme.primary
    val medidor = rememberTextMeasurer()
    val desde = ahora - ventanaSeg * 1000L
    val visibles = lecturas.filter { it.hora >= desde }

    Box(
        modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)).background(Paleta.Recesado),
    ) {
        if (visibles.size < 2) {
            Text(
                "Esperando lecturas del sismógrafo…",
                Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyMedium,
                color = Paleta.TintaSuave,
            )
        }
        Canvas(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp)) {
            val maxY = max(6f, (visibles.maxOfOrNull { it.magnitud }?.toFloat() ?: 0f) + 1f)
            fun y(m: Float) = size.height * (1f - m / maxY)

            // rejilla
            for (g in 2 until maxY.toInt() step 2) {
                drawLine(Paleta.Borde, Offset(0f, y(g.toFloat())), Offset(size.width, y(g.toFloat())), 1.dp.toPx())
            }
            // umbral
            drawLine(
                Paleta.Alerta, Offset(0f, y(umbral)), Offset(size.width, y(umbral)), 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)),
            )
            val etiqueta = medidor.measure(
                "Umbral ${umbral.f1()}",
                TextStyle(fontSize = 11.sp, color = Paleta.Alerta, fontWeight = FontWeight.SemiBold),
            )
            drawText(etiqueta, topLeft = Offset(size.width - etiqueta.size.width, y(umbral) - etiqueta.size.height - 2.dp.toPx()))

            if (visibles.size < 2) return@Canvas
            val columnas = 90
            val ancho = ventanaSeg * 1000f / columnas
            val puntos = (0 until columnas).mapNotNull { i ->
                val ini = desde + (i * ancho).toLong()
                val fin = ini + ancho.toLong()
                visibles.filter { it.hora in ini until fin }.maxOfOrNull { it.magnitud }?.let { m ->
                    Offset(size.width * (i + 0.5f) / columnas, y(m.toFloat()))
                }
            }
            if (puntos.size < 2) return@Canvas

            val trazo = Path().apply {
                moveTo(puntos.first().x, puntos.first().y)
                for (i in 1 until puntos.size) {
                    val a = puntos[i - 1]
                    val b = puntos[i]
                    val medio = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                    quadraticBezierTo(a.x, a.y, medio.x, medio.y)
                }
                lineTo(puntos.last().x, puntos.last().y)
            }
            val relleno = Path().apply {
                addPath(trazo)
                lineTo(puntos.last().x, size.height)
                lineTo(puntos.first().x, size.height)
                close()
            }
            drawPath(relleno, Brush.verticalGradient(listOf(linea.copy(alpha = 0.22f), Color.Transparent)))
            drawPath(trazo, linea, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(linea, 4.dp.toPx(), puntos.last())
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

// ------------------------------------------------------------------ tiempo

/** Hora actual que se refresca cada segundo (para contadores en vivo). */
@Composable
fun rememberAhora(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1000)
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

/** Tiempo transcurrido corto: "Ahora", "5 min", "2 h", "3 d". */
fun haceCorto(ms: Long, ahora: Long): String {
    val s = (ahora - ms) / 1000
    return when {
        s < 60 -> "Ahora"
        s < 3600 -> "${s / 60} min"
        s < 86_400 -> "${s / 3600} h"
        else -> "${s / 86_400} d"
    }
}

/** "2 h 14 min", "5 min 03 s" */
fun duracionLarga(ms: Long): String {
    val s = ms / 1000
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
