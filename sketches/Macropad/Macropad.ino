#include <Keyboard.h>
#include <Mouse.h>
#include <math.h>
#include <Wire.h>

byte rowPins[4] = { 12, 11, 10, 9 };
byte colPins[4] = { 8, 7, 6, 5 };

uint16_t currKeyStates;
uint16_t prevKeyStates;

#define NO_OF_KEYS 40
#define NO_OF_STRINGS 64
#define MAX_PROGRAM_SIZE 1024
#define SLAVE_ADDRESS 0x08

uint16_t offsets[NO_OF_KEYS];
uint16_t stringOffsets[NO_OF_STRINGS];

uint8_t lastKey = 0xff;

uint8_t iData[MAX_PROGRAM_SIZE];


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
  for (byte r = 0; r < 4; r++) {
    pinMode(rowPins[r], INPUT_PULLUP);
  }
  for (byte c = 0; c < 4; c++) {
    pinMode(colPins[c], OUTPUT);
    digitalWrite(colPins[c], 0);
    for (byte r = 0; r < 4; r++) {
      bitWrite(currKeyStates, (r * 4) + c, !digitalRead(rowPins[r]));
    }
    digitalWrite(colPins[c], 1);
    pinMode(colPins[c], INPUT);
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

// state 0 means released
// state 1 means currently pressed

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
        delay(20);
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
        // Single key press
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
          Keyboard.press(*temp);
        }
        break;
      case 2:
        // Delay (in ms)
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
      // Send to BLE Server
      Serial.println("Updating keynum");
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
}

void setup() {
  Serial.begin(115200);
  delay(3000);
  Serial.println("Starting Keypad Scanner");
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

    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
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
  Wire.begin(SLAVE_ADDRESS);
  Wire.onReceive(receiveEvent);
  Wire.onRequest(requestEvent);
  Mouse.begin();
  Keyboard.begin();
}

void loop() {
  scanKeys();
  processKeypad();
  delay(100);
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
    for (int i = 0; i < receivedSize; i++) {
      Serial.print((char)buffer[i]);
    }
    Serial.println("");
    handleAction(buffer, (instruction & 0x01), 41);
    delete[] buffer;
  }
}

void requestEvent() {
  Wire.write(lastKey);
  lastKey = 0xff;
}