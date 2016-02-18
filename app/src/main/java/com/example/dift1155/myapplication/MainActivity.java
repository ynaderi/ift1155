package com.example.dift1155.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import org.apache.commons.csv.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    public static final int DATABASE_INITIAL_SCHEMA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteDatabase("stm_gtfs");
        final SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);

        // création du schéma 1.0
        if (db.needUpgrade(DATABASE_INITIAL_SCHEMA)) {
            try {
                InputStream schemaStream = getResources().openRawResource(R.raw.schema);
                String schema = IOUtils.toString(schemaStream);
                IOUtils.closeQuietly(schemaStream);


                db.beginTransactionWithListener(new SQLiteTransactionListener() {
                    @Override
                    public void onBegin() {

                    }

                    @Override
                    public void onCommit() {

                        Log.i("test", "Le schéma a été créé!");

                        Intent populateDatabaseIntent = new Intent(MainActivity.this, PopulateDatabaseService.class);


                        String[] tables = {"stops", "routes", "shapes", "trips", "stop_times"};
                        populateDatabaseIntent.putExtra("tables", tables);

                        startService(populateDatabaseIntent);
                    }

                    @Override
                    public void onRollback() {
                        Log.e("test", "La création du schéma SQL a échouée.");
                    }
                });


                for (String statement : schema.split(";")) {
                    String st = StringUtils.normalizeSpace(statement.replaceAll("[\r\n]", ""));
                    if (!st.isEmpty())
                        db.execSQL(st);
                }
                db.setVersion(DATABASE_INITIAL_SCHEMA);
                db.setTransactionSuccessful();
                db.endTransaction();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
}
