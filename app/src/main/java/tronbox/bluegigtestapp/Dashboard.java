package tronbox.bluegigtestapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Dashboard extends Activity {

    Button connect, send;
    Intent serviceIntent;
    ProgressDialog progressDialog;

    EditText deviceName, deviceAdd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        connect = (Button)findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendBroadcast(new Intent("StartScan"));
                progressDialog = ProgressDialog.show(Dashboard.this, "Scanning Devices", "Please wait....",true);

            }
        });

        send = (Button)findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                 if(deviceAdd.getText().toString().length() > 0)
                {

                    JSONArray jsonArray1 = new JSONArray(); // 9582152153 - motoG, 9899972269-ayush, sis = 9873252281, guardian-2 = 9933350989

                    JSONObject post_data = new JSONObject();
                    String dataToBeSent = null;

                    jsonArray1.put(deviceAdd.getText().toString());

                    try {

                        post_data.put("devices", jsonArray1);
                        dataToBeSent = post_data.toString();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    send(post_data);



                }else {


                     Toast.makeText(getApplicationContext(), "Empty Details", Toast.LENGTH_SHORT).show();

                 }

            }
        });

        deviceName = (EditText)findViewById(R.id.device_name);

        deviceAdd = (EditText)findViewById(R.id.device_address);

        IntentFilter filter = new IntentFilter("info");
        registerReceiver(receiver, filter);

        serviceIntent = new Intent(getApplicationContext(), SaviourBrain.class);
        startService(serviceIntent);


    }

    @Override
    public void finish() {
        super.finish();

        unregisterReceiver(receiver);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

                    String name = intent.getExtras().getString("name");
                    String mac = intent.getExtras().getString("mac");

                    deviceName.setText(name);
                    deviceAdd.setText(mac);

            progressDialog.dismiss();
        }
    };

    public void send(JSONObject object){


        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, "http://54.169.182.72/api/v0/devices", object, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {


                try {
                    String status = jsonObject.getString("status");

                    Log.w("ServerResponseFromServer", status);

                    Toast.makeText(getApplicationContext(), "Mac Address Sent", Toast.LENGTH_LONG).show();

                    deviceName.setText("");
                    deviceAdd.setText("");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {


                 if(volleyError != null)
                {
                     if(volleyError.getMessage() != null)
                    {

                        Toast.makeText(getApplicationContext(), "Mac Address Error", Toast.LENGTH_LONG).show();

                        Log.w("ServerResponseFromServer", volleyError.getMessage().toString());


                    }

                }

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {


                HashMap<String, String> params = new HashMap<String, String>();

                params.put("Content-Type", "application/json");
                params.put("authorization", "bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiI1NTFiOTViZGMzOGYxNTVjNjJlMDdiNWEiLCJleHAiOiIyMDE1LTA0LTAxVDA3OjA5OjU4Ljg0NFoifQ.-Fex8dPBFRI5kgeaEYpKVVssUjElytuI5quVTyd7dV0");

                return params;
            }

        };


        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(jsObjRequest);
        requestQueue.start();
    }

}
