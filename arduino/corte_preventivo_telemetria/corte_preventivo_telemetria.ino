// VARIANTE CON TELEMETRÍA para la app Corte Sísmico.
// Es el mismo sketch de corte_preventivo.ino con 3 añadidos (marcados con [TELEMETRÍA]):
//   A:<m/s²>         aceleración actual, 5 veces por segundo
//   G:<m/s²>,<n>     pico sobre el umbral que todavía no llega a 4 (n = contador)
// La lógica de detección, el corte y el rearme NO cambian.

#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_ADXL345_U.h>
#include <Servo.h>
#include <SoftwareSerial.h>

// Configuración del Bluetooth HC-06 (Pin 10 = RX, Pin 11 = TX)
SoftwareSerial BT(10, 11);

// Configuración del acelerómetro ADXL345 y del servomotor
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);
Servo servoGas;

// Pines conectados en el Arduino Uno
const int pinBoton    = 2;
const int pinServo    = 6;
const int pinRelay    = 7;
const int pinBuzzer   = 8;
const int pinLedVerde = 9;
const int pinLedRojo  = 12;

// Variables para guardar el estado del sistema
float baseX = 0.0;
float baseY = 0.0;
float baseZ = 0.0;
bool sismoDetectado = false;
bool sensorActivo = false;
unsigned long tiempoUltimoPico = 0;
int contadorPicos = 0;
unsigned long ultimoEnvio = 0; // [TELEMETRÍA]

void setup() {
  Serial.begin(9600); // Comunicación con el computador (USB)
  BT.begin(9600);     // Comunicación con el celular (Bluetooth)

  // Evita que el Arduino se congele cuando el servo y el relé se activan
  Wire.begin();
  Wire.setWireTimeout(3000, true);

  // Configurar entradas y salidas
  pinMode(pinBoton, INPUT_PULLUP);
  pinMode(pinRelay, OUTPUT);
  pinMode(pinBuzzer, OUTPUT);
  pinMode(pinLedVerde, OUTPUT);
  pinMode(pinLedRojo, OUTPUT);

  // [TELEMETRÍA] servoGas.attach() se hace solo al mover el servo (corte / rearme)

  // Encender y revisar el acelerómetro ADXL345
  if (!accel.begin()) {
    sensorActivo = false;
    Serial.println("Aviso: ADXL345 no detectado");
  } else {
    sensorActivo = true;
    accel.setRange(ADXL345_RANGE_8_G);
    Serial.println("ADXL345 iniciado correctamente en 0x53");
  }

  // Dejar todo en estado normal al arrancar
  rearmarSistema();
}

void loop() {
  // 1. Revisar si llegó una letra ('C' o 'R') desde el celular por Bluetooth
  if (BT.available() > 0) {
    char dato = BT.read();
    if (dato >= 32 && dato <= 126) {
      procesarComando(dato);
    }
  }

  // 2. Revisar si llegó una letra desde el Monitor Serie del PC
  if (Serial.available() > 0) {
    char dato = Serial.read();
    if (dato >= 32 && dato <= 126) {
      procesarComando(dato);
    }
  }

  // 3. Revisar si alguien presionó el botón físico de reinicio
  if (digitalRead(pinBoton) == LOW) {
    delay(200);
    rearmarSistema();
  }

  // 4. Si no hay sismo, vigilar vibraciones; si hay sismo, sonar sirena
  if (sismoDetectado == false) {
    if (sensorActivo == true) {
      vigilarSismo();
    }
    delay(50);
  } else {
    digitalWrite(pinLedRojo, HIGH);
    tone(pinBuzzer, 2000);
    delay(250);
    digitalWrite(pinLedRojo, LOW);
    tone(pinBuzzer, 1200);
    delay(250);
  }
}

// --- FUNCIONES DEL SISTEMA ---

void procesarComando(char comando) {
  if (comando == 'R' || comando == 'r') {
    rearmarSistema();
  }
  if (comando == 'C' || comando == 'c') {
    ejecutarCorteEmergencia();
  }
}

void vigilarSismo() {
  sensors_event_t event;
  accel.getEvent(&event);

  // Calcular qué tanto se movió respecto a su posición en reposo
  float x = event.acceleration.x - baseX;
  float y = event.acceleration.y - baseY;
  float z = event.acceleration.z - baseZ;
  float magnitud = sqrt(x * x + y * y + z * z);

  // [TELEMETRÍA] Mandar la aceleración a la app cada 200 ms
  if (millis() - ultimoEnvio >= 200) {
    ultimoEnvio = millis();
    BT.print("A:");
    BT.println(magnitud, 2);
  }

  // Si la sacudida supera la fuerza mínima (3.0)
  if (magnitud > 3.0) {
    unsigned long ahora = millis();

    if (ahora - tiempoUltimoPico > 1500) {
      contadorPicos = 1;
    } else {
      contadorPicos++;
    }
    tiempoUltimoPico = ahora;

    // 4 sacudidas seguidas = Sismo real
    if (contadorPicos >= 4) {
      ejecutarCorteEmergencia();
    } else {
      tone(pinBuzzer, 1000, 100);
      Serial.print("Golpe ignorado. Mag: ");
      Serial.println(magnitud);
      // [TELEMETRÍA] Avisar a la app del pico (la app registra el golpe ignorado)
      BT.print("G:");
      BT.print(magnitud, 2);
      BT.print(",");
      BT.println(contadorPicos);
    }
    delay(200);
  }
}

void ejecutarCorteEmergencia() {
  sismoDetectado = true;
  digitalWrite(pinLedVerde, LOW);
  digitalWrite(pinRelay, HIGH);
  servoGas.attach(pinServo); // [TELEMETRÍA] el servo solo recibe pulsos mientras se mueve
  servoGas.write(90);

  delay(80);
  Serial.println("S! ALERTA SISMO! CORTE EJECUTADO");
  delay(40);
  BT.println("S! ALERTA SISMO! CORTE EJECUTADO");

  delay(400);          // [TELEMETRÍA] dejar que llegue a 90° y soltarlo: SoftwareSerial
  servoGas.detach();   // ya no puede hacerlo temblar
}

void rearmarSistema() {
  sismoDetectado = false;
  contadorPicos = 0;

  digitalWrite(pinLedRojo, LOW);
  digitalWrite(pinLedVerde, HIGH);
  digitalWrite(pinRelay, LOW);
  servoGas.attach(pinServo); // [TELEMETRÍA]
  servoGas.write(0);
  noTone(pinBuzzer);

  // Esperar un instante a que el servo termine de moverse antes de calibrar
  delay(400);
  servoGas.detach(); // [TELEMETRÍA] evita que el envío continuo por BT haga temblar el servo

  if (sensorActivo == true) {
    sensors_event_t event;
    accel.getEvent(&event);
    baseX = event.acceleration.x;
    baseY = event.acceleration.y;
    baseZ = event.acceleration.z;
  }

  Serial.println("N! SISTEMA CALIBRADO Y NORMAL");
  delay(40);
  BT.println("N! SISTEMA CALIBRADO Y NORMAL");
}
