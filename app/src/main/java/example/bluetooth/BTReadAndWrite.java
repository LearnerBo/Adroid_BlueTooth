package example.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Arrays;

public class BTReadAndWrite extends AppCompatActivity {
    private ArrayList<String> requestList = new ArrayList<>();
    private static final int REQ_PERMISSION_CODE = 1;
    private BluetoothSocket bluetoothSocket;
    private Toast mToast;
    private BTclient bTclient;
    public BlueToothController mController = new BlueToothController();
    public ArrayList<String> msglist = new ArrayList<>();
    public ListView listView;
    public ArrayAdapter<String> adapter1;
    public EditText editText;
    public Handler mHandler;
    public TextView textViewConnectionStatus;
    private Switch switchMinFrequency, switchMaxFrequency, switchSweepInterval, switchRadarDistance, switchHex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btread_andwrite);

        listView = findViewById(R.id.listView);
        editText = findViewById(R.id.editTextPersonName1);
        textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus);

        // 使用自定义的布局文件
        adapter1 = new ArrayAdapter<>(this, R.layout.received_list_item, R.id.received_item_text, msglist);
        listView.setAdapter(adapter1);

        switchMinFrequency = findViewById(R.id.switchMinFrequency);
        switchMaxFrequency = findViewById(R.id.switchMaxFrequency);
        switchSweepInterval = findViewById(R.id.switchSweepInterval);
        switchRadarDistance = findViewById(R.id.switchRadarDistance);
        switchHex = findViewById(R.id.switchHex);

        setupSwitchListeners();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String deviceAddr = bundle.getString("deviceAddr");

        BluetoothDevice device = mController.find_device(deviceAddr);
        bTclient = new BTclient(device);
        bTclient.start();

        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    String s = msg.obj.toString();
                    String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
                    msglist.add(currentTime + " 接收数据：" + s);
                    adapter1.notifyDataSetChanged();
                } else if (msg.what == 2) {
                    textViewConnectionStatus.setText("已连接");
                } else if (msg.what == 3) {
                    textViewConnectionStatus.setText("连接中");
                } else if (msg.what == 4) {
                    textViewConnectionStatus.setText("断开");
                }
            }
        };

        // 设置初始状态为连接中
        textViewConnectionStatus.setText("连接中");
        mHandler.sendEmptyMessage(3);
    }

    private void setupSwitchListeners() {
        switchMinFrequency.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchMaxFrequency.setChecked(false);
                switchSweepInterval.setChecked(false);
                switchRadarDistance.setChecked(false);
            }
        });

        switchMaxFrequency.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchMinFrequency.setChecked(false);
                switchSweepInterval.setChecked(false);
                switchRadarDistance.setChecked(false);
            }
        });

        switchSweepInterval.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchMinFrequency.setChecked(false);
                switchMaxFrequency.setChecked(false);
                switchRadarDistance.setChecked(false);
            }
        });

        switchRadarDistance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchMinFrequency.setChecked(false);
                switchMaxFrequency.setChecked(false);
                switchSweepInterval.setChecked(false);
            }
        });
    }

    public void sead_msg(View view) {
        String s = editText.getText().toString();
        if (!s.isEmpty()) {
            if (switchHex.isChecked()) {
                s = stringToHex(s);
            } else {
                try {
                    int decimal = Integer.parseInt(s);
                    s = String.format("%04X", decimal);
                } catch (NumberFormatException e) {
                    showToast("请输入有效的数字");
                    return;
                }
            }
            if (switchMinFrequency.isChecked()) {
                s = createCommandString(0x10, s);
            } else if (switchMaxFrequency.isChecked()) {
                s = createCommandString(0x20, s);
            } else if (switchSweepInterval.isChecked()) {
                s = createCommandString(0x30, s);
            } else if (switchRadarDistance.isChecked()) {
                s = createCommandString(0x40, s);
            }

            sendMessageHandle(s);
            String currentTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
            msglist.add(currentTime + " 发送数据：" + s);
            adapter1.notifyDataSetChanged();
        }
    }

    private String createCommandString(int command, String data) {
        return String.format("AA55%02X%s0016", command, data);
    }

    private String stringToHex(String str) {
        StringBuilder hex = new StringBuilder();
        for (char ch : str.toCharArray()) {
            hex.append(String.format("%02X", (int) ch));
        }
        return hex.toString();
    }

    public void clearMessages(View view) {
        msglist.clear();
        adapter1.notifyDataSetChanged();
    }

    private class BTclient extends Thread {
        private BluetoothDevice device;

        BTclient(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            connectDevice(device);
        }

        private void connectDevice(BluetoothDevice device) {
            try {
                getPermission();
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                bluetoothSocket.connect();
                showToast("蓝牙连接成功");
                mHandler.sendEmptyMessage(2);  // 连接成功后发送消息更新状态
                new ReadThread().start();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("蓝牙连接失败");
                mHandler.sendEmptyMessage(4);  // 连接失败或断开时发送消息更新状态
                closeSocket();
            }
        }
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestList.add(Manifest.permission.BLUETOOTH_SCAN);
            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            requestList.add(Manifest.permission.BLUETOOTH);
        }
        if (!requestList.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestList.toArray(new String[0]), REQ_PERMISSION_CODE);
        }
    }

    private void showToast(String text) {
        runOnUiThread(() -> {
            if (mToast == null) {
                mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            } else {
                mToast.setText(text);
            }
            mToast.show();
        });
    }

    private void sendMessageHandle(String msg) {
        getPermission();
        if (bluetoothSocket == null) {
            showToast("没有连接");
            return;
        }
        try {
            OutputStream os = bluetoothSocket.getOutputStream();
            os.write(hexStringToByteArray(msg)); // 使用16进制字符串发送数据
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private void closeSocket() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
                mHandler.sendEmptyMessage(4);  // 断开连接后更新状态
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReadThread extends Thread {
        private static final String TAG = "BluetoothReadThread";

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;

            try {
                mmInStream = bluetoothSocket.getInputStream();
            } catch (IOException e1) {
                e1.printStackTrace();
                return;
            }

            StringBuilder receivedData = new StringBuilder();
            while (true) {
                try {
                    if ((bytes = mmInStream.read(buffer)) > 0) {
                        Log.e(TAG, "Raw bytes: " + Arrays.toString(Arrays.copyOf(buffer, bytes)));
                        String readMessage = new String(buffer, 0, bytes, "UTF-8"); // 使用UTF-8解码接收数据
                        receivedData.append(readMessage);

                        // Check for new lines in the received data
                        int newLineIndex;
                        while ((newLineIndex = receivedData.indexOf("\n")) != -1) {
                            String completeMessage = receivedData.substring(0, newLineIndex).trim();
                            receivedData.delete(0, newLineIndex + 1); // Remove the processed message

                            Log.e(TAG, "run: " + completeMessage);
                            Message message = Message.obtain();
                            message.what = 1;
                            message.obj = completeMessage;
                            mHandler.sendMessage(message);

                            if (completeMessage.equalsIgnoreCase("o")) {
                                showToast("open");
                            } else if (completeMessage.equalsIgnoreCase("c")) {
                                showToast("closed");
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from InputStream", e);
                    closeInputStream(mmInStream);
                    break;
                }
            }
        }

        private void closeInputStream(InputStream inputStream) {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect(View view) {
        closeSocket();
        showToast("蓝牙已断开");
    }
}
