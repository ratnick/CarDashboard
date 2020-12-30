package com.racedash;

import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.sql.Timestamp;

public class Sensor {

    public static int MAX_KNOWN_SENSORS = 6;

    public int sensorID = -1;       // ID assigned within the app. varies from session to session
    public String deviceID = "";    // ID sent by the device itself. Must be hard-coded in the device itself.
    public String displayTitle1 = "";
    public String metricUnit1 = "";
    public String displayTitle2 = "";
    public String metricUnit2 = "";
    public double value1 = 0;
    public double value2 = 0;
    public InetAddress ip;
    public boolean connected = true;
    public Timestamp lastSampleTime = new Timestamp(System.currentTimeMillis());
    public TextView textviewItem1;
    public TextView textviewItem2;
    public TextView textviewItem3; // cold (input)
    public TextView textviewItem4; // hot  (live output)
    public TextView textViewDisplayTitle;
    public int lowerLimit1 = 0;
    public int upperLimit1 = 0;
    public int lowerLimit2 = 0;
    public int upperLimit2 = 0;
    public int proposedSampleFreq = 0; //ms

    public Sensor(JSONObject json, InetAddress ip, int sensorID) {

        try {
            if (json.has("deviceID")) {this.deviceID = json.getString("deviceID"); }
            if (json.has("displayTitle1")) {
                this.displayTitle1 = json.getString("displayTitle1"); }
            else {
                this.displayTitle1 = json.getString("deviceID");
            }
            if (json.has("metricUnit1")) {this.metricUnit1 = json.getString("metricUnit1"); }
            if (json.has("displayTitle2")) {this.displayTitle2 = json.getString("displayTitle2"); }
            if (json.has("metricUnit2")) {this.metricUnit2 = json.getString("metricUnit2"); }
            if (json.has("lowerLimit1")) {this.lowerLimit1 = json.getInt("lowerLimit1"); }
            if (json.has("upperLimit1")) {this.upperLimit1 = json.getInt("upperLimit1"); }
            if (json.has("lowerLimit2")) {this.lowerLimit2 = json.getInt("lowerLimit2"); }
            if (json.has("upperLimit2")) {this.upperLimit2 = json.getInt("upperLimit2"); }
            if (json.has("proposedSampleFreq")) {this.proposedSampleFreq = json.getInt("proposedSampleFreq"); }
            this.ip = ip;
            this.sensorID = sensorID;

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void SetSensorTextview(TextView textViewItem1, TextView textViewItem2, TextView textViewItem3, TextView textViewItem4, TextView _textViewDisplayTitle) {
        this.textviewItem1 = textViewItem1;
        this.textviewItem2 = textViewItem2;
        this.textviewItem3 = textViewItem3;
        this.textviewItem4 = textViewItem4;
        this.textViewDisplayTitle = _textViewDisplayTitle;
    }


    public static String[] knownSensors;  // These sensors will take a fixed place in the UI. If unlisted here, a sensor will be placed in random order.

    public static void InitKnownSensors(String[] allKnownSensors) {
        knownSensors = allKnownSensors;
    }


    public static int unknownSensorCount = 0;  // number of sensors which are not pre-registered, but should be displayed anyway

    public static int GetSensorID(String newDeviceID, Sensor[] sensorList) {
        int i;

        // Is it a known sensor?
        for (i=0;i<MAX_KNOWN_SENSORS;i++) {
            if (knownSensors[i].equals(newDeviceID)) {
                return i;
            }
        }

        // Is it an unknown, but still registered sensor
        for (i=MAX_KNOWN_SENSORS;i<unknownSensorCount+MAX_KNOWN_SENSORS;i++) {
            if (sensorList[i].deviceID.equals(newDeviceID)) {
                return i;
            }
        }
        // if we reach here, we don't know the sensor, and it's therefore and new and unknown sensor
        if (i >= Sensor.MAX_KNOWN_SENSORS) {
            unknownSensorCount++;
        }
        return i;
    }


    /*boolean SensorIsInitialized(int sensorID) {
        return (sensor[sensorID] != null);
    }*/


}
