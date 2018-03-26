package com.droid.ws;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
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

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private MobileServiceClient mClient;

    static int MAX_SENSORS = 10;
    Sensor[] sensor = new Sensor[MAX_SENSORS];  // 1-dimensional array of Sensor objects
    static int MAX_KNOWN_SENSORS = 4;
    String[] knownSensors;  // These sensors will take a fixed place in the UI. If unlisted here, a sensor will be placed in random order.
    int unknownSensorCount = 0;
    public static final int SENSOR_TIMEOUT = 5000; // ms
    public static final int SENSOR_DEEP_SLEEP_TIMEOUT = 20; // sec

    public static final int SAMPLE_INTERVAL_MS = 500;

    TextView sensorsDataReceivedTimeTextView;
    TextView LFtemperatureTextView;
    TextView RFtemperatureTextView;
    TextView LRtemperatureTextView;
    TextView RRtemperatureTextView;
    TextView ambientTemperatureTextView;

    ToggleButton raceOnOffButton;

    boolean appInBackground = false;
    boolean doneEditing = true;

    public MainActivity() {
        knownSensors = new String[]{"LF", "RF", "LR", "RR"};
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
        ambientTemperatureTextView = (TextView) findViewById(R.id.ambientTemperatureTextView);
        raceOnOffButton = (ToggleButton) findViewById(R.id.raceOnOffButton);

        if (CsvFileWriter.checkSDCard()) {
            CsvFileWriter.CreateCsvFile(knownSensors, MAX_KNOWN_SENSORS);
            CsvFileWriter.CloseCsvFile();
        }

        // Write to the cloud
        try {
            mClient = new MobileServiceClient("https://nnrmobileappservice.azurewebsites.net",this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // listen and process incoming UDP packets
        new Thread(new Runnable() {
            @Override
            public void run() {
                comm.wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                comm.dhcp = comm.wifi.getDhcpInfo();
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
                                int sensorID = GetSensorID(newDeviceID);
                                if (!SensorIsInitialized(sensorID)) {
                                    sensor[sensorID] = new Sensor(jsonIncoming, ip, sensorID);
                                    comm.CreateAndSendCmd_OkToJoin(newDeviceID , ip);
                                    TimedDelay(1200);
                                    if (sensorID >= MAX_KNOWN_SENSORS) {
                                        unknownSensorCount++;
                                    }
                                }
                                break;
                            case comm.CMD_SENSOR_DATA_REPLY :
                                updateSensor(jsonIncoming);
                                break;
                            default:
                                ;
                                // unexpected command
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        // poll for data from attached sensor. Reception handled in other thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                        for (int i = 0; i < MAX_SENSORS; i++) {
                            if (SensorIsInitialized(i)) {
                                // TODO: Check if the deadline for receiving answer from sensor has passed. If so, set the color to gray and mark as uninitialized.
                                Timestamp timeout = new Timestamp(System.currentTimeMillis() - SENSOR_TIMEOUT);
                                if (sensor[i].lastSampleTime.before(timeout)) {
                                    sensor[i].connected = false;
                                    UpdateSensorUI_Disconnect(i);
                                }
                                comm.CreateAndSendCmd_GetSensorReadings(sensor[i]);
                                if (!raceOnOffButton.isChecked()) {
                                    TimedDelay(SENSOR_TIMEOUT);
                                    comm.CreateAndSendCmd_GotoDeepSleep(sensor[i], SENSOR_DEEP_SLEEP_TIMEOUT);
                                }
                            }
                        }
                        TimedDelay(SAMPLE_INTERVAL_MS);
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

    boolean SensorIsInitialized(int sensorID) {
        return (sensor[sensorID] != null);
    }

    int GetSensorID(String newDeviceID) {
        int i;

        // Is it a known sensor?
        for (i=0;i<MAX_KNOWN_SENSORS;i++) {
            if (knownSensors[i].equals(newDeviceID)) {
                return i;
            }
        }

        // Is it an unknown, but still registered sensor
        for (i=MAX_KNOWN_SENSORS;i<unknownSensorCount+MAX_KNOWN_SENSORS;i++) {
            if (sensor[i].deviceID.equals(newDeviceID)) {
                return i;
            }
        }
        return i;
    }

    void updateSensor(JSONObject json){
        try {
            String deviceID = null;
            deviceID = json.getString("deviceID");
            int i = GetSensorID(deviceID);
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

    void UpdateSensorUI_NewValue(final int sensorID){
        final TextView innertextviewItem = sensor[sensorID].textviewItem1;
        double val = sensor[sensorID].value1;
        final double val_f = val;
        innertextviewItem.post(new Runnable() {
            public void run() {
                innertextviewItem.setText(String.valueOf(val_f).substring(0,2));
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

    public class Sensor {

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
        public int lowerLimit1 = 0;
        public int upperLimit1 = 0;
        public int lowerLimit2 = 0;
        public int upperLimit2 = 0;

        public Sensor(JSONObject json, InetAddress ip, int sensorID) {

            try {
                if (json.has("deviceID")) {this.deviceID = json.getString("deviceID"); }
                if (json.has("displayTitle1")) {this.displayTitle1 = json.getString("displayTitle1"); }
                if (json.has("metricUnit1")) {this.metricUnit1 = json.getString("metricUnit1"); }
                if (json.has("displayTitle2")) {this.displayTitle2 = json.getString("displayTitle2"); }
                if (json.has("metricUnit2")) {this.metricUnit2 = json.getString("metricUnit2"); }
                if (json.has("lowerLimit1")) {this.lowerLimit1 = json.getInt("lowerLimit1"); }
                if (json.has("upperLimit1")) {this.upperLimit1 = json.getInt("upperLimit1"); }
                if (json.has("lowerLimit2")) {this.lowerLimit2 = json.getInt("lowerLimit2"); }
                if (json.has("upperLimit2")) {this.upperLimit2 = json.getInt("upperLimit2"); }
                this.ip = ip;
                this.sensorID = sensorID;

                switch (sensorID) {
                    case 0 : this.textviewItem1 = LFtemperatureTextView; break;
                    case 1 : this.textviewItem1 = RFtemperatureTextView;
                             this.textviewItem2 = ambientTemperatureTextView; break;
                    case 2 : this.textviewItem1 = LRtemperatureTextView; break;
                    case 3 : this.textviewItem1 = RRtemperatureTextView; break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}