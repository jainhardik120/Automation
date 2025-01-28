#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <Keypad.h>
#include <Wire.h>

#define KEYPAD_SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define KEYPAD_KEY_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define KEYPAD_PROFILE_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a9"

#define LED_SERVICE_UUID "4fafc202-1fb5-459e-8fcc-c5c9c331914b"
#define LED_CHARACTERISTIC_UUID "beb5483f-36e1-4688-b7f5-ea07361b26a8"

#define SLAVE_ADDRESS 0x08

BLECharacteristic *pKeypadKeyCharacteristic;
BLECharacteristic *pKeypadProfileCharacteristic;
BLECharacteristic *pLedCharacteristic;

const int ledPins[4] = { 15, 2, 16, 17 };

const int connectionLedPin = 23;
const int incomingDataLedPin = 19;
const int outgoingDataLedPin = 18;

class LedControlCallback : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String value = pCharacteristic->getValue();
    if (value.length() > 0) {
      uint8_t ledState = value[0];
      for (int i = 0; i < 4; i++) {
        digitalWrite(ledPins[i], (ledState >> i) & 0x01);
      }
    }
  }
};

class KeypadControlCallback : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String value = pCharacteristic->getValue();
    if (value.length() > 0) {
      digitalWrite(incomingDataLedPin, HIGH);
      Wire.beginTransmission(SLAVE_ADDRESS);
      Wire.write((uint8_t *)value.c_str(), value.length());
      Wire.endTransmission();
      digitalWrite(incomingDataLedPin, LOW);
    }
  }
};

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *pServer) {
    pServer->startAdvertising();  // restart advertising
    digitalWrite(connectionLedPin, HIGH);
  };

  void onDisconnect(BLEServer *pServer) {
    pServer->startAdvertising();  // restart advertising
    digitalWrite(connectionLedPin, LOW);
  }
};

void setup() {
  Wire.begin(21, 22);
  Serial.begin(115200);
  Serial.println("Starting BLE work!");

  for (int i = 0; i < 4; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW);
  }

  pinMode(connectionLedPin, OUTPUT);
  digitalWrite(connectionLedPin, LOW);
  pinMode(incomingDataLedPin, OUTPUT);
  digitalWrite(incomingDataLedPin, LOW);
  pinMode(outgoingDataLedPin, OUTPUT);
  digitalWrite(outgoingDataLedPin, LOW);

  BLEDevice::init("ESP32 Keypad & LED Control");
  BLEServer *pServer = BLEDevice::createServer();

  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pKeypadService = pServer->createService(KEYPAD_SERVICE_UUID);
  pKeypadKeyCharacteristic = pKeypadService->createCharacteristic(
    KEYPAD_KEY_CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY);
  pKeypadKeyCharacteristic->setCallbacks(new KeypadControlCallback());
  pKeypadKeyCharacteristic->setValue("Waiting for keypress...");

  pKeypadProfileCharacteristic = pKeypadService->createCharacteristic(
    KEYPAD_PROFILE_CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY);
  pKeypadProfileCharacteristic->setCallbacks(new KeypadControlCallback());
  pKeypadProfileCharacteristic->setValue("Waiting for keypress...");
  pKeypadService->start();

  BLEService *pLedService = pServer->createService(LED_SERVICE_UUID);
  pLedCharacteristic = pLedService->createCharacteristic(
    LED_CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_WRITE);
  pLedCharacteristic->setCallbacks(new LedControlCallback());
  pLedService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(KEYPAD_SERVICE_UUID);
  pAdvertising->addServiceUUID(LED_SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
}

void loop() {
  Wire.requestFrom(SLAVE_ADDRESS, 1);
  int len = Wire.available();
  if (len > 0) {
    uint8_t *value = new uint8_t[len];
    for (int i = 0; i < len; i++) {
      value[i] = Wire.read();
      if (value[0] != 0xff) {
        digitalWrite(outgoingDataLedPin, HIGH);
        pKeypadKeyCharacteristic->setValue(value, len);
        pKeypadKeyCharacteristic->notify();
        digitalWrite(outgoingDataLedPin, LOW);
      }
    }
    delete[] value;
  }
  delay(30);
}
