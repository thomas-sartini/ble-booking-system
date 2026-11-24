// T8 – BLE-Demo der Ausleihbox: Advertising, Verbindung, Terminal-ID, OPEN und CLOSE (nach "BLE Demo Protocol", EN-03.05)
// SIMULATION wie terminal/simulator/terminal_simulator.py aus PR #144: Riegel, Türkontakt, Summer, LED und Speicher werden
// NICHT benutzt. OPEN und CLOSE melden nur einen gespielten Zustand ("Riegel frei/verriegelt", "Tür zu/offen").
// Bibliothek: BLE aus dem Paket "Arduino ESP32 Boards" (nichts zu installieren). Seriellen Monitor auf 115200 Baud, "Neue Zeile".
//
// Advertising wie T7: 02 01 06 | 11 07 + Service-UUID; Scan Response 13 09 + "Lending Station 01"; alle 100 ms; -24 dBm.
// GATT (Abschnitt 3 und 4 des Protokolls):
//   Booking Service       fd698d10-c5c7-429b-9fb3-18a41dd73998
//   Terminal Information  81f905bd-4d25-4eeb-bf1d-89e909368fae  READ          01 + Terminal-ID (16 Byte, höchstwertiges zuerst)
//   Door Command          08d0c22b-0be0-45c1-aea4-7d181b40f733  WRITE         [Version 01][Request-ID 2 Byte LE][Befehl 01/02]
//   Door Status           3b33ab4f-826c-497b-a653-b16c46425d73  READ, NOTIFY  [01][Request-ID][Befehl][Ergebnis][Riegel][Tür]
// Terminal-ID: 550e8400-e29b-41d4-a716-446655440000 (Beispiel-ID aus dem Protokoll, wie im Simulator)
//
// Ablauf eines Befehls wie im Simulator: sofort NOTIFY mit Ergebnis 00 (läuft), nach der Wartezeit (0,5 s) das Ergebnis.
// Szenarien wie im Simulator, im Seriellen Monitor als Zeile senden (gilt sofort, Zustand wie nach einem Neustart):
//   success (Start)  OPEN gibt den Riegel frei, CLOSE verriegelt; die Tür bleibt zu
//   door-open        die Tür meldet offen, CLOSE gibt Ergebnis 03
//   denied           Ergebnis 02        busy  Ergebnis 04        actuator-error  Ergebnis 05
//   timeout          nur 00, kein Ergebnis        disconnect  trennt die Verbindung vor dem Ergebnis
//   delay 2          Wartezeit in Sekunden (Start 0,5)
//   leere Zeile oder ?   Zustand und Prüfung der Pakete anzeigen
//
// Abweichung vom Simulator: Die BLE-Bibliothek des ESP32 quittiert jedes Schreiben auf Door Command mit "OK", sie kann keinen
// ATT-Fehler zurückgeben. Ein Befehl mit falscher Länge oder Version, oder einer, der kommt, während noch einer läuft, wird
// darum zwar quittiert, aber nicht ausgeführt und nicht beantwortet (im Monitor steht "verworfen"). Eure App sendet so etwas nie.

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <esp_bt.h>

const char* LOCAL_NAME   = "Lending Station 01";
const char* SERVICE_UUID = "fd698d10-c5c7-429b-9fb3-18a41dd73998";
const char* INFO_UUID    = "81f905bd-4d25-4eeb-bf1d-89e909368fae";
const char* COMMAND_UUID = "08d0c22b-0be0-45c1-aea4-7d181b40f733";
const char* STATUS_UUID  = "3b33ab4f-826c-497b-a653-b16c46425d73";
const uint16_t ADV_INTERVAL = 160;                       // 160 × 0,625 ms = 100 ms
const esp_power_level_t TX_POWER = ESP_PWR_LVL_N24;      // -24 dBm

// Terminal Information: Version 01 + 550e8400-e29b-41d4-a716-446655440000 in Netzwerk-Reihenfolge (Protokoll 4.1)
const uint8_t TERMINAL_INFO[17] = {
  0x01, 0x55, 0x0E, 0x84, 0x00, 0xE2, 0x9B, 0x41, 0xD4, 0xA7, 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
};
// Advertising-Paket laut Protokoll (Tabelle "Advertising packet (21 bytes)")
const uint8_t ADV_SOLL[21] = {
  0x02, 0x01, 0x06,
  0x11, 0x07,
  0x98, 0x39, 0xD7, 0x1D, 0xA4, 0x18, 0xB3, 0x9F, 0x9B, 0x42, 0xC7, 0xC5, 0x10, 0x8D, 0x69, 0xFD
};

enum Scenario { SC_SUCCESS, SC_DOOR_OPEN, SC_DENIED, SC_BUSY, SC_ACTUATOR_ERROR, SC_TIMEOUT, SC_DISCONNECT, N_SCENARIOS };
const char* SCENARIO_NAMES[N_SCENARIOS] = { "success", "door-open", "denied", "busy", "actuator-error", "timeout", "disconnect" };
Scenario scenario = SC_SUCCESS;
unsigned long delayMs = 500;

BLEServer* server = nullptr;
BLECharacteristic* statusChar = nullptr;
std::string advPayload, srPayload, srSoll;
bool advertising = false;

// gespielter Zustand wie im Simulator
bool locked = true, doorOpen = false;
uint8_t lastStatus[7] = { 1, 0, 0, 1, 0, 1, 1 };

// Übergabe vom Bluetooth-Task (Schreiben) an loop(): 0 = frei, 1 = neuer Befehl, 2 = wartet auf das Ergebnis
portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;
volatile uint8_t taskState = 0;
uint8_t taskReq[4];
unsigned long taskDue = 0;
volatile bool connected = false, disconnectedFlag = false;
volatile int droppedLen = -1;                            // zuletzt verworfener Befehl (Länge), -1 = keiner
uint8_t droppedReq[20];

void printBytes(const uint8_t* p, size_t n) {
  for (size_t i = 0; i < n; i++) { if (p[i] < 16) Serial.print("0"); Serial.print(p[i], HEX); Serial.print(" "); }
}

bool checkPacket(const char* title, const std::string& p, const uint8_t* soll, size_t sollLen) {
  Serial.print(title); Serial.print(" ("); Serial.print(p.size()); Serial.print(" Byte): ");
  printBytes((const uint8_t*)p.data(), p.size());
  Serial.println();
  bool ok = p.size() == sollLen && memcmp(p.data(), soll, sollLen) == 0;
  Serial.println(ok ? "  -> stimmt mit dem Protokoll ueberein" : "  -> FEHLER: weicht vom Protokoll ab");
  return ok;
}

void printState() {
  checkPacket("Advertising", advPayload, ADV_SOLL, sizeof(ADV_SOLL));
  checkPacket("Scan Response", srPayload, (const uint8_t*)srSoll.data(), srSoll.size());
  Serial.println(esp_ble_tx_power_get(ESP_BLE_PWR_TYPE_ADV) == TX_POWER ? "Sendeleistung: -24 dBm" : "FEHLER: Sendeleistung ist nicht -24 dBm");
  Serial.print("Terminal Information: "); printBytes(TERMINAL_INFO, sizeof(TERMINAL_INFO)); Serial.println();
  Serial.print("Szenario: "); Serial.print(SCENARIO_NAMES[scenario]);
  Serial.print(" | Wartezeit "); Serial.print(delayMs / 1000.0, 1); Serial.print(" s");
  Serial.print(" | gespielt: Riegel "); Serial.print(locked ? "verriegelt" : "frei");
  Serial.print(", Tuer "); Serial.println(doorOpen ? "offen" : "zu");
  Serial.println(connected ? "Handy verbunden" : (advertising ? "Advertising laeuft, nicht verbunden" : "Advertising NICHT gestartet"));
}

void setScenario(Scenario s) {                         // wie ein Neustart des Simulators mit --scenario
  portENTER_CRITICAL(&mux); taskState = 0; portEXIT_CRITICAL(&mux);
  scenario = s;
  locked = scenario != SC_DOOR_OPEN;
  doorOpen = scenario == SC_DOOR_OPEN;
  const uint8_t init[7] = { 1, 0, 0, 1, 0, 1, 1 };
  memcpy(lastStatus, init, 7);
  statusChar->setValue(lastStatus, 7);
  Serial.print("Szenario: "); Serial.println(SCENARIO_NAMES[scenario]);
}

void notifyStatus(const uint8_t* req, uint8_t result) {
  lastStatus[0] = 1; lastStatus[1] = req[1]; lastStatus[2] = req[2]; lastStatus[3] = req[3];
  lastStatus[4] = result; lastStatus[5] = locked ? 1 : 2; lastStatus[6] = doorOpen ? 2 : 1;
  statusChar->setValue(lastStatus, 7);
  statusChar->notify();
  Serial.print("NOTIFY "); printBytes(lastStatus, 7); Serial.println();
}

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* s) override { connected = true; Serial.println("VERBUNDEN"); }
  void onDisconnect(BLEServer* s) override {
    portENTER_CRITICAL(&mux); taskState = 0; portEXIT_CRITICAL(&mux);   // laufender Befehl verfällt wie im Simulator
    connected = false; disconnectedFlag = true;
    Serial.println("GETRENNT");
  }
};

class InfoCallbacks : public BLECharacteristicCallbacks {
  void onRead(BLECharacteristic* c) override {
    Serial.print("READ ID "); printBytes(TERMINAL_INFO, sizeof(TERMINAL_INFO)); Serial.println();
  }
};

class CommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* c) override {
    std::string v = c->getValue();
    portENTER_CRITICAL(&mux);
    if (v.size() == 4 && (uint8_t)v[0] == 1 && taskState == 0) {   // gültig und kein Befehl in Arbeit: übernehmen
      memcpy(taskReq, v.data(), 4);
      taskState = 1;
    } else if (droppedLen < 0) {                                    // sonst nur für die Meldung im Monitor merken
      droppedLen = v.size() > sizeof(droppedReq) ? sizeof(droppedReq) : v.size();
      memcpy(droppedReq, v.data(), droppedLen);
    }
    portEXIT_CRITICAL(&mux);
  }
};

void processCommand() {
  uint8_t state;
  portENTER_CRITICAL(&mux); state = taskState; portEXIT_CRITICAL(&mux);
  if (state == 1) {
    Serial.print("WRITE "); printBytes(taskReq, 4); Serial.println();
    uint8_t command = taskReq[3];
    if (command != 1 && command != 2) {                // unbekannter Befehl: sofort Ergebnis 06
      notifyStatus(taskReq, 6);
      portENTER_CRITICAL(&mux); taskState = 0; portEXIT_CRITICAL(&mux);
      return;
    }
    notifyStatus(taskReq, 0);                          // läuft
    taskDue = millis() + delayMs;
    portENTER_CRITICAL(&mux); if (taskState == 1) taskState = 2; portEXIT_CRITICAL(&mux);
    return;
  }
  if (state == 2 && (long)(millis() - taskDue) >= 0) {
    uint8_t req[4];
    memcpy(req, taskReq, 4);                           // bleibt gleich, solange taskState nicht 0 ist
    if (connected) {
      if (scenario == SC_TIMEOUT) Serial.println("Kein Ergebnis (Szenario timeout)");
      else if (scenario == SC_DISCONNECT) { Serial.println("Trenne vor dem Ergebnis (Szenario disconnect)"); server->disconnect(server->getConnId()); }
      else {
        uint8_t command = req[3];
        uint8_t result = scenario == SC_DENIED ? 2 : scenario == SC_BUSY ? 4 : scenario == SC_ACTUATOR_ERROR ? 5 : 1;
        if (result == 1 && command == 2 && doorOpen) result = 3;
        if (result == 1) locked = command == 2;
        notifyStatus(req, result);
      }
    }
    portENTER_CRITICAL(&mux); taskState = 0; portEXIT_CRITICAL(&mux);   // erst jetzt ist der nächste Befehl möglich
  }
}

void handleSerialLine(String line) {
  line.trim();
  if (line.length() == 0 || line == "?") { printState(); return; }
  for (int i = 0; i < N_SCENARIOS; i++) {
    if (line == SCENARIO_NAMES[i]) { setScenario((Scenario)i); return; }
  }
  if (line.startsWith("delay ")) {
    String a = line.substring(6); a.trim(); a.replace(',', '.');
    bool number = a.length() > 0 && a.length() <= 6;
    for (unsigned int i = 0; i < a.length(); i++) if (!isDigit(a[i]) && a[i] != '.') number = false;
    if (number) { delayMs = (unsigned long)(a.toFloat() * 1000 + 0.5); Serial.print("Wartezeit: "); Serial.print(delayMs / 1000.0, 1); Serial.println(" s"); }
    else Serial.println("Wartezeit als Zahl in Sekunden, z. B. delay 2");
    return;
  }
  Serial.println("Unbekannt. Moeglich: success, door-open, denied, busy, actuator-error, timeout, disconnect, delay <s>, ?");
}

void setup() {
  Serial.begin(115200);
  delay(1500);
  Serial.println("T8 BLE-Demo (SIMULATION, Riegel wird nicht bewegt)");

  BLEDevice::init(LOCAL_NAME);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, TX_POWER);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, TX_POWER);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, TX_POWER);

  server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());
  BLEService* service = server->createService(SERVICE_UUID);

  BLECharacteristic* infoChar = service->createCharacteristic(INFO_UUID, BLECharacteristic::PROPERTY_READ);
  infoChar->setValue((uint8_t*)TERMINAL_INFO, sizeof(TERMINAL_INFO));
  infoChar->setCallbacks(new InfoCallbacks());

  BLECharacteristic* commandChar = service->createCharacteristic(COMMAND_UUID, BLECharacteristic::PROPERTY_WRITE);
  commandChar->setCallbacks(new CommandCallbacks());

  statusChar = service->createCharacteristic(STATUS_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  statusChar->addDescriptor(new BLE2902());            // CCCD 0x2902: damit die App die Meldungen abonnieren kann
  statusChar->setValue(lastStatus, 7);
  service->start();

  BLEAdvertisementData advData;                        // wie T7: nur Flags und Service-UUID
  advData.setFlags(0x06);
  advData.setCompleteServices(BLEUUID(SERVICE_UUID));
  BLEAdvertisementData scanData;                       // wie T7: nur der Name
  scanData.setName(LOCAL_NAME);
  BLEAdvertising* adv = BLEDevice::getAdvertising();
  adv->setAdvertisementData(advData);
  adv->setScanResponseData(scanData);
  adv->setMinInterval(ADV_INTERVAL);
  adv->setMaxInterval(ADV_INTERVAL);

  advPayload = advData.getPayload();
  srPayload  = scanData.getPayload();
  srSoll = std::string(1, (char)(strlen(LOCAL_NAME) + 1)) + std::string(1, (char)0x09) + LOCAL_NAME;
  bool ok = advPayload.size() == sizeof(ADV_SOLL) && memcmp(advPayload.data(), ADV_SOLL, sizeof(ADV_SOLL)) == 0 && srPayload == srSoll
            && esp_ble_tx_power_get(ESP_BLE_PWR_TYPE_ADV) == TX_POWER;
  if (ok) { BLEDevice::startAdvertising(); advertising = true; }
  printState();
}

void loop() {
  processCommand();

  if (droppedLen >= 0) {
    uint8_t buf[20]; int n;
    portENTER_CRITICAL(&mux); n = droppedLen; memcpy(buf, droppedReq, n); droppedLen = -1; portEXIT_CRITICAL(&mux);
    Serial.print("WRITE verworfen (falsche Laenge/Version oder Befehl laeuft noch): "); printBytes(buf, n); Serial.println();
  }

  if (disconnectedFlag) {                              // nach dem Trennen kurz warten, dann wieder senden
    disconnectedFlag = false;
    delay(500);
    if (advertising) { BLEDevice::startAdvertising(); Serial.println("Advertising wieder an"); }
  }

  static String line;
  while (Serial.available()) {
    char ch = Serial.read();
    if (ch == '\n') { handleSerialLine(line); line = ""; }
    else if (ch != '\r' && line.length() < 40) line += ch;
  }
  delay(5);
}
