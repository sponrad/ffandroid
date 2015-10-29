package com.flashforceapp.www.ffandroid;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class FlashActivity extends AppCompatActivity {

    private double interval = 0.25;
    private int color = 0;
    private String[] givenColors = {"D4001F", "FFFFFF", "000000"};   //default bulls colors for testing
    private ArrayList<String> colors = new ArrayList<String>();
    private double[] brightnessArray = new double[0];
    private Timer timer = new Timer();
    private String givenTiming = "6_4_2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash);

        // Now get a handle to any View contained
        // within the main layout you are using
        View someView = findViewById(R.id.flash_handler);

        // Find the root view
        View root = someView.getRootView();

        // Set the color
        //root.getBackground().setColorFilter(Color.parseColor("#424242"));
        root.setBackgroundColor(Color.parseColor(colors.get(color)));

        String[] splitTiming = givenTiming.split("_");    //contains number of beats for each color

        for (int i = 0; i < givenColors.length; i++) {
            for (int j=0; j < Integer.parseInt(splitTiming[i]); j++){
                colors.add(givenColors[i]);
            }
        }

        class updateTask extends TimerTask {
            public void run(){
            //change the background color based on the array contents and value of color iterator
                color += 1;
                if (color == colors.size()){
                    color = 0;
                }
                View someView = findViewById(R.id.flash_handler);
                View root = someView.getRootView();
                root.setBackgroundColor(Color.parseColor(colors.get(color)));
            }
        };

        //timer.schedule(task, delay, period)
        //timer.schedule( new performClass(), 1000, 30000 );
        // or you can write in another way
        //timer.scheduleAtFixedRate(task, delay, period);
        timer.scheduleAtFixedRate(new updateTask(), 0, 250 );

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

}
