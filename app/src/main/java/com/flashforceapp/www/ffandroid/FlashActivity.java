package com.flashforceapp.www.ffandroid;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class FlashActivity extends AppCompatActivity {

    private double interval = 0.25;
    private int color = 0;
    private ArrayList<String> colors = new ArrayList<String>();
    private double[] brightnessArray = new double[0];
    private Timer timer = new Timer();
    private String patternid = "";
    private String givenTiming = "6_4_2";
    private List<String> givenColors = new ArrayList<String>();


    double current_time = System.currentTimeMillis() / 1000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("INFO", "started FlashActivity");
        setContentView(R.layout.activity_flash);

        givenColors.add("#D4001F");   //default bulls colors
        givenColors.add("#FFFFFF");
        givenColors.add("#000000");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            patternid = extras.getString("PATTERNID");
        }

        mainFlash();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_flash, menu);
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

    public void mainFlash(){
        // Now get a handle to any View contained
        // within the main layout you are using
        View someView = findViewById(R.id.flash_handler);

        // Find the root view
        View root = someView.getRootView();

        if (patternid != ""){
            //assign givenTiming and givenColors
            SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

            Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
            if (c.getCount() > 0){
                c.moveToLast();
                givenTiming = c.getString(c.getColumnIndex("timing"));
                //givenColors =
                givenColors.clear();
                //check each color field in database
                if (c.getString(c.getColumnIndex("pattern1")) != ""){
                    givenColors.add(c.getString(c.getColumnIndex("pattern1")));
                }
                if (c.getString(c.getColumnIndex("pattern2")) != ""){
                    givenColors.add(c.getString(c.getColumnIndex("pattern2")));
                }
                if (c.getString(c.getColumnIndex("pattern3")) != ""){
                    givenColors.add(c.getString(c.getColumnIndex("pattern3")));
                }
                if (c.getString(c.getColumnIndex("pattern4")) != ""){
                    givenColors.add(c.getString(c.getColumnIndex("pattern4")));
                }
                if (c.getString(c.getColumnIndex("pattern5")) != ""){
                    givenColors.add(c.getString(c.getColumnIndex("pattern5")));
                }
            }

        }

        String[] splitTiming = givenTiming.split("_");    //contains number of beats for each color

        for (int i = 0; i < givenColors.size(); i++) {
            for (int j=0; j < Integer.parseInt(splitTiming[i]); j++){
                colors.add(givenColors.get(i));
            }
        }

        double modOffset;
        long modDelay;
        double modNumber = colors.size() * interval;
        double offset = 0.0;

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT * FROM offsets", null);
        if (c.getCount() > 0){
            c.moveToLast();
            offset = c.getDouble(c.getColumnIndex("offset"));
        }
        db.close();

        Log.i("INFO", "OFFSET FROM DB: ".concat( Double.toString(offset)));

        modOffset = ((System.currentTimeMillis() / 1000.0)+offset-.4) % modNumber;  //add avgOffset to this before mod

        // Set the color
        color = (int) (modOffset / interval);
        Log.i("INFO", "COLOR INT: ".concat( Integer.toString(color)));
        //root.getBackground().setColorFilter(Color.parseColor("#424242"));
        root.setBackgroundColor(Color.parseColor(colors.get(color)));

        modDelay = (long) (interval - ((System.currentTimeMillis() / 1000.0) % interval) );

        //timer.schedule(task, delay, period)
        //timer.schedule( new performClass(), 1000, 30000 );
        // or you can write in another way
        //timer.scheduleAtFixedRate(task, delay, period);
        timer.schedule(new delayTask(), modDelay);
        //timer.scheduleAtFixedRate(new updateTask(), 0, 250 );
    }

    class delayTask extends TimerTask {
        public void run(){
            timer.scheduleAtFixedRate( new updateTask(), 0, 250 );
        }
    }

    class updateTask extends TimerTask {
        public void run(){
            //change the background color based on the array contents and value of color iterator
            color += 1;
            if (color == colors.size()){
                color = 0;
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View someView = findViewById(R.id.flash_handler);
                    View root = someView.getRootView();
                    root.setBackgroundColor(Color.parseColor(colors.get(color)));
                }
            });

        }
    };

}
