package com.droid.ws;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class comm {

    public static int UDP_PORT = 1026;  // same port for both directions
    public static DatagramSocket socket;
    public static WifiManager wifi = null; //new1
    public static DhcpInfo dhcp = null; //new1

    public static final int CMD_ASK_HOST_TO_JOIN_NETWORK=1;
    public static final int CMD_OK_TO_JOIN_NETWORK=2;
    public static final int CMD_READ_SENSOR_DATA=3;
    public static final int CMD_SENSOR_DATA_REPLY=4;
    public static final int CMD_GOTO_DEEP_SLEEP=5;

    public static void sendUdpDataJSON(JSONObject json, InetAddress ip) {
        String jsonStr = json.toString();
        byte[] jsonByteArray = jsonStr.getBytes();

        try {

            final DatagramPacket packet;
            int paramsLength = jsonStr.length();
            byte data[] = new byte[paramsLength + 1];
            System.arraycopy(jsonByteArray, 0, data, 0, jsonStr.length());
            packet = new DatagramPacket(data, data.length, ip, UDP_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void CreateAndSendCmd_OkToJoin(String deviceID, InetAddress ip) {
        JSONObject json = new JSONObject();
        try {
            json.put("commandID", CMD_OK_TO_JOIN_NETWORK);
            json.put("deviceID", deviceID);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sendUdpDataJSON(json, ip);
    }

    public static void CreateAndSendCmd_GetSensorReadings(MainActivity.Sensor sensor) {
        JSONObject json = new JSONObject();
        try {
            json.put("commandID", CMD_READ_SENSOR_DATA);
            json.put("deviceID", sensor.deviceID);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sendUdpDataJSON(json, sensor.ip);
    }

    public static void CreateAndSendCmd_GotoDeepSleep(MainActivity.Sensor sensor, int secondsToSleep) {
        JSONObject json = new JSONObject();
        try {
            json.put("commandID", CMD_GOTO_DEEP_SLEEP);
            json.put("deviceID", sensor.deviceID);
            json.put("secondsToSleep", secondsToSleep);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sendUdpDataJSON(json, sensor.ip);
    }

    InetAddress getBroadcastAddress() throws IOException {
        //WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //old1        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // getLocalIPAddress(wifi);
        //old1        DhcpInfo dhcp = wifi.getDhcpInfo();
        // no DHCP info...can't do nothing more
        if (dhcp == null) {
            return null;
        }
        int ipAddress = dhcp.gateway;
        byte[] ipQuads = new byte[4];
        ipQuads[0] = (byte) (ipAddress & 0xFF);
        ;
        ipQuads[1] = (byte) ((ipAddress >> 8) & 0xFF);
        ipQuads[2] = (byte) ((ipAddress >> 16) & 0xFF);
        ipQuads[3] = (byte) ((ipAddress >> 24) & 0xFF);

        // 192.168.43.255
        ipQuads[0] = (byte) -64;
        ipQuads[1] = (byte) -88;
        ipQuads[2] = (byte) -213;
        ipQuads[3] = (byte) (0xFF); // -246;

    /* 192.168.4.1 = [ -64, -88, 4, 1] */
        return InetAddress.getByAddress(ipQuads);
    }

    void sendUdpDataSimple (InetAddress ip){
        try {
            final DatagramPacket packet;
            byte cmdStr[] = new byte[2];
            cmdStr[0] = (byte) 50;
            cmdStr[1] = 0;

            packet = new DatagramPacket(cmdStr, 2,
                    ip, UDP_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    DatagramPacket receiveUdpData() {
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            if (socket == null) {
                return null;
            }
            socket.receive(packet);
            //String inStr = new String(packet.getData(),0, data.length);
            //Log.i("HD:receiveUdpData", new String(packet.getData()).trim());
            return packet;

        } catch (IOException e) {
            Log.e("HD:receiveUdpData", "Error occurred when receiving UDP data on port: " + UDP_PORT);
            e.printStackTrace();
            return null;
        }
    }

}
