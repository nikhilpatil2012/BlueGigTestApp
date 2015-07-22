package tronbox.bluegigtestapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class DeviceSyncService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private BluetoothDevice connectedDevice;
    private String bluetoothAddress;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothGattDescriptor bluetoothGattDescriptor;

    private BluetoothGattService batteryGattService;
    private BluetoothGattCharacteristic batteryGattCharacterstics;
    private BluetoothGattDescriptor batteryGattDescriptor;


    private String TAG = "SaviourBrain";

    boolean state = false;

    public GoogleApiClient googleApiClient;
    private LocationRequest mLocationRequest;

    boolean flip = false;


    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int i, byte[] bytes)

        {

            Log.w("DeviceName", device.getName());

              if (device.getAddress().equals("F4:B8:5E:4B:89:DC"))
            {
                bluetoothAdapter.stopLeScan(leScanCallback); // Stop the scan as no longer needed
                bluetoothAddress = device.getAddress();

                connectedDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

                if (connectedDevice != null)
                {
                    connectedDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallback);
                }

                Log.w(TAG, "Device Found " + device.getName() + "_" + device.getAddress());

            } else {

                Log.w(TAG, "Device Not Found");

            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.w("Operation", "Service has started");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {

            boolean value = bluetoothAdapter.startLeScan(leScanCallback);
            Log.w(TAG, "Scan Result " + value);

        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        IntentFilter intentFilter = new IntentFilter("Button_Pressed");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


                Bundle bundle = intent.getExtras();

                String name = bundle.getString("Data");

                try{

                    if(name.equals("Time")){


                        String cmd = "@"+bundle.getString("Hour")+bundle.getString("Min")+"100003*";

                        Toast.makeText(getApplicationContext(), "Time", Toast.LENGTH_SHORT).show();
                        send(cmd.getBytes("UTF-8")); // 12:45, 5 sec, 5 blinks

                    }else if(name.equals("Vibrate")){

                        Toast.makeText(getApplicationContext(), "Vibrate", Toast.LENGTH_SHORT).show();
                        send("#1000012000*".getBytes("UTF-8")); // 5 sec vibrate, 1 time loop, 2 sec gap

                    }else if(name.equals("LED")){

                        String cmd = "%"+bundle.getString("Num")+"100005*";

                        Toast.makeText(getApplicationContext(), "LED "+cmd, Toast.LENGTH_SHORT).show();
                        send(cmd.getBytes("UTF-8")); // Led no. 1, blink duration 5 sec, 5 blinks
                    }

                }catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


            }

        }, intentFilter);


        registerReceiver(broadcastReceiver, new IntentFilter("IncommingCall"));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //  bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    boolean value = bluetoothAdapter.startLeScan(leScanCallback);
                    Log.w(TAG, "Scan Result " + value);
                }

            }
        }, new IntentFilter("AfterDelete"));

        return START_STICKY;
    }

    protected void startLocationUpdates()
    {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) { // If connection to the remote device is active

                bluetoothGatt = gatt; // Store the BluetoothGatt for the future use.
                gatt.discoverServices(); // Retrieve all the Services being offered by the BLE.

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { // If connection to the remote device is active

                Log.w(TAG, "Saviour Disconnected");
                bluetoothAdapter.startLeScan(leScanCallback);

            }


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                bluetoothGattService = gatt.getService(BluetoothHelper.sixteenBitUuid(0xFFE0)); // UUID_SERVICE is the desired service.

                if (bluetoothGattService != null) { // Found your service.

                    Log.w(TAG, "My Service Found");

                    bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(BluetoothHelper.sixteenBitUuid(0xFFE1));

                    if (bluetoothGattCharacteristic != null) {

                        bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(BluetoothHelper.sixteenBitUuid(0x2902));

                        Log.w(TAG + "_Data", "My Characterstics Found");

                        if (bluetoothGattDescriptor != null) {

                            Log.w(TAG + "_Data", "My Descripter Found");

                            gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);


                            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            //  bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);


                            gatt.writeDescriptor(bluetoothGattDescriptor);

                            gatt.readCharacteristic(bluetoothGattCharacteristic);

                        }else {

                            Log.w(TAG + "_Data", "My Descripter Not Found");
                        }

                    }
                }


            } // service ends here


            /*if (status == BluetoothGatt.GATT_SUCCESS) {

                batteryGattService = gatt.getService(BluetoothHelper.sixteenBitUuid(0x180F)); // UUID_SERVICE is the desired service.

                if (batteryGattService != null) { // Found your service.

                    Log.w(TAG, "My Service Found");

                    batteryGattCharacterstics = batteryGattService.getCharacteristic(BluetoothHelper.sixteenBitUuid(0x2A19));

                    if (batteryGattCharacterstics != null) {

                        batteryGattDescriptor = batteryGattCharacterstics.getDescriptor(BluetoothHelper.sixteenBitUuid(0x2908));

                        Log.w(TAG + "_Data", "My Characterstics Found");

                        if (batteryGattDescriptor != null) {

                            Log.w(TAG + "_Data", "My Descripter Found");

                            gatt.setCharacteristicNotification(batteryGattCharacterstics, true);

                            batteryGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            batteryGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
*//*

                            byte[] v1 = {0x01};
                            byte[] v2 = {0x02};

                            bluetoothGattDescriptor.setValue(v1);
                            bluetoothGattDescriptor.setValue(v2);
*//*

                            gatt.writeDescriptor(batteryGattDescriptor);

                            gatt.readCharacteristic(batteryGattCharacterstics);

                        }else {

                            Log.w(TAG + "_Data", "My Descripter Not Found");
                        }

                    }
                }


            }*/


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                String readData = BluetoothHelper.bytesToString(characteristic.getValue());

                Log.w("Reading_Data", readData);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            String readData = characteristic.getStringValue(0);
            String readData1 = characteristic.getStringValue(1);


            Log.w("Reading_Data", ""+characteristic.getProperties());


            Log.w(TAG, ""+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
            Log.w(TAG, ""+characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1));


            Log.w(TAG, ""+characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0));
            Log.w(TAG, ""+characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1));


            Log.w(TAG, BluetoothHelper.bytesToString(characteristic.getValue()));

            Log.w(TAG, readData + readData1);


            if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8,0) == 01)
            {

                Log.w("TrackingClicks", "Single Click");
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);


                //Toast.makeText(getApplicationContext(), "3 times pressed", Toast.LENGTH_SHORT).show();

            }else if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8,0) == 02)
            {

                Log.w("TrackingClicks", "Single Click");
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);
                v.vibrate(1000);

                //Toast.makeText(getApplicationContext(), "2 times pressed", Toast.LENGTH_SHORT).show();

            }else if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8,0) == 03)
            {
                //Toast.makeText(getApplicationContext(), "3 times pressed", Toast.LENGTH_SHORT).show();

                Log.w("TrackingClicks", "Single Click");
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);
                v.vibrate(1000);
                v.vibrate(1000);

            }else if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8,0) == 255)
            {

                //Toast.makeText(getApplicationContext(), "Long pressed", Toast.LENGTH_SHORT).show();

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(4000);
            }


            /* if(flip == false)
            {
                flip = true;
                send(new byte[]{0});
            }*/

            /*
              else if(flip == true)
             {
                 flip = false;
                 send(new byte[]{1});
             }*/

/*
                state = true;
                googleApiClient.connect();*/


            Log.w("Reading_Data", readData);
        }
    };

    @Override
    public void onConnected(Bundle bundle) {

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if(state == true){

            state = false;

            Toast.makeText(getApplicationContext(), "Updated", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public boolean send(byte[] data) {
        if (bluetoothGatt == null || bluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }


        BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(BluetoothHelper.sixteenBitUuid(0x1802)).getCharacteristic(BluetoothHelper.sixteenBitUuid(0x2A06));

        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        Log.w(TAG, "Sending Data");

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Toast toast = Toast.makeText(context, "IncommingRecieved", Toast.LENGTH_SHORT);
            toast.show();

            send(new byte[]{2});

        }
    };
}


class BluetoothHelper {
    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

    public static UUID sixteenBitUuid(long shortUuid) {
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }


    private static String parseScanRecord(byte[] scanRecord) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < scanRecord.length) {
            int len = scanRecord[i++] & 0xFF;
            if (len == 0) break;
            switch (scanRecord[i] & 0xFF) {
                // https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
                case 0x0A: // Tx Power
                    output.append("\n  Tx Power: ").append(scanRecord[i+1]);
                    break;
                case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
                    output.append("\n  Advertisement Data: ")
                            .append(BluetoothHelper.bytesToHex(scanRecord, i + 3, len));

                    String ascii = BluetoothHelper.bytesToAsciiMaybe(scanRecord, i + 3, len);
                    if (ascii != null) {
                        output.append(" (\"").append(ascii).append("\")");
                    }
                    break;
            }
            i += len;
        }
        return output.toString();
    }

    public static int PRINTABLE_ASCII_MIN = 0x20; // ' '
    public static int PRINTABLE_ASCII_MAX = 0x7E; // '~'

    public static boolean isPrintableAscii(int c) {
        return c >= PRINTABLE_ASCII_MIN && c <= PRINTABLE_ASCII_MAX;
    }


    public static String bytesToHex(byte[] data, int offset, int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder hex = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            hex.append(String.format(" %02X", data[i] % 0xFF));
        }
        hex.deleteCharAt(0);
        return hex.toString();
    }



    public static String bytesToAsciiMaybe(byte[] data, int offset, int length) {
        StringBuilder ascii = new StringBuilder();
        boolean zeros = false;
        for (int i = offset; i < offset + length; i++) {
            int c = data[i] & 0xFF;
            if (isPrintableAscii(c)) {
                if (zeros) {
                    return null;
                }
                ascii.append((char) c);
            } else if (c == 0) {
                zeros = true;
            } else {
                return null;
            }
        }
        return ascii.toString();
    }

    public static String bytesToString(byte[] data){

        return new String(data);
    }



}







