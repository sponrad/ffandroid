package com.flashforceapp.www.ffandroid;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public double avgOffset = 0.0;
    public List<Double> offsets = new ArrayList<Double>();

    public boolean ffdbLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton image = (ImageButton) findViewById(R.id.ff_icon);
        image.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                performSync();
                return true;
            }
        });

        //
        //initialStates()
        //Button button = (Button)findViewById(R.id.flash_button);
        //button.setText("Click Me !");

        //NSNotificationCenter.defaultCenter().addObserver(self, selector:"checkOffsetAge", name:UIApplicationDidBecomeActiveNotification, object: nil) // adding observer for syncing

        //checkDatabase() // check database and load data if needed

        //checkOffsetAge() //change appearance of flash force icon based on offset age, and run performSync if needed

        //updateDisplay()  //update screen based on pattern and ownership

        //setAverageOffset() //set the offset used while flashing

        /*
        if (isAppAlreadyLaunchedOnce() == false){
            firstTimeBoot()  //get owned IAPs and show tutorial images
        }
         */
        //new RetrieveFeedTask().execute();
        //TODO: do offset 5 times
        new GetOffset().execute();
        //TODO: average the offsets into avgOffset at completion of GetOffsets.. or do this at flashtime
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user clicks the Flash button
     */
    public void flash_handler(View view) {
        Log.i("INFO", "flash_handler called");
        // Do something in response to button
        Intent intent = new Intent(this, FlashActivity.class);
        //EditText editText = (EditText) findViewById(R.id.edit_message);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void performSync() {
        ImageButton image = (ImageButton) findViewById(R.id.ff_icon);
        image.setBackground();
    }

    public void checkDatabase() {
        if (ffdbLoaded == false) {
            try {
                loadDatabase();
            } catch (IOException e) {
                //report on this
            }
        } else {
        }
    }

    public void loadDatabase() throws IOException {
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        db.execSQL("CREATE TABLE IF NOT EXISTS TutorialsPoint(Username VARCHAR,Password VARCHAR);");

        db.execSQL("INSERT INTO TutorialsPoint VALUES('admin','admin');");

        //FileReader file = new FileReader("data.csv");

        AssetManager am = getBaseContext().getAssets();
        InputStream is = am.open("ffinput.csv"); //src/main/assets
        BufferedReader buffer = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String line = "";
        String tableName = "TABLE_NAME";
        String columns = "_id, name, dt1, dt2, dt3";
        String str1 = "INSERT INTO " + tableName + " (" + columns + ") values(";
        String str2 = ");";

        db.beginTransaction();
        while ((line = buffer.readLine()) != null) {
            StringBuilder sb = new StringBuilder(str1);
            String[] str = line.split(",");
            sb.append("'" + str[0] + "',");
            sb.append(str[1] + "',");
            sb.append(str[2] + "',");
            sb.append(str[3] + "'");
            sb.append(str[4] + "'");
            sb.append(str2);
            db.execSQL(sb.toString());
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }


    class GetOffset extends AsyncTask<Void, Void, String> {

        private Exception exception;

        protected String doInBackground(Void... urls) {
            try {
                //TODO: include a check for timing and discard the bad results
                URL url = new URL("https://alignthebeat.appspot.com");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if (response == null) {
                response = "THERE WAS AN ERROR";
            }
            //progressBar.setVisibility(View.GONE);
            Log.i("INFO", response);
            //responseView.setText(response);

            try {
                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                String date = object.getString("date");
                double epoch = object.getInt("epoch");
                offsets.add(epoch);

            } catch (JSONException e) {
                // Appropriate error handling code
            }
        }
    }
}
