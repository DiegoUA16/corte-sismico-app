/*
 * Sismógrafo con MPU6050 + HC-06
 * Envía por Bluetooth una línea por segundo:
 *   MAG:x.xx      lectura normal
 *   ALERTA:x.xx   cuando la magnitud estimada supera UMBRAL_ALERTA
 * Si la app manda "CAL", el sensor se recalibra.
 *
 * Conexiones:
 *   MPU6050  VCC->5V  GND->GND  SDA->A4  SCL->A5
 *   HC-06    VCC->5V  GND->GND  TXD->pin 10
 *            RXD->pin 11 (con divisor de voltaje 1k/2k: el HC-06 trabaja a 3.3V)
 *
 * IMPORTANTE: un solo acelerómetro no mide la magnitud real de un sismo
 * (para eso hace falta la distancia al epicentro y varias estaciones).
 * Aquí se ESTIMA a partir de la aceleración pico; calibra la fórmula
 * de estimarMagnitud() con tus pruebas.
 *
 * Si ya tienes tu propio código del sismógrafo, solo asegúrate de enviar
 * las líneas con el formato de arriba (terminadas en '\n').
 */

#include <Wire.h>
#include <SoftwareSerial.h>
#include <math.h>

SoftwareSerial bt(10, 11);  // RX, TX

const uint8_t MPU = 0x68;
const float LSB_POR_G = 16384.0;    // rango ±2g
const float GAL_POR_G = 980.665;    // 1 g = 980.665 cm/s²
const float RUIDO_GAL = 5.0;        // por debajo de esto se considera ruido
const float UMBRAL_ALERTA = 4.0;    // magnitud a partir de la cual se manda ALERTA
const unsigned long VENTANA_MS = 1000;

float baseX = 0, baseY = 0, baseZ = 0;
float picoGal = 0;
unsigned long inicioVentana = 0;
String comando = "";

void leerAcel(float &ax, float &ay, float &az) {
  Wire.beginTransmission(MPU);
  Wire.write(0x3B);
  Wire.endTransmission(false);
  Wire.requestFrom(MPU, (uint8_t)6, (uint8_t)true);
  int16_t x = (Wire.read() << 8) | Wire.read();
  int16_t y = (Wire.read() << 8) | Wire.read();
  int16_t z = (Wire.read() << 8) | Wire.read();
  ax = x / LSB_POR_G;
  ay = y / LSB_POR_G;
  az = z / LSB_POR_G;
}

// Promedia el sensor en reposo para restar la gravedad y el desnivel.
void calibrar() {
  const int N = 300;
  float sx = 0, sy = 0, sz = 0, ax, ay, az;
  for (int i = 0; i < N; i++) {
    leerAcel(ax, ay, az);
    sx += ax; sy += ay; sz += az;
    delay(3);
  }
  baseX = sx / N; baseY = sy / N; baseZ = sz / N;
}

// Estimación aproximada: 10 gal -> 3.0, 100 gal -> 5.0, 1000 gal -> 7.0
float estimarMagnitud(float pga) {
  if (pga < RUIDO_GAL) return 0.0;
  float m = 2.0 * log10(pga) + 1.0;
  return constrain(m, 0.0, 9.9);
}

void setup() {
  Serial.begin(9600);
  bt.begin(9600);  // velocidad por defecto del HC-06
  Wire.begin();
  Wire.beginTransmission(MPU);
  Wire.write(0x6B);  // despertar el MPU6050
  Wire.write(0);
  Wire.endTransmission(true);
  delay(100);
  calibrar();
  inicioVentana = millis();
}

void loop() {
  float ax, ay, az;
  leerAcel(ax, ay, az);
  float dx = ax - baseX, dy = ay - baseY, dz = az - baseZ;
  float gal = sqrt(dx * dx + dy * dy + dz * dz) * GAL_POR_G;
  if (gal > picoGal) picoGal = gal;

  if (millis() - inicioVentana >= VENTANA_MS) {
    float mag = estimarMagnitud(picoGal);
    String linea = (mag >= UMBRAL_ALERTA ? "ALERTA:" : "MAG:") + String(mag, 2);
    bt.println(linea);
    Serial.println(linea);  // para ver en el Monitor Serie
    picoGal = 0;
    inicioVentana = millis();
  }

  // Comandos desde la app
  while (bt.available()) {
    char c = bt.read();
    if (c == '\n') {
      comando.trim();
      if (comando == "CAL") calibrar();
      comando = "";
    } else {
      comando += c;
    }
  }

  delay(5);
}
