package com.flashforceapp.www.ffandroid;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

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


public class MainActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {
    BillingProcessor bp;
    //public double avgOffset = 0.0;
    //public List<Double> offsets = new ArrayList<Double>();
    public String patternid = "basevalue";
    public String team = "";
    public boolean ffdbLoaded = false;
    public String selectedStoreId = "";
    public String selectedPrice = "";
    public double offsetAgeforResync = 300.0; // allowable offset age in seconds (600 = 10 min)
    public String actionButtonStatus = "None";
    /*
    var actionButtonStatus = "None"
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bp = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuwrizMHIUtw3NtOCP4NxIeYqbASNEI8gxjEMBiKJvQ/RVMTGezdwd0HzbY/FNEw4n/v162PsejIAw8Y8rBP0mYvl/QKkHx+teOBABzZJkMItUSNIZQXW6OYTFI4OYQedOpSbjYka0BNgKaJI5919gDAtlGi/6nDW/eh6wrZjsvHZLJS7vBtQdtHST8pfdPbWVIBo/J33YeTjFyDHBAPnOGIKwAqryi2rTGKP7EeWJnCUPUD13UkEg9wEO8TYKyS/6WDPg+dkzXEblDaonJBjLpGky1VZnpzytAuhmrkntRlRZ/rAnpPYydv3+Z8+HGyfER1Rt0rbfFetXZMalzVETwIDAQAB", this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            patternid = extras.getString("PATTERNID");
            team = extras.getString("TEAM");
        }

        //initialStates();

        checkDatabase(); // check database and load data if needed

        checkOffsetAge(); //change appearance of flash force icon based on offset age, and run performSync if needed

        updateDisplay();  //update screen based on pattern and ownership

        //setAverageOffset(); //set the offset used while flashing
        /*
        if (isAppAlreadyLaunchedOnce() == false){
            firstTimeBoot();  //get owned IAPs and show tutorial images
        }
         */

        ImageButton image = (ImageButton) findViewById(R.id.ff_icon);
        image.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Log.i("INFO", "tap button tapped");
                performSync();
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        if (bp != null)
            bp.release();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBillingInitialized() {
        /*
         * Called when BillingProcessor was initialized and it's ready to purchase
         */
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        /*
         * Called when requested PRODUCT ID was successfully purchased
         */
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
        if (c.getCount() > 0){
            c.moveToLast();
            String storecode = c.getString(c.getColumnIndex("storecode"));
            String name = c.getString(c.getColumnIndex("name"));
            db.execSQL("insert into ownedpatterns values(NULL,'"+storecode+"','"+name+"','"+String.valueOf(patternid)+"')");
        }
        c.close();
        db.close();

        actionButtonStatus = "flash";
        Button flash_button = (Button) findViewById(R.id.flash_button);
        flash_button.setText(getString(R.string.textflash));
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        /*
         * Called when some error occurred. See Constants class for more details
         */
    }

    @Override
    public void onPurchaseHistoryRestored() {
        /*
         * Called when purchase history was restored and the list of all owned PRODUCT ID's
         * was loaded from Google Play
         */
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        for(String sku : bp.listOwnedProducts()) {
            Log.i("INFO", "Owned Managed Product: " + sku);

            Cursor c = db.rawQuery("SELECT * FROM patterns WHERE storecode='" + sku + "'", null);
            if (c.getCount() > 0){
                c.moveToFirst();
                while (!c.isAfterLast()){
                    if (!listOfOwnedPatterns().contains( sku )) {
                        String id = c.getString(c.getColumnIndex("id"));
                        String name = c.getString(c.getColumnIndex("name"));
                        db.execSQL("insert into ownedpatterns values(NULL,'"+sku+"','"+name+"','"+id+"')");
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }

        db.close();
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
        switch (actionButtonStatus) {
            case "flash":
                Intent intent = new Intent(this, FlashActivity.class);
                intent.putExtra("PATTERNID", patternid);
                startActivity(intent);
                break;
            case "sync":
                performSync();
                break;
            case "buy":
                Button flash_button = (Button) findViewById(R.id.flash_button);
                flash_button.setText(getString(R.string.textpurchasing));
                actionButtonStatus = "purchasing";
                buyFlash();
                break;
            case "getfree":
                purchaseFreeFlash();
                break;
            default:
                break;
        }
    }

    public void browse_handler(View view) {
        Intent intent = new Intent(this, BrowseActivity.class);
        startActivity(intent);
    }

    public void second_browse_handler(View view) {
        Intent intent = new Intent(this, SecondBrowseActivity.class);
        Button browse_button = (Button) findViewById(R.id.browse_button);

        intent.putExtra("CATEGORY", browse_button.getText() );
        startActivity(intent);
    }

    public void alternate_handler(View view) {
        Intent intent = new Intent(this, AlternateActivity.class);
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
        c.moveToLast();

        String group_id = c.getString(c.getColumnIndex("groupid"));

        c.close();
        db.close();

        intent.putExtra("GROUPID", group_id );
        startActivity(intent);
    }

    public void performSync() {
        Log.i("INFO", "Started Perform Sync");

        ImageButton imagebutton = (ImageButton) findViewById(R.id.ff_icon);
        imagebutton.setBackgroundResource(R.drawable.flashforwardthreeboxesgray);

        //TODO: animate the icon
        //AnimatedGifImageView animatedGifImageView = ((AnimatedGifImageView)findViewById(R.id.ff_icon));
        //animatedGifImageView.setAnimatedGif(R.drawable.animatedthreeboxes, AnimatedGifImageView.TYPE.FIT_CENTER);

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        db.execSQL("DELETE FROM offsets");
        db.close();

        Log.i("INFO", "Starting test of offset");
        new GetOffset().execute();
        new GetOffset().execute();
        new GetOffset().execute();
        // average the offsets into avgOffset at completion of GetOffsets. just use most recent in flashactivity

        //revert to static image
        //imagebutton.setBackgroundResource(R.drawable.flashforwardthreeboxes);

    }

    public void checkDatabase() {
        if (patternid.equals("basevalue")) {
            try {
                loadDatabase();

                //enable load owned purchases
                bp.loadOwnedPurchasesFromGoogle();

                //TODO: check for sharedprefs free cheer. Request restore of SharedPrefs, then on success run the SharedPref stuff below
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.userpref), Context.MODE_PRIVATE);
                String defaultValue = "none";
                String freeflashstorecode = sharedPref.getString(getString(R.string.freeFlashString), defaultValue);
                if (!freeflashstorecode.equals(defaultValue)){
                    Log.i("INFO", "GOT A STORECODE FROM SHARED PREFERENCES");
                    Log.i("INFO", "STORECODE: " + freeflashstorecode);

                    if (!listOfOwnedPatterns().contains(freeflashstorecode)){
                        Log.i("INFO", "STORECODE; AND IT IS NOT IN THE OWNED PATTERNS YET");

                        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);
                        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE storecode='" + freeflashstorecode + "'", null);
                        if (c.getCount() > 0){
                            c.moveToLast();
                            String id = c.getString(c.getColumnIndex("id"));
                            String name = c.getString(c.getColumnIndex("name"));
                            db.execSQL("insert into ownedpatterns values(NULL,'"+freeflashstorecode+"','"+name+"','"+id+"')");
                        }
                        c.close();
                        db.close();
                    }
                    else {
                        Log.i("INFO", "STORECODE; AND IT'S ALREADY IN");
                    }
                }
                else {
                    Log.i("INFO", "NO STORECODE IN SHARED PREFERENCES");
                }

                Log.i("INFO","DATABASE LOADED");
            } catch (IOException e) {
                //report on this
            }
        }
    }

    public void loadDatabase() throws IOException {
        Log.i("INFO", "LOADING DATABASE");
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        db.execSQL("drop table if exists patterns");

        db.execSQL("drop table if exists offsets");

        db.execSQL("create table if not exists patterns(id integer primary key autoincrement, storecode text, name text, groupid text, category text, timing text, price real, pattern1 text, pattern2 text, pattern3 text, pattern4 text, pattern5 text, alt1 text)");

        db.execSQL("create table if not exists offsets(id integer primary key autoincrement, offset real, timestamp real)");

        db.execSQL("create table if not exists ownedpatterns(id integer primary key autoincrement, storecode text, name text, patternid integer)");

        db.execSQL("create table if not exists freepattern(id integer primary key autoincrement, storecode text, name text, patternid integer)");

        AssetManager am = getBaseContext().getAssets();
        InputStream is = am.open("ffinput.csv"); //src/main/assets
        BufferedReader buffer = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String line;
        String str1 = "insert into patterns values(NULL,";
        String str2 = ");";

        db.beginTransaction();
        while ((line = buffer.readLine()) != null) {
            StringBuilder sb = new StringBuilder(str1);
            String[] str = line.split(",");
            sb.append("'" + str[0] + "',");  //storecode
            sb.append("'" + str[3] + "',");  //name
            sb.append("'" + str[2] + "',");  //groupid
            sb.append("'" + str[1] + "',");  //category
            sb.append("'" + str[19] + "',");  //timing
            sb.append("'" + str[5] + "',");  //price
            sb.append("'" + str[6] + "',");  //pattern1
            sb.append("'" + str[7] + "',");  //pattern2
            sb.append("'" + str[8] + "',");  //pattern3
            sb.append("'" + str[9] + "',");  //pattern4
            sb.append("'" + str[10] + "',");  //pattern5
            sb.append("'" + str[4] + "'");  //alternate text
            sb.append(str2);
            db.execSQL(sb.toString());
        }
        db.setTransactionSuccessful();
        db.endTransaction();

        //insert one offset
        //db.execSQL("insert into offsets values (NULL, '0.0','0.0')");

        db.close();
        //am.close();
        //is.close();
        //buffer.close();

        ffdbLoaded = true;
    }


    class GetOffset extends AsyncTask<Void, Void, String> {
        //private double offset;
        //private double ping;
        //private double count = 0.0;
        private double ct;
        private double nct;


        //private Exception exception;

        protected String doInBackground(Void... urls) {
            try {
                //TODO: include a check for timing and discard the bad results
                URL url = new URL("https://alignthebeat.appspot.com");
                ct = System.currentTimeMillis() / 1000.0;
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
                nct = System.currentTimeMillis() / 1000.0;

                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                //String date = object.getString("date");
                double epoch = object.getDouble("epoch");

                Log.i("INFO", "GOT AN EPOCH: ".concat( Double.toString(epoch) ) );
                Log.i("INFO", "ct: ".concat(Double.toString(ct)));
                Log.i("INFO", "nct: ".concat(Double.toString(nct)));

                double ping = nct - ct;

                Log.i("INFO", "GOT A PING: ".concat( Double.toString(ping)));

                double offset = epoch - nct + ping;

                Log.i("INFO", "GOT AN OFFSET: ".concat( Double.toString(offset)));
                //offsets.add(offset);

                SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

                //db.execSQL("create table if not exists offsets(id integer primary key autoincrement, offset real, timestamp real)");
                db.execSQL("insert into offsets values(NULL, '"+ Double.toString(offset) + "', '" + Double.toString(nct) +"')");

                db.close();


            } catch (JSONException e) {
                // Appropriate error handling code
            }
        }
    }

    public void checkOffsetAge(){
        Log.i("INFO", "CHECKING OFFSET AGE");

        //get last offset timestamp (seconds from epoch)
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);
        Cursor c = db.rawQuery("SELECT * FROM offsets", null);
        if (c.getCount() > 0){
            c.moveToLast();
            double storednct = c.getDouble(c.getColumnIndex("timestamp"));
            //get the system time in seconds from epoch
            double nct = System.currentTimeMillis() / 1000.0;

            Log.i("INFO", "storednct: ".concat(Double.toString(storednct)));
            Log.i("INFO", "nct: ".concat(Double.toString(nct)));

            //if offset older than x seconds, get a new one
            if ( (nct - storednct) > offsetAgeforResync ){
                performSync();
            }
        }
        else {
            //if no offset, get one
            performSync();
        }
        c.close();
        db.close();
    }

    public void updateDisplay(){
        if (patternid.equals("basevalue")){
            //no pattern selected
            Button team_button = (Button) findViewById(R.id.team_button);
            Button outfit_button = (Button) findViewById(R.id.outfit_button);

            team_button.setText("");
            outfit_button.setText("");

            team_button.setOnClickListener(null);
            outfit_button.setOnClickListener(null);

            clearBoxArea();
        }
        else {
            loadPatternInformation();
            checkOwnership();
        }
    }

    public void checkOwnership(){
        Boolean owned = false;

        //check area for free flash, shared prefs
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.userpref), Context.MODE_PRIVATE);
        String defaultValue = "";
        if (selectedStoreId.equals(sharedPref.getString(getString(R.string.freeFlashString), defaultValue)) ){
            owned = true;
        }

        //then check against stored owned cheers
        if (listOfOwnedPatterns().contains( selectedStoreId )) {
            owned = true;
        }

        if (selectedPrice.equals("0.0") ){
            owned = true;
        }

        //change the display of the flash button and the actionButtonStatus variable
        Button flash_button = (Button) findViewById(R.id.flash_button);

        if (owned){
            flash_button.setText(getString(R.string.textflash));
            actionButtonStatus = "flash";
        }
        else {
            //check for if first item has been given for free
            SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);
            Cursor c = db.rawQuery("SELECT * FROM freepattern", null);
            if (c.getCount() == 0) {
                flash_button.setText(getString(R.string.textgetfree));
                actionButtonStatus = "getfree";
            }
            else{
                flash_button.setText(String.format("Buy $%s", selectedPrice));
                actionButtonStatus = "buy";
            }
            c.close();
            db.close();
        }
    }

    public void loadPatternInformation(){
        Button browse_button = (Button) findViewById(R.id.browse_button);
        Button team_button = (Button) findViewById(R.id.team_button);
        Button outfit_button = (Button) findViewById(R.id.outfit_button);

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
        c.moveToLast();

        browse_button.setText(c.getString(c.getColumnIndex("category")));
        team_button.setText(team);
        outfit_button.setText(c.getString(c.getColumnIndex("alt1")));

        selectedStoreId = c.getString(c.getColumnIndex("storecode"));
        selectedPrice = c.getString(c.getColumnIndex("price"));

        c.close();

        c = db.rawQuery("SELECT * FROM patterns WHERE name='" + team + "'", null);
        if (c.getCount() == 1){
            //no alternates
            outfit_button.setText("");
        }
        c.close();
        db.close();

        drawBoxes();
    }

    public void drawBoxes(){
        String givenTiming;
        List<String> givenColors = new ArrayList<>();

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);
        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
        c.moveToLast();
        givenTiming = c.getString(c.getColumnIndex("timing"));
        String[] splitTiming = givenTiming.split("_");

        if (!c.getString(c.getColumnIndex("pattern1")).isEmpty()){
            for (int i = 0; i < Integer.parseInt(splitTiming[0]); i++) {
                givenColors.add("#" + c.getString(c.getColumnIndex("pattern1")));
            }
        }
        if (!c.getString(c.getColumnIndex("pattern2")).isEmpty()){
            for (int i = 0; i < Integer.parseInt(splitTiming[1]); i++) {
                givenColors.add("#" + c.getString(c.getColumnIndex("pattern2")));
            }
        }
        if (!c.getString(c.getColumnIndex("pattern3")).isEmpty()){
            for (int i = 0; i < Integer.parseInt(splitTiming[2]); i++) {
                givenColors.add("#" + c.getString(c.getColumnIndex("pattern3")));
            }
        }
        if (!c.getString(c.getColumnIndex("pattern4")).isEmpty()){
            for (int i = 0; i < Integer.parseInt(splitTiming[3]); i++) {
                givenColors.add("#" + c.getString(c.getColumnIndex("pattern4")));
            }
        }
        if (!c.getString(c.getColumnIndex("pattern5")).isEmpty()){
            for (int i = 0; i < Integer.parseInt(splitTiming[4]); i++) {
                givenColors.add("#" + c.getString(c.getColumnIndex("pattern5")));
            }
        }
        c.close();
        db.close();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        ImageView myImageView = (ImageView) findViewById(R.id.canvas_space);
        Bitmap myBitmap = Bitmap.createBitmap(width, 100, Bitmap.Config.ARGB_8888);

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        int boxWidth = width / givenColors.size();

        for (int i = 0; i < givenColors.size(); i++){
            Paint myRectPaint = new Paint();
            myRectPaint.setColor(Color.parseColor(givenColors.get(i)));
            int xCoord = (i * boxWidth);
            tempCanvas.drawRoundRect(new RectF(xCoord, 0, (xCoord + boxWidth), 100), 2, 2, myRectPaint);
        }

        //Attach the canvas to the ImageView
        myImageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
    }

    public void clearBoxArea(){
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);
        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
        c.moveToLast();
        c.close();
        db.close();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        ImageView myImageView = (ImageView) findViewById(R.id.canvas_space);
        Bitmap myBitmap = Bitmap.createBitmap(width, 100, Bitmap.Config.ARGB_8888);

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the cavas
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        Paint myRectPaint = new Paint();
        myRectPaint.setColor(Color.WHITE);
        //myRectPaint.setStrokeWidth(3);

        //Draw everything else you want into the canvas, in this example a rectangle with rounded edges
        tempCanvas.drawRoundRect(new RectF(0, 0, width, 100), 2, 2, myRectPaint);

        //Attach the canvas to the ImageView
        myImageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
    }

    public List<String> listOfOwnedPatterns(){
        List<String> ownedPatterns = new ArrayList<>();

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);
        Cursor c = db.rawQuery("SELECT * FROM ownedpatterns", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            while (!c.isAfterLast()){
                if (!c.getString(c.getColumnIndex("storecode")).equals("") ) {
                    ownedPatterns.add(c.getString(c.getColumnIndex("storecode")));
                }
                c.moveToNext();
            }
        }

        c.close();
        db.close();

        return ownedPatterns;
    }

    public void buyFlash(){

        boolean isAvailable = BillingProcessor.isIabServiceAvailable(getBaseContext());
        if(isAvailable) {
            // continue
            bp.purchase(this, selectedStoreId); //this is completed in onProductPurchased()
        }
    }

    public void purchaseFreeFlash(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                //store in shared preferences

                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.userpref), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.freeFlashString), selectedStoreId);
                editor.apply();

                //put shared preferences in backup
                BackupManager backupManager = new BackupManager(getBaseContext());
                backupManager.dataChanged();

                //store in ownedpatterns and freepattern
                SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

                Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
                if (c.getCount() > 0){
                    c.moveToLast();
                    String storecode = c.getString(c.getColumnIndex("storecode"));
                    String name = c.getString(c.getColumnIndex("name"));
                    db.execSQL("insert into ownedpatterns values(NULL,'"+storecode+"','"+name+"','"+String.valueOf(patternid)+"')");
                    db.execSQL("insert into freepattern values(NULL,'"+storecode+"','"+name+"','"+String.valueOf(patternid)+"')");
                }
                c.close();
                db.close();

                actionButtonStatus = "flash";
                Button flash_button = (Button) findViewById(R.id.flash_button);
                flash_button.setText(getString(R.string.textflash));

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Set other dialog properties
        builder.setMessage(R.string.freepurchaseprompt);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();

    }
}