package com.itripatch.nine_ti_sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private String START_CONNECTION = BleConfig.START_CONNECTION;
    private String STOP_CONNECTION = BleConfig.STOP_CONNECTION;
    private String CLOSE_GATT = BleConfig.CLOSE_GATT;
    private String[] BODY = BleConfig.BODY;
    private String ACTION_GATT_CONNECTED = BleConfig.ACTION_GATT_CONNECTED;
    private String ACTION_GATT_DISCONNECTED = BleConfig.ACTION_GATT_DISCONNECTED;
    private String ACTION_GATT_SERVICES_DISCOVERED = BleConfig.ACTION_GATT_SERVICES_DISCOVERED;
    private String RECEIVE_DATA_IDENTIFIER = BleConfig.RECEIVE_DATA_IDENTIFIER;
    private String RECEIVE_DATA_SETTING_IDENTIFIER = BleConfig.RECEIVE_DATA_SETTING_IDENTIFIER;
    private String ACTION_SENDREAD_COUNT = BleConfig.ACTION_SENDREAD_COUNT;
    private final int FIRST = BleConfig.FIRST;

    private String mDeviceName = null;
    private String mDeviceAddress = null;
    private String mDeviceType = null;
    private String mSendReadPeriod = null;

    private TextView device_name, device_address, device_status;
    private TextView[] show_aXYZ = new TextView[3], show_gXYZ = new TextView[3], show_mXYZ = new TextView[3];
    private TextView show_emBit, show_power, show_accScale, show_firstConn, show_sendReadCount, show_getReadCount, show_getReadTime, show_sendReadPeriod;
    private Button appFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        Bundle message = getIntent().getExtras();
        String action = message.getString("action");
        mDeviceName = message.getString("deviceName");
        mDeviceAddress = message.getString("deviceAddress");
        mDeviceType = message.getString("deviceType");
        mSendReadPeriod = message.getString("sendReadPeriod");
        bodyAction(action, FIRST, null);

        device_name = (TextView) findViewById(R.id.device_name);
        device_name.setText(mDeviceName);
        device_address = (TextView) findViewById(R.id.device_address);
        device_address.setText(mDeviceAddress);
        device_status = (TextView) findViewById(R.id.device_status);
        device_status.setText(R.string.connecting);
        show_aXYZ[0] = (TextView) findViewById(R.id.aX_value);
        show_aXYZ[1] = (TextView) findViewById(R.id.aY_value);
        show_aXYZ[2] = (TextView) findViewById(R.id.aZ_value);
        show_gXYZ[0] = (TextView) findViewById(R.id.gX_value);
        show_gXYZ[1] = (TextView) findViewById(R.id.gY_value);
        show_gXYZ[2] = (TextView) findViewById(R.id.gZ_value);
        show_mXYZ[0] = (TextView) findViewById(R.id.mX_value);
        show_mXYZ[1] = (TextView) findViewById(R.id.mY_value);
        show_mXYZ[2] = (TextView) findViewById(R.id.mZ_value);
        show_emBit = (TextView) findViewById(R.id.emBit_value);
        show_power = (TextView) findViewById(R.id.power_value);
        show_accScale = (TextView) findViewById(R.id.accScale_value);
        show_firstConn = (TextView) findViewById(R.id.firstConn_value);
        show_sendReadCount = (TextView) findViewById(R.id.sendReadCount_value);
        show_getReadCount = (TextView) findViewById(R.id.getReadCount_value);
        show_getReadTime = (TextView) findViewById(R.id.getReadTime_value);
        show_sendReadPeriod = (TextView) findViewById(R.id.sendReadPeriod_value);
        show_sendReadPeriod.setText(mSendReadPeriod + " ms");

        appFinish = (Button) findViewById(R.id.appFinish);
        appFinish.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                bodyAction("disconnect", FIRST, null);
                bodyAction("close", FIRST, null);
                setResult(RESULT_OK);
                finish();
            }
        });

    }

    private void bodyAction(final String action, final int body, final String data) {
        switch (action) {
            case "connect": {
                Intent intent = new Intent(START_CONNECTION);
                Bundle message = new Bundle();
                message.putInt("index", body);
                message.putString("deviceName", mDeviceName);
                message.putString("deviceAddress", mDeviceAddress);
                message.putString("deviceType", mDeviceType);
                message.putString("sendReadPeriod", mSendReadPeriod);
                intent.putExtras(message);
                sendBroadcast(intent);
                break;
            }
            case "disconnect": {
                Intent intent = new Intent(STOP_CONNECTION);
                Bundle message = new Bundle();
                message.putInt("index", body);
                intent.putExtras(message);
                sendBroadcast(intent);
                break;
            }

            case "close": {
                Intent intent = new Intent(CLOSE_GATT);
                sendBroadcast(intent);
                break;
            }

            case "readTI": {
                String[] cutString = data.split(",");
                float a_x, a_y, a_z;
                int scale = 16384; //2G:16384 4G:8192 8G:4096
                a_x = Float.valueOf(cutString[3]) / scale;
                a_y = Float.valueOf(cutString[4]) / scale;
                a_z = Float.valueOf(cutString[5]) / scale;

                show_aXYZ[0].setText(String.valueOf(a_x));
                show_aXYZ[1].setText(String.valueOf(a_y));
                show_aXYZ[2].setText(String.valueOf(a_z));
                float a_xyz = (float) Math.sqrt(Math.pow(a_x, 2) + Math.pow(a_y, 2) + Math.pow(a_z, 2));

                float g_x, g_y, g_z;
                float gcale = 65536 / 500;
                g_x = Float.valueOf(cutString[0]) / gcale;
                g_y = Float.valueOf(cutString[1]) / gcale;
                g_z = Float.valueOf(cutString[2]) / gcale;
                show_gXYZ[0].setText(String.valueOf(g_x));
                show_gXYZ[1].setText(String.valueOf(g_y));
                show_gXYZ[2].setText(String.valueOf(g_z));

                float m_x, m_y, m_z;
                int mscale = 4096;
                m_x = (Float.valueOf(cutString[6]) * 2400) / mscale;
                m_y = (Float.valueOf(cutString[7]) * 2400) / mscale;
                m_z = (Float.valueOf(cutString[8]) * 2400) / mscale;
                show_mXYZ[0].setText(String.valueOf(m_x));
                show_mXYZ[1].setText(String.valueOf(m_y));
                show_mXYZ[2].setText(String.valueOf(m_z));
                show_getReadCount.setText(cutString[9]);
                show_getReadTime.setText(cutString[10]);
                break;
            }

            case "readITRI": {
                String[] cutString = data.split(",");
                float a_x, a_y, a_z;
                int scale = 16384; //2G:16384 4G:8192 8G:4096
                a_x = Float.valueOf(cutString[3]) / scale;
                a_y = Float.valueOf(cutString[4]) / scale;
                a_z = Float.valueOf(cutString[5]) / scale;

                show_aXYZ[0].setText(String.valueOf(a_x));
                show_aXYZ[1].setText(String.valueOf(a_y));
                show_aXYZ[2].setText(String.valueOf(a_z));
                float a_xyz = (float) Math.sqrt(Math.pow(a_x, 2) + Math.pow(a_y, 2) + Math.pow(a_z, 2));

                float g_x, g_y, g_z;
                float gcale = 65536 / 500;
                g_x = Float.valueOf(cutString[0]) / gcale;
                g_y = Float.valueOf(cutString[1]) / gcale;
                g_z = Float.valueOf(cutString[2]) / gcale;
                show_gXYZ[0].setText(String.valueOf(g_x));
                show_gXYZ[1].setText(String.valueOf(g_y));
                show_gXYZ[2].setText(String.valueOf(g_z));

                float m_x, m_y, m_z;
                int mscale = 4096;
                m_x = (Float.valueOf(cutString[6]) * 2400) / mscale;
                m_y = (Float.valueOf(cutString[7]) * 2400) / mscale;
                m_z = (Float.valueOf(cutString[8]) * 2400) / mscale;
                show_mXYZ[0].setText(String.valueOf(m_x));
                show_mXYZ[1].setText(String.valueOf(m_y));
                show_mXYZ[2].setText(String.valueOf(m_z));

                show_emBit.setText(cutString[9]);
                show_power.setText(cutString[10] + " %");
                show_accScale.setText(cutString[11]);
                show_getReadCount.setText(cutString[12]);
                show_getReadTime.setText(cutString[13]);
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter(BODY[FIRST]));
    }

    private IntentFilter makeGattUpdateIntentFilter(String pBody) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(pBody + ACTION_GATT_CONNECTED);
        intentFilter.addAction(pBody + ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(pBody + ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(pBody + RECEIVE_DATA_IDENTIFIER);
        intentFilter.addAction(pBody + RECEIVE_DATA_SETTING_IDENTIFIER);
        intentFilter.addAction(pBody + ACTION_SENDREAD_COUNT);

        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        bodyAction("disconnect", FIRST, null);
        bodyAction("close", FIRST, null);
        setResult(RESULT_CANCELED);
        super.onDestroy();
    }

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String pBody = BODY[FIRST];
            final String action = intent.getAction();
            if ((pBody + ACTION_GATT_CONNECTED).equals(action)) {
                device_status.setText(R.string.connect);
                Bundle message = intent.getExtras();
                String data = message.getString("data");
                show_firstConn.setText(data);
            } else if ((pBody + ACTION_GATT_DISCONNECTED).equals(action)) {
                device_status.setText(R.string.disconnect);
                show_aXYZ[0].setText(R.string.no_data);
                show_aXYZ[1].setText(R.string.no_data);
                show_aXYZ[2].setText(R.string.no_data);
            } else if ((pBody + ACTION_GATT_SERVICES_DISCOVERED).equals(action)) {
                device_status.setText(R.string.sensor_configuration);
            } else if ((pBody + RECEIVE_DATA_IDENTIFIER).equals(action)) {
                device_status.setText(R.string.reading);
                Bundle rawDataBundle = intent.getExtras();

                switch (mDeviceType) {
                    case "TI":
                        bodyAction("readTI", FIRST, rawDataBundle.getString(pBody + RECEIVE_DATA_IDENTIFIER));
                        break;
                    case "ITRI":
                        bodyAction("readITRI", FIRST, rawDataBundle.getString(pBody + RECEIVE_DATA_IDENTIFIER));
                        break;
                }

            } else if ((pBody + RECEIVE_DATA_SETTING_IDENTIFIER).equals(action)) {
                Bundle rawDataBundle = intent.getExtras();
                Toast.makeText(getApplicationContext(), "DATA_SETTING:" + rawDataBundle.getString(pBody + RECEIVE_DATA_SETTING_IDENTIFIER), Toast.LENGTH_SHORT).show();
            } else if ((pBody + ACTION_SENDREAD_COUNT).equals(action)) {
                Bundle message = intent.getExtras();
                String data = message.getString("data");
                show_sendReadCount.setText(data);
            }
        }
    };
}
