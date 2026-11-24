// T4 – Uhr DS3231 (I2C: SDA = A4, SCL = A5, VCC = 3V3, GND)
// Bibliothek: "RTClib" von Adafruit (Bibliotheksverwalter).
//
// 1. Sucht alle I2C-Geräte. Erwartet: 0x68 (DS3231) und meist 0x57 (EEPROM auf dem Modul).
// 2. Liest die Uhrzeit jede Sekunde.
// Befehle im Seriellen Monitor (115200, "Neue Zeile"):
//   s  -> Uhr auf die Kompilierzeit stellen (direkt nach dem Hochladen eingeben)
//   t  -> Temperatur des DS3231 anzeigen

#include <Wire.h>
#include <RTClib.h>

RTC_DS3231 rtc;
bool rtcOk = false;

void scanI2C() {
  Serial.println("I2C-Suche ...");
  int n = 0;
  for (uint8_t a = 1; a < 127; a++) {
    Wire.beginTransmission(a);
    if (Wire.endTransmission() == 0) {
      Serial.print("  Geraet bei 0x"); if (a < 16) Serial.print("0"); Serial.println(a, HEX);
      n++;
    }
  }
  if (n == 0) Serial.println("  nichts gefunden -> Kabel SDA/SCL/VCC/GND pruefen");
}

void setup() {
  Serial.begin(115200);
  delay(1500);
  Wire.begin();                 // Nano ESP32: A4 = SDA, A5 = SCL
  scanI2C();
  rtcOk = rtc.begin();
  if (!rtcOk) { Serial.println("DS3231 nicht gefunden!"); return; }
  if (rtc.lostPower()) {
    Serial.println("Uhr hatte keinen Strom (Zelle leer/fehlt) -> stelle auf Kompilierzeit");
    rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
  }
  Serial.println("Befehle: s = Zeit setzen, t = Temperatur");
}

void loop() {
  if (Serial.available()) {
    char c = Serial.read();
    if (c == 's' && rtcOk) { rtc.adjust(DateTime(F(__DATE__), F(__TIME__))); Serial.println("Zeit gesetzt."); }
    if (c == 't' && rtcOk) { Serial.print("Temperatur: "); Serial.print(rtc.getTemperature()); Serial.println(" C"); }
  }
  static unsigned long last = 0;
  if (rtcOk && millis() - last >= 1000) {
    last = millis();
    Serial.println(rtc.now().timestamp(DateTime::TIMESTAMP_FULL));
  }
}
