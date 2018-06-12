// UDP local variables
unsigned int localPort = UDP_PORT;
char incomingUDPPacketBuf[UDP_TX_PACKET_MAX_SIZE]; //buffer to hold incoming packet,
char outgoingUDPPacketBuf[UDP_TX_PACKET_MAX_SIZE]; //buffer to hold outgoing packet,

bool connectUDP() {
	bool state = false;
	if (UDP.begin(localPort) == 1) {
		Serial.println(" success. Port=" + String(localPort));
		state = true;
	}
	return state;
}

boolean initUDP() {
	udpConnected = false;
	// only proceed if wifi connection successful
	if (WiFi.status() == WL_CONNECTED) {
		Serial.print("Connecting to UDP ... ");
		while (!connectUDP()) {
			Serial.print(".");
			delay(453);  // keep trying after a while
		}
		BuildAndSendUDPToHost_AskToJoin();
		delay(800);
		int cmdID = ReadCmdFromUDP();
		if ( cmdID > 0) {            // we accept both CMD_OK_TO_JOIN_NETWORK, CMD_READ_SENSOR_DATA, and CMD_GOTO_DEEP_SLEEP
										// since all commands means that device is registered at host
			Serial.println("* Connected to host.");
			udpConnected = true; 
			ProcessCommand(cmdID);
		} 
	}
	return udpConnected;
}

void BuildAndSendUDPToHost_AskToJoin() {

	// For JSON
	StaticJsonBuffer<SENSORDATA_JSON_SIZE> jsonBuffer;
	JsonObject& jsonObject = jsonBuffer.createObject();

	Serial.print("\nSend UDP to host - AskToJoin: ");
	serializeJson_AskToJoin(jsonObject);
	jsonObject.prettyPrintTo(Serial);
	UDP.remoteIP() = hostIPaddress;
	sendJsonViaUDP(jsonObject);
}

void sendJsonViaUDP(JsonObject& json) {
	UDP.beginPacket(hostIPaddress, UDP_PORT);
	json.printTo(UDP);
	UDP.println();
	UDP.endPacket();
}

int ReadCmdFromUDP() {   // return 0 if no UDP on port or if not to this device. Else return command
	UDPCommand cmd;
	int commandID = 0;

	// if there’s data available, read a packet
	int packetSize = UDP.parsePacket();
	if (packetSize)
	{
		IPAddress remote = UDP.remoteIP();
		// Serial.printf("\nReceived packet of size %d from ", packetSize);
		// Serial.print(UDP.remoteIP());
		// Serial.printf(":%d", UDP.remotePort());

		// read the packet into packetBufffer
		UDP.read(incomingUDPPacketBuf, UDP_TX_PACKET_MAX_SIZE);
		Serial.printf("\nContents: %s" , incomingUDPPacketBuf);
		commandID = deserializeCmd(cmd, incomingUDPPacketBuf);
		if (strcmp(cmd.deviceID, DEVICE_ID) == 0) {
			hostIPaddress = remote;
		}
		else {
			commandID = 0; // this packet is not for me. Up to sender to deal with retransmission
			Serial.printf("ReadCmdFromUDP: not for this device");
		}
	}
	return commandID;
}


// not used from here
void SendUDPToMyself() {
	Serial.print("Send UDP to myself = ");
	Serial.print(UDP.beginPacket("10.5.5.101", UDP_PORT));
	Serial.print(UDP.write(2));
	Serial.println(UDP.endPacket());
	ReadCmdFromUDP();
}

void WriteDataToUDP(byte buf) {

	// send a reply, to the IP address and port that sent us the packet we received
	//UDP.beginPacket(UDP.remoteIP(), UDP.remotePort());
	//UDP.beginPacket("255.255.255.255", 1026);
	UDP.beginPacket(hostIPaddress, UDP_PORT);
	UDP.write(buf);
	UDP.endPacket();
	Serial.print(buf);
}

