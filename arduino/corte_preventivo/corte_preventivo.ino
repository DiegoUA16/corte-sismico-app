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

// Umbral de aceleración sísmica en m/s² (1.6 m/s² ≈ 0.16g - Mercalli VI)
const float umbralSismo = 1.6;

// Variables de estado del sistema
float baseX = 0.0;
float baseY = 0.0;
float baseZ = 0.0;
bool sismoDetectado = false;
bool sensorActivo = false;
unsigned long tiempoUltimoPico = 0;
int contadorPicos = 0;

// Perfil de alarma activo (puedes cambiarlo del 1 al 4 sin recompilar)
int tipoAlarma = 1;

void setup() {
  Serial.begin(9600);
  BT.begin(9600);

  Wire.begin();
  Wire.setWireTimeout(3000, true);

  pinMode(pinBoton, INPUT_PULLUP);
  pinMode(pinRelay, OUTPUT);
  pinMode(pinBuzzer, OUTPUT);
  pinMode(pinLedVerde, OUTPUT);
  pinMode(pinLedRojo, OUTPUT);

  servoGas.attach(pinServo);

  if (!accel.begin()) {
    sensorActivo = false;
    Serial.println("Aviso: ADXL345 no detectado");
  } else {
    sensorActivo = true;
    accel.setRange(ADXL345_RANGE_2_G); // Mayor sensibilidad para maqueta
    Serial.println("ADXL345 iniciado correctamente en 0x53");
  }

  Serial.println("Comandos disponibles: C=Corte, R=Rearme, 1-4=Cambiar alarma, T=Probar sonido, ?=Estado");
  rearmarSistema();
}

void loop() {
  // 1. Vaciar todos los comandos desde el celular (Bluetooth)
  while (BT.available() > 0) {
    char dato = BT.read();
    if (dato >= 32 && dato <= 126) {
      procesarComando(dato);
    }
  }

  // 2. Vaciar todos los comandos desde el Monitor Serie (USB)
  while (Serial.available() > 0) {
    char dato = Serial.read();
    if (dato >= 32 && dato <= 126) {
      procesarComando(dato);
    }
  }

  // 3. Revisar botón físico de rearme (con antirrebote)
  if (digitalRead(pinBoton) == LOW) {
    delay(50);
    if (digitalRead(pinBoton) == LOW) {
      unsigned long t = millis();
      while (digitalRead(pinBoton) == LOW && millis() - t < 1500) {
        delay(10);
      }
      rearmarSistema();
    }
  }

  // 4. Vigilar sismos o ejecutar la alarma seleccionada
  if (sismoDetectado == false) {
    if (sensorActivo == true) {
      vigilarSismo();
    }
    delay(50);
  } else {
    sonarCicloAlarma();
  }
}

// --- COMANDOS EN VIVO (SIN RECOMPILAR) ---

void procesarComando(char comando) {
  if (comando == 'R' || comando == 'r') {
    rearmarSistema();
  }
  else if (comando == 'C' || comando == 'c') {
    ejecutarCorteEmergencia();
  }
  else if (comando == '?') {
    if (sismoDetectado) {
      BT.println("S! ALERTA SISMO! CORTE EJECUTADO");
    } else {
      BT.println("N! SISTEMA CALIBRADO Y NORMAL");
    }
  }
  else if (comando >= '1' && comando <= '4') {
    tipoAlarma = comando - '0'; // Convierte el carácter '1'..'4' en número 1..4
    Serial.print("Perfil de alarma cambiado a opcion: ");
    Serial.println(tipoAlarma);
    if (!sismoDetectado) probarAlarma();
  }
  else if (comando == 'T' || comando == 't') {
    Serial.print("Probando alarma opcion: ");
    Serial.println(tipoAlarma);
    if (!sismoDetectado) probarAlarma();
  }
}

// --- DETECCIÓN SÍSMICA ---

void vigilarSismo() {
  sensors_event_t event;
  accel.getEvent(&event);

  float x = event.acceleration.x - baseX;
  float y = event.acceleration.y - baseY;
  float z = event.acceleration.z - baseZ;
  float magnitud = sqrt(x * x + y * y + z * z);

  if (magnitud > umbralSismo) {
    unsigned long ahora = millis();

    if (ahora - tiempoUltimoPico > 1500) {
      contadorPicos = 1;
    } else {
      contadorPicos++;
    }
    tiempoUltimoPico = ahora;

    if (contadorPicos >= 4) {
      ejecutarCorteEmergencia();
    } else {
      // Enviar G: ANTES de tone() para evitar interferencia de Timer2 con SoftwareSerial
      BT.print("G:");
      BT.print(magnitud, 2);
      BT.print(",");
      BT.println(contadorPicos);
      delay(10);

      tone(pinBuzzer, 1000, 80); // Pitido sutil de impacto aislado
      Serial.print("Golpe ignorado (");
      Serial.print(contadorPicos);
      Serial.print("/4). Mag: ");
      Serial.println(magnitud);
    }
    delay(200);
  }
}

// --- ACTUADORES Y CONFIRMACIONES SONORAS ---

void ejecutarCorteEmergencia() {
  sismoDetectado = true;
  digitalWrite(pinLedVerde, LOW);
  digitalWrite(pinRelay, HIGH);
  servoGas.write(90);

  delay(80);
  Serial.println("S! ALERTA SISMO! CORTE EJECUTADO");
  delay(40);
  BT.println("S! ALERTA SISMO! CORTE EJECUTADO");
}

void rearmarSistema() {
  sismoDetectado = false;
  contadorPicos = 0;

  digitalWrite(pinLedRojo, LOW);
  digitalWrite(pinLedVerde, HIGH);
  digitalWrite(pinRelay, LOW);
  servoGas.write(0);
  noTone(pinBuzzer);

  // Esperar a que el servo deje de moverse antes de calibrar el cero
  delay(400);

  if (sensorActivo == true) {
    sensors_event_t event;
    accel.getEvent(&event);
    baseX = event.acceleration.x;
    baseY = event.acceleration.y;
    baseZ = event.acceleration.z;
  }

  // Confirmación sonora de sistema armado (dos tonos ascendentes suaves)
  tone(pinBuzzer, 1047, 90);  // Nota Do6
  delay(110);
  tone(pinBuzzer, 1568, 130); // Nota Sol6
  delay(150);                 // Pausa para que el buzzer quede en silencio antes de vigilar

  Serial.println("N! SISTEMA CALIBRADO Y NORMAL");
  delay(40);
  BT.println("N! SISTEMA CALIBRADO Y NORMAL");
}

// --- GENERADOR DE SONIDOS DE ALARMA ---

void sonarCicloAlarma() {
  if (tipoAlarma == 1) {
    // Opción 1: Bitono clásico
    digitalWrite(pinLedRojo, HIGH);
    tone(pinBuzzer, 2000);
    delay(200);
    digitalWrite(pinLedRojo, LOW);
    tone(pinBuzzer, 1200);
    delay(200);
    noTone(pinBuzzer); // Pausa breve para que SoftwareSerial reciba 'R' sin interferencia
    delay(120);
  }
  else if (tipoAlarma == 2) {
    // Opción 2: Barrido ascendente tipo sirena electrónica
    digitalWrite(pinLedRojo, HIGH);
    for (int freq = 900; freq <= 2600; freq += 100) {
      tone(pinBuzzer, freq);
      delay(15);
    }
    digitalWrite(pinLedRojo, LOW);
    noTone(pinBuzzer);
    delay(120);
  }
  else if (tipoAlarma == 3) {
    // Opción 3: Pulso industrial intermitente (3 pitidos agudos)
    for (int i = 0; i < 3; i++) {
      digitalWrite(pinLedRojo, HIGH);
      tone(pinBuzzer, 2700);
      delay(80);
      digitalWrite(pinLedRojo, LOW);
      noTone(pinBuzzer);
      delay(70);
    }
    delay(180);
  }
  else if (tipoAlarma == 4) {
    // Opción 4: Alerta sísmica rápida de doble tono agudo
    for (int i = 0; i < 2; i++) {
      digitalWrite(pinLedRojo, HIGH);
      tone(pinBuzzer, 1800);
      delay(100);
      digitalWrite(pinLedRojo, LOW);
      tone(pinBuzzer, 2400);
      delay(100);
    }
    noTone(pinBuzzer); // Pausa breve para que SoftwareSerial reciba 'R' sin interferencia
    delay(120);
  }
}

void probarAlarma() {
  // Reproduce 2 ciclos del sonido elegido sin activar el corte de gas/energía
  for (int i = 0; i < 2; i++) {
    sonarCicloAlarma();
  }
  noTone(pinBuzzer);
  digitalWrite(pinLedRojo, LOW);
}
