package tronbox.bluegigtestapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    EditText ledNum, hour, min;
    TextView textView;
    Button time, vibrate, led;

    private Boolean clickCount = false;
    private long timer = 0;

    int i = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ledNum = (EditText)findViewById(R.id.led_num);
        hour = (EditText)findViewById(R.id.hour);
        min = (EditText)findViewById(R.id.min);

        textView = (TextView)findViewById(R.id.device_message);

        time = (Button)findViewById(R.id.send_time);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent("Button_Pressed");
                intent.putExtra("Data", "Time");
                intent.putExtra("Hour", hour.getText().toString());
                intent.putExtra("Min", min.getText().toString());
                sendBroadcast(intent);

            }
        });

        vibrate = (Button)findViewById(R.id.vibrate_device);
        vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent("IncommingCall");
                sendBroadcast(intent);

            }
        });

        led = (Button)findViewById(R.id.led_on);
        led.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if(clickCount == false){
                    clickCount = true;

                    i = 1;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {

                            if(i == 1){

                              Log.w("TrackingClicks", "Single Click");

                            }else if(i == 2){


                              Log.w("TrackingClicks", "Double Click");

                            }


                            clickCount = false;

                        }
                    }, 1000);

                }else {

                    i = 2;


                }


/*
                if(clickCount == false)
                {
                    clickCount = true;

                    i = 1;


                    //timer = System.currentTimeMillis()/1000; // start the timer

                    //Log.w("TrackingClicks", "Single Click");

                   // Log.w("TrackingClicks", ""+timer);


                }else {

                    i = 2;

                    long diff = (System.currentTimeMillis()/1000) - timer;


                 //   Log.w("TrackingClicks", "Diff " + diff);


                    if( diff < 2)
                    {
                        Log.w("TrackingClicks", "Double Click");

                    }else {

                        Log.w("TrackingClicks", "Single Click");

                    }

                    clickCount = false;
                    timer = 0;

                }*/


/*
                Intent intent = new Intent("Button_Pressed");
                intent.putExtra("Data", "LED");
                intent.putExtra("Num", ledNum.getText().toString());
                sendBroadcast(intent);*/

            }
        });

        startService(new Intent(getApplicationContext(), DeviceSyncService.class));

    }

    @Override
    protected void onDestroy() {

        sendBroadcast(new Intent("AfterDelete"));

        super.onDestroy();


    }
}
