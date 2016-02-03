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

public class AlternateActivity extends AppCompatActivity {
    ListView listView ;
    List<String> patternids = new ArrayList<>();
    String team = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternate);

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list);
        String group_id = "";

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            group_id = extras.getString("GROUPID");
        }

        // Defined Array values to show in ListView
        List<String> values = new ArrayList<>();

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT alt1, name, id FROM patterns WHERE groupid='" + group_id + "' ORDER BY alt1", null);
        if (c.getCount() > 0){
            c.moveToFirst();
            while(!c.isAfterLast()) {
                values.add(c.getString(c.getColumnIndex("alt1")));
                patternids.add(c.getString(c.getColumnIndex("id")));
                team = c.getString(c.getColumnIndex("name"));
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
                //String  itemValue = (String) listView.getItemAtPosition(position);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("PATTERNID", patternids.get(position));
                intent.putExtra("TEAM", team);
                startActivity(intent);

            }

        });
    }

}