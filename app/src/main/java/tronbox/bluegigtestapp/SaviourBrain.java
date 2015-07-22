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
import java.nio.charset.UnsupportedCharsetException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class SaviourBrain extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private BluetoothDevice connectedDevice;
    private String bluetoothAddress;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothGattDescriptor bluetoothGattDescriptor;
    private String TAG = "SaviourBrain";
    boolean state = false;
    public GoogleApiClient googleApiClient;
    private LocationRequest mLocationRequest;
    private Boolean clickCount = false;
    private long timer = 0;


    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int i, byte[] bytes) {

            Intent intent = new Intent("info");
            intent.putExtra("name", device.getName());
            intent.putExtra("mac", device.getAddress());
            sendBroadcast(intent);

            BluetoothAdapter.getDefaultAdapter().stopLeScan(leScanCallback); // Stop the scan as no longer needed

        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {

                boolean value = bluetoothAdapter.startLeScan(leScanCallback);
                Log.w(TAG, "Scan Result " + value);

            } else {


            }

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.w("Operation", "Service has started");

        registerReceiver(broadcastReceiver, new IntentFilter("StartScan"));

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

                        //@1245050005*

                      Toast.makeText(getApplicationContext(), "Time", Toast.LENGTH_SHORT).show();
                      send(new byte[]{'@','1','2','4','5','0','5','0','0','0','5','*'}); // 12:45, 5 sec, 5 blinks


                    }else if(name.equals("Vibrate")){

                        //#0100101000*

                        Toast.makeText(getApplicationContext(), "Vibrate", Toast.LENGTH_SHORT).show();
                        send(new byte[]{'#','0','1','0','0','1','0','1','0','0','0','*'}); // 12:45, 5 sec, 5 blinks


                    }else if(name.equals("LED")){

                        Toast.makeText(getApplicationContext(), "LED", Toast.LENGTH_SHORT).show();
                        send("%01500005*".getBytes("UTF-8")); // Led no. 1, blink duration 5 sec, 5 blinks
                    }

                }catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


            }

        }, intentFilter);

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


                Intent intent = new Intent("connect");
                intent.putExtra("data", "disconnected");
                sendBroadcast(intent);

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

                        Intent intent = new Intent("connect");
                        intent.putExtra("data", "connected");
                        sendBroadcast(intent);



                        if (bluetoothGattDescriptor != null) {

                            gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);

                            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                            gatt.writeDescriptor(bluetoothGattDescriptor);

                            gatt.readCharacteristic(bluetoothGattCharacteristic);

                        }

                    }
                }


            }


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                String readData = BluetoothHelper.bytesToString(characteristic.getValue());

                Log.w("Reading_Data", readData);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {



            if(clickCount == false){
                clickCount = true;


                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {

                        Master.i = 1;

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {

                                if(Master.i == 1){

                                    Log.w("TrackingClicks", "Single Click");


                                }else if(Master.i == 2){


                                    Log.w("TrackingClicks", "Double Click");

                                }


                                clickCount = false;
                                Master.i = 0;

                            }
                        }, 1000);

                    }
                }, 0);

            }else if(clickCount == true) {

                Master.i = 2;


            }





            /*String readData = BluetoothHelper.bytesToString(characteristic.getValue());

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);

                state = true;
                googleApiClient.connect();

*/
           // Log.w("Reading_Data", readData);
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

        BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(BluetoothHelper.sixteenBitUuid(0x2222));

        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        Log.w(TAG, "Sending Data");

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

}

/*
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



}*/







