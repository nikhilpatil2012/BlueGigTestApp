package tronbox.bluegigtestapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class LocationUpdateServer extends AsyncTask<String, String, String>{

    private Context context;
    private String verificationCode;

    public LocationUpdateServer(Context context){

        this.context = context;

    }

    /*
        params[0] = State --> Normal Or Distress
        params[1] = url where data needs to be send.
        params[2] = Data to be sent.

     */


    @Override
    protected String doInBackground(String... params) {

        String status = "";

        try{

            HttpURLConnection httpUrlConnection = null;
            URL url = new URL("http://54.169.182.72/api/v0/devices");
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.addRequestProperty("content-type","application/json");
            httpUrlConnection.addRequestProperty("authorization","bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiI1NTFiOTViZGMzOGYxNTVjNjJlMDdiNWEiLCJleHAiOiIyMDE1LTA0LTAxVDA3OjA5OjU4Ljg0NFoifQ.-Fex8dPBFRI5kgeaEYpKVVssUjElytuI5quVTyd7dV0");
            httpUrlConnection.setDoOutput(true);

            if (params[0] != null) {
                OutputStreamWriter wr = new OutputStreamWriter(httpUrlConnection.getOutputStream());
                wr.write(params[0]);
                wr.flush();
            }

            InputStream responseStream = new BufferedInputStream(httpUrlConnection.getInputStream());

            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null)
            {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();

            String response = stringBuilder.toString();
            Log.w("ServerResponseFromServer", response);

            JSONObject jsonObject = new JSONObject(response);
            status = jsonObject.getString("status");


            Intent intent = new Intent("info");
            intent.putExtra("name", "");
            intent.putExtra("mac", "");
            context.sendBroadcast(intent);


        } catch(IOException io){}
          catch (JSONException e) {e.printStackTrace();}

        return status;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

         if(s.length() > 0)
        {
//            Toast.makeText(context, s, Toast.LENGTH_SHORT).show();


        }

    }
}
