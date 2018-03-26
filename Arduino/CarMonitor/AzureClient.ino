#include <ESP8266WiFi.h>
#include <Wire.h>
#include <TimeLib.h>        // http://playground.arduino.cc/code/time - installed via library manager
#include "globals.h"        // global structures and enums used by the applocation
#include <time.h>
#include "Constants.h"

// Workflow
#define DOWNLOAD_PICTURE_FROM_WEB false
#define TAKE_AND_UPLOAD_PICTURE_TO_BLOB true
#define CALL_BARCODERECOGNITION_WEBJOB true

// The Arduino device itself
DeviceConfig wifiDevice;

// Wifi and network globals
#define AT_HOME false
#define XPERIAHOTSPOT true

const char timeServer[] = "0.dk.pool.ntp.org"; // Danish NTP Server 
WiFiClient wifiClient;

// Image storage globals
MessageData msgData;

// Enable below if you want to read the whole image from the camera into an internal buffer. This limits the image size.
// Disable if you want to read directly from camera to http request without internal buffering. Works for all image sizes.
//#define USE_IMAGE_BUFFER 
#ifdef USE_IMAGE_BUFFER
	#define MAX_IMAGE_SIZE 10000 // if compile problem "Error linking for board WeMos D1 R2 & mini" reduce this number to reduce memory footprint
	imageBufferSourceEnum imageBufferSource = InternalImageBuffer;
#else
	#define MAX_IMAGE_SIZE 1 
	imageBufferSourceEnum imageBufferSource = DirectFromCamera;
#endif
byte imageBuffer[MAX_IMAGE_SIZE]; //nnr char imageBuffer[10000];

// Azure Cloud globals
CloudConfig cloud;


void initDeviceConfig() {
	wifiDevice.boardType = Other;            // BoardType enumeration: NodeMCU, WeMos, SparkfunThing, Other (defaults to Other). This determines pin number of the onboard LED for wifi and publish status. Other means no LED status 
	wifiDevice.deepSleepSeconds = 0;         // if greater than zero with call ESP8266 deep sleep (default is 0 disabled). GPIO16 needs to be tied to RST to wake from deepSleep. Causes a reset, execution restarts from beginning of sketch
}

void setup() {

	String wifiname;
	String wifipwd;

	Serial.begin(115200);
	Serial.println("AzureClient START");

	initFlashLED();
	LED_Flashes(5, 25);
	initArduCAM();
	delay(100);
	initWifiCredentials(wifiname, wifipwd);
	initDeviceConfig();
	azureCloudConfig(wifiname, wifipwd);
	initWifi();
	//initialiseAzure(); // https://msdn.microsoft.com/en-us/library/azure/dn790664.aspx  
}

void loop() {
	int published = false;
	int dataSize = 0;	
	boolean success = false;
	long bytesRead = 0;

	getCurrentTime();

	if (TAKE_AND_UPLOAD_PICTURE_TO_BLOB) {
		if (DOWNLOAD_PICTURE_FROM_WEB) {
			// This snippet gets image from http server somewhere (for testing)
			//success = readReplyFromServer(imageBuffer, msgData.blobSize);
			//while (!getImageData()) {
				// keep trying
				//Serial.println("ERROR: Problem fetching test image. Terminating");
				//delay(2000);
			//} 
		}
		else {
			LED_ON();
			takePicture(msgData.blobSize);  // note that this number indicates the size of the CROPPED image
			LED_OFF();
		}
		if (imageBufferSource == InternalImageBuffer) {
			LED_ON();
			readPicFromCamToImageBuffer(imageBuffer, msgData.blobSize);
			LED_OFF();
			dumpImageData(imageBuffer, msgData.blobSize);
		}
		else {
			// do nothing. The pic will be read directly from the camera from transmitDataOnWifi called by UploadToBlobOnAzure
		}
	}
	else {
		Serial.println("NO PHOTO TAKEN (enable compile flag to do this)");
	}
	msgData.blobName = FIXED_BLOB_NAME; 
	msgData.fullBlobName = "/" + String(cloud.storageContainerName) + "/" + String(cloud.deviceId) + "/" + String(msgData.blobName); // code39ex1.jpg";// " / input / " + String(cloud.deviceId) + " / input / " + String(msgData.blobName);

	if (WiFi.status() == WL_CONNECTED) {
		//Serial.println("WiFi.status() == WL_CONNECTED");

		if (TAKE_AND_UPLOAD_PICTURE_TO_BLOB) {
			if (UploadToBlobOnAzure(imageBufferSource, imageBuffer)) {
				Serial.println("Published Blob to Azure.");
			}
			else {
				Serial.println("ERROR: Could not publish Blob to Azure.");
			}
			delay(5000); // Cloud needs a little time to settle
		}

		if (CALL_BARCODERECOGNITION_WEBJOB) {
			if (SendDeviceToCloudHttpFunctionRequest(imageBufferSource, imageBuffer)) {
				//Serial.println("Sent HTTPPOST Trigger request to Azure: SUCCESS");
			}
			else {
				Serial.println("Sent HTTPPOST Trigger request to Azure: ERROR or no recognition");
			}
		}

/*		if (wifiDevice.deepSleepSeconds > 0) {
		  ESP.deepSleep(1000000 * wifiDevice.deepSleepSeconds, WAKE_RF_DEFAULT); // GPIO16 needs to be tied to RST to wake from deepSleep. Execute restarts from beginning of sketch
		}
		else {
		  delay(cloud.publishRateInSeconds * 1000);  // limit publishing rate
		}
*/	}
	else {
		initWifi();
		delay(250);
	}
	closeCamera();
	Serial.println("Stopping.");

	LED_OFF();    // turn the LED off by making the voltage LOW

	while (true) { delay(10000); Serial.print("#"); } //endless loop. We're done

}

void dumpImageData(byte *buf, long nbrOfBytes) {

	long length = 0;

	Serial.printf("\dumpImageData: %d bytes \n", length);

	while (length++ < nbrOfBytes)
	{
		if (length % 50 == 0) {
			Serial.print(length-1); Serial.print("=");; Serial.print((int)buf[length-1]); Serial.print(" ");
		}
	}

}


void dumpBinaryData(byte *buf, long nbrOfBytes) {

	long i = 0;
	Serial.printf("\dumpBinaryData: %d bytes \n", nbrOfBytes);

	while (i < nbrOfBytes)
	{
		Serial.printf("%d=%x ", i, (char)buf[i]);
		i++;
	}
}
