package com.racedash;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.SparseArray;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TPMS_scanner {

    public class TPMSDevice {
        public String deviceName;
        public String deviceID; // LF, RF, LR, RR
        public int wheelPos; // LF, RF, LR, RR
        public Timestamp lastSampleTime = new Timestamp(System.currentTimeMillis());;
        public double pressureBar = 0.0;
        public double temperature = 0.0;
        public int battery = 0;
        public boolean flat = false;
        public double rssi = 0.0;

        public TPMSDevice() {
             deviceName = "NOT SET";
             pressureBar = 0.0;
             temperature = 0.0;
             battery = 0;
             flat = false;
             rssi = 0.0;
        }
    }

    public final static int MAX_TPMS_DEVICES = 4;
    public TPMSDevice[] tpmsDevice;

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;
    private Handler handler;
    private boolean scanning = false;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public TPMS_scanner(Context ctx) {
        // Initializes Bluetooth adapter.
        final BluetoothManager btManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = null;
        if (btManager != null) {
            btAdapter = btManager.getAdapter();
            btScanner = btAdapter.getBluetoothLeScanner();
        }
        handler = new Handler();
        tpmsDevice = new TPMSDevice[MAX_TPMS_DEVICES];

        for (int i=0; i< MAX_TPMS_DEVICES; i++){
            tpmsDevice[i] = new TPMSDevice();
            tpmsDevice[i].wheelPos = i+1;
            if (i==0) {tpmsDevice[i].deviceID = "LF";}
            if (i==1) {tpmsDevice[i].deviceID = "RF";}
            if (i==2) {tpmsDevice[i].deviceID = "LR";}
            if (i==3) {tpmsDevice[i].deviceID = "RR";}
        }
        tpmsDevice[1].deviceName = "TPMS";
    }

    private static final long SCAN_PERIOD = 5000;  //ms


    public void scanLeDevice() {

        // https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();

        List<ScanFilter> filters = null;

        // Filter on UUID
/*        UUID BLP_SERVICE_UUID = UUID.fromString("0000fbb0-0000-1000-8000-00805f9b34fb");
        UUID[] serviceUUIDs = new UUID[]{BLP_SERVICE_UUID};
        if(serviceUUIDs != null) {
            filters = new ArrayList<>();
            for (UUID serviceUUID : serviceUUIDs) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(serviceUUID))
                        .build();
                filters.add(filter);
            }
        }
*/

        // Filter on deviceName
        filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName("TPMS_C03")
                .build();
        filters.add(filter);


        if(btScanner != null) {
            if (!scanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        btScanner.stopScan(leScanCallback);
                    }
                }, SCAN_PERIOD);

                scanning = true;
                btScanner.startScan(filters, scanSettings, leScanCallback);
            } else {
                scanning = false;
                btScanner.stopScan(leScanCallback);
            }
        }
    }

    private int GetWheelPos(String deviceName){
        char pos = deviceName.charAt(4);
        return (pos - 48);
    }

    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    SparseArray<byte[]> manufacturerData = result.getScanRecord().getManufacturerSpecificData();
                    String deviceName = result.getScanRecord().getDeviceName();
                    //TODO: here we could check that it's our own sensor through parsing deviceName
                    int wheelPos = GetWheelPos(deviceName);
                    DecodeTPMSData(manufacturerData, wheelPos);
                    tpmsDevice[wheelPos-1].rssi = result.getRssi();
                }
            };


    public static int toInt(byte[] bytes, int offset) {
        int ret = 0;
        for (int i=0; i<4 && i+offset<bytes.length; i++) {
            ret <<= 8;
            ret |= (int)bytes[i] & 0xFF;
        }
        return ret;
    }

    private void DecodeTPMSData(SparseArray<byte[]> manf_data, int wheelPos){
        // http://forum.espruino.com/conversations/357349/
        byte[] data;
        ArrayList<String> ar = new ArrayList<String>();

        data = manf_data.get(manf_data.keyAt(0));
        int i = wheelPos - 1;

        tpmsDevice[i].battery = data[14];  // pct
        tpmsDevice[i].flat = (data[15] == 1);
        int temperature = (data[13] << 24) + (data[12] << 16) + (data[11] << 8) + (data[10] << 0);
        tpmsDevice[i].temperature = (float) temperature / 100;
        int pressure =     (data[9] << 24) + (data[8] << 16) +  (data[7] << 8) +  (data[6] << 0);
        tpmsDevice[i].pressureBar = (double) pressure / 100000.0 *(1.7/1.05);  // adjustment factor = K_hokus
        tpmsDevice[i].lastSampleTime = new Timestamp(System.currentTimeMillis());

        // for printout for debugging
        String s;
        ar.add("DATA READING   ");
        for (int j=0; j<16; j++) {
            ar.add(" " + String.format("%02X", data[j]));
        }
        ar.add(String.format("   temp=%d  pres=%d  batt=%d", temperature, pressure, tpmsDevice[i].battery));
        System.out.println(ar);
    }

}
