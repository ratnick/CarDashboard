package com.droid.ws;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import com.microsoft.windowsazure.mobileservices.MobileServiceClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.droid.ws.Sensor.GetSensorID;
import static com.droid.ws.Sensor.MAX_KNOWN_SENSORS;

public class MainActivity extends Activity {

    //private MobileServiceClient mClient;

    static int MAX_SENSORS = 10;
    Sensor[] sensor = new Sensor[MAX_SENSORS];  // 1-dimensional array of Sensor objects
    public static final int SENSOR_TIMEOUT = 20000; // ms
    //public static final int SENSOR_DEEP_SLEEP_TIMEOUT = 20; // sec
    //public static final int SAMPLE_INTERVAL_MS = 500;

    TextView sensorsDataReceivedTimeTextView;
    TextView LFtemperatureTextView;
    TextView RFtemperatureTextView;
    TextView LRtemperatureTextView;
    TextView RRtemperatureTextView;
    TextView sensorDataVoltagetextView;
    TextView ambientTemperatureTextView;
    TextView sensorSleepTime;
    TextView sampleTime;
    TextView tempConvertAlpha;
    TextView tempConvertBeta;
    TextView wifiSSIDTextView;

    ToggleButton raceOnOffButton;
    Button exitButton;

    boolean appInBackground = false;
    boolean doneEditing = true;

    public MainActivity() {
        //knownSensors = new String[]{"LF", "RF", "LR", "RR"};
        String[] ks = new String[]{"LF", "RF", "LR", "RR", "V"};
        Sensor.InitKnownSensors(ks);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appInBackground = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInBackground = true;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_main);
        // disable auto turn screen off feature
        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); // Turn screen on if off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Keep screen on

        sensorsDataReceivedTimeTextView = (TextView) findViewById(R.id.sensorsDataReceivedTimeTextView);
        LFtemperatureTextView = (TextView) findViewById(R.id.LFtemperatureTextView);
        RFtemperatureTextView = (TextView) findViewById(R.id.RFtemperatureTextView);
        LRtemperatureTextView = (TextView) findViewById(R.id.LRtemperatureTextView);
        RRtemperatureTextView = (TextView) findViewById(R.id.RRtemperatureTextView);
        sensorDataVoltagetextView = (TextView) findViewById(R.id.sensorDataVoltagetextView);
        ambientTemperatureTextView = (TextView) findViewById(R.id.ambientTemperatureTextView);
        raceOnOffButton = (ToggleButton) findViewById(R.id.raceOnOffButton);
        exitButton = (Button) findViewById(R.id.exitButton);
        sensorSleepTime = (TextView) findViewById(R.id.sensorSleepTime);
        sampleTime = (TextView) findViewById(R.id.sampleTime);
        tempConvertAlpha = (TextView) findViewById(R.id.tempConvertAlpha);
        tempConvertBeta = (TextView) findViewById(R.id.tempConvertBeta);
        wifiSSIDTextView = (TextView) findViewById(R.id.wifiSSIDTextView);

        if (CsvFileWriter.checkSDCard()) {
            CsvFileWriter.CreateCsvFile(getApplicationContext());
        }

        // Write to the cloud
//        try {
//            mClient = new MobileServiceClient("https://nnrmobileappservice.azurewebsites.net",this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        comm.wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        comm.dhcp = comm.wifi.getDhcpInfo();
        WifiInfo info = comm.wifi.getConnectionInfo ();
        final String ssidStr  = info.getSSID();
        UpdateSSID(ssidStr);

        // listen and process INCOMING UDP packets
        new Thread(new Runnable() {
            @Override
            public void run() {
                //comm.wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                //comm.dhcp = comm.wifi.getDhcpInfo();

                //old1: WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                //old1: DhcpInfo dhcp = wifi.getDhcpInfo();

                try {
                    comm.socket = new DatagramSocket(comm.UDP_PORT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (true) {
                    // Receive next datagram
                    byte[] data = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    try {
                        if (comm.socket == null) {
                            Log.e("HD:receiveUdpData", "socket == null");
                            continue;
                        }
                        comm.socket.receive(packet);
                    } catch (IOException e) {
                        Log.e("HD:receiveUdpData", "Error occurred when receiving UDP data on port: " + comm.UDP_PORT);
                        e.printStackTrace();
                    }
                    if (packet == null) { continue; }
                    String udpPacketData = new String(packet.getData());
                    InetAddress ip = packet.getAddress();

                    // process depending on command
                    try {
                        JSONObject jsonIncoming = new JSONObject(udpPacketData);
                        int cmd = jsonIncoming.getInt("commandID");
                        switch (cmd) {
                            case comm.CMD_ASK_HOST_TO_JOIN_NETWORK :
                                String newDeviceID = jsonIncoming.getString("deviceID");
                                int sensorID = GetSensorID(newDeviceID, sensor);
                                if ( sensor[sensorID] == null) {  //sensor not yet registered
                                    sensor[sensorID] = new Sensor(jsonIncoming, ip, sensorID);
                                    AssignTextViewsToSensor(sensorID);
                                }
                                comm.CreateAndSendCmd_OkToJoin(newDeviceID , ip);
                                TimedDelay(1200);
                                break;
                            case comm.CMD_SENSOR_DATA_REPLY :
                                updateSensor(jsonIncoming);
                                break;
                            default:
                                ;  // unexpected command received
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        // Request (i.e. OUTGOING packets) poll for data from all attached sensors in same thread. Reception handled in other thread
        new Thread(new Runnable() {
            @Override
            public void run() {
            int sampletime = 2000;
            while (true) {
                for (int i = 0; i < MAX_SENSORS; i++) {
                    if (sensor[i] != null) {  //SensorIsInitialized(i)) {
                        // TODO: Check if the deadline for receiving answer from sensor has passed. If so, set the color to gray and mark as uninitialized.
                        Timestamp timeout = new Timestamp(System.currentTimeMillis() - SENSOR_TIMEOUT);
                        if (sensor[i].lastSampleTime.before(timeout)) {
                            sensor[i].connected = false;
                            UpdateSensorUI_Disconnect(i);
                        }
                        comm.CreateAndSendCmd_GetSensorReadings(sensor[i]);
                        if (!raceOnOffButton.isChecked()) {
                            int sensorsleeptime = Integer.parseInt(sensorSleepTime.getText().toString());
                            //TimedDelay(2000); // wait for sensor to reply to the last data request
                            comm.CreateAndSendCmd_GotoDeepSleep(sensor[i], sensorsleeptime);
                        }
                    }
                }
                sampletime = Integer.parseInt(sampleTime.getText().toString());
                TimedDelay(sampletime);  //TODO: introduce a listener for changes to this value instead of reading every time from the UI.
                                         //TODO: Use proposedSampleFreq sent from each device. That will require a loop or thread per device. Disavantage: Not runtime configurable AND hard to do since UDP queue is shared.
            }
            }
        }).start();

        raceOnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (raceOnOffButton.isChecked()) {
                    raceOnOffButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                    raceOnOffButton.setTextColor(Color.GREEN);
                } else {
                    raceOnOffButton.getBackground().setColorFilter(Color.YELLOW,PorterDuff.Mode.LIGHTEN);
                    raceOnOffButton.setTextColor(Color.BLUE);

                }
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CsvFileWriter.CloseCsvFile(getApplicationContext());
                finish();
                System.exit(0);
            }
        });
    }

    void AssignTextViewsToSensor(int sensorID) {
        switch (sensorID) {
            case 0:
                sensor[sensorID].SetSensorTextview(LFtemperatureTextView, null);
                break;
            case 1:
                sensor[sensorID].SetSensorTextview(RFtemperatureTextView, ambientTemperatureTextView);
                break;
            case 2:
                sensor[sensorID].SetSensorTextview(LRtemperatureTextView, null);
                break;
            case 3:
                sensor[sensorID].SetSensorTextview(RRtemperatureTextView, null);
                break;
            case 4:
                sensor[sensorID].SetSensorTextview(sensorDataVoltagetextView, null);
                break;
        }
    }

    void TimedDelay(int delay_ms) {
        try
        {
            Thread.sleep(delay_ms);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    void updateSensor(JSONObject json){
        try {
            String deviceID = null;
            deviceID = json.getString("deviceID");
            int i = Sensor.GetSensorID(deviceID, sensor);
            sensor[i].value1 = json.getDouble("value1");
            sensor[i].value2 = json.getDouble("value2");
            sensor[i].connected = true;
            sensor[i].lastSampleTime = new Timestamp(System.currentTimeMillis());
            UpdateSensorUI_NewValue(i);
            CsvFileWriter.AppendValueToFile(sensor[i]);
            SendSampleToAzure(); // TODO HERTIL
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void SendSampleToAzure() {  // HERTIL mht Azure upload
/*        TodoItem item = new TodoItem();
        item.Text = "Awesome item";
        mClient.getTable(TodoItem.class).insert(item, new TableOperationCallback<item>() {
            public void onCompleted(TodoItem entity, Exception exception, ServiceFilterResponse response) {
                if (exception == null) {
                    // Insert succeeded
                } else {
                    // Insert failed
                }
            }

        });
*/
    }

    double ConvertTemperature(double orgVal) {
        double alpha = Double.parseDouble(tempConvertAlpha.getText().toString());
        double beta = Double.parseDouble(tempConvertBeta.getText().toString());
        return alpha + beta*orgVal;
    }

    void UpdateSensorUI_NewValue(final int sensorID){
        final TextView innertextviewItem = sensor[sensorID].textviewItem1;
        double val = sensor[sensorID].value1;

        // If this is a temperature sensor, convert the value
        if (sensor[sensorID].deviceID.equals("LF") ||
            sensor[sensorID].deviceID.equals("RF") ||
            sensor[sensorID].deviceID.equals("LR") ||
            sensor[sensorID].deviceID.equals("RR") ) {
            val = ConvertTemperature(val);
        }

        final double val_f = val;
        innertextviewItem.post(new Runnable() {
            public void run() {

                if (sensor[sensorID].deviceID.equals("V")) {   // TODO: Do this by making a sensor type or something similar transferred from the sensor itself
                    innertextviewItem.setText(String.format(Locale.ENGLISH, "%.1f", val_f));
                } else {
                    innertextviewItem.setText(String.format(Locale.ENGLISH, "%.0f", val_f));
                }
                if (val_f < sensor[sensorID].lowerLimit1 ) {
                    innertextviewItem.setTextColor(Color.BLUE);
                } else if (sensor[sensorID].upperLimit1 < val_f) {
                    innertextviewItem.setTextColor(Color.RED);
                } else {
                    innertextviewItem.setTextColor(Color.GREEN);
                }
            }
        });

        if (sensor[sensorID].textviewItem2 != null) {
            final TextView innertextviewItem2 = sensor[sensorID].textviewItem2;
            double val2 = sensor[sensorID].value2;
            final double val2_f = val2;
            innertextviewItem2.post(new Runnable() {
                public void run() {
                    innertextviewItem2.setText(String.valueOf(val2_f).substring(0,4));
                    if (val2_f < sensor[sensorID].lowerLimit2 ) {
                        innertextviewItem2.setTextColor(Color.BLUE);
                    } else if (sensor[sensorID].upperLimit2 < val2_f) {
                        innertextviewItem2.setTextColor(Color.RED);
                    } else {
                        innertextviewItem2.setTextColor(Color.GREEN);
                    }
                }
            });
        }
        sensorsDataReceivedTimeTextView.post(new Runnable() {
            public void run() {
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                sensorsDataReceivedTimeTextView.setText(dateFormat.format(new Date()));
            }
        });
    }

    void UpdateSensorUI_Disconnect(int sensorID){
        final TextView innertextviewItem = sensor[sensorID].textviewItem1;
        innertextviewItem.post(new Runnable() {
            public void run() {
                innertextviewItem.setTextColor(Color.GRAY);
            }
        });
    }

    void UpdateSSID(final String ssid) {
        wifiSSIDTextView.post(new Runnable() {
            public void run() {
                wifiSSIDTextView.setText(ssid);
            }
        });
    }
}