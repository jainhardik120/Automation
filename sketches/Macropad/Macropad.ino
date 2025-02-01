#include <Keyboard.h>
#include <Mouse.h>
#include <math.h>
#include <Wire.h>

#define NO_OF_KEYS 40
#define NO_OF_STRINGS 64
#define MAX_PROGRAM_SIZE 1024
#define SLAVE_ADDRESS 0x08

#define enc1A 5
#define enc1B 4
#define enc2A 0
#define enc2B 1

#define joy1S A5
#define joy2S A4
#define joy1X A3
#define joy1Y A2
#define joy2X A1
#define joy2Y A0

uint8_t rowPins[4] = { 9, 8, 7, 6 };
uint8_t colPins[4] = { 13, 12, 11, 10 };

uint8_t iData[MAX_PROGRAM_SIZE];
uint16_t offsets[NO_OF_KEYS];
uint16_t stringOffsets[NO_OF_STRINGS];

uint64_t currKeyStates;
uint64_t prevKeyStates;
uint8_t lastKey = 0xff;

uint8_t tempEncState;
uint16_t center[4];
uint8_t inversion = 0b1001;

uint8_t *readAction(uint8_t *temp) {
  if ((*temp & 0xc0) == 0x00) {
    temp++;
  } else if ((*temp & 0xc0) == 0x40) {
    uint8_t no_of_actions = (*temp++ & 0x3f);
    while (no_of_actions--) {
      temp = readAction(temp);
    }
  } else if ((*temp & 0xf8) == 0x80) {
    temp += (*temp & 0x07);
    temp++;
  } else if (*temp == 0x91) {
    temp += 3;
  } else if ((*temp & 0xfc) == 0x88) {
    temp += 2;
  } else if (*temp == 0xb0) {
    temp++;
    uint8_t stringLen = (*temp);
    temp += (stringLen);
    temp++;
  } else {
    temp++;
  }
  return temp;
}

void readData() {
  uint8_t *temp = iData;
  uint16_t *offsetPointer = offsets;
  while (*temp != 0xfe) {
    *offsetPointer = (temp - iData);
    offsetPointer++;
    temp = readAction(temp);
  }
  temp++;
  uint16_t *stringOffsetPointer = stringOffsets;
  while (*temp != 0xff) {
    *stringOffsetPointer = (temp - iData);
    stringOffsetPointer++;
    temp += (*temp);
    temp++;
  }
}

void scanKeys() {
  prevKeyStates = currKeyStates;
  currKeyStates = 0;

  for (byte c = 0; c < 4; c++) {
    pinMode(colPins[c], OUTPUT);
    digitalWrite(colPins[c], LOW);
    for (byte r = 0; r < 4; r++) {
      currKeyStates |= uint64_t(!digitalRead(rowPins[r])) << ((r << 2) | c);
    }
    digitalWrite(colPins[c], HIGH);
    pinMode(colPins[c], INPUT);
  }

  tempEncState = (tempEncState & ~0x02) | (digitalRead(enc1A) << 1);
  if ((tempEncState & 0x03) == 1 || (tempEncState & 0x03) == 2) {
    currKeyStates |= (1ULL << 16);
    if (digitalRead(enc1B) ^ (tempEncState >> 1 & 1)) {
      currKeyStates &= ~(1ULL << 17);
    } else {
      currKeyStates |= (1ULL << 17);
    }
  } else {
    currKeyStates &= ~(1ULL << 16);
  }
  tempEncState = (tempEncState & ~0x01) | (tempEncState >> 1 & 0x01);

  tempEncState = (tempEncState & ~0x08) | (digitalRead(enc2A) << 3);
  if ((tempEncState & 0x0C) == 8 || (tempEncState & 0x0C) == 4) {
    currKeyStates |= (1ULL << 18);
    if (digitalRead(enc2B) ^ (tempEncState >> 3 & 1)) {
      currKeyStates &= ~(1ULL << 19);
    } else {
      currKeyStates |= (1ULL << 19);
    }
  } else {
    currKeyStates &= ~(1ULL << 18);
  }
  tempEncState = (tempEncState & ~0x04) | ((tempEncState & 0x08) >> 1);

  uint8_t switchStates = ((~digitalRead(joy1S) & 1) << 2) | ((~digitalRead(joy2S) & 1) << 3);
  currKeyStates |= (uint64_t)switchStates << 20;

  for (int i = 0; i < 4; i++) {
    int rawReading = analogRead(A3 - i);
    int diff = rawReading - center[i];
    int abs = (min(abs(diff), 511) >> 1);
    uint16_t encodedValue = ((abs > 128)) | (((diff < 0) ^ ((inversion >> i) & 1)) << 1) | (abs << 2);
    currKeyStates |= (uint64_t)encodedValue << (24 + i * 10);
  }
}

uint8_t *handleMouse(uint8_t *temp, bool state) {
  if (*temp & 0x08) {
    if (state) {
      Mouse.release(*temp & 0x07);
    } else {
      Mouse.press(*temp & 0x07);
    }
  } else {
    if (!state) {
      if (*temp & 0x02) {
        Mouse.move(0, 0, 1 - ((*temp & 0x01) << 1));
      } else {
        temp++;
        signed char x = *temp;
        temp++;
        signed char y = *temp;
        Mouse.move(x, y, 0);
      }
    }
  }
  return temp;
}

void writeString(uint8_t *ptr, uint8_t len) {
  while (len--) {
    Keyboard.print((char)*ptr++);
    delay(50);
  }
}

uint8_t *handleAction(uint8_t *temp, bool state, uint8_t keynum) {
  if ((*temp & 0xc0) == 0x00) {
    if (state) {
      uint8_t *strptr = iData + stringOffsets[(*temp & 0x3f)];
      writeString(strptr + 1, *strptr);
    }
    temp++;
  } else if ((*temp & 0xc0) == 0x40) {
    uint8_t no_of_actions = (*temp++ & 0x3f);
    while (no_of_actions--) {
      temp = handleAction(temp, state, keynum);
    }
  } else if ((*temp & 0xf8) == 0x80) {
    uint8_t len = (*temp) & (0x07);
    while (len--) {
      temp++;
      if ((*temp & 0xf0) == 0x90) {
        temp = handleMouse(temp, state);
      } else {
        if (state) {
          Keyboard.press(*temp);
        } else {
          Keyboard.release(*temp);
        }
      }
    }
    temp++;
  } else if ((*temp) == 0x91) {
    handleMouse(temp, state);
    temp++;
  } else if (((*temp & 0xf8) == 0x88)) {
    switch ((*temp++ & 0x07)) {
      case 0:
        if ((*temp & 0xf0) == 0x90) {
          temp = handleMouse(temp, state);
        } else {
          if (state) {
            Keyboard.press(*temp);
          } else {
            Keyboard.release(*temp);
          }
        }
        break;
      case 1:
        uint8_t *start = temp;
        if ((*temp & 0xf0) == 0x90) {
          temp = handleMouse(temp, true);
        } else {
          Keyboard.press(*temp);
        }
        delay(30);
        temp = start;
        if ((*temp & 0xf0) == 0x90) {
          temp = handleMouse(temp, false);
        } else {
          Keyboard.release(*temp);
        }
        break;
      case 2:
        unsigned char power_of_10 = (*temp) & 0x0F;
        unsigned char number = ((*temp) >> 4) & 0x0F;
        delay(number * pow(10, power_of_10));
        break;
      default:
        break;
    }
    temp++;
  } else {
    if (*temp == 0xa0) {
      lastKey = keynum;
    } else if (*temp == 0xb0) {
      temp++;
      uint8_t len = (*temp);
      writeString(temp + 1, len);
      temp += len;
    }
    temp++;
  }
  return temp;
}

void handleKey(uint8_t keynum, bool state) {
  uint8_t *temp = iData + offsets[keynum];
  temp = handleAction(temp, state, keynum);
}

void processKeypad() {
  for (byte k = 0; k < 16; k++) {
    if (bitRead(currKeyStates, k) ^ bitRead(prevKeyStates, k)) {
      handleKey(k, bitRead(currKeyStates, k));
    }
  }
  if (bitRead(currKeyStates, 16)) {
    handleKey(16 + bitRead(currKeyStates, 17), true);
    handleKey(16 + bitRead(currKeyStates, 17), false);
  }
  if (bitRead(currKeyStates, 18)) {
    handleKey(18 + bitRead(currKeyStates, 19), true);
    handleKey(18 + bitRead(currKeyStates, 19), false);
  }
  for (byte k = 22; k < 24; k++) {
    if (bitRead(currKeyStates, k) ^ bitRead(prevKeyStates, k)) {
      handleKey(k, bitRead(currKeyStates, k));
    }
  }
  for (byte j = 0; j < 4; j++) {
    uint8_t prev = (prevKeyStates >> (24 + (j * 10))) & 0x03;
    uint8_t curr = (currKeyStates >> (24 + (j * 10))) & 0x03;
    if (!((prev & 1) | (curr & 1))) continue;
    if ((prev ^ curr) == 0) continue;
    if (prev & 1) {
      handleKey(24 + (2 * j) + ((prev >> 1) & 1), false);
    }
    if (curr & 1) {
      handleKey(24 + (2 * j) + ((curr >> 1) & 1), true);
    }
  }
}

void setPinModes() {
  pinMode(enc1A, INPUT_PULLUP);
  pinMode(enc1B, INPUT_PULLUP);
  pinMode(enc2A, INPUT_PULLUP);
  pinMode(enc2B, INPUT_PULLUP);

  pinMode(joy1X, INPUT);
  pinMode(joy1Y, INPUT);
  pinMode(joy1S, INPUT_PULLUP);

  pinMode(joy2X, INPUT);
  pinMode(joy2Y, INPUT);
  pinMode(joy2S, INPUT_PULLUP);

  for (byte r = 0; r < 4; r++) {
    pinMode(rowPins[r], INPUT_PULLUP);
  }
}

void calibrateJoysticks() {
  for (int i = 0; i < 10; i++) {
    center[0] += analogRead(joy1X);
    center[1] += analogRead(joy1Y);
    center[2] += analogRead(joy2X);
    center[3] += analogRead(joy2Y);
    delay(1);
  }
  center[0] = center[0] / 10;
  center[1] = center[1] / 10;
  center[2] = center[2] / 10;
  center[3] = center[3] / 10;
}

void initializeData() {
  uint8_t temp[] = {
    0x00,
    0x81, 0x92,
    0x01,
    0x02,

    0x03,
    0x81, 0x93,
    0x82, KEY_RIGHT_CTRL, 0x92,
    0x82, KEY_RIGHT_CTRL, 0x61,

    0x82, KEY_RIGHT_CTRL, 0x63,
    0x82, KEY_RIGHT_CTRL, 0x76,
    0x82, KEY_RIGHT_CTRL, 0x93,
    0x88, KEY_DOWN_ARROW,

    0xa0,
    0xa0,
    0xa0,
    0xa0,

    0x82, KEY_RIGHT_CTRL, '+',
    0x82, KEY_RIGHT_CTRL, '-',
    0x82, 0x85, 0xDA,
    0x82, 0x85, 0xD9,

    0x00,
    0x00,
    0x00,
    0x00,

    0xa0,
    0xa0,
    0xa0,
    0xa0,

    0xa0,
    0xa0,
    0xa0,
    0xa0,

    0x00,
    0x00,
    0x00,
    0x00,

    0x00,
    0x00,
    0x00,
    0x00,

    254,
    11, 72, 97, 114, 100, 105, 107, 32, 74, 97, 105, 110,
    23, 106, 97, 105, 110, 104, 97, 114, 100, 105, 107, 49, 50, 48, 64, 103, 109, 97, 105, 108, 46, 99, 111, 109,
    15, 86, 97, 115, 104, 110, 105, 32, 65, 103, 114, 97, 104, 97, 114, 105,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 255
  };

  for (int i = 0; i < sizeof(temp); i++) {
    iData[i] = temp[i];
  }
  readData();
}

void setup() {
  Serial.begin(115200);
  initializeData();
  setPinModes();
  calibrateJoysticks();
  bitWrite(tempEncState, 0, digitalRead(enc1A));
  bitWrite(tempEncState, 2, digitalRead(enc2A));
  Wire.begin(SLAVE_ADDRESS);
  Wire.onReceive(receiveEvent);
  Wire.onRequest(requestEvent);
  Mouse.begin();
  Keyboard.begin();
}

void loop() {
  scanKeys();
  processKeypad();
}

void receiveEvent(int bytes) {
  uint8_t instruction = Wire.read();
  uint16_t receivedSize = Wire.read();
  receivedSize = receivedSize << 8;
  receivedSize |= Wire.read();
  if ((instruction & 0xf8) == 0x80) {
    for (int i = 0; i < receivedSize && Wire.available(); i++) {
      iData[i] = Wire.read();
    }
    readData();
    while (Wire.available()) {
      Wire.read();
    }
  } else if ((instruction & 0xf8) == 0x88) {
    uint8_t *buffer = new uint8_t[receivedSize];
    for (int i = 0; i < receivedSize && Wire.available(); i++) {
      buffer[i] = Wire.read();
    }
    handleAction(buffer, (instruction & 0x01), 41);
    delete[] buffer;
  }
}

void requestEvent() {
  Wire.write(lastKey);
  lastKey = 0xff;
}