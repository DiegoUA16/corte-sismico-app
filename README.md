# Corte Sísmico — Sistema automático de corte preventivo por aceleración sísmica

Prototipo funcional que **mide la aceleración local del terreno** con un acelerómetro triaxial
**ADXL345** y, ante un sismo fuerte, **cierra la válvula de gas, corta la energía eléctrica y
envía una alerta por Bluetooth** para prevenir desastres secundarios (fugas de gas, cortocircuitos,
incendios).

No es un sismógrafo científico: no registra temblores lejanos ni calcula magnitudes. Vigila la
vibración *en el lugar donde está instalado* y actúa solo cuando la sacudida es peligrosa.
Para no cortar por vibraciones cotidianas o golpes accidentales, **filtra los impactos aislados**
y solo actúa ante **oscilaciones sostenidas** sobre el umbral de riesgo. Después de un corte
exige un **rearme manual seguro**.

Este repositorio contiene:

| Carpeta | Qué es |
|---|---|
| `arduino/corte_preventivo/` | Sketch del equipo (Arduino Uno + ADXL345 + servo + relé + HC-06) |
| `arduino/corte_preventivo_telemetria/` | Mismo sketch + envío opcional de aceleración en vivo a la app |
| `app/` | App Android **Corte Sísmico** (Kotlin + Jetpack Compose) |
| `apk/CorteSismico-v2.0.apk` | APK lista para instalar |
| `diseno/` | Pantallas diseñadas en Google Stitch |

---

## Cómo decide cortar

```
ADXL345 ──► |a − a_reposo| ──► ¿> 3.0 m/s²? ──► cuenta picos ──► ¿4 picos con < 1.5 s entre ellos?
                                   │ no                                 │ no → golpe ignorado (pitido corto)
                                   ▼                                    ▼ sí
                               sigue vigilando              CORTE: servo 90° (gas cerrado)
                                                                   relé ON (energía cortada)
                                                                   sirena + LED rojo
                                                                   BT → "S! ALERTA SISMO! CORTE EJECUTADO"
```

| Parámetro | Valor en el sketch |
|---|---|
| Umbral de riesgo | `3.0` m/s² (≈ 0.31 g) sobre la posición de reposo |
| Picos necesarios | `4` |
| Tiempo máximo entre picos | `1500` ms (si pasa más, el conteo vuelve a 1) |
| Rango del ADXL345 | ±8 g (`ADXL345_RANGE_8_G`) |
| Lectura | cada ~50 ms (`delay(50)` del `loop`) |

**Rearme:** botón físico (pin 2), o la letra `R` por Bluetooth / Monitor Serie. Apaga sirena, abre
la válvula (servo 0°), reconecta la energía (relé OFF), espera 400 ms y **recalibra** la posición de
reposo del sensor. Por eso el sensor no debe moverse en ese instante.

## Hardware

| Componente | Conexión (Arduino Uno) |
|---|---|
| Acelerómetro ADXL345 | SDA → A4, SCL → A5, VCC → 3.3 V/5 V (según módulo), GND. Dirección I²C `0x53` |
| Bluetooth HC-06 | TX del HC-06 → **pin 10** (RX), RX del HC-06 → **pin 11** (TX) con divisor de voltaje 1 kΩ / 2 kΩ (el RX del HC-06 es de 3.3 V) |
| Servo de la válvula de gas | Señal → **pin 6** (alimentación externa recomendada) |
| Relé de energía | IN → **pin 7** (HIGH = energía cortada) |
| Buzzer (sirena) | **pin 8** |
| LED verde (normal) | **pin 9** con resistencia 220 Ω |
| LED rojo (alerta) | **pin 12** con resistencia 220 Ω |
| Botón de rearme | **pin 2** a GND (usa `INPUT_PULLUP`) |

> Alimenta el servo y el relé desde una fuente aparte (GND común con el Arduino): si comparten los
> 5 V del USB, el pico de corriente puede reiniciar la placa o colgar el bus I²C. El sketch ya usa
> `Wire.setWireTimeout(3000, true)` para no congelarse si eso pasa.

**Librerías** (Arduino IDE → *Administrar bibliotecas*): `Adafruit ADXL345`, `Adafruit Unified Sensor`.
`Servo`, `Wire` y `SoftwareSerial` vienen con el IDE.

## Protocolo Bluetooth (HC-06, 9600 baudios)

| Dirección | Mensaje | Significado |
|---|---|---|
| Arduino → app | `S! ALERTA SISMO! CORTE EJECUTADO` | Corte ejecutado: gas cerrado, energía cortada, sirena |
| Arduino → app | `N! SISTEMA CALIBRADO Y NORMAL` | Al encender y después de cada rearme |
| app → Arduino | `C` | Corte de emergencia manual |
| app → Arduino | `R` | Rearme (equivale al botón físico) |
| Arduino → app *(variante)* | `A:0.42` | Aceleración actual en m/s², 5 veces por segundo |
| Arduino → app *(variante)* | `G:4.10,2` | Pico sobre el umbral que aún no llega a 4 (magnitud, contador) |

El sketch base solo envía `S!` y `N!`. Con eso la app ya muestra el estado, alerta, permite cortar y
rearmar. Si cargas `corte_preventivo_telemetria.ino`, la app además dibuja la aceleración en vivo,
llena el indicador de picos y registra los **golpes ignorados**. La lógica de detección es idéntica.

## La app Android

| Pantalla | Qué hace |
|---|---|
| **Monitor** | Estado del sistema (vigilando / corte activo), válvula de gas, energía y sirena; botones **Corte manual** (con confirmación) y **Rearmar**; aceleración en vivo con la línea del umbral; filtro de 4 picos; actividad reciente |
| **Alerta** | Pantalla completa roja al recibir `S!`: estado de la instalación, "agáchate · cúbrete · sujétate", qué hacer después, llamar al 123, silenciar. También notificación + alarma + vibración aunque la app esté en segundo plano |
| **Rearme seguro** | Lista de 4 verificaciones (terminó el movimiento, no hay olor a gas, no hay cables dañados, todos a salvo) y botón de **mantener presionado**. Envía `R` y espera la confirmación `N!` |
| **Eventos** | Registro de cortes automáticos y manuales, rearmes (desde la app o con el botón físico), golpes ignorados y conexiones. Resumen de 30 días, filtros, compartir y borrar |
| **Sistema** | Conexión con el HC-06, dispositivos emparejados, cómo funciona, parámetros del filtro, pines y diagnóstico del último mensaje |
| **Ajustes** | Notificación, sonido, vibración, pantalla completa, reconexión automática, probar alerta y **modo demostración** |

**Modo demostración:** simula el ADXL345 dentro del teléfono con la misma lógica de
`vigilarSismo()`. Los botones *Golpe aislado* y *Sismo* permiten enseñar el filtro y el corte sin el
equipo. Corte manual y rearme funcionan igual que con el Arduino.

### Instalar el APK
1. Copia `apk/CorteSismico-v2.0.apk` al celular (cable, Drive, WhatsApp…) y ábrelo.
2. Si Android lo pide, permite *Instalar apps desconocidas* para esa app.
3. Acepta los permisos de Bluetooth y notificaciones.

Es una compilación *debug*: sirve para pruebas y para la entrega, no para publicar en Play Store.

### Probar con el equipo
1. Carga `arduino/corte_preventivo/corte_preventivo.ino` (o la variante con telemetría).
2. Empareja el HC-06 en *Ajustes > Bluetooth* del celular (PIN `1234` o `0000`).
3. En la app: **Sistema** → *Conectar* en el HC-06. El Monitor muestra el estado cuando llega `N!`
   (pulsa rearmar o el botón físico si el equipo ya estaba encendido).
4. Da un golpe a la mesa: el filtro lo ignora. Sacude el sensor de forma sostenida: el equipo corta
   y la app lanza la alerta.
5. Haz el **rearme seguro** desde la app o con el botón físico.

### Compilar
Android Studio → *File > Open* → esta carpeta → ▶ Run. O por consola con JDK 17 y el SDK de Android:
`./gradlew assembleDebug` (sale en `app/build/outputs/apk/debug/`).

### Estructura
```
app/src/main/java/com/sismografo/app/
├── MainActivity.kt
├── SismografoViewModel.kt       estado, protocolo S!/N!, comandos C/R, reconexión, demo
├── NotificadorAlertas.kt        notificación de corte (con pantalla completa)
├── AlarmaLocal.kt               alarma y vibración en bucle
├── bluetooth/ConexionHC06.kt    socket RFCOMM (SPP) con el HC-06
├── datos/Sistema.kt             parámetros del filtro, estados, eventos y protocolo
├── datos/SimuladorArduino.kt    réplica de vigilarSismo() para el modo demostración
├── datos/Almacen.kt             ajustes, registro y último estado en el teléfono
└── ui/
    ├── tema/Tema.kt             colores y tipografía (sistema de diseño de Stitch)
    ├── Componentes.kt           tarjetas, chips, barra de aceleración, acelerograma, picos
    ├── AppSismografo.kt         navegación, permisos y capas de alerta/rearme
    ├── PantallaMonitor.kt  PantallaEventos.kt  PantallaSistema.kt  PantallaAjustes.kt
    └── PantallaAlerta.kt   PantallaRearme.kt
```

## Diseño (Google Stitch)
Las pantallas se diseñaron en Google Stitch y se implementaron en Jetpack Compose. Referencias en `diseno/`:
`1-monitor.png`, `2-corte-ejecutado.png`, `3-rearme-seguro.png`, `4-eventos.png`.

Sistema de diseño: verde azulado `#0E5A6D`, fondo marfil `#FAF9F6`, tipografía **Plus Jakarta Sans**
con números tabulares y tarjetas de 24 dp. El rojo se reserva para el corte y el ámbar para los
picos/golpes.

## Limitaciones conocidas
- La app escucha mientras está abierta o en segundo plano reciente. Si Android la cierra, el equipo
  **sigue protegiendo igual** (la decisión de cortar es del Arduino); solo se pierde el aviso en el
  teléfono. Para vigilancia 24/7 en el celular habría que pasar la conexión a un *Foreground Service*.
- El HC-06 no avisa si el Arduino se reinicia; el estado de la app se confirma con el siguiente `S!` o `N!`.
- Es un prototipo académico: el umbral de 3.0 m/s² está calibrado para demostración sobre una mesa,
  no para una instalación real de gas o electricidad.
