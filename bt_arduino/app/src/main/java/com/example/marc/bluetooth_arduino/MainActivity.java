package com.example.marc.bluetooth_arduino;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.view.View;
import android.os.Handler;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {  //implements OnItemClickListener

    private static final int REQUEST_ENABLE_BT = 1;
    private Button onBtn;
    private Button offBtn;
    private Button listBtn;
    private Button findBtn;
    private TextView text;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ListView myListView;
    private ListView myPairedView;
    private ArrayAdapter<String> BTArrayAdapter;
    private ArrayAdapter<String> BTPairedArray;
    private ArrayList<BluetoothDevice> devices;

    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice = null;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };

    class ONListener implements View.OnClickListener {
        public void onClick(View v) {
            if (!myBluetoothAdapter.isEnabled()) {
                Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

                Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                        Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    class OFFListener implements View.OnClickListener {
        public void onClick(View v) {
            myBluetoothAdapter.disable();
            text.setText("Status: Disconnected");

            Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                    Toast.LENGTH_LONG).show();
        }
    }
    class LISTListener implements View.OnClickListener {
        public void onClick(View v) {
            // get paired devices
            pairedDevices = myBluetoothAdapter.getBondedDevices();
            // put it's one to the adapter
            for(BluetoothDevice device : pairedDevices)
                BTPairedArray.add(device.getName()+ "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(),"Show Paired Devices",
                    Toast.LENGTH_SHORT).show();
        }
    }
    class FINDListener implements View.OnClickListener {
        public void onClick(View v) {
            if (myBluetoothAdapter.isDiscovering()) {
                 // the button is pressed when it discovers, so cancel the discovery
                myBluetoothAdapter.cancelDiscovery();
            }
            else {
                BTArrayAdapter.clear();
                myBluetoothAdapter.startDiscovery();
                registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
        }
    }
    class ItemListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice selectedDevice = devices.get(position);
            Toast.makeText(getApplicationContext(), "Device is paired", Toast.LENGTH_SHORT).show();
            ConnectThread connect = new ConnectThread(selectedDevice);
            connect.start();
            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

            // create the data transfer thread
            ConnectedThread myconnected = new ConnectedThread(mmSocket);
            myconnected.start();
            System.err.println("Device is ready for data transmission");
        }
    }
    class MYBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
                // add the name and the MAC address of the object to the arrayAdapter
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                BTArrayAdapter.notifyDataSetChanged();
            }
            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) { // if arduino is connected
                System.err.println("Device is connected");
                try {
                    OutputStream dest = mmSocket.getOutputStream();
                    String start = "$$$";
                    byte[] data = start.getBytes();
                    dest.write(data);
                    System.err.println("send data");

                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.err.println("Socket ID: " + mmSocket);
            }
        }
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devices = new ArrayList<BluetoothDevice>();

        if (myBluetoothAdapter == null) {
            onBtn.setEnabled(false);
            offBtn.setEnabled(false);
            listBtn.setEnabled(false);
            findBtn.setEnabled(false);
            text.setText("Status: not supported");
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            // initialise buttons and lists
            text = (TextView) findViewById(R.id.text);
            onBtn = (Button) findViewById(R.id.turnOn);
            onBtn.setOnClickListener(new ONListener());
            offBtn = (Button) findViewById(R.id.turnOff);
            offBtn.setOnClickListener(new OFFListener());
            listBtn = (Button) findViewById(R.id.paired);
            listBtn.setOnClickListener(new LISTListener());
            findBtn = (Button) findViewById(R.id.search);
            findBtn.setOnClickListener(new FINDListener());
            myListView = (ListView) findViewById(R.id.listView1);
            myPairedView = (ListView) findViewById(R.id.listView2);

            // create the arrayAdapter and BTPairedArray that contains the BTDevices
            BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            myListView.setAdapter(BTArrayAdapter);
            BTPairedArray = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            myPairedView.setAdapter(BTPairedArray);
            //to click on a device from the list
            myPairedView.setOnItemClickListener(new ItemListener());

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(myBluetoothAdapter.isEnabled()) {
                text.setText("Status: Enabled");
            } else {
                text.setText("Status: Disabled");
            }
        }
    }
    BroadcastReceiver bReceiver = new MYBroadcastReceiver();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

    private class ConnectThread extends Thread {
        public ConnectThread(BluetoothDevice device) {
            final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }
        public void run() {
            myBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}

