package com.flashforceapp.www.ffandroid;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class BrowseActivity extends AppCompatActivity {
    ListView listView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list);

        // Defined Array values to show in ListView
        List<String> values = new ArrayList<>();

        values.add("My Flashes");

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT DISTINCT category FROM patterns ORDER BY category", null);
        if (c.getCount() > 0){
            c.moveToFirst();
            while(!c.isAfterLast()) {
                //Log.i("INFO", c.getString(c.getColumnIndex("category")));
                values.add(c.getString(c.getColumnIndex("category")));
                c.moveToNext();
            }
        }
        c.close();
        db.close();

        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);


        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                //int itemPosition = position;

                // ListView Clicked item value
                String  itemValue    = (String) listView.getItemAtPosition(position);

                Intent intent = new Intent(getApplicationContext(), SecondBrowseActivity.class);
                intent.putExtra("CATEGORY", itemValue);
                startActivity(intent);

            }

        });
    }

}