# Sismógrafo — App de alertas sísmicas por Bluetooth

App Android (Kotlin + Jetpack Compose) que se conecta a un sismógrafo hecho con
Arduino y un módulo **HC-06**, muestra la magnitud en tiempo real y lanza
**alertas** (notificación + vibración + aviso en pantalla) cuando se detecta un sismo.

## Funciones
- Conexión al HC-06 emparejado (Bluetooth clásico, perfil SPP).
- Magnitud actual con su clasificación en la escala de Richter (color + descripción).
- Gráfica de las últimas 60 lecturas, con la línea del umbral de alerta.
- Umbral de alerta ajustable (de 2.0 a 8.0).
- Notificación de alta prioridad con vibración aunque estés en otra app.
- Historial de alertas con fecha y hora.
- Botón **Recalibrar**: manda `CAL` al Arduino.

## Protocolo (Arduino → app)
Una línea por mensaje, terminada en `\n`, a 9600 baudios:

| Mensaje       | Significado                               |
|---------------|-------------------------------------------|
| `MAG:3.42`    | Lectura normal                            |
| `ALERTA:5.10` | El Arduino detectó un sismo (alerta segura)|
| `3.42`        | También se acepta solo el número          |

La app también alerta si un `MAG:` supera el umbral configurado en la app.

## Instalar el APK
El APK listo para instalar está en `apk/Sismografo-v1.0.apk`.
1. Pásalo al celular (cable, Drive, WhatsApp…) y ábrelo.
2. Si Android lo pide, permite *Instalar apps desconocidas* para esa app.
3. Al abrir Sismógrafo, acepta los permisos de Bluetooth y notificaciones.

Es una compilación *debug*: sirve para pruebas y para la entrega, no para publicar en Play Store.

## Cómo abrirla
1. Instala **Android Studio** (trae el SDK y Java).
2. *File > Open* → carpeta `SismografoApp`. Deja que sincronice Gradle.
3. En el celular: activa *Opciones de desarrollador* y *Depuración USB*, conéctalo y pulsa ▶ Run.

## Cómo probarla
1. Carga `arduino/sismografo_hc06/sismografo_hc06.ino` en el Arduino.
2. Empareja el HC-06 desde *Ajustes > Bluetooth* del celular (PIN `1234` o `0000`).
3. Abre la app → **Conectar sismógrafo** → elige el HC-06.
4. Golpea la mesa: deberías ver subir la magnitud y, si pasa el umbral, la alerta.

## Diseño (Google Stitch)
Las interfaces se diseñaron en Google Stitch (proyecto *"Sismógrafo — Alerta Sísmica HC-06"*)
y luego se implementaron en Jetpack Compose. Las capturas de referencia están en `diseno/`:

| Pantalla | Qué muestra |
|---|---|
| `1-monitor.png` | Medidor semicircular por escala de Richter, estadísticas, sismograma en vivo, alertas recientes |
| `2-alerta.png` | Alerta a pantalla completa: magnitud, protocolo "agáchate · cúbrete · sujétate", "Estoy a salvo" |
| `3-historial.png` | Filtros, resumen de 30 días con barras por día, lista agrupada por fecha |
| `4-dispositivo.png` | Estado del enlace HC-06, diagnóstico, dispositivos emparejados |
| `5-ajustes.png` | Umbral con degradado de severidad, modos de aviso, reconexión |

Sistema de diseño: verde azulado océano `#0E5A6D`, fondo marfil `#FAF9F6` (sin blanco ni negro
puros), tipografía **Plus Jakarta Sans** con números tabulares, tarjetas de 24dp y colores de
severidad reservados solo para la magnitud.

## Estructura
```
app/src/main/java/com/sismografo/app/
├── MainActivity.kt             punto de entrada
├── SismografoViewModel.kt      estado, lectura de datos, episodios de alerta, reconexión
├── NotificadorAlertas.kt       notificación (con pantalla completa)
├── AlarmaLocal.kt              sonido de alarma y vibración en bucle
├── bluetooth/ConexionHC06.kt   socket RFCOMM con el HC-06
├── datos/Sismo.kt              lectura, evento, escala de Richter y protocolo
├── datos/Almacen.kt            ajustes e historial guardados en el teléfono
└── ui/
    ├── tema/Tema.kt            colores y tipografía del sistema de diseño
    ├── Componentes.kt          tarjetas, chips, medidor, sismograma, interruptores
    ├── AppSismografo.kt        navegación inferior y permisos
    ├── PantallaMonitor.kt  PantallaHistorial.kt  PantallaDispositivo.kt
    └── PantallaAjustes.kt  PantallaAlerta.kt
arduino/sismografo_hc06/        sketch de ejemplo (MPU6050 + HC-06)
diseno/                         capturas de Stitch
```

## Limitaciones conocidas
- La conexión vive mientras la app esté abierta o en segundo plano reciente; si
  Android cierra la app, deja de escuchar. Para vigilancia 24/7 habría que pasar la
  conexión a un *Foreground Service*.
- La magnitud del sketch de ejemplo es una **estimación** a partir de la aceleración
  pico de un solo sensor; no equivale a la magnitud oficial de un sismo.
