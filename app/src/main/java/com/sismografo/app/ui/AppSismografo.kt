package com.sismografo.app.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sismografo.app.SismografoViewModel
import com.sismografo.app.bluetooth.Emparejado
import com.sismografo.app.ui.tema.Paleta

private enum class Pestana(val etiqueta: String, val icono: ImageVector) {
    MONITOR("Monitor", Icons.Rounded.ShowChart),
    HISTORIAL("Historial", Icons.Rounded.History),
    DISPOSITIVO("Dispositivo", Icons.Rounded.SettingsInputAntenna),
    AJUSTES("Ajustes", Icons.Rounded.Settings),
}

private fun permisosNecesarios(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun tienePermisoBluetooth(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun AppSismografo(vm: SismografoViewModel) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsState()

    var pestana by rememberSaveable { mutableStateOf(Pestana.MONITOR) }
    var permisoBt by remember { mutableStateOf(tienePermisoBluetooth(context)) }
    var refresco by remember { mutableIntStateOf(0) }

    val pedirPermisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permisoBt = tienePermisoBluetooth(context)
        refresco++
    }
    val encenderBt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresco++
        vm.autoconectar()
    }

    LaunchedEffect(Unit) {
        val permisos = permisosNecesarios()
        if (permisos.isNotEmpty()) pedirPermisos.launch(permisos)
    }
    // En cuanto haya permiso, intenta reconectar al último HC-06 usado.
    LaunchedEffect(permisoBt) { if (permisoBt) vm.autoconectar() }

    val btEncendido = remember(refresco, ui.estado) { vm.bluetoothEncendido }
    val emparejados: List<Emparejado> = remember(refresco, permisoBt, btEncendido) {
        if (permisoBt && btEncendido) vm.emparejados() else emptyList()
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Paleta.Tarjeta, tonalElevation = 0.dp) {
                Pestana.entries.forEach { p ->
                    NavigationBarItem(
                        selected = pestana == p,
                        onClick = { pestana = p; if (p == Pestana.DISPOSITIVO) refresco++ },
                        icon = { Icon(p.icono, null) },
                        label = { Text(p.etiqueta) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = Paleta.TintaSuave,
                            unselectedTextColor = Paleta.TintaSuave,
                        ),
                    )
                }
            }
        },
    ) { interno ->
        val dir = LocalLayoutDirection.current
        val padding = PaddingValues(
            start = interno.calculateStartPadding(dir) + 20.dp,
            end = interno.calculateEndPadding(dir) + 20.dp,
            top = interno.calculateTopPadding() + 8.dp,
            bottom = interno.calculateBottomPadding() + 24.dp,
        )

        Crossfade(pestana, label = "pestanas") { actual ->
            when (actual) {
                Pestana.MONITOR -> PantallaMonitor(
                    ui, padding,
                    onIrADispositivo = { pestana = Pestana.DISPOSITIVO; refresco++ },
                    onIrAHistorial = { pestana = Pestana.HISTORIAL },
                )
                Pestana.HISTORIAL -> PantallaHistorial(ui.eventos, padding, onBorrar = vm::borrarHistorial)
                Pestana.DISPOSITIVO -> PantallaDispositivo(
                    ui = ui,
                    contentPadding = padding,
                    permisoBt = permisoBt,
                    btEncendido = btEncendido,
                    emparejados = emparejados,
                    onPedirPermiso = { pedirPermisos.launch(permisosNecesarios()) },
                    onEncenderBt = { encenderBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                    onConectar = vm::conectar,
                    onDesconectar = vm::desconectar,
                    onRecalibrar = vm::recalibrar,
                    onRefrescar = { refresco++ },
                    onAbrirAjustesBt = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                )
                Pestana.AJUSTES -> PantallaAjustes(
                    ui.ajustes, padding,
                    onCambio = vm::actualizarAjustes,
                    onProbarAlerta = vm::probarAlerta,
                )
            }
        }
    }

    // Alerta a pantalla completa por encima de todo
    val alerta = ui.alertaActiva
    AnimatedVisibility(
        visible = alerta != null && (ui.ajustes.pantallaCompleta || ui.alertaEsPrueba),
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut(),
    ) {
        // Se conserva el último evento durante la animación de salida
        val ultimo = remember { mutableStateOf(alerta) }
        if (alerta != null) ultimo.value = alerta
        ultimo.value?.let {
            PantallaAlerta(
                evento = it,
                esPrueba = ui.alertaEsPrueba,
                nombreSensor = ui.dispositivo?.nombre,
                alarmaSonando = ui.alarmaSonando,
                onEstoyASalvo = vm::estoyASalvo,
                onSilenciar = vm::silenciar,
            )
        }
    }

    // Si la pantalla completa está desactivada, se muestra un aviso compacto
    if (alerta != null && !ui.ajustes.pantallaCompleta && !ui.alertaEsPrueba) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::estoyASalvo,
            containerColor = Color.White,
            title = { Text("¡Alerta sísmica!") },
            text = { Text("Magnitud ${"%.1f".format(alerta.magnitud)}. Agáchate, cúbrete y sujétate.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = vm::estoyASalvo) { Text("Estoy a salvo") } },
        )
    }
    }
}
