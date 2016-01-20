package com.flashforceapp.www.ffandroid;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {
    public double avgOffset = 0.0;
    public List<Double> offsets = new ArrayList<Double>();
    public String patternid = "";
    public String team = "";
    public boolean ffdbLoaded = false;
    public String selectedStoreId = "";
    public String selectedPrice = "";
    public double offsetAgeforResync = 600.0; // allowable offset age in seconds (600 = 10 min)
    /*
    var actionButtonStatus = "None"
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            patternid = extras.getString("PATTERNID");
            team = extras.getString("TEAM");
        }

        //initialStates();

        checkDatabase(); // check database and load data if needed

        checkOffsetAge(); //change appearance of flash force icon based on offset age, and run performSync if needed

        //performSync();  // handled by checkOffsetAge

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
        intent.putExtra("PATTERNID", patternid);

        startActivity(intent);
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
        AnimatedGifImageView animatedGifImageView = ((AnimatedGifImageView)findViewById(R.id.ff_icon));
        animatedGifImageView.setAnimatedGif(R.drawable.animatedthreeboxes,
                AnimatedGifImageView.TYPE.FIT_CENTER);

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        db.execSQL("DELETE FROM offsets");
        db.close();

        Log.i("INFO", "Starting test of offset");
        new GetOffset().execute();
        new GetOffset().execute();
        new GetOffset().execute();
        //TODO: average the offsets into avgOffset at completion of GetOffsets. just use most recent in flashactivity

        //revert to static image
        //imagebutton.setBackgroundResource(R.drawable.flashforwardthreeboxes);

    }

    public void checkDatabase() {
        if (ffdbLoaded == false && patternid == "") {
            try {
                loadDatabase();
                Log.i("INFO","DATABASE LOADED");
            } catch (IOException e) {
                //report on this
            }
        } else {
        }
    }

    public void loadDatabase() throws IOException {
        Log.i("INFO", "LOADING DATABASE");
        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        db.execSQL("drop table if exists patterns");

        db.execSQL("create table if not exists patterns(id integer primary key autoincrement, storecode text, name text, groupid text, category text, timing text, price real, pattern1 text, pattern2 text, pattern3 text, pattern4 text, pattern5 text, alt1 text)");

        db.execSQL("create table if not exists offsets(id integer primary key autoincrement, offset real, timestamp real)");

        db.execSQL("create table if not exists ownedpatterns(id integer primary key autoincrement, storecode text, name text, patternid integer)");

        AssetManager am = getBaseContext().getAssets();
        InputStream is = am.open("ffinput.csv"); //src/main/assets
        BufferedReader buffer = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String line = "";
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

        ffdbLoaded = true;
    }


    class GetOffset extends AsyncTask<Void, Void, String> {

        private double offset;
        private double ping;
        private double count = 0.0;
        private double ct;
        private double nct;


        private Exception exception;

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
                String date = object.getString("date");
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
        if (patternid == ""){
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
        }
    }

    public void loadPatternInformation(){
        Button browse_button = (Button) findViewById(R.id.browse_button);
        Button team_button = (Button) findViewById(R.id.team_button);
        Button outfit_button = (Button) findViewById(R.id.outfit_button);

        SQLiteDatabase db = openOrCreateDatabase("ff.db", MODE_PRIVATE, null);

        Cursor c = db.rawQuery("SELECT * FROM patterns WHERE id='" + patternid + "'", null);
        c.moveToLast();

        String[] timing = c.getString(c.getColumnIndex("timing")).split("_");

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
        String givenTiming = "";
        List<String> givenColors = new ArrayList<String>();

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
        int height = size.y;

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
}


public class AnimatedGifImageView extends ImageView {
    public static enum TYPE {
        FIT_CENTER, STREACH_TO_FIT, AS_IS
    };

    public AnimatedGifImageView(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }

    public AnimatedGifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedGifImageView(Context context) {
        super(context);
    }

    boolean animatedGifImage = false;
    private InputStream is = null;
    private Movie mMovie = null;
    private long mMovieStart = 0;
    private TYPE mType = TYPE.FIT_CENTER;

    public void setAnimatedGif(int rawResourceId, TYPE streachType) {
        setImageBitmap(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mType = streachType;
        animatedGifImage = true;
        is = getContext().getResources().openRawResource(rawResourceId);
        try {
            mMovie = Movie.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            byte[] array = streamToBytes(is);
            mMovie = Movie.decodeByteArray(array, 0, array.length);
        }
        p = new Paint();
    }

    public void setAnimatedGif(String filePath, TYPE streachType) throws FileNotFoundException {
        setImageBitmap(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mType = streachType;
        animatedGifImage = true;
        InputStream is;
        try {
            mMovie = Movie.decodeFile(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            is = new FileInputStream(filePath);
            byte[] array = streamToBytes(is);
            mMovie = Movie.decodeByteArray(array, 0, array.length);
        }
        p = new Paint();
    }

    public void setAnimatedGif(byte[] byteArray, TYPE streachType) throws FileNotFoundException {
        setImageBitmap(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mType = streachType;
        animatedGifImage = true;

        try {
            mMovie = Movie.decodeByteArray(byteArray, 0, byteArray.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        p = new Paint();
    }


    @Override
    public void setImageResource(int resId) {
        animatedGifImage = false;
        super.setImageResource(resId);
    }

    @Override
    public void setImageURI(Uri uri) {
        animatedGifImage = false;
        super.setImageURI(uri);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        animatedGifImage = false;
        super.setImageDrawable(drawable);
    }

    Paint p;
    private float mScaleH = 1f, mScaleW = 1f;
    private int mMeasuredMovieWidth;
    private int mMeasuredMovieHeight;
    private float mLeft;
    private float mTop;

    private static byte[] streamToBytes(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = is.read(buffer)) >= 0) {
                os.write(buffer, 0, len);
            }
        } catch (java.io.IOException e) {
        }
        return os.toByteArray();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMovie != null) {
            int movieWidth = mMovie.width();
            int movieHeight = mMovie.height();
			/*
			 * Calculate horizontal scaling
			 */
            int measureModeWidth = MeasureSpec.getMode(widthMeasureSpec);
            float scaleW = 1f, scaleH = 1f;
            if (measureModeWidth != MeasureSpec.UNSPECIFIED) {
                int maximumWidth = MeasureSpec.getSize(widthMeasureSpec);
                if (movieWidth > maximumWidth) {
                    scaleW = (float) movieWidth / (float) maximumWidth;
                } else {
                    scaleW = (float) maximumWidth / (float) movieWidth;
                }
            }

			/*
			 * calculate vertical scaling
			 */
            int measureModeHeight = MeasureSpec.getMode(heightMeasureSpec);

            if (measureModeHeight != MeasureSpec.UNSPECIFIED) {
                int maximumHeight = MeasureSpec.getSize(heightMeasureSpec);
                if (movieHeight > maximumHeight) {
                    scaleH = (float) movieHeight / (float) maximumHeight;
                } else {
                    scaleH = (float) maximumHeight / (float) movieHeight;
                }
            }

			/*
			 * calculate overall scale
			 */
            switch (mType) {
                case FIT_CENTER:
                    mScaleH = mScaleW = Math.min(scaleH, scaleW);
                    break;
                case AS_IS:
                    mScaleH = mScaleW = 1f;
                    break;
                case STREACH_TO_FIT:
                    mScaleH = scaleH;
                    mScaleW = scaleW;
                    break;
            }

            mMeasuredMovieWidth = (int) (movieWidth * mScaleW);
            mMeasuredMovieHeight = (int) (movieHeight * mScaleH);

            setMeasuredDimension(mMeasuredMovieWidth, mMeasuredMovieHeight);

        } else {
            setMeasuredDimension(getSuggestedMinimumWidth(),
                    getSuggestedMinimumHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mLeft = (getWidth() - mMeasuredMovieWidth) / 2f;
        mTop = (getHeight() - mMeasuredMovieHeight) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animatedGifImage) {
            long now = android.os.SystemClock.uptimeMillis();
            if (mMovieStart == 0) { // first time
                mMovieStart = now;
            }
            if (mMovie != null) {
                p.setAntiAlias(true);
                int dur = mMovie.duration();
                if (dur == 0) {
                    dur = 1000;
                }
                int relTime = (int) ((now - mMovieStart) % dur);
                mMovie.setTime(relTime);
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.scale(mScaleW, mScaleH);
                mMovie.draw(canvas, mLeft / mScaleW, mTop / mScaleH);
                canvas.restore();
                invalidate();
            }
        }

    }

}
