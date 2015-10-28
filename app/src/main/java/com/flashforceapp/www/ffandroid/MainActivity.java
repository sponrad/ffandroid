package com.flashforceapp.www.ffandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

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
    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, FlashActivity.class);
        //EditText editText = (EditText) findViewById(R.id.edit_message);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
