package example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMISSION_CODE = 1;
    public BlueToothController btController = new BlueToothController();
    private Toast mToast;
    public ArrayList<String> requestList = new ArrayList<>();
    private IntentFilter foundFilter;
    public ArrayAdapter adapter1;
    public ArrayList<String> arrayList = new ArrayList<>();
    public ArrayList<String> deviceName = new ArrayList<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    showToast("STATE_OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    showToast("STATE_ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    showToast("STATE_TURNING_OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    showToast("STATE_TURNING_ON");
                    break;
                default:
                    showToast("UnKnow STATE");
                    unregisterReceiver(this);
                    break;
            }
        }
    };

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceNameStr = device.getName() != null ? device.getName() : "未命名设备";
                String deviceAddress = device.getAddress();
                String bondState;
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDED:
                        bondState = "已配对";
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        bondState = "配对中";
                        break;
                    case BluetoothDevice.BOND_NONE:
                    default:
                        bondState = "未配对";
                        break;
                }

                String deviceInfo = "设备名：" + deviceNameStr + "\n设备地址：" + deviceAddress + "\n连接状态：" + bondState;
                if (!arrayList.contains(deviceAddress)) {
                    arrayList.add(deviceAddress);
                    deviceName.add(deviceInfo);
                    adapter1.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showToast("搜索结束");
                unregisterReceiver(this);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                showToast("开始搜索");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
        foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        foundFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        foundFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        ListView listView = findViewById(R.id.listview1);
        adapter1 = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, deviceName);
        listView.setAdapter(adapter1);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CharSequence content = ((TextView) view).getText();
                String con = content.toString();
                String[] conArray = con.split("\n");
                String rightStr = conArray[1].substring(5);
                BluetoothDevice device = btController.find_device(rightStr);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    btController.cancelSearch();
                    device.createBond();
                }
            }
        });

        Button button_3 = findViewById(R.id.button7);
        button_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnONbt();
            }
        });

        Button button_4 = findViewById(R.id.button8);
        button_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPermision();
                btController.turnOffBlueTooth();
            }
        });

        Button button_6 = findViewById(R.id.button10);
        button_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPermision();
                registerReceiver(bluetoothReceiver, foundFilter);
                arrayList.clear();
                deviceName.clear();
                adapter1.notifyDataSetChanged();
                btController.findDevice();
            }
        });

        Button button_7 = findViewById(R.id.button11);
        button_7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPermision();
                deviceName.clear();
                arrayList.clear();
                adapter1.notifyDataSetChanged();
                ArrayList<BluetoothDevice> bluetoothDevices = btController.getBondedDeviceList();
                for (BluetoothDevice device : bluetoothDevices) {
                    arrayList.add(device.getAddress());
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        deviceName.add("设备名：" + device.getName() + "\n设备地址：" + device.getAddress() + "\n连接状态：已配对");
                    } else {
                        deviceName.add("设备名：" + device.getName() + "\n设备地址：" + device.getAddress() + "\n连接状态：未配对");
                    }
                    adapter1.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            showToast("open successfully");
        } else {
            showToast("open unsuccessfully");
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnONbt() {
        getPermision();
        btController.turnOnBlueTooth(this, 1);
    }

    public void BTVisible() {
        getPermision();
        btController.enableVisibly(this);
    }

    public void getPermision() {
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

    public void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    private void startBTReadAndWriteActivity(BluetoothDevice device) {
        Intent intent = new Intent(this, BTReadAndWrite.class);
        intent.putExtra("deviceAddr", device.getAddress());
        startActivity(intent);
    }

    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    showToast("配对成功：" + device.getName());
                    startBTReadAndWriteActivity(device);
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    showToast("配对失败：" + device.getName());
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondStateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bondStateReceiver);
    }
}
