// T7 – BLE-Advertising der Ausleihbox (Demo nach "BLE Demo Protocol", EN-03.05, Abschnitt 2)
// Nur Advertising: Der Nano meldet sich mit der gemeinsamen Service-UUID und dem Namen "Lending Station 01".
// Noch kein GATT (Terminal-ID, OPEN/CLOSE): Ein Handy kann sich verbinden, findet aber keinen Booking Service.
// Prüfen mit einer BLE-App (z. B. nRF Connect): Name, Service-UUID und die rohen Bytes mit dem Protokoll vergleichen.
// Bibliothek: BLE aus dem Paket "Arduino ESP32 Boards" (nichts zu installieren). Seriellen Monitor auf 115200 Baud.
// Servo, Türkontakt, Summer, LED und Uhr werden hier nicht benutzt.
//
// Advertising-Paket (21 Byte):  02 01 06 | 11 07 + Service-UUID (16 Byte, niederwertigstes Byte zuerst)
// Scan Response (20 Byte):      13 09 + "Lending Station 01"
// Verbindbar und scanbar (legacy, ADV_IND), alle 100 ms.
// Sendeleistung fest -24 dBm, die kleinste Stufe des ESP32-S3.
// Sie verkleinert die Reichweite, macht aber keine scharfe Grenze: Den Abstand (etwa 1 m) regelt die App über die
// gemessene Signalstärke (RSSI). Die Pakete selbst bleiben unverändert (kein Feld für die Sendeleistung).
// Im Seriellen Monitor: eine beliebige Zeile senden (z. B. nur Enter) -> die Prüfung der zwei Pakete kommt nochmals.

#include <BLEDevice.h>
#include <BLEServer.h>
#include <esp_bt.h>

const char* LOCAL_NAME   = "Lending Station 01";
const char* SERVICE_UUID = "fd698d10-c5c7-429b-9fb3-18a41dd73998";   // Booking Service, auf allen Terminals gleich
const uint16_t ADV_INTERVAL = 160;                                   // 160 × 0,625 ms = 100 ms
const esp_power_level_t TX_POWER = ESP_PWR_LVL_N24;                  // -24 dBm

// So muss das Advertising-Paket laut Protokoll aussehen (Tabelle "Advertising packet (21 bytes)")
const uint8_t ADV_SOLL[21] = {
  0x02, 0x01, 0x06,
  0x11, 0x07,
  0x98, 0x39, 0xD7, 0x1D, 0xA4, 0x18, 0xB3, 0x9F, 0x9B, 0x42, 0xC7, 0xC5, 0x10, 0x8D, 0x69, 0xFD
};

bool printHex(const char* title, const std::string& p, const uint8_t* soll, size_t sollLen) {
  Serial.print(title); Serial.print(" ("); Serial.print(p.size()); Serial.print(" Byte): ");
  for (size_t i = 0; i < p.size(); i++) {
    uint8_t b = (uint8_t)p[i];
    if (b < 16) Serial.print("0");
    Serial.print(b, HEX); Serial.print(" ");
  }
  Serial.println();
  if (soll == nullptr) return true;
  bool ok = p.size() == sollLen && memcmp(p.data(), soll, sollLen) == 0;
  Serial.println(ok ? "  -> stimmt mit dem Protokoll ueberein" : "  -> FEHLER: weicht vom Protokoll ab");
  return ok;
}

std::string advPayload, srPayload, srSoll;   // die zwei Pakete, wie sie gesendet werden, und die erwartete Scan Response
bool advertising = false;

bool printCheck() {
  bool ok = printHex("Advertising", advPayload, ADV_SOLL, sizeof(ADV_SOLL));
  ok &= printHex("Scan Response", srPayload, (const uint8_t*)srSoll.data(), srSoll.size());
  bool txOk = esp_ble_tx_power_get(ESP_BLE_PWR_TYPE_ADV) == TX_POWER;
  Serial.println(txOk ? "Sendeleistung: -24 dBm" : "FEHLER: Sendeleistung ist nicht -24 dBm");
  ok &= txOk;
  if (advertising) { Serial.print("Advertising laeuft: \""); Serial.print(LOCAL_NAME); Serial.println("\", alle 100 ms"); }
  else Serial.println("Advertising NICHT gestartet.");
  return ok;
}

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* s) override {
    Serial.println("Handy verbunden (in T7 noch kein Booking Service)");
  }
  void onDisconnect(BLEServer* s) override {
    Serial.println("Handy getrennt -> Advertising wieder an");
    BLEDevice::startAdvertising();          // nach einer Verbindung hört das Advertising sonst auf
  }
};

void setup() {
  Serial.begin(115200);
  delay(1500);
  Serial.println("T7 BLE-Advertising");

  BLEDevice::init(LOCAL_NAME);              // auch der Gerätename, den ein Handy nach dem Verbinden liest
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, TX_POWER);   // Sendeleistung für alles (auch eine spätere Verbindung)
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, TX_POWER);       // ... und ausdrücklich fürs Advertising
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, TX_POWER);
  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEAdvertisementData advData;             // Advertising-Paket: nur Flags und Service-UUID (kein Name, keine Sendeleistung)
  advData.setFlags(0x06);                   // allgemein auffindbar, nur BLE (kein klassisches Bluetooth)
  advData.setCompleteServices(BLEUUID(SERVICE_UUID));

  BLEAdvertisementData scanData;            // Scan Response: nur der Name
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
  if (ok) { BLEDevice::startAdvertising(); advertising = true; }     // nur senden, wenn Pakete und Sendeleistung stimmen
  printCheck();
}

void loop() {
  if (Serial.available()) {                 // eine Zeile im Monitor gesendet: Prüfung nochmals zeigen
    delay(20);
    while (Serial.available()) Serial.read();
    printCheck();
  }
  delay(50);
}
