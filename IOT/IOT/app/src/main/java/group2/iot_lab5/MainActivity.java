package group2.iot_lab5;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {


    protected static final int RESULT_SPEECH = 1;
    private ImageButton button_speak;
    private TextView txtView2;
    private Button button_send;
    String Translation_output = new String();
    String responseMessage = new String();
    int responseCode2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtView2 = (TextView) findViewById(R.id.txtView2);
        button_speak = (ImageButton) findViewById(R.id.button_speak);
        button_send = (Button) findViewById(R.id.button_send);

        // Translate voice to text
        button_speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    txtView2.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Your device doesn't support Voice Search",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

        // Click send button, display "request sent"
        View.OnClickListener OnClickButtonSend = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendPostRequest().execute();
                txtView2.setText("Request Sent");
            }
        };
        button_send.setOnClickListener(OnClickButtonSend);

        // Toolbar display
        // Show group and members
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "By Group2\nXiaotian Hu     Ming Gao     Han Zhang", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }


    public class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){}

        protected String doInBackground(String... arg0) {

            try{
                // Connect to the server
                // URL url = new URL("http://160.39.238.165");      // Local IP
                URL url = new URL("http://5ea71874.ngrok.io");       // Global IP
                JSONObject postDataParams = new JSONObject();
                // Display command
                if(Translation_output.compareTo("display time")==0)
                {
                    postDataParams.put("DisplayTime", "email");
                }
                else if(Translation_output.compareTo("weather")==0)
                {
                    postDataParams.put("Weather", "email");
                }
                else if(Translation_output.compareTo("display on")==0)
                {
                    postDataParams.put("DisplayON", "email");
                }
                else if(Translation_output.compareTo("display off")==0)
                {
                    postDataParams.put("DisplayOFF", "email");
                }
                else if(Translation_output.indexOf("display message")!=-1)
                {
                    String delimeter = "display message ";
                    String Translation_output2;
                    Translation_output2=Translation_output.split(delimeter)[1];
                    postDataParams.put("message", Translation_output2.toUpperCase());
                }
                else
                {
                    postDataParams.put("InvalidInstruction", "email");
                }

                Log.e("params",postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(3000);
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                responseMessage = conn.getResponseMessage();

                int responseCode=conn.getResponseCode();
                responseCode2=responseCode;

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in=new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    BufferedReader er=new BufferedReader(new
                            InputStreamReader(
                            conn.getErrorStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line="";
                    StringBuffer sb2 = new StringBuffer("");

                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }

                    while((line = er.readLine()) != null) {
                        sb2.append(line);
                        break;
                    }

                    in.close();
                    er.close();
                    return sb.toString();
                }
                else {
                    return new String("false : "+responseCode);
                }
            }
            catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }

        }

        @Override
        protected void onPostExecute(String result) {
            txtView2.setText(responseMessage);
        }


        public String getPostDataString(JSONObject params) throws Exception {

            StringBuilder result = new StringBuilder();
            boolean first = true;

            Iterator<String> itr = params.keys();

            while(itr.hasNext()){

                String key= itr.next();
                Object value = params.get(key);

                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value.toString(), "UTF-8"));

            }
            return result.toString();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    // Display translation output
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Translation_output=text.get(0);
                    txtView2.setText(Translation_output);
                }
                break;
            }

        }
    }



}
