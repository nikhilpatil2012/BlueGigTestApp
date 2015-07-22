package tronbox.bluegigtestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by nikhil on 4/11/15.
 */

public class IncommingCallHandler extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            // TELEPHONY MANAGER class object to register one listner
            TelephonyManager tmgr = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            //Create Listner
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener(context);

            // Register listener for LISTEN_CALL_STATE
            tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        Context context;

        MyPhoneStateListener(Context context){

            this.context = context;

        }

        public void onCallStateChanged(int state, String incomingNumber) {

            // Log.d("MyPhoneListener",state+"   incoming no:"+incomingNumber);

            // state = 1 means when phone is ringing
            if (state == 1) {

                String msg = " New Phone Call Event. Incomming Number : "+incomingNumber;
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, msg, duration);
                toast.show();

                context.sendBroadcast(new Intent("IncommingCall"));
            }
        }
    }
}
