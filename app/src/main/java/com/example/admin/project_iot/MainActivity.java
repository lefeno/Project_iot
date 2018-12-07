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
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    public static Handler mHandler = new Handler();
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_POT_EACH_BLE = 2;
    private static final int MAX_BLE = 2;
    public static ParamPot listAutoPot[] = new ParamPot[MAX_POT_EACH_BLE];
    public static boolean auto[] = new boolean[MAX_POT_EACH_BLE];

    private BluetoothAdapter mBluetoothAdapter = null;
    public static String EXTRA_ADDRESS = "device_address";
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList list = new ArrayList();
    private InputStream msg = null;
    private String code;

    public static BluetoothSocket btSocket = null;
    private boolean isBtnConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String topicWriteHumid = "humid";
    public static final String topicWriteLog = "log";
    public static final String topicRead = "command";
    private DatabaseReference databaseParam;

    private int i = 0;
    private DatabaseReference dbMQTT[] = new DatabaseReference[MAX_POT_EACH_BLE];
    private String cmd;
//    private MqttControl mqttControlRead;
//    private MqttControl mqttControlWriteHumid;
//    private MqttControl mqttControlWriteLog;
    String address = "00:18:E4:00:11:E4";
    public static boolean sendCodeSuccess = false;
    private int timer = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < listAutoPot.length; ++i) {
            listAutoPot[i] = new ParamPot(0, -1, -1);
            auto[i] = false;
        }


        // Register to MQTT
//        try {
//            mqttControlRead = new MqttControl(MainActivity.topicRead, "A", true);
//            mqttControlWriteHumid = new MqttControl(MainActivity.topicWriteHumid, "B", false);
//            mqttControlWriteLog = new MqttControl(MainActivity.topicWriteLog, "C", false);
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
        // Register to Firebase
        databaseParam = FirebaseDatabase.getInstance().getReference("db");

        for (i = 0; i < MAX_POT_EACH_BLE; ++i){
            dbMQTT[i] = FirebaseDatabase.getInstance().getReference("db").child("user0").child(address).child("pot" + (i+1)).child("commands");
            dbMQTT[i].addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Commands key = dataSnapshot.getValue(Commands.class);
                    cmd = key.getKey();
                    checkPayload(cmd);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

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
                // Check if device is a HC-06 bluetooth
                if ((bt.getName()).indexOf("HC-06") > -1) {
                    list.add(bt.getName() + "\n" + bt.getAddress());
//                    Log.d(TAG, "Found");
                }
            }
        } else {
            Log.d(TAG, "No Paired Bluetooth Devices Found");
        }
    }

    private void checkPayload(String payload) {
        if(payload.length() < 21){
            return;
        }
        String tmp;
        int id;
        int humid_max;
        int humid_min;
        tmp = payload.substring(1);
        id = Integer.parseInt(payload.substring(1, 2));
        humid_max = Integer.parseInt(payload.substring(2, 4));
        humid_min = Integer.parseInt(payload.substring(4, 6));
        switch (payload.charAt(0)) {
            case 'B':
                if (tmp.length() == 22) {
                    ParamPot pot = new ParamPot(id, humid_max, humid_min);
                    MainActivity.listAutoPot[id-1] = pot;
                    MainActivity.listAutoPot[id - 1].potAvailable += 1;
                    Log.d(TAG,"List2:" + MainActivity.listAutoPot[id - 1].potAvailable);
                    MainActivity.auto[id-1] = true;
                }
                break;
            case 'C':
                Log.d(TAG,"C");
                if (tmp.length() == 20 && MainActivity.btSocket != null) {
                    MainActivity.sendCodeSuccess = true;
                    try {
                        (MainActivity.btSocket).getOutputStream().write(payload.substring(1,4).getBytes());
                    } catch (IOException e) {
                        Log.d(TAG, "Error");
                    }
                }
                break;
            case 'R':
                if (tmp.length() == 20) {
                    Log.d(TAG,"R");
                    MainActivity.listAutoPot[id-1].setId(0);
                    MainActivity.auto[id - 1] = false;
                }
                break;
            default:
                break;
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
                    address = info.substring(info.length() - 17);
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

//                finish();
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
//                    Log.d("DO",msg.read() + "");
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
                    } else {
                        Log.d(TAG,"Data receive error");
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Error");
                }
            }
            mHandler.postDelayed(mHandleData, 200);
        }
    };

    private void handleCode() {
        if (code.length() == 3) {
            try{
                int d = Integer.parseInt(code);
            } catch (NumberFormatException nfe){
                Log.d(TAG,"Error value");
                return;
            }
            String check = code.substring(1);
            switch (check) {
                case "EE":
                case "FF":
                    sendCodeSuccess = false;
                    timer = 0;
//                    try {
                        String pot = code.substring(0, 1);
                        Commands key = new Commands("0");
                        FirebaseDatabase.getInstance().getReference("db").child("user0").child(address).child("pot1").child("commands").setValue(key);

//                        mqttControlWriteLog.sendmessage(code.substring(1, 2) + pot + "00" + address, MainActivity.topicWriteLog);

                        if (check.equals("EE")) {
                            addParam("Succeed", "logs", pot);
                        } else {
                            addParam("Fail", "logs", pot);
                        }
//                    } catch (MqttException e) {
//                        e.printStackTrace();
//                    }
                    break;
                default:
//                    try {
                        int p = Integer.parseInt(code.substring(0, 1));
                        String data = "D" + code + address;
//                        mqttControlWriteHumid.sendmessage(data, MainActivity.topicWriteHumid);
                        databaseParam.child("user0").child(address).child("pot" + p).child("value").setValue(code.substring(1));
                        addParam(code.substring(1), "data", p + "");
                        if (sendCodeSuccess) {
                            ++timer;
                        }
                        //After 5 min from when timer is trigger
                        if (timer > 50) {
                            timer = 0;
                            Log.d(TAG, "Please send code again");
                            sendCodeSuccess = false;
//                            mqttControlWriteLog.sendmessage("F" + pot + "00" + address, MainActivity.topicWriteLog);
                            addParam("Fail", "logs", p + "");
                        }
                        if (auto[p - 1] && !sendCodeSuccess) {
                            Log.d(TAG, "Auto");
                            ParamPot tmp = null;
                            tmp = listAutoPot[p - 1];
                            Log.d(TAG, "Humid min: " + listAutoPot[p - 1].getHumid_min());
                            int humid = Integer.parseInt(check);
                            if (humid < tmp.getHumid_min()) {
                                String send = Integer.toString(p) + Integer.toString(tmp.getHumid_max());
                                Log.d(TAG, "Send");
                                try {
                                    btSocket.getOutputStream().write(send.getBytes());
                                    sendCodeSuccess = true;
                                } catch (IOException e) {
                                    Log.d(TAG, "Error");
                                }
                                break;
                            }
                        }
//                    } catch (MqttException e) {
//                        e.printStackTrace();
//                    }
                    break;
            }
        } else {
            Log.d(TAG, "Code received invalid");
        }
    }

    private void addParam(String value, String type, String pot) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        String time = formatter.format(date);
//        String id = "0";
        String id = databaseParam.child("user1").child(address).child("pot" + pot).child(type).push().getKey();
        Param param = new Param(id, value, time);
        databaseParam.child("user0").child(address).child("pot" + pot).child(type).child(id).setValue(param);
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

//        try {
//            mqttControlRead.close();
//            mqttControlWriteHumid.close();
//            mqttControlWriteLog.close();
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
    }
}

