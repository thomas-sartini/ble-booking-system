// T1 – RGB-LED des Nano ESP32 testen
// Board: "Arduino Nano ESP32" · Seriellen Monitor auf 115200 Baud stellen.
// Die RGB-LED ist "aktiv LOW": LOW = an, HIGH = aus.
// Erwartung: durch den Lichtleiter in der Frontblende leuchtet es nacheinander rot, grün, blau.

void setLed(bool r, bool g, bool b) {
  digitalWrite(LED_RED,   r ? LOW : HIGH);
  digitalWrite(LED_GREEN, g ? LOW : HIGH);
  digitalWrite(LED_BLUE,  b ? LOW : HIGH);
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);
  setLed(false, false, false);
  delay(1500);
  Serial.println("T1 LED-Test: rot, gruen, blau, orange");
}

void loop() {
  Serial.println("rot");    setLed(true, false, false);  delay(1000);
  Serial.println("gruen");  setLed(false, true, false);  delay(1000);
  Serial.println("blau");   setLed(false, false, true);  delay(1000);
  Serial.println("orange"); setLed(true, true, false);   delay(1000);
  setLed(false, false, false); delay(600);
}
