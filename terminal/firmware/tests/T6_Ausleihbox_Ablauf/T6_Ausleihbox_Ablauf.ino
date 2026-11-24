// T6 – Gesamtablauf der Ausleihbox nach T-015, Kapitel 4 (Stand V14)
// Die App (BLE) wird hier durch Befehle im Seriellen Monitor ersetzt.
// Seriellen Monitor auf 115200 Baud und Zeilenende "Neue Zeile" stellen. Jeder Befehl ist eine ganze Zeile.
// Eine Zeile mit einem anderen Zeichen als druckbarem ASCII (z. B. Nullzeichen, Tab, Escape, Umlaut) wird ganz verworfen:
//   a = Ausleihen (CHECK_IN)      z = Zurückgeben (CHECK_OUT)      s = Status      h = Hilfe
//   v = ausführlich ein/aus: meldet zusätzlich Servo, Licht und Summer (Zeilen mit "[v]"); nach jedem Start aus
// Servicebefehle (nur im passenden Speicherzustand, siehe 01_Handbuch.pdf, 11.3 "T6: Speicherzustand beim Start"):
//   INIT EMPTY       nur bei "NICHT INITIALISIERT": legt den Zustand "keine Ausleihe" an
//   MIGRATE CONFIRM  nur bei "ALTZUSTAND GEFUNDEN": übernimmt den angezeigten Zustand aus V9/V10
// Bibliotheken: ESP32Servo, RTClib (Adafruit). nvs.h gehört zum ESP32-Board-Paket.
// V13 (08.11.2026): neu nur der Befehl v (ausführliche Meldungen). Ablauf, Pins und Speicher wie V12.
// V14 (08.11.2026): Verriegeln bricht bei der ersten HIGH-Abtastung sofort ab, der Riegel fährt zurück auf frei.
//   Die [v]-Meldung zum Servo kommt erst nach der Bewegung. Befehle, Pins und Speicher wie V13.
//
// Regeln (T-015):
//  * CHECK_IN: zuerst speichern (mit Commit und Rücklesen), dann Riegel freigeben. Grün + 1 Piep (0,5 s).
//  * Der Riegel bleibt während der Ausleihe frei. Tür zu allein verriegelt NICHT.
//  * CHECK_OUT: nur mit aktiver Ausleihe UND geschlossener Tür. Dann speichern (mit Rücklesen) und verriegeln.
//    Verriegelt wird nur, wenn D2 unmittelbar vor dem Servobefehl und bei jeder Abtastung bis zum Ende der Bewegung
//    LOW ist. Die erste HIGH-Abtastung bricht ab: Der Servo fährt sofort zurück auf frei, rot + 2 Pieps, "keine
//    Ausleihe" bleibt gespeichert, verriegelt wird automatisch, sobald die Tür wieder 500 ms zu ist (dann grün + Piep).
//    Gleiches gilt für jede automatische Verriegelung.
//  * Neustart: Ausleihe gespeichert -> Riegel frei. Keine Ausleihe -> verriegeln, sobald die Tür stabil zu ist.
//  * Speicher: Nur ein gelesener gültiger Wert (0xA5 oder 0x5A) gibt den Betrieb frei. Jeder Öffnungs-, Lese-, Typ-,
//    Wert-, Schreib-, Commit- oder Rücklesefehler sperrt alle Aktionen und alle Riegelbewegungen (orange + 3 kurze Pieps).
//    Ein Lesefehler löst nie einen Schreibzugriff aus. Die NVS-Partition wird nie automatisch gelöscht.
//  * Türkontakt: Er wird in der Hauptschleife etwa alle 5 ms und während Piepsen und Riegelbewegung etwa alle 2 ms
//    gelesen. Eine gelesene HIGH-Abtastung gilt sofort als "offen". "Zu" gilt erst, wenn alle Abtastungen seit
//    mindestens 500 ms LOW sind. Impulse, die kürzer sind als der Abstand zweier Abtastungen, werden nicht sicher erfasst.
//    Jede Ausgabe über USB kann diesen Abstand verlängern. Darum gibt es zwischen dem letzten Lesen von D2 und dem
//    Servobefehl zum Verriegeln und während dieser Bewegung keine Ausgabe. "Tuer OFFEN" wird dort erst gemeldet,
//    wenn der Servo schon den Befehl zurück auf frei hat.
//  * Die automatische Verriegelung nach dem Start wird aus dem aktuellen Zustand entschieden (servicePendingLock),
//    nicht aus dem Moment, in dem die Tür zu geht. Das Lesen des Kontakts (updateDoor, doorSample) bewegt nie den
//    Riegel. Nur die Riegelbewegung selbst (moveLatch) schickt den Servo beim Verriegeln bei HIGH zurück auf frei.
//
// Werkzeuge -> Pin Numbering: "By GPIO number (legacy)" (sonst bricht das Kompilieren mit einer Meldung ab).
//
// LOCK_ANGLE und RELEASE_ANGLE aus T5 eintragen!

#include <ESP32Servo.h>
#include <Wire.h>
#include <RTClib.h>
#include <nvs.h>

// ESP32Servo steuert den Servo am ESP32-S3 über den MCPWM-Baustein und übergibt die Pinnummer dabei ohne Umrechnung.
// Darum muss unter Werkzeuge -> Pin Numbering "By GPIO number (legacy)" gewählt sein (dann ist D9 = GPIO18).
#if defined(BOARD_HAS_PIN_REMAP) && !defined(BOARD_USES_HW_GPIO_NUMBERS)
#error "Werkzeuge -> Pin Numbering -> 'By GPIO number (legacy)' waehlen: sonst steuert ESP32Servo den Servo am falschen Pin"
#endif

const int PIN_DOOR = D2;
const int PIN_BUZZER = D3;
const int PIN_SERVO = D9;

int LOCK_ANGLE = 100;    // <- Wert aus T5 ("VERRIEGELT")
int RELEASE_ANGLE = 10;  // <- Wert aus T5 ("FREI")

const unsigned long DOOR_STABLE_MS = 500;   // so lange muss D2 durchgehend LOW sein, bis die Tür als zu gilt
const unsigned long MOVE_MS = 600;          // Zeit für die Vierteldrehung

// ---------------------------------------------------------------- Speicher: Schlüssel und Werte
const char *NVS_NS = "ausleihbox";
const char *KEY_NEW = "loan_v11";           // ab V11: nur 0xA5 oder 0x5A
const char *KEY_OLD = "loan";               // V9 (0/1) und V10 (0x5A/0xA5): wird nur gelesen, nie geändert
const uint8_t LOAN_YES = 0xA5, LOAN_NO = 0x5A;

enum StoreState : uint8_t {
  ST_OK,             // gültiger Zustand gelesen bzw. geschrieben und zurückgelesen
  ST_UNINIT,         // beide Schlüssel nachweislich nicht vorhanden -> INIT EMPTY
  ST_LEGACY,         // nur der alte Schlüssel mit gültigem Wert -> MIGRATE CONFIRM
  ST_ERR_OPEN,       // Speicher liess sich nicht öffnen
  ST_ERR_READ,       // Lesefehler (Fehlercode im Status)
  ST_ERR_TYPE,       // neuer Schlüssel hat einen anderen Typ als u8
  ST_ERR_VALUE,      // neuer Schlüssel: Byte ist weder 0xA5 noch 0x5A
  ST_ERR_OLD_TYPE,   // alter Schlüssel hat einen anderen Typ als u8
  ST_ERR_OLD_VALUE,  // alter Schlüssel: Byte ist weder 0, 1, 0x5A noch 0xA5
  ST_ERR_WRITE,      // nvs_set_u8 fehlgeschlagen
  ST_ERR_COMMIT,     // nvs_commit fehlgeschlagen
  ST_ERR_VERIFY      // Rücklesen fehlgeschlagen oder anderer Wert gelesen
};
// Ergebnis eines Lesezugriffs. Steht vor der ersten Funktion, weil die Arduino-IDE dort die Prototypen einfügt.
enum ReadResult : uint8_t { RD_OK, RD_NOT_FOUND, RD_WRONG_TYPE, RD_ERROR };

const char *storeStateName(StoreState s) {
  switch (s) {
    case ST_OK: return "OK";
    case ST_UNINIT: return "NICHT INITIALISIERT (Befehl: INIT EMPTY)";
    case ST_LEGACY: return "ALTZUSTAND GEFUNDEN (Befehl: MIGRATE CONFIRM)";
    case ST_ERR_OPEN: return "FEHLER: Speicher laesst sich nicht oeffnen";
    case ST_ERR_READ: return "FEHLER: Lesefehler";
    case ST_ERR_TYPE: return "FEHLER: loan_v11 hat falschen Typ";
    case ST_ERR_VALUE: return "FEHLER: loan_v11 hat ungueltigen Wert";
    case ST_ERR_OLD_TYPE: return "FEHLER: alter Schluessel loan hat falschen Typ";
    case ST_ERR_OLD_VALUE: return "FEHLER: alter Schluessel loan hat ungueltigen Wert";
    case ST_ERR_WRITE: return "FEHLER: Schreiben fehlgeschlagen";
    case ST_ERR_COMMIT: return "FEHLER: Commit fehlgeschlagen";
    case ST_ERR_VERIFY: return "FEHLER: Ruecklesen fehlgeschlagen oder Wert falsch";
  }
  return "?";
}

Servo servo;
RTC_DS3231 rtc;
bool rtcOk = false;

StoreState storeState = ST_ERR_READ;   // bis zum ersten erfolgreichen Lesen gesperrt
esp_err_t storeErr = ESP_OK;           // letzter NVS-Fehlercode
int storeRaw = -1;                     // zuletzt gelesenes Byte (-1 = keins)
bool legacyLoan = false;               // erkannter Altzustand (nur bei ST_LEGACY gültig)

bool loan = false;                     // gespeicherter Zustand: Ausleihe aktiv?
int latchPos = -1;                     // -1 unbekannt, 0 frei, 1 verriegelt
bool latchBusy = false;                // läuft gerade eine Riegelbewegung?
bool pendingLock = false;              // verriegeln, sobald die Tür stabil zu ist (nur ohne Ausleihe)
bool confirmOnLock = false;            // nach einer nicht abgeschlossenen Rückgabe: beim Verriegeln grün + Piep
int doorStable = HIGH;                 // entprellter Zustand: LOW = zu, HIGH = offen
bool doorLowValid = false;             // läuft gerade eine LOW-Phase?
unsigned long doorLowSince = 0;        // Beginn der aktuellen LOW-Phase
bool ledTimed = false;
unsigned long ledOffAt = 0;
bool verbose = false;                  // Befehl v: zusätzlich Servo, Licht und Summer melden; nach jedem Start aus

// ---------------------------------------------------------------- Vorwärtsdeklarationen
void updateDoor();
void doorSample(int r);
void servicePendingLock();
void printStoreState();
void startOperation();
bool detail();

// ---------------------------------------------------------------- Ausgabe
void led(char c) {          // 'r' rot, 'g' grün, 'o' orange, 'b' blau, 'x' aus (LED ist aktiv LOW)
  bool r = (c == 'r' || c == 'o'), g = (c == 'g' || c == 'o'), b = (c == 'b');
  digitalWrite(LED_RED, r ? LOW : HIGH);
  digitalWrite(LED_GREEN, g ? LOW : HIGH);
  digitalWrite(LED_BLUE, b ? LOW : HIGH);
}
// warten und dabei den Türkontakt weiter lesen. Hier wird nie ein Riegel bewegt.
void wait(unsigned long ms) { unsigned long t0 = millis(); while (millis() - t0 < ms) { updateDoor(); delay(2); } }
void beep(int ms) { digitalWrite(PIN_BUZZER, HIGH); wait(ms); digitalWrite(PIN_BUZZER, LOW); }
// 'k' bestätigt, 'n' abgelehnt, 'e' Speicher/technischer Fehler, 'u' Uhr fehlt (Ablauf läuft trotzdem)
// Takt nach dem Vorbild kontaktloser Kartenterminals (EMV): bestätigt 1 Ton 0,5 s, abgelehnt 2 Töne à 0,2 s mit 0,2 s
// Pause; technischer Fehler 3 Töne à 0,2 s (der aktive Summer hat einen festen Ton, unterschieden wird über den Rhythmus)
void feedback(char kind) {
  if (detail()) Serial.println(kind == 'k' ? "Licht gruen, Summer D3: 1 x 500 ms"
                             : kind == 'n' ? "Licht rot, Summer D3: 2 x 200 ms mit 200 ms Pause"
                             : kind == 'u' ? "Licht blau (Uhr fehlt), kein Ton"
                             : "Licht orange, Summer D3: 3 x 200 ms mit 200 ms Pause");
  if (kind == 'k')      { led('g'); beep(500); }
  else if (kind == 'n') { led('r'); beep(200); wait(200); beep(200); }
  else if (kind == 'u') { led('b'); }
  else                  { led('o'); beep(200); wait(200); beep(200); wait(200); beep(200); }
  ledTimed = true; ledOffAt = millis() + 2500;
}
String stamp() { return rtcOk ? rtc.now().timestamp(DateTime::TIMESTAMP_FULL) : String("--:--:--"); }
void logEvent(const char *e) { Serial.print(stamp()); Serial.print("  "); Serial.println(e); }
// Zusatzmeldung, nur mit Befehl v: Zeitstempel wie bei logEvent(), dann "[v] "; den Text schreibt der Aufrufer dahinter
bool detail() { if (!verbose) return false; Serial.print(stamp()); Serial.print("  [v] "); return true; }

// ---------------------------------------------------------------- Speicher: lesen

// Ein typisierter Zugriff, der den Schlüssel nicht als u8 findet: zeigt er einen anderen Typ (-> falscher Typ),
// einen echten Fehler oder "auch so nicht vorhanden"?
ReadResult classifyProbe(esp_err_t e, esp_err_t &err) {
  if (e == ESP_OK || e == ESP_ERR_NVS_TYPE_MISMATCH) return RD_WRONG_TYPE;
  if (e != ESP_ERR_NVS_NOT_FOUND) { err = e; return RD_ERROR; }
  return RD_NOT_FOUND;
}

// Liest ein Byte und unterscheidet: gelesen / ausdrücklich nicht vorhanden / anderer Typ / Lesefehler.
ReadResult readU8(nvs_handle_t h, const char *key, uint8_t &out, esp_err_t &err) {
  err = nvs_get_u8(h, key, &out);
  if (err == ESP_OK) return RD_OK;
  if (err == ESP_ERR_NVS_TYPE_MISMATCH) return RD_WRONG_TYPE;
  if (err != ESP_ERR_NVS_NOT_FOUND) return RD_ERROR;
  // esp-idf sucht nach Schlüssel UND Typ: prüfen, ob der Schlüssel mit einem anderen Typ existiert
  int8_t i8; uint16_t u16; int16_t i16; uint32_t u32; int32_t i32; uint64_t u64; int64_t i64; size_t len = 0;
  ReadResult r;
  if ((r = classifyProbe(nvs_get_i8(h, key, &i8), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_u16(h, key, &u16), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_i16(h, key, &i16), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_u32(h, key, &u32), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_i32(h, key, &i32), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_u64(h, key, &u64), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_i64(h, key, &i64), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_str(h, key, NULL, &len), err)) != RD_NOT_FOUND) return r;
  if ((r = classifyProbe(nvs_get_blob(h, key, NULL, &len), err)) != RD_NOT_FOUND) return r;
  err = ESP_ERR_NVS_NOT_FOUND;
  return RD_NOT_FOUND;
}

void setStore(StoreState s, esp_err_t e) { storeState = s; storeErr = e; }

// Liest den Zustand aus dem Speicher. Ergebnis steht in storeState (und loan bzw. legacyLoan).
void loadState() {
  nvs_handle_t h;
  esp_err_t e = nvs_open(NVS_NS, NVS_READONLY, &h);
  if (e == ESP_ERR_NVS_NOT_FOUND) { storeRaw = -1; setStore(ST_UNINIT, e); return; }   // Namespace nie angelegt
  if (e != ESP_OK) { setStore(ST_ERR_OPEN, e); return; }
  uint8_t v = 0; esp_err_t re = ESP_OK;
  ReadResult r = readU8(h, KEY_NEW, v, re);
  if (r == RD_OK) {
    storeRaw = v;
    if (v == LOAN_YES || v == LOAN_NO) { loan = (v == LOAN_YES); setStore(ST_OK, ESP_OK); }
    else setStore(ST_ERR_VALUE, ESP_OK);
  } else if (r == RD_WRONG_TYPE) setStore(ST_ERR_TYPE, re);
  else if (r == RD_ERROR) setStore(ST_ERR_READ, re);
  else {
    // neuer Schlüssel ausdrücklich nicht vorhanden: nur jetzt den alten Schlüssel ansehen
    r = readU8(h, KEY_OLD, v, re);
    if (r == RD_OK) {
      storeRaw = v;
      if (v == 0 || v == LOAN_NO)       { legacyLoan = false; setStore(ST_LEGACY, ESP_OK); }
      else if (v == 1 || v == LOAN_YES) { legacyLoan = true;  setStore(ST_LEGACY, ESP_OK); }
      else setStore(ST_ERR_OLD_VALUE, ESP_OK);
    } else if (r == RD_WRONG_TYPE) setStore(ST_ERR_OLD_TYPE, re);
    else if (r == RD_ERROR) setStore(ST_ERR_READ, re);
    else { storeRaw = -1; setStore(ST_UNINIT, ESP_ERR_NVS_NOT_FOUND); }
  }
  nvs_close(h);
}

// ---------------------------------------------------------------- Speicher: schreiben mit Commit und Rücklesen
bool writeVerified(const char *key, uint8_t value) {
  nvs_handle_t h;
  esp_err_t e = nvs_open(NVS_NS, NVS_READWRITE, &h);
  if (e != ESP_OK) { setStore(ST_ERR_OPEN, e); return false; }
  e = nvs_set_u8(h, key, value);
  if (e != ESP_OK) { nvs_close(h); setStore(ST_ERR_WRITE, e); return false; }
  e = nvs_commit(h);
  nvs_close(h);
  if (e != ESP_OK) { setStore(ST_ERR_COMMIT, e); return false; }
  e = nvs_open(NVS_NS, NVS_READONLY, &h);          // mit neuem, nur lesendem Handle zurücklesen
  if (e != ESP_OK) { setStore(ST_ERR_VERIFY, e); return false; }
  uint8_t back = 0;
  e = nvs_get_u8(h, key, &back);
  nvs_close(h);
  if (e != ESP_OK) { setStore(ST_ERR_VERIFY, e); return false; }
  if (back != value) { storeRaw = back; setStore(ST_ERR_VERIFY, ESP_OK); return false; }
  storeRaw = back;
  return true;
}

bool storeReady() { return storeState == ST_OK; }

// Speichert den Ausleihzustand. RAM-Zustand erst nach erfolgreichem Rücklesen ändern.
bool saveLoan(bool v) {
  if (!storeReady()) return false;
  if (!writeVerified(KEY_NEW, v ? LOAN_YES : LOAN_NO)) {
    logEvent("FEHLER: Zustand nicht sicher gespeichert -> Aktionen gesperrt");
    return false;
  }
  loan = v;
  return true;
}

// ---------------------------------------------------------------- Riegel
// Fährt den Riegel in die verlangte Stellung und wartet MOVE_MS. Der Türkontakt wird dabei etwa alle 2 ms gelesen.
// Beim Verriegeln bricht die erste HIGH-Abtastung ab: Der Servo bekommt sofort RELEASE_ANGLE (noch vor der Meldung
// "Tuer OFFEN"), dann wird der Rückweg abgewartet. Die letzte Abtastung liegt am Ende der Bewegung.
// Die [v]-Meldung kommt erst danach, damit keine Ausgabe die Abtastung während der Bewegung aufhält.
// true = Stellung wie verlangt; false = nicht bewegt (gesperrt) oder Verriegeln abgebrochen, Riegel frei.
// Eine tatsächliche Riegelstellung wird nicht gemessen.
bool moveLatch(bool lock) {
  if (latchBusy || !storeReady()) return false;   // nie verschachtelt, nie ohne gültigen Speicherzustand
  latchBusy = true;
  int angle = lock ? LOCK_ANGLE : RELEASE_ANGLE;
  bool aborted = false;
  servo.attach(PIN_SERVO, 500, 2400);
  servo.write(angle);
  unsigned long t0 = millis(), dt = 0;
  do {
    delay(2);
    dt = millis() - t0;
    int r = digitalRead(PIN_DOOR);
    if (lock && r == HIGH) { servo.write(RELEASE_ANGLE); aborted = true; }   // sofort zurück auf frei
    doorSample(r);                                 // erst danach melden ("Tuer OFFEN")
  } while (!aborted && dt < MOVE_MS);
  if (aborted) wait(MOVE_MS);                      // Rückweg abwarten, Türkontakt weiter lesen
  servo.detach();             // danach kein PWM-Signal mehr: kein Dauerdrücken, kein Brummen. Der Servo bleibt an 5 V.
  latchPos = (lock && !aborted) ? 1 : 0;
  latchBusy = false;
  if (detail()) {
    if (aborted) Serial.printf("Servo D9 -> %d Grad (verriegeln), nach %lu ms abgebrochen -> %d Grad (freigeben), %lu ms, dann Signal aus\n",
                               LOCK_ANGLE, dt, RELEASE_ANGLE, MOVE_MS);
    else Serial.printf("Servo D9 -> %d Grad (%s), %lu ms, dann Signal aus\n", angle, lock ? "verriegeln" : "freigeben", MOVE_MS);
  }
  return !aborted;
}

// ---------------------------------------------------------------- Türkontakt
// Liest den Kontakt einmal und führt den entprellten Zustand nach. Bewegt nie den Riegel.
void updateDoor() { doorSample(digitalRead(PIN_DOOR)); }

// Führt den entprellten Zustand mit einer schon gelesenen Abtastung r von D2 nach. Bewegt nie den Riegel.
void doorSample(int r) {
  unsigned long now = millis();
  if (r == HIGH) {                               // offen: gilt sofort
    doorLowValid = false;
    if (doorStable != HIGH) { doorStable = HIGH; logEvent("Tuer OFFEN (D2 = HIGH)"); }
  } else {                                       // LOW: muss 500 ms am Stück gemessen sein
    if (!doorLowValid) { doorLowValid = true; doorLowSince = now; }
    if (doorStable != LOW && now - doorLowSince >= DOOR_STABLE_MS) {
      doorStable = LOW;
      logEvent("Tuer ZU (D2 seit 500 ms LOW)");
    }
  }
}

bool doorClosedNow() { return digitalRead(PIN_DOOR) == LOW && doorStable == LOW; }

// Verriegelt nur, wenn die Tür stabil zu ist und D2 bis zum Ende der Riegelbewegung bei jeder Abtastung LOW bleibt.
// D2 wird unmittelbar vor dem Servobefehl ein letztes Mal gelesen, dazwischen gibt es keine Ausgabe. Wird während der
// Bewegung eine offene Tür gelesen, fährt der Riegel sofort zurück auf frei (sonst stünde er bei offener Tür im Weg
// des Hakens). Ein neuer Versuch kommt erst, wenn die Tür wieder 500 ms zu ist (servicePendingLock).
// true = verriegelt, Tür war bei jeder Abtastung zu. Eine tatsächliche Riegelstellung wird nicht gemessen.
bool lockIfStillClosed() {
  if (latchBusy || !storeReady()) return false;
  int r = digitalRead(PIN_DOOR);                 // letzte Abtastung vor dem Servobefehl
  doorSample(r);                                 // HIGH zählt auch für die Entprellung (500 ms neu)
  if (r != LOW || doorStable != LOW) return false;
  if (moveLatch(true)) return true;
  logEvent("Tuer waehrend des Verriegelns geoeffnet -> Riegel wieder frei");
  return false;
}

// Entscheidet aus dem aktuellen Zustand, ob jetzt verriegelt wird (nicht aus einem Flankenereignis).
// pendingLock bleibt gesetzt, bis wirklich bei geschlossener Tür verriegelt wurde.
void servicePendingLock() {
  if (!pendingLock || !storeReady() || loan || latchBusy) return;
  updateDoor();
  if (!doorClosedNow()) return;
  logEvent("Keine Ausleihe, Tuer stabil zu -> verriegeln");
  if (!lockIfStillClosed()) return;              // Tür ging auf: später erneut, sobald sie stabil zu ist
  pendingLock = false;
  logEvent("Riegel verriegelt");
  if (confirmOnLock) { confirmOnLock = false; logEvent("Rueckgabe abgeschlossen"); feedback('k'); }
}

// ---------------------------------------------------------------- Betriebsstart (nach Neustart, INIT EMPTY oder MIGRATE CONFIRM)
void startOperation() {
  pendingLock = !loan;                           // Verriegelungsbedarf aus dem gespeicherten Zustand
  if (loan) {
    moveLatch(false);
    logEvent("Ausleihe aktiv -> Riegel frei");
    return;
  }
  wait(DOOR_STABLE_MS + 10);                     // Tür beobachten
  if (doorClosedNow()) { servicePendingLock(); return; }
  moveLatch(false);                              // Tür offen: frei, damit sie sich schliessen lässt
  logEvent("Keine Ausleihe, Tuer offen -> verriegelt, sobald sie 500 ms zu ist");
  servicePendingLock();                          // falls die Tür während der Bewegung schon stabil zu ist
}

// ---------------------------------------------------------------- Aktionen
void checkIn() {
  if (!storeReady()) { logEvent("Speicher nicht betriebsbereit -> gesperrt"); printStoreState(); feedback('e'); return; }
  if (loan)          { logEvent("CHECK_IN abgelehnt: Geraet ist bereits ausgeliehen"); feedback('n'); return; }
  if (!saveLoan(true)) { printStoreState(); feedback('e'); return; }   // zuerst speichern und zurücklesen ...
  logEvent("CHECK_IN gespeichert und zurueckgelesen");
  pendingLock = false; confirmOnLock = false;
  moveLatch(false);                                                    // ... dann freigeben
  logEvent("Riegel frei - Tuer kann geoeffnet werden");
  feedback('k');
}

void checkOut() {
  if (!storeReady())    { logEvent("Speicher nicht betriebsbereit -> gesperrt"); printStoreState(); feedback('e'); return; }
  if (!loan)            { logEvent("CHECK_OUT abgelehnt: keine Ausleihe aktiv"); feedback('n'); return; }
  updateDoor();
  if (!doorClosedNow()) { logEvent("CHECK_OUT abgelehnt: Tuer ist nicht (stabil) zu"); feedback('n'); return; }
  if (!saveLoan(false)) { printStoreState(); feedback('e'); return; }
  logEvent("CHECK_OUT gespeichert und zurueckgelesen");
  pendingLock = true;                            // ab jetzt gilt "keine Ausleihe": verriegeln, sobald die Tür stabil zu ist
  if (lockIfStillClosed()) {                     // Tür nach dem Speichern und nach der Bewegung erneut geprüft
    pendingLock = false;
    logEvent("Riegel verriegelt - Rueckgabe abgeschlossen");
    feedback('k');
    return;
  }
  confirmOnLock = true;
  logEvent("Rueckgabe gespeichert, aber Tuer offen -> NICHT verriegelt. Tuer schliessen: verriegelt, sobald sie 500 ms zu ist");
  feedback('n');
}

void initEmpty() {
  if (storeState != ST_UNINIT) { logEvent("INIT EMPTY nicht moeglich: nur im Zustand NICHT INITIALISIERT"); feedback('n'); return; }
  loadState();                                   // unmittelbar vorher erneut lesen: beide Schlüssel müssen fehlen
  if (storeState != ST_UNINIT) { logEvent("INIT EMPTY abgebrochen: Speicherzustand hat sich geaendert"); printStoreState(); feedback('e'); return; }
  if (!writeVerified(KEY_NEW, LOAN_NO)) { logEvent("INIT EMPTY fehlgeschlagen -> gesperrt"); printStoreState(); feedback('e'); return; }
  loan = false; setStore(ST_OK, ESP_OK);
  logEvent("Erstinbetriebnahme: Zustand 'keine Ausleihe' angelegt und zurueckgelesen");
  feedback('k');
  startOperation();
}

void migrateConfirm() {
  if (storeState != ST_LEGACY) { logEvent("MIGRATE CONFIRM nicht moeglich: nur im Zustand ALTZUSTAND GEFUNDEN"); feedback('n'); return; }
  bool want = legacyLoan; int raw = storeRaw;
  loadState();                                   // erneut lesen: Altzustand muss unverändert sein
  if (storeState != ST_LEGACY || legacyLoan != want || storeRaw != raw) {
    logEvent("MIGRATE CONFIRM abgebrochen: Speicherzustand hat sich geaendert"); printStoreState(); feedback('e'); return;
  }
  if (!writeVerified(KEY_NEW, want ? LOAN_YES : LOAN_NO)) { logEvent("MIGRATE CONFIRM fehlgeschlagen -> gesperrt"); printStoreState(); feedback('e'); return; }
  loan = want; setStore(ST_OK, ESP_OK);          // alter Schlüssel bleibt unverändert
  logEvent(want ? "Altzustand uebernommen: Ausleihe aktiv" : "Altzustand uebernommen: keine Ausleihe");
  feedback('k');
  startOperation();
}

void printStoreState() {
  Serial.print("Speicher:  "); Serial.println(storeStateName(storeState));
  if (storeErr != ESP_OK) Serial.printf("           Fehlercode 0x%X (%s)\n", (unsigned)storeErr, esp_err_to_name(storeErr));
  if (storeRaw >= 0) Serial.printf("           gelesenes Byte 0x%02X\n", (unsigned)storeRaw);
  if (storeState == ST_LEGACY)
    Serial.printf("           Altzustand (Schluessel loan): %s -> zum Uebernehmen genau 'MIGRATE CONFIRM' senden\n",
                  legacyLoan ? "AUSLEIHE AKTIV" : "KEINE AUSLEIHE");
  if (storeState == ST_UNINIT)
    Serial.println("           Kein gespeicherter Zustand. Nur bei einem neuen Geraet: genau 'INIT EMPTY' senden");
}

void status() {
  Serial.println("----- Status -----");
  if (storeReady()) { Serial.print("Ausleihe:  "); Serial.println(loan ? "aktiv" : "keine"); }
  else Serial.println("Ausleihe:  unbekannt (Speicher nicht betriebsbereit)");
  Serial.print("Riegel:    "); Serial.println(latchPos == 1 ? "verriegelt" : (latchPos == 0 ? "frei" : "unbekannt (seit Start nicht bewegt)"));
  Serial.print("Tuer:      "); Serial.println(doorClosedNow() ? "zu (D2 seit 500 ms LOW)" : "offen oder noch nicht stabil zu");
  Serial.print("Verriegeln sobald zu: "); Serial.println(pendingLock ? "ja" : "nein");
  Serial.print("Uhr:       "); Serial.println(rtcOk ? stamp() : String("nicht gefunden"));
  Serial.print("Ausfuehrlich: "); Serial.println(verbose ? "ein" : "aus");
  printStoreState();
}

void help() {
  Serial.println("Befehle (jeweils eine ganze Zeile): a = Ausleihen, z = Zurueckgeben, s = Status, v = ausfuehrlich ein/aus, h = Hilfe");
  Serial.println("Service: INIT EMPTY (nur NICHT INITIALISIERT), MIGRATE CONFIRM (nur ALTZUSTAND GEFUNDEN)");
}

// ---------------------------------------------------------------- serielle Befehle, zeilenweise
char lineBuf[24];
uint8_t lineLen = 0;
bool lineOverflow = false;
bool lineInvalid = false;              // Zeile enthält ein unzulässiges Zeichen -> ganze Zeile verwerfen

void handleCommand(char *cmd) {
  while (*cmd == ' ') cmd++;                                   // Leerzeichen am Rand ignorieren
  int n = strlen(cmd); while (n > 0 && cmd[n - 1] == ' ') cmd[--n] = 0;
  if (n == 0) return;
  if (!strcmp(cmd, "a")) checkIn();
  else if (!strcmp(cmd, "z")) checkOut();
  else if (!strcmp(cmd, "s")) status();
  else if (!strcmp(cmd, "h") || !strcmp(cmd, "?")) help();
  else if (!strcmp(cmd, "v")) {
    verbose = !verbose;
    logEvent(verbose ? "Ausfuehrlich ein: Servo, Licht und Summer werden gemeldet (v = aus)" : "Ausfuehrlich aus (v = ein)");
  }
  else if (!strcmp(cmd, "INIT EMPTY")) initEmpty();
  else if (!strcmp(cmd, "MIGRATE CONFIRM")) migrateConfirm();
  else { Serial.print("Unbekannter Befehl (nichts ausgefuehrt): "); Serial.println(cmd); }
}

void serviceSerial() {
  while (Serial.available() > 0) {
    int c = Serial.read();
    if (c < 0) break;
    if (c == '\r' || c == '\n') {
      if (lineOverflow) logEvent("Eingabe zu lang -> verworfen");
      else if (lineInvalid) logEvent("Eingabe mit unzulaessigem Zeichen -> ganze Zeile verworfen");
      else if (lineLen > 0) { lineBuf[lineLen] = 0; handleCommand(lineBuf); }
      lineLen = 0; lineOverflow = false; lineInvalid = false;
    } else if (c < 0x20 || c > 0x7E) lineInvalid = true;   // nur druckbares ASCII; NUL würde die Zeile sonst abschneiden
    else if (lineLen < sizeof(lineBuf) - 1) lineBuf[lineLen++] = (char)c;
    else lineOverflow = true;
  }
}

// ---------------------------------------------------------------- Start
void setup() {
  Serial.begin(115200);
  pinMode(PIN_DOOR, INPUT_PULLUP);
  pinMode(PIN_BUZZER, OUTPUT); digitalWrite(PIN_BUZZER, LOW);
  pinMode(LED_RED, OUTPUT); pinMode(LED_GREEN, OUTPUT); pinMode(LED_BLUE, OUTPUT);
  led('x');
  ESP32PWM::allocateTimer(0);
  servo.setPeriodHertz(50);
  delay(1500);
  Serial.println("T6 Ausleihbox - Ablauf nach T-015 (V14)");

  Wire.begin();
  rtcOk = rtc.begin();
  if (!rtcOk) { Serial.println("Uhr DS3231 nicht gefunden (nur Zeitstempel fehlen, Ablauf laeuft)"); feedback('u'); }

  loadState();                                   // nur lesen
  printStoreState();
  if (storeReady()) startOperation();
  else { Serial.println("Keine Aktionen und keine Riegelbewegung, bis der Speicherzustand geklaert ist."); feedback('e'); }
  help();
}

void loop() {
  updateDoor();
  serviceSerial();
  servicePendingLock();
  if (ledTimed && (long)(millis() - ledOffAt) >= 0) { led('x'); ledTimed = false; if (detail()) Serial.println("Licht aus"); }
  delay(5);
}
