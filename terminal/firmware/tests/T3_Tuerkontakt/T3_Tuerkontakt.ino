// T3 – Türkontakt an D2 (über K6) und GND (über K5)
//
// Logik:  Kontakt ist "NO" = schliesst, wenn der Magnet nah ist.
//         D2 hat den internen Pull-up-Widerstand -> ohne Verbindung liest D2 HIGH.
//   Tür zu   -> Magnet nah -> Kontakt geschlossen -> D2 mit GND verbunden -> LOW
//   Tür offen-> Kontakt offen -> Pull-up zieht D2 hoch                -> HIGH
//   Kabel ab -> wie offen -> HIGH (sicher: die Box hält die Tür dann nie für "zu")
//
// Entprellen: ein neuer Zustand gilt erst, wenn er 300 ms stabil ist.
// Erwartung: Tür zu = LED grün + "ZU (D2=LOW)", Tür offen = LED rot + "OFFEN (D2=HIGH)".
// Test: Tür langsam öffnen und den Spalt messen, bei dem auf OFFEN gewechselt wird.

const int PIN_DOOR = D2;
const unsigned long STABLE_MS = 300;

int stableState = -1;           // -1 = noch unbekannt
int lastRead = HIGH;
unsigned long lastChange = 0;

void setLed(bool r, bool g) {
  digitalWrite(LED_RED,   r ? LOW : HIGH);
  digitalWrite(LED_GREEN, g ? LOW : HIGH);
  digitalWrite(LED_BLUE,  HIGH);
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_DOOR, INPUT_PULLUP);
  pinMode(LED_RED, OUTPUT); pinMode(LED_GREEN, OUTPUT); pinMode(LED_BLUE, OUTPUT);
  setLed(false, false);
  delay(1500);
  Serial.println("T3 Tuerkontakt-Test (D2, INPUT_PULLUP)");
}

void loop() {
  int r = digitalRead(PIN_DOOR);
  if (r != lastRead) { lastRead = r; lastChange = millis(); }
  if (millis() - lastChange >= STABLE_MS && r != stableState) {
    stableState = r;
    if (stableState == LOW) { Serial.println("Tuer ZU    (D2 = LOW)");  setLed(false, true); }
    else                    { Serial.println("Tuer OFFEN (D2 = HIGH)"); setLed(true, false); }
  }
  delay(5);
}
