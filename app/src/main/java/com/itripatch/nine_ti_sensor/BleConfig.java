package com.itripatch.nine_ti_sensor;

public class BleConfig {
    public static final String START_CONNECTION=".START_CONNECTION";
    public static final String STOP_CONNECTION=".STOP_CONNECTION";
    public static final String CLOSE_GATT=".CLOSE_GATT";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final String ACTION_GATT_CONNECTED = ".ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = ".ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = ".ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = ".ACTION_DATA_AVAILABLE";
    public static final String ACTION_SENDREAD_COUNT = ".ACTION_SENDREAD_COUNT";

    //取九軸
    public static final String RECEIVE_DATA_IDENTIFIER = ".RECEIVE_DATA_IDENTIFIER";
    public static final String RECEIVE_DATA_SETTING_IDENTIFIER = ".RECEIVE_DATA_SETTING_IDENTIFIER";

    public static final int BLE_TOTAL = 1;
    public static final int FIRST = 0;
    public static final String[] BODY = new String[]{"First"};

}
