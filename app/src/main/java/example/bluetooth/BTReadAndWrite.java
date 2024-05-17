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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btread_andwrite);

        listView = findViewById(R.id.listView);
        editText = findViewById(R.id.editTextPersonName1);

        adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, msglist);
        listView.setAdapter(adapter1);

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
                    msglist.add("接收数据：" + s);
                    adapter1.notifyDataSetChanged();
                }
            }
        };
    }

    public void sead_msg(View view) {
        String s = editText.getText().toString();
        if (!s.isEmpty()) {
            sendMessageHandle(s);
            msglist.add("发送数据：" + s + "\n");
            adapter1.notifyDataSetChanged();
        }
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
                new ReadThread().start();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("蓝牙连接失败");
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
            os.write(msg.getBytes("UTF-8")); // 使用UTF-8编码发送数据
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSocket() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
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
}
