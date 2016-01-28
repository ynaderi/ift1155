package com.example.dift1155.myapplication;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import org.apache.commons.csv.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new AsyncTask<String,Integer,Void>() {

            @Override
            protected Void doInBackground(String... params) {

                Integer[] progress = new Integer[params.length];

                ZipInputStream zis =  new ZipInputStream(getResources().openRawResource(R.raw.gtfs_stm));

                ZipEntry currentEntry;

                try {
                    while ((currentEntry = zis.getNextEntry()) != null) {
                        for (int i = 0; i < params.length; i++) {
                            String tableName = params[i];

                            if (tableName.equals(currentEntry.getName().substring(0, currentEntry.getName().lastIndexOf(".")))) {

                                CSVParser parser = new CSVParser(new InputStreamReader(zis), CSVFormat.RFC4180);

                                for (CSVRecord row : parser) {
                                    progress[i] += 1;
                                    publishProgress(progress);
                                }
                            }

                        }
                    }
                } catch (IOException err) {
                    // ...
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                // TODO: progrès!
            }

            @Override
            protected void onPostExecute(Void result) {

            }

        }.execute("routes", "stop_times", "stops", "trips");

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
