package com.example.admin.project_iot;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.List;

public class MqttControl implements MqttCallback {
    private static final String username = "uaijweiy";
    private static final String password = "Fsz3BK1eUXev";
    private static final String serveruri = "tcp://m10.cloudmqtt.com:13170";
    private String clientId = "raspberry";
    private String toPic = "com";
    private static final String TAG = MqttControl.class.getSimpleName();
    private static final int QOs = 1;
    private MqttClient client;
    private boolean MqttRead = true;
//    public static List<ParamPot> listAutoPot;

    public MqttControl(String topic, String id, boolean read) throws MqttException {
        clientId = id;
        toPic = topic;
        MqttRead = read;
        subcribeToTopic();
    }

    public void subcribeToTopic() throws MqttException {
        client = new MqttClient(serveruri, clientId, new MemoryPersistence());
        client.connect(connectOptionchoice());
        if (MqttRead) {
            client.subscribe(toPic);
        }
        client.setCallback(this);
        Log.d(TAG, "nice connected ~");

    }

    private static MqttConnectOptions connectOptionchoice() {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setUserName(username);
        connectOptions.setPassword(password.toCharArray());
        connectOptions.setCleanSession(true);
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setKeepAliveInterval(200);
        return connectOptions;
    }

    public void close() throws MqttException {
        client.disconnect();
        client.close();
        Log.d(TAG, "client  disconnected ! ");
    }

    public void sendmessage(String data, String topic) throws MqttException {
        if (!client.isConnected()) {
            subcribeToTopic();
        }
        MqttMessage message = new MqttMessage(data.getBytes());
        message.setQos(QOs);
        client.publish(topic, message);
    }

    @Override
    public void connectionLost(Throwable cause) {
        try {
//            MainActivity.mHandler.removeCallbacksAndMessages(null);
            Log.d(TAG, "Reconnecting ~ ................(-______-)#......");
            client.connect(MqttControl.connectOptionchoice());
            Log.d(TAG, "Connected success ! ");


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //Receive code to control arduino
    @Override
    public void messageArrived(String topic, MqttMessage message) throws IOException, MqttException {
        String payload = new String(message.getPayload());
        Log.d(TAG, "message received: " + payload);
        // Check payload
        checkPayload(payload);
    }

    private void checkPayload(String payload) {
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

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

}


