package com.example.admin.project_iot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */

public class MainActivity extends Activity {
    private Handler mHandler = new Handler();
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_POT_EACH_BLE = 5;
    private static final int MAX_BLE = 2;
    private List<ParamPot> listAutoPot;

    private BluetoothAdapter mBluetoothAdapter = null;
    public static String EXTRA_ADDRESS = "device_address";
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList list = new ArrayList();
    private InputStream msg = null;
    private String code;

    public static BluetoothSocket btSocket = null;
    private boolean isBtnConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    MqttControl mqttControlRead;
    MqttControl mqttControlWriteHumid;
    MqttControl mqttControlWriteLog;

    private static final String topicWriteHumid = "humid";
    private static final String topicWriteLog = "log";
    private static final String topicRead = "command";

    public static DatabaseReference databaseParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register to MQTT
        try {
            mqttControlRead = new MqttControl(topicRead, "ClientRead", true);
            mqttControlWriteHumid = new MqttControl(topicWriteHumid, "ClientHumid", false);
            mqttControlWriteLog = new MqttControl(topicWriteLog, "ClientLog", false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        // Register to Firebase
        databaseParam = FirebaseDatabase.getInstance().getReference("db");

//        //if the device has bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        //Check BLE adapter and scan all paired BLE
        if (mBluetoothAdapter == null) {
            //Show a message that the device has no bluetooth adapter
            Log.d(TAG, "Bluetooth Device Not Available");

            //finish apk
            finish();
        } else {
            if (mBluetoothAdapter.isEnabled()) {

            } else {
                // Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            }

            // Get a list of all HC-06 paired devices
            pairedDevicesList();

            if (list.size() == 0) {
                Log.d(TAG, "No HC-06 paired bluetooth found");
                finish();
            }

            // Connect to all HC06 Bluetooth in paired list
            new ConnectBT().execute();

            mHandler.post(mHandleData);
        }
    }



    // We assume that all bluetooths have been paired with raspberry pi to avoid complexity
    private void pairedDevicesList() {
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                // Get the device's name and the address
                Log.d(TAG, bt.getName() + "\n" + bt.getAddress());
                // Check if device is a HC-06 bluetooth
                if ((bt.getName()).indexOf("HC-06") > -1) {
                    list.add(bt.getName() + "\n" + bt.getAddress());
                    Log.d(TAG, "Found");
                }
            }
        } else {
            Log.d(TAG, "No Paired Bluetooth Devices Found");
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>// UI thread
    {
        // if it's here, it's almost connected
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
        }

        //while the progress dialog is shown, the connection is done in background
        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtnConnected) {
                    //get the mobile bluetooth device
//                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    //connect to the device's address and checks if it's available
                    String info = list.get(0).toString();
                    String address = info.substring(info.length() - 17);
                    Log.d(TAG, address);
                    BluetoothDevice dispositivo = mBluetoothAdapter.getRemoteDevice(address);
                    //create a RFCOMM (SPP) connection
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    //start connection
                    btSocket.connect();
                }
            } catch (IOException e) {
                //if the try failed, u can check the exception here
                Log.d("DEBUG", "Error in connecting btSocket");
                ConnectSuccess = false;
            }
            return null;
        }

        // after the doInBackground, it checks if everything went fine
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!ConnectSuccess) {
                Log.d(TAG, "Connection Failed, Is it a SPP BLuetooth? Try again.");
                finish();
            } else {
                Log.d(TAG, "Connected.");
                isBtnConnected = true;
            }
        }
    }

    private Runnable mHandleData = new Runnable() {
        @Override
        public void run() {
            if (btSocket != null) {
                try {
                    byte[] buffer = new byte[1024];
                    int bytes = 0;
                    code = "";
                    msg = btSocket.getInputStream();
                    if (msg.read() == 'x') {
                        while (buffer[bytes] != 'y') {
                            buffer[bytes] = (byte) msg.read();

                            if (buffer[bytes] == 'y') {
                                code = new String(buffer, 0, bytes);
                                Log.d(TAG, code);
                                handleCode();
                                bytes = 0;
                                code = "";
                            } else {
                                ++bytes;
                            }
                        }
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Error");
                }
            }
            mHandler.postDelayed(mHandleData, 50);
        }
    };

    private void handleCode() {
        if (code.length() == 3 && Integer.parseInt(code) >= 100 && Integer.parseInt(code) <= 918) {
            String check = code.substring(0,2);
            switch (check) {
                case "90":
                case "91":
                    try {
                        String pot = code.substring(2);
                        mqttControlWriteLog.sendmessage(code + "", topicWriteLog);
                        addParam(check,"logs",pot);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    try {
                        String pot = code.substring(2);
                        mqttControlWriteHumid.sendmessage(code + "", topicWriteHumid);
                        addParam(check,"data",pot);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            Log.d(TAG, "Code received invalid");
        }

    }

    private void addParam(String value, String type, String pot){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        String time = formatter.format(date);
        String id = databaseParam.child("user1").child(pot).child(type).push().getKey();
        Param param = new Param(id, value ,time);
        databaseParam.child("user1").child(pot).child(type).child(id).setValue(param);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btSocket != null) {
            //if the socket is busy
            try {
                btSocket.close();//close connection
            } catch (IOException e) {
                Log.d(TAG, "Error");
            }
        }

        try {
            mqttControlRead.close();
            mqttControlWriteHumid.close();
            mqttControlWriteLog.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
