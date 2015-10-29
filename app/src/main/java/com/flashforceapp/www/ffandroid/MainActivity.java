package com.flashforceapp.www.ffandroid;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;

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


public class MainActivity extends AppCompatActivity {
    public double avgOffset = 0.0;
    public boolean ffdbLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //
        //initialStates()
        //Button button = (Button)findViewById(R.id.flash_button);
        //button.setText("Click Me !");

        //NSNotificationCenter.defaultCenter().addObserver(self, selector:"checkOffsetAge", name:UIApplicationDidBecomeActiveNotification, object: nil) // adding observer for syncing

        //databaseCheck() // check database and load data if needed

        //checkOffsetAge() //change appearance of flash force icon based on offset age, and run performSync if needed

        //updateDisplay()  //update screen based on pattern and ownership

        //setAverageOffset() //set the offset used while flashing

        /*
        if (isAppAlreadyLaunchedOnce() == false){
            firstTimeBoot()  //get owned IAPs and show tutorial images
        }
         */
        //new RetrieveFeedTask().execute();
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

    /** Called when the user clicks the Flash button */
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


    }

    public void checkDatabase(){
        if (ffdbLoaded == false) {
            loadDatabase();
        }
        else {
        }
    }

    public void loadDatabase(){
        String mCSVfile = "file.csv";
        AssetManager manager = context.getAssets();
        InputStream inStream = null;
        try {
            inStream = manager.open(mCSVfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader buffer = new BufferedReader(new InputStreamReader(inStream));

        BufferedReader buffer = new BufferedReader(new InputStreamReader(inStream));
        String line = "";
        db.beginTransaction();
        try {
            while ((line = buffer.readLine()) != null) {
                String[] colums = line.split(",");
                if (colums.length != 4) {
                    Log.d("CSVParser", "Skipping Bad CSV Row");
                    continue;
                }
                ContentValues cv = new ContentValues(3);
                cv.put(dbCol0, colums[0].trim());
                cv.put(dbCol1, colums[1].trim());
                cv.put(dbCol2, colums[2].trim());
                cv.put(dbCol3, colums[3].trim());
                cv.put(dbCol4, colums[4].trim());
                db.insert(TABLE, null, cv);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }


}

public class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DICTIONARY_TABLE_NAME = "dictionary";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + DICTIONARY_TABLE_NAME + " (" +
                    KEY_WORD + " TEXT, " +
                    KEY_DEFINITION + " TEXT);";

    DictionaryOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

}


class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

    private Exception exception;

    protected void onPreExecute() {
        //progressBar.setVisibility(View.VISIBLE);
        //responseView.setText("");
    }

    protected String doInBackground(Void... urls) {
        //String email = emailText.getText().toString();
        // Do some validation here

        try {
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
            }
            finally{
                urlConnection.disconnect();
            }
        }
        catch(Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }
    }

    protected void onPostExecute(String response) {
        if(response == null) {
            response = "THERE WAS AN ERROR";
        }
        //progressBar.setVisibility(View.GONE);
        Log.i("INFO", response);
        //responseView.setText(response);

        try {
            JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
            String requestID = object.getString("requestId");
            int likelihood = object.getInt("likelihood");
            JSONArray photos = object.getJSONArray("photos");

        } catch (JSONException e) {
            // Appropriate error handling code
        }
    }
}
