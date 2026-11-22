package com.sismografo.app.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.sismografo.app.R

// Paleta del sistema de diseño "Sismógrafo" generado en Google Stitch.
object Paleta {
    val Primario = Color(0xFF0E5A6D)
    val PrimarioOscuro = Color(0xFF004151)
    val PrimarioSuave = Color(0xFFDCEFF4)
    val Fondo = Color(0xFFFAF9F6)
    val Tarjeta = Color(0xFFFFFFFF)
    val Recesado = Color(0xFFF1F0EC)
    val Tinta = Color(0xFF1A2124)
    val TintaSuave = Color(0xFF526066)
    val Borde = Color(0xFFE3E2DE)
    val Exito = Color(0xFF2E7D32)
    val Alerta = Color(0xFFDC2626)
    val AlertaSuave = Color(0xFFFDECEC)
    val Ambar = Color(0xFFD97706)
    val ExitoSuave = Color(0xFFE8F5E9)
}

@OptIn(ExperimentalTextApi::class)
private fun peso(w: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val PlusJakarta = FontFamily(peso(400), peso(500), peso(600), peso(700), peso(800))

/** Números tabulares: no "bailan" cuando el valor cambia cada segundo. */
val Tabular = TextStyle(fontFeatureSettings = "tnum")

private val base = Typography()
private fun TextStyle.j() = copy(fontFamily = PlusJakarta)

private val Tipografia = Typography(
    displayLarge = base.displayLarge.j(),
    displayMedium = base.displayMedium.j(),
    displaySmall = base.displaySmall.j(),
    headlineLarge = base.headlineLarge.j().copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.j().copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.j().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.j().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.j().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.j().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.j(),
    bodyMedium = base.bodyMedium.j(),
    bodySmall = base.bodySmall.j(),
    labelLarge = base.labelLarge.j().copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.j().copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.j(),
)

private val Colores = lightColorScheme(
    primary = Paleta.Primario,
    onPrimary = Color.White,
    primaryContainer = Paleta.PrimarioSuave,
    onPrimaryContainer = Paleta.PrimarioOscuro,
    secondary = Color(0xFF4A7C89),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3ECF3),
    onSecondaryContainer = Color(0xFF164D59),
    tertiary = Paleta.Exito,
    background = Paleta.Fondo,
    onBackground = Paleta.Tinta,
    surface = Paleta.Fondo,
    onSurface = Paleta.Tinta,
    surfaceVariant = Paleta.Recesado,
    onSurfaceVariant = Paleta.TintaSuave,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F3F1),
    surfaceContainer = Color(0xFFEFEEEB),
    surfaceContainerHigh = Color(0xFFE9E8E5),
    surfaceContainerHighest = Color(0xFFE3E2E0),
    outline = Color(0xFF70787C),
    outlineVariant = Paleta.Borde,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

@Composable
fun TemaSismografo(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, typography = Tipografia, content = content)
}
