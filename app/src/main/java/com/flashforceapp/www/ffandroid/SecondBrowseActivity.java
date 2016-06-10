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

public class SecondBrowseActivity extends AppCompatActivity {
    ListView listView ;
    List<String> patternids = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list);
        String category = "";

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            category = extras.getString("CATEGORY");
        }

        // Defined Array values to show in ListView
        List<String> values = new ArrayList<>();

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        if (category != null && category.equals("My Flashes")){
            Cursor c = db.rawQuery("SELECT name, id FROM patterns WHERE storecode IN (SELECT storecode FROM ownedpatterns) GROUP BY name ORDER BY name", null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    values.add(c.getString(c.getColumnIndex("name")));
                    patternids.add(c.getString(c.getColumnIndex("id")));
                    c.moveToNext();
                }
            }
            c.close();
        }
        else {
            Cursor c = db.rawQuery("SELECT name, id FROM patterns WHERE category='" + category + "' AND alt1='Home' UNION  SELECT name, id FROM patterns WHERE category='" + category + "' AND name NOT IN (SELECT name FROM patterns WHERE category='" + category + "' AND alt1='Home') GROUP BY name ORDER BY name", null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    values.add(c.getString(c.getColumnIndex("name")));
                    patternids.add(c.getString(c.getColumnIndex("id")));
                    c.moveToNext();
                }
            }
            c.close();
        }

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
                String  itemValue = (String) listView.getItemAtPosition(position);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("PATTERNID", patternids.get(position));
                intent.putExtra("TEAM", itemValue);
                startActivity(intent);

            }

        });
    }

}