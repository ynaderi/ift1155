package com.example.dift1155.myapplication;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PopulateDatabaseService extends IntentService {

    public PopulateDatabaseService() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);


        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String[] tables = intent.getExtras().getStringArray("tables");

        Log.d("test", "Population des tables: " + StringUtils.join(tables, ", "));

        OkHttpClient client = new OkHttpClient();


        try {
            String lastModified = client.newCall(new Request.Builder()
                    .head()
                    .url("http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip")
                    .build()).execute().header("Last-Modified");

            String feedEndData = db.query("feed_info", new String[]{"feed_end_date"}, null, new String[]{}, null, null, null).getString(0);

            // TODO: comparer lastModified > feedEndDate (alors télécharger la mise à jour)

        } catch (IOException e) {
            e.printStackTrace();
        }

        Request req = new Request.Builder()
                .get()
                .url("http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip")
                .build();

        Response res = null;
        try {
            res = client.newCall(req).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        InputStream inputStream = res.body().byteStream();

        ZipInputStream zis = new ZipInputStream(inputStream);

        ZipEntry currentEntry;


        Log.d("test", "Max size: " + db.getMaximumSize());

        long begin  = System.nanoTime();

        // vider les tables en question!
        for (String table : tables) {
            db.delete(table, null, new String[]{});
        }

        Random rnd = new Random();

        try {
            while ((currentEntry = zis.getNextEntry()) != null) {

                Log.d("test", currentEntry.getName());

                for (int i = 0; i < tables.length; i++) {

                    String tableName = tables[i];

                    if (tableName.equals(currentEntry.getName().substring(0, currentEntry.getName().lastIndexOf(".")))) {

                        Log.i("test", "Chargement de la table " + tableName + "...");

                        nm.notify(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                                .setContentTitle("Chargement de la table " + tableName + "...")
                                .setSmallIcon(android.R.drawable.ic_popup_sync)
                                .setCategory(Notification.CATEGORY_PROGRESS)
                                .setProgress(100, 0, true)
                                .build());

                        CSVParser parser = new CSVParser(new InputStreamReader(zis), CSVFormat.RFC4180.withHeader());

                        long cumulativeSize = 0;

                        for (CSVRecord row : parser) {

                            // TODO: ...
                            //if ((row.getRecordNumber() - 2) % BATCH_SIZE == 0)

                            ContentValues values = new ContentValues();

                            for (Map.Entry<String, String> e : row.toMap().entrySet()) {
                                if (e.getValue().isEmpty()) { // gère les valeurs vides en CSV
                                    values.putNull(e.getKey());
                                } else {
                                    values.put(e.getKey(), e.getValue());
                                }
                            }

                            cumulativeSize += row.toString().length();

                            nm.notify(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                                    .setContentTitle("Chargement de la table " + tableName + "...")
                                    .setSmallIcon(android.R.drawable.ic_popup_sync)
                                    .setCategory(Notification.CATEGORY_PROGRESS)
                                    .setProgress(100, (int) (cumulativeSize / currentEntry.getSize()) * 100, false)
                                    .build());

                            db.insert(tableName, null, values);
                        }
                    }
                }
            }

            db.setTransactionSuccessful();

        } catch (IOException err) {
            Log.e("test", "", err);
        } finally {
            nm.cancel(R.id.PROGRESS_NOTIFICATION_ID);
            db.endTransaction();
        }

        Toast.makeText(PopulateDatabaseService.this, "Imported tables " + Arrays.toString(tables), Toast.LENGTH_SHORT).show();
    }
}
