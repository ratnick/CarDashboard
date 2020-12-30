JsonObject& buildJson(JsonBuffer& jsonBuffer) {
		JsonObject& root = jsonBuffer.createObject();

		JsonArray& analogValues = root.createNestedArray("analog");
		for (int pin = 0; pin < 6; pin++) {
			int value = analogRead(pin);
			analogValues.add(value);
		}

		JsonArray& digitalValues = root.createNestedArray("digital");
		for (int pin = 0; pin < 14; pin++) {
			int value = digitalRead(pin);
			digitalValues.add(value);
		}

		return root;
	}

	int deserializeCmd(UDPCommand& cmd, char* json)
	{
		StaticJsonBuffer<SENSORDATA_JSON_SIZE> jsonBuffer;
		JsonObject& root = jsonBuffer.parseObject(json);
		cmd.commandID = root["commandID"];
		cmd.deviceID = root["deviceID"];
		cmd.displayTitle1 = root["displayTitle1"];
		cmd.metricUnit1 = root["metricUnit1"];
		cmd.displayTitle2 = root["displayTitle2"];
		cmd.metricUnit2 = root["metricUnit2"];
		if (root.containsKey("secondsToSleep")) {
			deepSleepPeriod = root["secondsToSleep"];
			rtcData.deepSleepPeriod = deepSleepPeriod;
			rtcData.crc32 = calculateCRC32((uint8_t*)&rtcData.deepSleepPeriod, sizeof(rtcData.deepSleepPeriod));
			ESP.rtcUserMemoryWrite(0, (uint32_t*)&rtcData, sizeof(rtcData));  // store value in deep-sleep persistent memory 
			Serial.print("\ndeepSleepPeriod=");
			Serial.println(deepSleepPeriod);
		}
		return cmd.commandID;
	}

	void serializeJson_ReadSensorData(const SensorData& data, JsonObject& root)
	{
		root["commandID"] = CMD_SENSOR_DATA_REPLY;
		root["deviceID"] = data.deviceID;
		switch (device.sensorType) {
		case HeatSensor:
			root["value1"] = data.temperature;
			root["value2"] = data.ambTemperature;
			break;
		case VoltageSensor:
			root["value1"] = data.temperature;
			root["value2"] = data.ambTemperature;
			break;
		case HumAndTempDHTSensor:
			root["value1"] = data.temperature;
			root["value2"] = data.humidity;
			break;
		}

	}

	void serializeJson_AskToJoin(JsonObject& root)
	{
		root["commandID"]     = CMD_ASK_HOST_TO_JOIN_NETWORK;
		root["deviceID"]      = DEVICE_ID;
		root["displayTitle1"] = displayTitle1 ;
		root["metricUnit1"]   = metricUnit1;
		root["displayTitle2"] = displayTitle2;
		root["metricUnit2"]   = metricUnit2;
		root["proposedSampleFreq"] = proposedSampleFreq;
		root["lowerLimit1"] = lowerLimit1;
		root["upperLimit1"] = upperLimit1;
		root["lowerLimit2"] = lowerLimit2;
		root["upperLimit2"] = upperLimit2;
	}

	void SimpleJsonForTest() {
		StaticJsonBuffer<200> jsonBuffer;
		JsonObject& root = jsonBuffer.createObject();

		root["sensor"] = "gps";
		root["time"] = 1351824120;
		root["temperature"] = 23;
		root["ambTemperature"] = 230;
		root["humidity"] = 17.3;
		root["avgHumidity"] = 33.2;
		root["voltage"] = 11;
		root["freeRam"] = 111;

		// Add a nested array.
		JsonArray& data = root.createNestedArray("data");
		data.add(48.756080);
		data.add(2.302038);

		UDP.beginPacket(UDP.remoteIP(), UDP.remotePort());
		root.printTo(UDP);

		root.printTo(Serial);
		// This prints:
		// {"sensor":"gps","time":1351824120,"data":[48.756080,2.302038]}
		Serial.println();
		root.prettyPrintTo(Serial);

		UDP.println();
		UDP.endPacket();

	}

