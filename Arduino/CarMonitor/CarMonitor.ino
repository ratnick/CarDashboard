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

void initDeviceConfig() {
	wifiDevice.boardType = WeMos;            // BoardType enumeration: NodeMCU, WeMos, SparkfunThing, Other (defaults to Other). This determines pin number of the onboard LED for wifi and publish status. Other means no LED status 
	wifiDevice.deepSleepSeconds = 0;         // if greater than zero with call ESP8266 deep sleep (default is 0 disabled). GPIO16 needs to be tied to RST to wake from deepSleep. Causes a reset, execution restarts from beginning of sketch
}

void setup() {

	String wifiname;
	String wifipwd;

	Serial.begin(115200);
	Serial.println("TyreHeatMonitor START");

	initFlashLED();
	LED_Flashes(5, 25);
	delay(100);
	initDeviceConfig();
	//initWifiAccesspoint();
	initWifi();
	PrintIPAddress();
	setupHeatSensor();
	initUDP();  // UDP setup and communication with host
}

int i = 0;

time_t watchdogTimestamp;

void DoSampling() {
	StaticJsonBuffer<SENSORDATA_JSON_SIZE> jsonBuffer;
	JsonObject& jsonObject = jsonBuffer.createObject();

	readHeatSensor();
	serializeJson_ReadSensorData(sensorData, jsonObject);
	jsonObject.prettyPrintTo(Serial);
	sendJsonViaUDP(jsonObject);
	// We expect to reach this point at least every WATCHDOG_TIMEOUT ms. If we don't re-initiate connection to host.
}

void loop() {

	StaticJsonBuffer<SENSORDATA_JSON_SIZE> jsonBuffer;
	JsonObject& jsonObject = jsonBuffer.createObject();

	if (WiFi.status() == WL_CONNECTED) {
		if (udpConnected && (now() < watchdogTimestamp) ) {
			switch (ReadCmdFromUDP()) {  
				case CMD_GOTO_DEEP_SLEEP:
					Serial.println("going to deep sleep");
					watchdogTimestamp = now() + deep_sleep_period;
					ESP.deepSleep(deep_sleep_period * 1000000);
					break;
				case CMD_READ_SENSOR_DATA :
					readHeatSensor();
					serializeJson_ReadSensorData(sensorData, jsonObject);
					jsonObject.prettyPrintTo(Serial);
					sendJsonViaUDP(jsonObject);
					// We expect to reach this point at least every WATCHDOG_TIMEOUT ms. If we don't re-initiate connection to host.
					watchdogTimestamp = now() + WATCHDOG_TIMEOUT;
					break;
			}
			delay(200);
		} else {
			Serial.println("calling initUDP");
			initUDP();
			watchdogTimestamp = now() + WATCHDOG_TIMEOUT;
		}
	}
	else {
		initWifi();
		delay(250);
	}
/*	if (i++ == 200) {
		i = 0;
		Serial.print(".");
	}*/
}

// If Arduino should be used as access point instead of client
void loop2() {
	StartWifiAccesspoint();
}

