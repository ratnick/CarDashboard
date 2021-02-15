/*
	Name:       CarMonitor.ino
	Author:     Nikolaj Nøhr-Rasmussen
*/

// Including the libraries below, allows to turn off Deep Search in libraries => faster compiling

#include "FirebaseLib.h"
#include <WifiLib.h>
#include <SysCall.h>
#include <sdios.h>
#include <SdFatConfig.h>
#include <SdFat.h>
#include <MinimumSerial.h>
#include <FreeStack.h>
#include <BlockDriver.h>
#include <SDFSFormatter.h>
#include <SDFS.h>
#include <SD.h>
#include <FirebaseJson.h>    // works with 2.2.8, but NOT 2.3.9. This will give random errors in communication with Firebase. I think it's becaise FirebaseJSON struct or class is changed
// end 

// OTA upload
#include <LEAmDNS_Priv.h>
#include <LEAmDNS_lwIPdefs.h>
#include <LEAmDNS.h>
#include <ESP8266mDNS_Legacy.h>
#include <ESP8266mDNS.h>
#include <ArduinoOTA.h>
// end


#include <FirebaseESP8266.h>
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <WiFiServer.h>
#include <WiFiClient.h>

#include <SPI.h>
#include <FirebaseESP8266HTTPClient.h>

#include <SoftwareSerial.h>
#include <Wire.h>
#include <DHTesp.h>
#include <EEPROM.h>
#include <TimeLib.h>        // http://playground.arduino.cc/code/time - installed via library manager
#include <time.h>

// own libraries
#include "globals.h"
#include "OTALib.h"
#include <LogLib.h>
#include <ArduinoJson.h>



#define DEBUGLEVEL 4

// The Arduino device itself
Device device;

// For UDP
WiFiUDP UDP;

// Current sensorData
SensorData sensorData;

int i = 0;
time_t watchdogTimestamp;

uint32_t calculateCRC32(const uint8_t* data, size_t length) {

	uint32_t crc = 0xffffffff;
	while (length--) {
		uint8_t c = *data++;
		for (uint32_t i = 0x80; i > 0; i >>= 1) {
			bool bit = crc & 0x80000000;
			if (c & i) {
				bit = !bit;
			}
			crc <<= 1;
			if (bit) {
				crc ^= 0x04c11db7;
			}
		}
	}
	return crc;
}

void DoSampling() {
	StaticJsonBuffer<SENSORDATA_JSON_SIZE> jsonBuffer;
	JsonObject& jsonObject = jsonBuffer.createObject();

	readSensor();
	serializeJson_ReadSensorData(sensorData, jsonObject);
	jsonObject.prettyPrintTo(Serial);
	sendJsonViaUDP(jsonObject);
	// We expect to reach this point at least every WATCHDOG_TIMEOUT ms. If we don't re-initiate connection to host.
}

void GoToDeepSleep() {
	ESP.rtcUserMemoryRead(0, (uint32_t*)&rtcData, sizeof(rtcData));  // store value in deep-sleep persistent memory 
	deepSleepPeriod = rtcData.deepSleepPeriod;

	if (deepSleepPeriod > 0) {
		Serial.println("Going to deep sleep");
		watchdogTimestamp = now() + deepSleepPeriod;
		Serial.print("GoToDeepSleep(): deepSleepPeriod = ");
		Serial.println(deepSleepPeriod);
//		ESP.deepSleep(deepSleepPeriod * 1000000);
	}	else {
		Serial.println("Deep sleep request NOT met. Value is zero");
	}
}

void ProcessCommand(int CmdID) {
	StaticJsonBuffer<SENSORDATA_JSON_SIZE> jsonBuffer;
	JsonObject& jsonObject = jsonBuffer.createObject();

	switch (CmdID) {
		case CMD_GOTO_DEEP_SLEEP:
			Serial.print("\nCMD_GOTO_DEEP_SLEEP");
			GoToDeepSleep();
			break;
		case CMD_READ_SENSOR_DATA:
			Serial.print("\nCMD_READ_SENSOR_DATA");
			readSensor();
			serializeJson_ReadSensorData(sensorData, jsonObject);
			jsonObject.prettyPrintTo(Serial);
			sendJsonViaUDP(jsonObject);
			// We expect to reach this point at least every WATCHDOG_TIMEOUT ms. If we don't re-initiate connection to host.
			watchdogTimestamp = now() + WATCHDOG_TIMEOUT;
			Serial.println(": EXECUTED");
			break;
	}
}

void initDeviceConfig() {
	device.boardType = WeMos;            // BoardType enumeration: NodeMCU, WeMos, SparkfunThing, Other (defaults to Other). This determines pin number of the onboard LED for wifi and publish status. Other means no LED status 
	device.deepSleepSeconds = 0;         // if greater than zero with call ESP8266 deep sleep (default is 0 disabled). GPIO16 needs to be tied to RST to wake from deepSleep. Causes a reset, execution restarts from beginning of sketch

	// read static data and ensure they are valid theough CRC check
	ESP.rtcUserMemoryRead(0, (uint32_t*)&rtcData, sizeof(rtcData));

	// check validity of data (https://github.com/esp8266/Arduino/blob/master/libraries/esp8266/examples/RTCUserMemory/RTCUserMemory.ino)
	uint32_t crcOfData = calculateCRC32((uint8_t*)&rtcData.deepSleepPeriod, sizeof(rtcData.deepSleepPeriod));
	Serial.print("CRC32 of data: ");
	Serial.println(crcOfData, HEX);
	Serial.print("CRC32 read from RTC: ");
	Serial.println(rtcData.crc32, HEX);
	if (crcOfData != rtcData.crc32) {
		Serial.println("CRC32 in RTC memory doesn't match CRC32 of data. Data is probably invalid! Re-initialize");
		rtcData.deepSleepPeriod = DS_INIT_VALUE;
		rtcData.crc32 = crcOfData;
		ESP.rtcUserMemoryWrite(0, (uint32_t*)&rtcData, sizeof(rtcData));
	}
	else {
		Serial.println("CRC32 check ok, data is probably valid.");
	}
}

void ConnectToWifi() {
	int wifiIndex = initWifi(&device.wifi);
	delay(250);
	if (wifiIndex == -1 || WiFi.status() != WL_CONNECTED) {
		LogLine(0, __FUNCTION__, "Could not connect to wifi. ");
	}
	else if (wifiIndex == 100 || wifiIndex == 200) {
		// Connected. Do nothing.
	}
	else {
		PrintIPAddress();
	}
}

void setup() {

	String wifiname;
	String wifipwd;

	Serial.begin(115200);
	Serial.println("CarMonitor START");
	InitDebugLevel(DEBUGLEVEL);

	initFlashLED();
	LED_Flashes(5, 25);
	delay(100);
	initDeviceConfig();
	setupSensor();
	ConnectToWifi();
	PrintIPAddress();
	SetupOTA();

}


void loop() {

	int cmdID;

	ArduinoOTA.handle();
	// for test
	ProcessCommand(CMD_READ_SENSOR_DATA);

	if (WiFi.status() == WL_CONNECTED) {
		if (udpConnected && (now() < watchdogTimestamp) ) {
			cmdID = ReadCmdFromUDP();
			ProcessCommand(cmdID);
			delay(200);
		} else {
			if (initUDP()) {
				watchdogTimestamp = now() + WATCHDOG_TIMEOUT;
			} else {
				Serial.println("InitUDP: Could not connect to host: ");
				delay(2000);
				GoToDeepSleep();
			}
		}
	}
	else {
		initWifi(&device.wifi);
		delay(250);
		if (WiFi.status() != WL_CONNECTED) {
			Serial.print("\nCould not connect to wifi: ");
			GoToDeepSleep();
		}
	}
}

