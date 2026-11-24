// T5 – Servo MG90S an D9: Mitte setzen und Endlagen des Riegels finden
// Bibliothek: "ESP32Servo" von Kevin Harrington (Bibliotheksverwalter).
// Der Servo bekommt 5 V direkt von K1/K3 (nicht vom Nano). Im Testaufbau 11.2 kommt die gemeinsame Masse über die Brücke K4 -> Nano-GND.
//
// SCHRITT A (Riegel noch NICHT montiert): Sketch hochladen. Der Servo fährt auf 90°.
//           Jetzt das Horn mit dem Riegel so aufstecken, dass der Riegel waagrecht nach LINKS zeigt.
// SCHRITT B (Riegel montiert, Tür zu): mit den Tasten die Endlagen suchen:
//   +  / -   : 1° weiter / zurück          ]  / [  : 10° weiter / zurück
//   l        : aktuelle Stellung als VERRIEGELT merken (Riegel waagrecht, frei im Haken)
//   r        : aktuelle Stellung als FREI merken (Riegel senkrecht nach oben, ganz im Elektronikfach)
//   L / R    : zur gemerkten Stellung fahren      m : Mitte 90°      ?  : Hilfe
// Regel: der Riegel darf in keiner Endlage irgendwo anliegen (sonst brummt der Servo).
// Die zwei Zahlen dann in T6 bei LOCK_ANGLE und RELEASE_ANGLE eintragen.
//
// Werkzeuge -> Pin Numbering muss auf "By GPIO number (legacy)" stehen (siehe Prüfung unten); sonst bricht das
// Kompilieren mit einer Meldung ab.

#include <ESP32Servo.h>

// ESP32Servo steuert den Servo am ESP32-S3 über den MCPWM-Baustein und übergibt die Pinnummer dabei ohne Umrechnung.
// Darum muss unter Werkzeuge -> Pin Numbering "By GPIO number (legacy)" gewählt sein (dann ist D9 = GPIO18).
#if defined(BOARD_HAS_PIN_REMAP) && !defined(BOARD_USES_HW_GPIO_NUMBERS)
#error "Werkzeuge -> Pin Numbering -> 'By GPIO number (legacy)' waehlen: sonst steuert ESP32Servo den Servo am falschen Pin"
#endif

const int PIN_SERVO = D9;
Servo servo;
int angle = 90;
int lockA = -1, releaseA = -1;

void moveTo(int a) {
  angle = constrain(a, 0, 180);
  servo.write(angle);
  Serial.print("Winkel = "); Serial.println(angle);
}

void help() {
  Serial.println("+/- 1 Grad, ]/[ 10 Grad, l = VERRIEGELT merken, r = FREI merken, L/R anfahren, m = 90");
}

void setup() {
  Serial.begin(115200);
  delay(1500);
  ESP32PWM::allocateTimer(0);
  servo.setPeriodHertz(50);
  servo.attach(PIN_SERVO, 500, 2400);   // MG90S: ca. 0,5 – 2,4 ms
  moveTo(90);
  Serial.println("T5 Servo: steht auf 90 Grad (Mitte).");
  help();
}

void loop() {
  if (!Serial.available()) return;
  char c = Serial.read();
  switch (c) {
    case '+': moveTo(angle + 1); break;
    case '-': moveTo(angle - 1); break;
    case ']': moveTo(angle + 10); break;
    case '[': moveTo(angle - 10); break;
    case 'm': moveTo(90); break;
    case 'l': lockA = angle;    Serial.print("VERRIEGELT = "); Serial.println(lockA); break;
    case 'r': releaseA = angle; Serial.print("FREI = ");       Serial.println(releaseA); break;
    case 'L': if (lockA >= 0) moveTo(lockA); break;
    case 'R': if (releaseA >= 0) moveTo(releaseA); break;
    case '?': help(); break;
    default: break;
  }
  if (lockA >= 0 && releaseA >= 0 && (c == 'l' || c == 'r')) {
    Serial.print("-> In T6 eintragen: LOCK_ANGLE = "); Serial.print(lockA);
    Serial.print(";  RELEASE_ANGLE = "); Serial.println(releaseA);
  }
}
