const char DEVICE_ID[] = "XRR";                // "V"           / "RF"
const char displayTitle1[] = "Ambient temp."; // "Voltage"     / "Ambient temp."
const char metricUnit1[] = "C";               // "V"           / "C";
const char displayTitle2[] = "Tyre temp";
const char metricUnit2[] = "C";
const int lowerLimit1 = 35;                   // 11            / 40;
const int upperLimit1 = 80;                   // 15            / 80;
const int lowerLimit2 = 15;
const int upperLimit2 = 30;
const int  proposedSampleFreq = 5000; // ms   //NOTE: Not used at receiving end (android app). For future use.

enum SensorType {
	HeatSensor,
	VoltageSensor
};
SensorType SENSORTYPE = HeatSensor;           // VoltageSensor / HeatSensor;

// UDP comm
//char AckReply[] = "acknowledged from Ardu"; // a string to send back
boolean udpConnected = false;
const int CMD_ASK_HOST_TO_JOIN_NETWORK = 1;
const int CMD_OK_TO_JOIN_NETWORK = 2;
const int CMD_READ_SENSOR_DATA = 3;
const int CMD_SENSOR_DATA_REPLY = 4;
const int CMD_GOTO_DEEP_SLEEP = 5;
const int UDP_PORT = 1026;
IPAddress hostIPaddress = (0xFFFFFFFF);
time_t WATCHDOG_TIMEOUT = 0; //20; // sec

const time_t DS_INIT_VALUE = 3;
time_t deepSleepPeriod = DS_INIT_VALUE; // sec   NOTE: This is the initial value. It is modified via CMD_GOTO_DEEP_SLEEP 
struct {
	uint32_t crc32;
	int deepSleepPeriod;
} rtcData;


// Pins on WeMos D1 Mini
const int SPI_CS = 16; 
const int LED_PIN = D4;

struct UDPCommand {
	int commandID;
	const char* deviceID = DEVICE_ID;
	const char* displayTitle1 = "";
	const char* metricUnit1 = "";
	const char* displayTitle2 = "";
	const char* metricUnit2 = "";
	const int proposedSampleFreq = 1000; // ms
	const int lowerLimit1 = 0;
	const int upperLimit1 = 0;
	const int lowerLimit2 = 0;
	const int upperLimit2 = 0;
};


enum LedState {
  LED_Off,
  LED_On
};

enum SensorMode {
  None,
  Bmp180Mode,
  DhtShieldMode
};

enum BoardType {
  NodeMCU,
  WeMos,
  SparkfunThing,
  Other
};

struct SensorData {
	const char* deviceID = DEVICE_ID;
	float value;
	double temperature;
	double ambTemperature;
	double humidity;
	double avgHumidity;
	double voltage;
	int freeRam;
};
// size = max nbr of elements in SensorData
#define SENSORDATA_JSON_SIZE (JSON_OBJECT_SIZE(15))  

struct DeviceConfig {
  int WifiIndex = 0;
  unsigned long LastWifiTime = 0;
  int WiFiConnectAttempts = 0;
  int wifiPairs = 1;
  String ssid;
  String currentSSID;
  String pwd;
  BoardType boardType = Other;            // OperationMode enumeration: NodeMCU, WeMos, SparkfunThing, Other
  SensorMode sensorMode = None;           // OperationMode enumeration: DemoMode (no sensors, fakes data), Bmp180Mode, Dht11Mode
  unsigned int deepSleepSeconds = 0;      // Number of seconds for the ESP8266 chip to deepsleep for.  GPIO16 needs to be tied to RST to wake from deepSleep http://esp8266.github.io/Arduino/versions/2.0.0/doc/libraries.html
  SensorType sensorType = SENSORTYPE;
};

