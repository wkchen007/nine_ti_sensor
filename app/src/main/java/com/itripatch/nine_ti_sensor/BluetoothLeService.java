package com.itripatch.nine_ti_sensor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by A50584 on 2017/2/14.
 */

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private String ACTION_GATT_CONNECTED = null;
    private String ACTION_GATT_DISCONNECTED = null;
    private String ACTION_GATT_SERVICES_DISCOVERED = null;
    private String ACTION_DATA_AVAILABLE = null;
    private String RECEIVE_DATA_IDENTIFIER = null;
    private String RECEIVE_DATA_SETTING_IDENTIFIER = null;
    private String ACTION_SENDREAD_COUNT = null;

    private int STATE_DISCONNECTED = BleConfig.STATE_DISCONNECTED;
    private int STATE_CONNECTING = BleConfig.STATE_CONNECTING;
    private int STATE_CONNECTED = BleConfig.STATE_CONNECTED;

    private String START_CONNECTION = null;
    private String STOP_CONNECTION = null;
    private String CLOSE_GATT = null;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private int BLE_TOTAL = BleConfig.BLE_TOTAL;
    private String[] mBluetoothDeviceName = new String[BLE_TOTAL];
    private String[] mBluetoothDeviceType = new String[BLE_TOTAL];
    private String[] mBluetoothDeviceAddress = new String[BLE_TOTAL];
    private Date[] mFirstConnectTime = new Date[BLE_TOTAL];
    private Date[] mGetReadTime = new Date[BLE_TOTAL];
    private int[] mSendReadCount = new int[BLE_TOTAL];
    private int[] mGetReadCount = new int[BLE_TOTAL];
    private int[] mGetRssi = new int[BLE_TOTAL];
    private int RSSI;
    private BluetoothGatt[] mBluetoothGatt = new BluetoothGatt[BLE_TOTAL];
    private int[] mConnectionState = new int[BLE_TOTAL];
    private BluetoothGattCallback[] mGattCallback = new BluetoothGattCallback[BLE_TOTAL];
    private ArrayList<ArrayList<BluetoothGattCharacteristic>>[] mGattCharacteristics = (ArrayList<ArrayList<BluetoothGattCharacteristic>>[]) new ArrayList[BLE_TOTAL];
    private int mSendReadPeriod = 200;
    private Boolean[] startReadTimerOn = new Boolean[BLE_TOTAL];
    private TimerTask[] readtask = new TimerTask[BLE_TOTAL];
    private Timer[] readtimer = new Timer[BLE_TOTAL];
    private String[] BODY = null;

    private BroadcastReceiver BLEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (START_CONNECTION.equals(intent.getAction())) {
                Bundle message = intent.getExtras();
                int index = message.getInt("index");
                String deviceName = message.getString("deviceName");
                String deviceAddress = message.getString("deviceAddress");
                String deviceType = message.getString("deviceType");
                String sendReadPeriod = message.getString("sendReadPeriod");
                String[][] BleConfig = {
                        {deviceName, deviceAddress, deviceType, sendReadPeriod}
                };
                connect(index, BleConfig[index]);
            } else if (STOP_CONNECTION.equals(intent.getAction())) {
                Bundle message = intent.getExtras();
                int index = message.getInt("index");
                disconnect(index);

            } else if (CLOSE_GATT.equals(intent.getAction())) {
                close();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public BluetoothLeService() {

    }

    @Override
    public void onCreate() {
        if (initialize()) {
            Log.i(TAG, "Unable to initialize Bluetooth");
        }
        START_CONNECTION = BleConfig.START_CONNECTION;
        STOP_CONNECTION = BleConfig.STOP_CONNECTION;
        CLOSE_GATT = BleConfig.CLOSE_GATT;

        registerReceiver(BLEReceiver, new IntentFilter(START_CONNECTION));
        registerReceiver(BLEReceiver, new IntentFilter(STOP_CONNECTION));
        registerReceiver(BLEReceiver, new IntentFilter(CLOSE_GATT));
        Log.i("KKK", "onCreate: HIHI");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(BLEReceiver);
        Log.i("KKK", "onDestroy: byebye");
        super.onDestroy();
    }

    private void setBluetoothGattCallback(final int index) {
        // Implements callback methods for GATT events that the app cares about.  For example,
        // connection change and services discovered.
        mGattCallback[index] = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String intentAction;
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    intentAction = BODY[index] + ACTION_GATT_CONNECTED;
                    mConnectionState[index] = STATE_CONNECTED;
                    mFirstConnectTime[index] = new Date(System.currentTimeMillis());
                    broadcastUpdate(intentAction, new SimpleDateFormat("HH:mm:ss").format(mFirstConnectTime[index]));
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt[index].discoverServices());
                    Log.i("KKK", BODY[index] + ACTION_GATT_CONNECTED + " " + mBluetoothDeviceAddress[index]);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = BODY[index] + ACTION_GATT_DISCONNECTED;
                    mConnectionState[index] = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction, null);
                    Log.i("KKK", BODY[index] + ACTION_GATT_DISCONNECTED + " " + mBluetoothDeviceAddress[index]);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    final byte[] data = characteristic.getValue();
                    //CH Add - Get RSSI value
                    mBluetoothGatt[index].readRemoteRssi();   //啟動BLE開始一直送RSSI
                    mGetRssi[index] = RSSI;
                    Log.i("KKK", BODY[index] + RECEIVE_DATA_IDENTIFIER + " Y " + mBluetoothDeviceAddress[index] + " " + Arrays.toString(data));
//                    Log.i("KKK", "RECEIVE_DATA " + Arrays.toString(data));
                    mGetReadCount[index]++;
                    mGetReadTime[index] = new Date(System.currentTimeMillis());
                    broadcastUpdate(index, BODY[index] + RECEIVE_DATA_IDENTIFIER, characteristic);
                } else {
                    Log.i("KKK", BODY[index] + RECEIVE_DATA_IDENTIFIER + " N " + mBluetoothDeviceAddress[index]);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    String type = getBluetoothDeviceType(index);
                    switch (type) {
                        case "TI": {
                            setGattCharacteristics(index);
                            setTISensor(index);
                            delayMS(400);
                            startReadTimer(index, type);
                            break;
                        }
                        case "ITRI": {
                            setGattCharacteristics(index);
                            startReadTimer(index, type);
                            break;
                        }
                    }
                    broadcastUpdate(BODY[index] + ACTION_GATT_SERVICES_DISCOVERED, null);
                    Log.i("KKK", BODY[index] + ACTION_GATT_SERVICES_DISCOVERED + " Y " + mBluetoothDeviceAddress[index]);
                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                    Log.i("KKK", BODY[index] + ACTION_GATT_SERVICES_DISCOVERED + " N " + mBluetoothDeviceAddress[index]);
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                Log.i("TTT", "onReadRemoteRssi: " + rssi);
                RSSI =rssi;
            }
        };
    }

    private void broadcastUpdate(final String action, final String data) {
        final Intent intent = new Intent(action);
        if (data != null) {
            Bundle message = new Bundle();
            message.putString("data", data);
            intent.putExtras(message);
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final int index, final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        // For all other profiles, writes the data formatted in HEX.
        // for TI
        if (UUID.fromString("f000aa81-0451-4000-b000-000000000000").equals(characteristic.getUuid())) {

            final byte[] data = characteristic.getValue();
            int i;
            String hexString = "";
            String tempString = "";

            for (i = 0; i < data.length; i++) {

                Byte b = new Byte(data[i]);
                int xxx = b.intValue();

//                String hex_value =  String.format("%.2s", Integer.toHexString(xxx));
                String hex_value = Integer.toHexString(xxx);

                switch (hex_value.length()) {
                    case 1:
                        hex_value = "0" + hex_value;
                        break;
                    case 8:
                        hex_value = hex_value.substring(6, 8);
                        break;
                }

                if ((i % 2) == 0) {

                    tempString = tempString + hex_value;
                } else {

                    if (i == data.length - 1) {

                        tempString = hex_value + tempString;
                    } else {

                        tempString = hex_value + tempString + ",";
                    }
                    hexString = hexString + tempString;
                    tempString = "";


                }

            }

            if (data != null && data.length > 0) {

                dataParseTI(index, action, hexString);
            }
        }
        // for ITRI 九軸、緊急bit、電量、加速度解析度
        else if (UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb").equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            int i;
            String hexString = "";
            String tempString = "";
            for (i = 0; i < data.length - 3; i++) {

                Byte b = new Byte(data[i]);
                int xxx = b.intValue();

//                    String hex_value = String.format("%.2s", Integer.toHexString(xxx));
                String hex_value = Integer.toHexString(xxx);

                switch (hex_value.length()) {
                    case 1:
                        hex_value = "0" + hex_value;
                        break;
                    case 8:
                        hex_value = hex_value.substring(6, 8);
                        break;
                }

                if ((i % 2) == 0) {

                    tempString = tempString + hex_value;
                } else {

                    if (i == data.length - 1) {

                        tempString = hex_value + tempString;
                    } else {

                        tempString = hex_value + tempString + ",";
                    }
                    hexString = hexString + tempString;
                    tempString = "";


                }

            }

            if (data != null && data.length > 0) {

                dataParseITRI(index, action, hexString.substring(0, hexString.length() - 1), data[18] + "", data[19] + "", data[20] + "");
            }
        }
    }

    // for TI
    public void dataParseTI(final int index, final String action, final String data) {


        if (data != null) {
            if (data.length() == 44) {

                Intent i = new Intent(action);


                String[] ns = data.split(",");

                String realNumberString = "";
                int j;
                for (j = 0; j < ns.length; j++) {


                    short shortNumber = (short) Integer.parseInt(ns[j], 16);

                    if (j == ns.length - 1) {

                        realNumberString += String.valueOf(shortNumber);
                    } else {

                        realNumberString += String.valueOf(shortNumber) + ",";
                    }

                }//end of for
                String getReadTime = new SimpleDateFormat("HH:mm:ss").format(mGetReadTime[index]);
                realNumberString = realNumberString + "," + mGetReadCount[index] + "," + getReadTime;
                i.putExtra(action, realNumberString);
                sendBroadcast(i);

            } else {
                String body = action.split("\\.")[0];
                Intent i = new Intent(body + RECEIVE_DATA_SETTING_IDENTIFIER);
                i.putExtra(body + RECEIVE_DATA_SETTING_IDENTIFIER, data);
                sendBroadcast(i);
            }
        }//end of  data != null
    }//end of  dataParse


    // for ITRI
    public void dataParseITRI(final int index, final String action, final String data, final String emBit, final String power, final String accScale) {


        if (data != null) {
            if (data.length() == 44) {

                Intent i = new Intent(action);


                String[] ns = data.split(",");

                String realNumberString = "";
                int j;
                for (j = 0; j < ns.length; j++) {


                    short shortNumber = (short) Integer.parseInt(ns[j], 16);

                    if (j == ns.length - 1) {

                        realNumberString += String.valueOf(shortNumber);
                    } else {

                        realNumberString += String.valueOf(shortNumber) + ",";
                    }

                }//end of for
                String getReadTime = new SimpleDateFormat("HH:mm:ss").format(mGetReadTime[index]);
                realNumberString = realNumberString + "," + emBit + "," + power + "," + accScale + "," + mGetReadCount[index] + "," + getReadTime;
                i.putExtra(action, realNumberString);
                sendBroadcast(i);

            } else {
                String body = action.split("\\.")[0];
                Intent i = new Intent(body + RECEIVE_DATA_SETTING_IDENTIFIER);
                i.putExtra(body + RECEIVE_DATA_SETTING_IDENTIFIER, data);
                sendBroadcast(i);
            }
        }//end of  data != null
    }//end of  dataParse

    public void setBluetoothSendReadPeriod(final int sendReadPeriod) {
        mSendReadPeriod = sendReadPeriod;
    }

    public void setBluetoothDeviceName(final int index, final String name) {
        mBluetoothDeviceName[index] = name;
    }

    public void setBluetoothDeviceAddress(final int index, final String address) {
        mBluetoothDeviceAddress[index] = address;
    }

    public void setBluetoothDeviceType(final int index, final String type) {
        mBluetoothDeviceType[index] = type;
    }

    public String getBluetoothDeviceType(final int index) {
        return mBluetoothDeviceType[index];
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public boolean connect(final int index, final String[] ble) {
        String name = ble[0];
        String address = ble[1];
        String type = ble[2];
        int sendReadPeriod = Integer.parseInt(ble[3]);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress[index] != null && address.equals(mBluetoothDeviceAddress[index])
                && mBluetoothGatt[index] != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            setBluetoothSendReadPeriod(sendReadPeriod);
            if (mBluetoothGatt[index].connect()) {
                Log.i("TTT", "reconnect: ");
                mConnectionState[index] = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        setBluetoothGattCallback(index);

        BODY = BleConfig.BODY;
        ACTION_GATT_CONNECTED = BleConfig.ACTION_GATT_CONNECTED;
        ACTION_GATT_DISCONNECTED = BleConfig.ACTION_GATT_DISCONNECTED;
        ACTION_GATT_SERVICES_DISCOVERED = BleConfig.ACTION_GATT_SERVICES_DISCOVERED;
        ACTION_DATA_AVAILABLE = BleConfig.ACTION_DATA_AVAILABLE;
        RECEIVE_DATA_IDENTIFIER = BleConfig.RECEIVE_DATA_IDENTIFIER;
        RECEIVE_DATA_SETTING_IDENTIFIER = BleConfig.RECEIVE_DATA_SETTING_IDENTIFIER;
        ACTION_SENDREAD_COUNT = BleConfig.ACTION_SENDREAD_COUNT;
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt[index] = device.connectGatt(this, false, mGattCallback[index]);
        Log.d(TAG, "Trying to create a new connection.");
        setBluetoothDeviceAddress(index, address);
        setBluetoothDeviceName(index, name);
        setBluetoothDeviceType(index, type);
        setBluetoothSendReadPeriod(sendReadPeriod);
        mConnectionState[index] = STATE_CONNECTING;
        startReadTimerOn[index] = false;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(final int index) {
        if (mBluetoothAdapter == null || mBluetoothGatt[index] == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        stopReadTimer(index);
        mBluetoothGatt[index].disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        for (int i = 0; i < mBluetoothGatt.length; i++) {
            if (mBluetoothGatt[i] == null) {
                break;
            }
            mBluetoothGatt[i].close();
            mBluetoothGatt[i] = null;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final int index, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt[index] == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt[index].readCharacteristic(characteristic);
    }

    public void writeCharacteristic(final int index, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt[index] == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt[index].writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(final int index, final boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt[index] == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        String systemId = "0000fff4-0000-1000-8000-00805f9b34fb";
        for (int i = 0; i < mGattCharacteristics[index].size(); i++) {
            for (int j = 0; j < mGattCharacteristics[index].get(i).size(); j++) {
                if (mGattCharacteristics[index].get(i).get(j).getUuid().toString().equals(systemId)) {
                    final BluetoothGattCharacteristic characteristic = mGattCharacteristics[index].get(i).get(j);
                    mBluetoothGatt[index].setCharacteristicNotification(characteristic, enabled);
                    String ClientCharacteristicConfig = "00002902-0000-1000-8000-00805f9b34fb";
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(ClientCharacteristicConfig));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt[index].writeDescriptor(descriptor);
                }
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */

    public List<BluetoothGattService> getSupportedGattServices(final int index) {
        if (mBluetoothGatt[index] == null) return null;

        return mBluetoothGatt[index].getServices();
    }

    public void setGattCharacteristics(final int index) {
        mGattCharacteristics[index] = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        List<BluetoothGattService> gattServices = getSupportedGattServices(index);

        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
            }
            mGattCharacteristics[index].add(charas);
        }
    }

    public void setTISensor(final int index) {
        BluetoothGattCharacteristic pWriteCharacteristic;
        //for TI
        String systemId = "f000aa82-0451-4000-b000-000000000000";

        for (int i = 0; i < mGattCharacteristics[index].size(); i++) {
            for (int j = 0; j < mGattCharacteristics[index].get(i).size(); j++) {
                if (mGattCharacteristics[index].get(i).get(j).getUuid().toString().equals(systemId)) {

                    pWriteCharacteristic = mGattCharacteristics[index].get(i).get(j);
                    byte[] dataBytes = new byte[2];
                    dataBytes[0] = (byte) 0xff;          //0xff 的 bit 7 Wake-On-Motion Enable
                    dataBytes[1] = 0x00;          //設定三軸加速路的Base 0=2G, 1=4G, 2=8G, 3=16G

                    pWriteCharacteristic.setValue(dataBytes);
                    pWriteCharacteristic.setWriteType(1);
                    writeCharacteristic(index, pWriteCharacteristic);
                }
            }
        }
        systemId = "f000aa83-0451-4000-b000-000000000000";
        for (int i = 0; i < mGattCharacteristics[index].size(); i++) {
            for (int j = 0; j < mGattCharacteristics[index].get(i).size(); j++) {
                if (mGattCharacteristics[index].get(i).get(j).getUuid().toString().equals(systemId)) {

                    pWriteCharacteristic = mGattCharacteristics[index].get(i).get(j);
                    byte[] dataBytes = new byte[1];
                    dataBytes[0] = (byte) 0x0a;

                    pWriteCharacteristic.setValue(dataBytes);
                    pWriteCharacteristic.setWriteType(1);
                    writeCharacteristic(index, pWriteCharacteristic);
                }
            }
        }
    }

    public void getTISensor(final int index) {
        mSendReadCount[index]++;
        broadcastUpdate(BODY[index] + ACTION_SENDREAD_COUNT, mSendReadCount[index]+"");

//        delayMS(200);
        String systemId = "f000aa81-0451-4000-b000-000000000000"; //for TI
        for (int i = 0; i < mGattCharacteristics[index].size(); i++) {
            for (int j = 0; j < mGattCharacteristics[index].get(i).size(); j++) {
                if (mGattCharacteristics[index].get(i).get(j).getUuid().toString().equals(systemId)) {
                    final BluetoothGattCharacteristic characteristic = mGattCharacteristics[index].get(i).get(j);
                    readCharacteristic(index, characteristic);
                    Log.i("KKK", "run: " + BODY[index]);
//                    Log.i("KKK", "read BLE " + getBluetoothDeviceAddress(index));
                }
            }
        }
    }


    public void getITRISensor(final int index) {
        mSendReadCount[index]++;
        broadcastUpdate(BODY[index] + ACTION_SENDREAD_COUNT, mSendReadCount[index]+"");
//        delayMS(200);
        String systemId = "0000fff5-0000-1000-8000-00805f9b34fb"; //for ITRI
        for (int i = 0; i < mGattCharacteristics[index].size(); i++) {
            for (int j = 0; j < mGattCharacteristics[index].get(i).size(); j++) {
                if (mGattCharacteristics[index].get(i).get(j).getUuid().toString().equals(systemId)) {
                    final BluetoothGattCharacteristic characteristic = mGattCharacteristics[index].get(i).get(j);
                    readCharacteristic(index, characteristic);
                    Log.i("KKK", "run: " + BODY[index]);
//                    Log.i("KKK", "read BLE " + getBluetoothDeviceAddress(index));
                }
            }
        }
    }

    public void delayMS(int pDelayValue) {

        try {
            Thread.sleep(pDelayValue); // do nothing for 1000 miliseconds (1 second)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startReadTimer(final int index, final String type) {

        if (readtimer[index] == null) {
            readtimer[index] = new Timer();
        }

        if (readtask[index] == null) {
            readtask[index] = new TimerTask() {
                public void run() {
                    switch (type) {
                        case "TI": {
                            getTISensor(index);
                            break;
                        }
                        case "ITRI": {
                            getITRISensor(index);
                            break;
                        }
                    }
                }
            };
        }
        if (readtimer[index] != null && readtask[index] != null && !startReadTimerOn[index]) {

            readtimer[index].schedule(readtask[index], 1, mSendReadPeriod);   //Period must = 200 ms to Read Bluetooth Data
            startReadTimerOn[index] = true;
            mSendReadCount[index] = 0;
            mGetReadCount[index] = 0;
        }
    }

    public void stopReadTimer(final int index) {

        if (readtimer[index] != null) {
            readtimer[index].cancel();
            readtimer[index] = null;
        }

        if (readtask[index] != null) {
            readtask[index].cancel();
            readtask[index] = null;
        }

        startReadTimerOn[index] = false;
    }

}
