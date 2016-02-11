package com.example.dift1155.myapplication;

import android.app.Activity;
import android.content.Intent;
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

        SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);

        // création du schéma 1.0
        if (db.getVersion() < DATABASE_INITIAL_SCHEMA) {
            try {
                String schema = IOUtils.toString(getResources().openRawResource(R.raw.schema));

                db.beginTransactionWithListener(new SQLiteTransactionListener() {
                    @Override
                    public void onBegin() {

                    }

                    @Override
                    public void onCommit() {
                        Intent populateDatabaseIntent = new Intent(MainActivity.this, PopulateDatabaseService.class);

                        String[] tables = {"stops", "routes", "shapes", "trips", "stop_times"};
                        populateDatabaseIntent.putExtra("tables", tables);

                        startService(populateDatabaseIntent);
                    }

                    @Override
                    public void onRollback() {
                        Log.e("", "La création du schéma SQL a échouée.");
                    }
                });

                for (String statement : schema.split(";")) {
                    db.execSQL(statement);
                }
                db.setVersion(DATABASE_INITIAL_SCHEMA);
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
