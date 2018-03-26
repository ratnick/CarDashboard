#include <Wire.h>
#include <Adafruit_MLX90614.h>

// http://www.arduinoprojects.net/temperature-projects/mlx90614-infrared-thermometer.php#codesyntax_1

Adafruit_MLX90614 mlx = Adafruit_MLX90614();

void setupHeatSensor()
{
	mlx.begin();
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
