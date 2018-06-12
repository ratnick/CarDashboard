#include <Wire.h>
#include <Adafruit_MLX90614.h>

// http://www.arduinoprojects.net/temperature-projects/mlx90614-infrared-thermometer.php#codesyntax_1

Adafruit_MLX90614 mlx = Adafruit_MLX90614();

void setupSensor() {
	Serial.print("\nSensor name = ");
	Serial.println(sensorData.deviceID);

	switch (wifiDevice.sensorType) {
		case HeatSensor:
			setupHeatSensor();
			break;
		case VoltageSensor:
			; //do nothing
			break;
	}
	ReadI2CAddresses();
}

void setupHeatSensor()
{
	mlx.begin();
}

void readSensor() {
	switch (wifiDevice.sensorType) {
	case HeatSensor:
		readHeatSensor();
		break;
	case VoltageSensor:
		readVoltageSensor();
		break;
	}

}

void readHeatSensor()
{
	sensorData.temperature = mlx.readObjectTempC();
	sensorData.ambTemperature = mlx.readAmbientTempC();

	Serial.print("\nreadHeatSensor: temp = ");
	Serial.print(sensorData.temperature);
	Serial.print(", amb.temp = ");
	Serial.println(sensorData.ambTemperature);

}


void readVoltageSensor()
{
	sensorData.temperature = 12.0*analogRead(A0)/600;

	Serial.print("\readVoltageSensor: Voltage = ");
	Serial.println(sensorData.voltage);
}

void ReadI2CAddresses() {

	Serial.println("Scanning for attached I2C devices ...");
	byte count = 0;

	Wire.begin();
	for (byte i = 8; i < 120; i++)
	{
		Wire.beginTransmission(i);
		if (Wire.endTransmission() == 0)
		{
			Serial.print("Found address: ");
			Serial.print(i, DEC);
			Serial.print(" (0x");
			Serial.print(i, HEX);
			Serial.println(")");
			count++;
			delay(1);  // maybe unneeded?
		} 
	} 
	Serial.print("Found ");
	Serial.print(count, DEC);
	Serial.println(" device(s).");
} 
