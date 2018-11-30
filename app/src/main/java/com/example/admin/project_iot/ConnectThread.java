package com.example.admin.project_iot;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ConnectThread extends Thread {
    public ConnectThread(String btAddress) {
        Log.d(TAG, "Go to thread");
        address = btAddress;
//        connect(bTDevice,mUUID);
    }

    private BluetoothSocket btSocket;
    private String address;
    private final String TAG = ConnectThread.class.getSimpleName();

    private InputStream inStream;
    private OutputStream outStream;
    private DatabaseReference databaseParam;
    private MqttControl mqttControlRead;
    private MqttControl mqttControlWriteHumid;
    private MqttControl mqttControlWriteLog;

    public boolean connect(BluetoothDevice bTDevice, UUID mUUID) {
        try {
            btSocket = bTDevice.createInsecureRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.d("CONNECTTHREAD", "Could not create RFCOMM socket:" + e.toString());
            return false;
        }
        try {
            Log.d(TAG, "Connect to " + address);
            btSocket.connect();
        } catch (IOException e) {
            Log.d("CONNECTTHREAD", "Could not connect: " + e.toString());
            try {
                btSocket.close();
            } catch (IOException close) {
                Log.d("CONNECTTHREAD", "Could not close connection:" + e.toString());
            } finally {
                return false;
            }
        }
        Log.d(TAG, "Connect to Bluetooth");
        return true;
    }


    public void connect2DB() {
        // Register to MQTT
        try {
            mqttControlRead = new MqttControl(MainActivity.topicRead, "ClientRead", true);
            mqttControlWriteHumid = new MqttControl(MainActivity.topicWriteHumid, "ClientHumid", false);
            mqttControlWriteLog = new MqttControl(MainActivity.topicWriteLog, "ClientLog", false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        // Register to Firebase
        databaseParam = FirebaseDatabase.getInstance().getReference("db");
    }

    public void run() {
        String code = "";
        while (true) {
            try {
                byte[] buffer = new byte[1024];
                int bytes = 0;
                InputStream msg = btSocket.getInputStream();
                if (msg.read() == 'x') {
                    while (buffer[bytes] != 'y') {
                        buffer[bytes] = (byte) msg.read();

                        if (buffer[bytes] == 'y') {
                            code = new String(buffer, 0, bytes);
                            Log.d(TAG, code);
                            handleCode(code);
                            bytes = 0;
                            code = "";
                        } else {
                            ++bytes;
                        }
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
            }
        }
    }

    private void handleCode(String code) {
        if (code.length() == 3 && Integer.parseInt(code) >= 100 && Integer.parseInt(code) <= 918) {
            String check = code.substring(0, 2);
            switch (check) {
                case "90":
                case "91":
                    try {
                        String pot = code.substring(2);
                        mqttControlWriteLog.sendmessage(code + "", MainActivity.topicWriteLog);
//                        addParam(check,"logs",pot);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    try {
                        String pot = code.substring(2);
                        mqttControlWriteHumid.sendmessage(code + "", MainActivity.topicWriteHumid);
//                        addParam(check,"data",pot);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            Log.d(TAG, "Code received invalid");
        }
    }

    public void cancel() {
        try {
            btSocket.close();
            mqttControlRead.close();
            mqttControlWriteHumid.close();
            mqttControlWriteLog.close();
        } catch (IOException e) {
            Log.d("CONNECTTHREAD", "Could not close connection:" + e.toString());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
