package com.itripatch.nine_ti_sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
    private TextView show_emBit, show_power, show_accScale, show_firstConn, show_sendReadCount, show_getReadCount, show_getReadTime, show_sendReadPeriod, show_RSSI, show_lossRate;
    private Button appFinish;

    private ArrayList<String> xyzString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

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
        show_RSSI = (TextView) findViewById(R.id.rssi_value);
        show_firstConn = (TextView) findViewById(R.id.firstConn_value);
        show_sendReadCount = (TextView) findViewById(R.id.sendReadCount_value);
        show_getReadCount = (TextView) findViewById(R.id.getReadCount_value);
        show_getReadTime = (TextView) findViewById(R.id.getReadTime_value);
        show_sendReadPeriod = (TextView) findViewById(R.id.sendReadPeriod_value);
        show_sendReadPeriod.setText(mSendReadPeriod + " ms");
        show_lossRate = (TextView) findViewById(R.id.lossRate_value);
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
        xyzString = new ArrayList<String>();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                bodyAction("disconnect", FIRST, null);
                bodyAction("close", FIRST, null);
                arrayTofile(xyzString);
                setResult(RESULT_CANCELED);
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
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
                float show_a_x, show_a_y, show_a_z, show_a_xyz;
                int scale = 16384; //2G:16384 4G:8192 8G:4096
                String temp = "";
                temp += cutString[10] + ",";
                a_x = Float.valueOf(cutString[3]) / scale;show_a_x = (float) ((int) Math.floor(a_x * 10000) / 10000.0);temp += show_a_x + ",";
                a_y = Float.valueOf(cutString[4]) / scale;show_a_y = (float) ((int) Math.floor(a_y * 10000) / 10000.0);temp += show_a_y + ",";
                a_z = Float.valueOf(cutString[5]) / scale;show_a_z = (float) ((int) Math.floor(a_z * 10000) / 10000.0);temp += show_a_z + ",";

                show_aXYZ[0].setText(String.valueOf(show_a_x));
                show_aXYZ[1].setText(String.valueOf(show_a_y));
                show_aXYZ[2].setText(String.valueOf(show_a_z));
                float a_xyz = (float) Math.sqrt(Math.pow(a_x, 2) + Math.pow(a_y, 2) + Math.pow(a_z, 2));
                show_a_xyz = (float) ((int) Math.floor(a_xyz * 10000) / 10000.0);temp += show_a_xyz + ",";

                float g_x, g_y, g_z;
                float show_g_x, show_g_y, show_g_z;
                float gcale = 65536 / 500;
                g_x = Float.valueOf(cutString[0]) / gcale;show_g_x = (float) ((int) Math.floor(g_x * 10000) / 10000.0);temp += show_g_x + ",";
                g_y = Float.valueOf(cutString[1]) / gcale;show_g_y = (float) ((int) Math.floor(g_y * 10000) / 10000.0);temp += show_g_y + ",";
                g_z = Float.valueOf(cutString[2]) / gcale;show_g_z = (float) ((int) Math.floor(g_z * 10000) / 10000.0);temp += show_g_z + ",";
                show_gXYZ[0].setText(String.valueOf(show_g_x));
                show_gXYZ[1].setText(String.valueOf(show_g_y));
                show_gXYZ[2].setText(String.valueOf(show_g_z));

                float m_x, m_y, m_z;
                float show_m_x, show_m_y, show_m_z;
                int mscale = 4096;
                m_x = (Float.valueOf(cutString[6]) * 2400) / mscale;show_m_x = (float) ((int) Math.floor(m_x * 10000) / 10000.0);temp += show_m_x + ",";
                m_y = (Float.valueOf(cutString[7]) * 2400) / mscale;show_m_y = (float) ((int) Math.floor(m_y * 10000) / 10000.0);temp += show_m_y + ",";
                m_z = (Float.valueOf(cutString[8]) * 2400) / mscale;show_m_z = (float) ((int) Math.floor(m_z * 10000) / 10000.0);temp += show_m_z + ",";
                show_mXYZ[0].setText(String.valueOf(show_m_x));
                show_mXYZ[1].setText(String.valueOf(show_m_y));
                show_mXYZ[2].setText(String.valueOf(show_m_z));
                
                show_getReadCount.setText(cutString[9]);
                show_getReadTime.setText(cutString[10]);
                show_RSSI.setText(cutString[11] + " db");temp += cutString[11] + ",";
                temp += cutString[12] + ",";
                show_lossRate.setText(cutString[13]);temp += cutString[13];
                xyzString.add(temp);
                break;
            }

            case "readITRI": {
                String[] cutString = data.split(",");
                float a_x, a_y, a_z;
                float show_a_x, show_a_y, show_a_z, show_a_xyz;
                int scale = 16384; //2G:16384 4G:8192 8G:4096
                String temp = "";
                temp += cutString[13] + ",";
                a_x = Float.valueOf(cutString[3]) / scale;show_a_x = (float) ((int) Math.floor(a_x * 10000) / 10000.0);temp += show_a_x + ",";
                a_y = Float.valueOf(cutString[4]) / scale;show_a_y = (float) ((int) Math.floor(a_y * 10000) / 10000.0);temp += show_a_y + ",";
                a_z = Float.valueOf(cutString[5]) / scale;show_a_z = (float) ((int) Math.floor(a_z * 10000) / 10000.0);temp += show_a_z + ",";

                show_aXYZ[0].setText(String.valueOf(show_a_x));
                show_aXYZ[1].setText(String.valueOf(show_a_y));
                show_aXYZ[2].setText(String.valueOf(show_a_z));
                float a_xyz = (float) Math.sqrt(Math.pow(a_x, 2) + Math.pow(a_y, 2) + Math.pow(a_z, 2));
                show_a_xyz = (float) ((int) Math.floor(a_xyz * 10000) / 10000.0);temp += show_a_xyz + ",";

                float g_x, g_y, g_z;
                float show_g_x, show_g_y, show_g_z;
                float gcale = 65536 / 500;
                g_x = Float.valueOf(cutString[0]) / gcale;show_g_x = (float) ((int) Math.floor(g_x * 10000) / 10000.0);temp += show_g_x + ",";
                g_y = Float.valueOf(cutString[1]) / gcale;show_g_y = (float) ((int) Math.floor(g_y * 10000) / 10000.0);temp += show_g_y + ",";
                g_z = Float.valueOf(cutString[2]) / gcale;show_g_z = (float) ((int) Math.floor(g_z * 10000) / 10000.0);temp += show_g_z + ",";
                show_gXYZ[0].setText(String.valueOf(show_g_x));
                show_gXYZ[1].setText(String.valueOf(show_g_y));
                show_gXYZ[2].setText(String.valueOf(show_g_z));

                float m_x, m_y, m_z;
                float show_m_x, show_m_y, show_m_z;
                int mscale = 4096;
                m_x = (Float.valueOf(cutString[6]) * 2400) / mscale;show_m_x = (float) ((int) Math.floor(m_x * 10000) / 10000.0);temp += show_m_x + ",";
                m_y = (Float.valueOf(cutString[7]) * 2400) / mscale;show_m_y = (float) ((int) Math.floor(m_y * 10000) / 10000.0);temp += show_m_y + ",";
                m_z = (Float.valueOf(cutString[8]) * 2400) / mscale;show_m_z = (float) ((int) Math.floor(m_z * 10000) / 10000.0);temp += show_m_z + ",";
                show_mXYZ[0].setText(String.valueOf(show_m_x));
                show_mXYZ[1].setText(String.valueOf(show_m_y));
                show_mXYZ[2].setText(String.valueOf(show_m_z));

                show_emBit.setText(cutString[9]);
                show_power.setText(cutString[10] + " %");
                show_accScale.setText(cutString[11]);
                show_getReadCount.setText(cutString[12]);
                show_getReadTime.setText(cutString[13]);
                show_RSSI.setText(cutString[14] + " db");temp += cutString[14] + ",";
                temp += cutString[15] + ",";
                show_lossRate.setText(cutString[16]);temp += cutString[16];
                xyzString.add(temp);
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
                    case "ITRI30":
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

    public String getDateTime(int timeFormat) {
        Date dateNow = new Date();
        SimpleDateFormat formatter = null;
        switch (timeFormat) {

            case 0: //格式 :20xx - xx - xx  xx : xx : xx
                formatter = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss"); //HH是表示24小時制, hh是表示12小時制
                break;

            case 1://格式 :20xx-xx-xx_xx.xx.xx
                formatter = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss"); //HH是表示24小時制, hh是表示12小時制
                break;

            case 2://格式 :20xx-xx-xx
                formatter = new SimpleDateFormat("yyyy-MM-dd"); //HH是表示24小時制, hh是表示12小時制
                break;
            case 3:
                formatter = new SimpleDateFormat("yyyy - MM - dd");
                String temp1 = formatter.format(dateNow);
                formatter = new SimpleDateFormat("HH : mm : ss");
                String temp2 = formatter.format(dateNow);
                return (temp1 + "\n" + temp2);
        }
        return formatter.format(dateNow);
    }

    private void arrayTofile(ArrayList<String> numbers) {
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/Log");
        // ----如要在SD卡中建立數據庫文件，先做如下的判斷和建立相對應的目錄和文件----
        if (!dir.exists()) { // 判斷目錄是否存在
            dir.mkdirs(); // 建立目錄
        } else {
        }
        try {
            File myFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Log/" + getDateTime(1) + ".csv");

            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut, "UTF-8");
            myOutWriter.write("address: " + mDeviceAddress + "," + "sendReadPeriod (ms): " + mSendReadPeriod + "," + "firstConn: " + show_firstConn.getText() + "\n");
            switch (mDeviceType){
                case "TI":
                case "ITRI30":
                    myOutWriter.write("time,a_x,a_y,a_z,a_xyz,g_x,g_y,g_z,m_x,m_y,m_z,rssi (db),sendCount,lossRate" + "\n");
                    break;
            }
            int i;
            for (i = 0; i < numbers.size(); i++) {

                myOutWriter.write(numbers.get(i) + "\n");
            }
            myOutWriter.close();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
