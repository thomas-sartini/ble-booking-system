// T2 – Aktiver Summer an D3 (Plus an D3, Minus an GND)
// Ein aktiver Summer piept von selbst, sobald D3 HIGH ist (kein tone() nötig).
// Signale wie T6 (Takt nach dem Vorbild kontaktloser Kartenterminals, EMV):
// Erwartung: 1 Piep 0,5 s, 2 kurze Pieps, 3 kurze Pieps – der ganze Durchlauf dauert etwa 6,5 Sekunden.

const int PIN_BUZZER = D3;

void beep(int ms) {
  digitalWrite(PIN_BUZZER, HIGH);
  delay(ms);
  digitalWrite(PIN_BUZZER, LOW);
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_BUZZER, OUTPUT);
  digitalWrite(PIN_BUZZER, LOW);
  delay(1500);
  Serial.println("T2 Summer-Test");
}

void loop() {
  Serial.println("OK: 1 Piep 0,5 s");          beep(500);                                                  delay(1200);
  Serial.println("Abgelehnt: 2 kurze Pieps");  beep(200); delay(200); beep(200);                           delay(1200);
  Serial.println("Fehler: 3 kurze Pieps");     beep(200); delay(200); beep(200); delay(200); beep(200);    delay(2000);
}
