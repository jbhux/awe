package com.example.marc.bluetooth_arduino;

import android.os.Bundle;
import android.os.Message;
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
    // Message types used by the Handler
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_READ = 2;

    BroadcastReceiver bReceiver = new MYBroadcastReceiver();
    private final Handler mHandler = new myHandler();

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
            System.err.println("check");
                BluetoothDevice selectedDevice = devices.get(position);
                System.err.println("position: " +position);
                Toast.makeText(getApplicationContext(), "Device is paired", Toast.LENGTH_SHORT).show();
                ConnectThread connect = new ConnectThread(selectedDevice);
                connect.start();
                registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

                // create the data transfer thread
                ConnectedThread mConnected = new ConnectedThread(mmSocket);
                mConnected.start();
                System.err.println("Device is ready for data transmission");
                String start = "$$$";
                byte[] data = start.getBytes();
                mConnected.write(data);
                System.err.println("Send data with connected thread write");
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
    class myHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    //Do something when writing
                    break;
                case MESSAGE_READ:
                    //Get the bytes from the msg.obj
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
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
        private final OutputStream mmOutStream;
        private final InputStream mmInStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;
            InputStream tmpIn = null;
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmOutStream = tmpOut;
            mmInStream = tmpIn;
        }
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
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

