#include <ArduinoJson.h>
#include <WiFiUDP.h>
#include <WiFiServer.h>
#include <WiFiClientSecure.h>
#include <WiFiClient.h>
#include <ESP8266WiFiType.h>
#include <ESP8266WiFiSTA.h>
#include <ESP8266WiFiScan.h>
#include <ESP8266WiFiMulti.h>
#include <ESP8266WiFiGeneric.h>
#include <ESP8266WiFiAP.h>
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <Wire.h>
#include <TimeLib.h>        // http://playground.arduino.cc/code/time - installed via library manager
#include "globals.h"        // global structures and enums used by the applocation
#include <time.h>

// The Arduino device itself
DeviceConfig wifiDevice;

// Wifi and network globals
const char timeServer[] = "0.dk.pool.ntp.org"; // Danish NTP Server 
WiFiClient wifiClient;

// For UDP
WiFiUDP UDP;

// Current sensorData
SensorData sensorData;

uint32_t calculateCRC32(const uint8_t *data, size_t length);

void initDeviceConfig() {
	wifiDevice.boardType = WeMos;            // BoardType enumeration: NodeMCU, WeMos, SparkfunThing, Other (defaults to Other). This determines pin number of the onboard LED for wifi and publish status. Other means no LED status 
	wifiDevice.deepSleepSeconds = 0;         // if greater than zero with call ESP8266 deep sleep (default is 0 disabled). GPIO16 needs to be tied to RST to wake from deepSleep. Causes a reset, execution restarts from beginning of sketch

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

void setup() {

	String wifiname;
	String wifipwd;

	Serial.begin(115200);
	Serial.println("CarMonitor START");

	initFlashLED();
	LED_Flashes(5, 25);
	delay(100);
	initDeviceConfig();
	setupSensor();
	//initWifiAccesspoint();
	initWifi();
	PrintIPAddress();
}

int i = 0;

time_t watchdogTimestamp;

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
		ESP.deepSleep(deepSleepPeriod * 1000000);
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

void loop() {

	int cmdID;

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
		initWifi();
		delay(250);
		if (WiFi.status() != WL_CONNECTED) {
			Serial.print("\nCould not connect to wifi: ");
			GoToDeepSleep();
		}
	}
}

// If Arduino should be used as access point instead of client
void loop2() {
	StartWifiAccesspoint();
}


uint32_t calculateCRC32(const uint8_t *data, size_t length) {

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