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
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PopulateDatabaseService extends IntentService {

    public static final int BATCH_SIZE = 50;

     public PopulateDatabaseService() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String[] tables =  intent.getExtras().getStringArray("tables");

        Log.d("test", "Population des tables: " + StringUtils.join(tables, ", "));

        ZipInputStream zis =  new ZipInputStream(getResources().openRawResource(R.raw.gtfs_stm));

        ZipEntry currentEntry;

        SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);

        db.setForeignKeyConstraintsEnabled(true);

        try {
            while ((currentEntry = zis.getNextEntry()) != null) {

                Log.d("test", currentEntry.getName());

                for (int i = 0; i < tables.length; i++) {

                    String tableName = tables[i];

                    Bundle progressBundle = new Bundle();

                    progressBundle.putInt(Notification.EXTRA_PROGRESS, i);
                    progressBundle.putInt(Notification.EXTRA_PROGRESS_MAX, tables.length);

                    nm.notify(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                            .setContentTitle("Chargement de la table " + tableName + "...")
                            .setCategory(Notification.CATEGORY_PROGRESS)
                            .setExtras(progressBundle)
                            .build());

                    if (tableName.equals(currentEntry.getName().substring(0, currentEntry.getName().lastIndexOf(".")))) {

                        CSVParser parser = new CSVParser(new InputStreamReader(zis), CSVFormat.RFC4180);
                        Iterator<CSVRecord> iter = parser.iterator();
                        while (iter.hasNext()) {
                            CSVRecord row = iter.next();

                            // TODO: ...
                            if (row.getRecordNumber() % BATCH_SIZE == 0)
                                db.beginTransaction();

                            ContentValues values = new ContentValues();

                            for (Map.Entry<String, String> e : row.toMap().entrySet()) {
                                values.put(e.getKey(), e.getValue());
                            }

                            db.insert(tableName, null, values);

                            if (row.getRecordNumber() % BATCH_SIZE == BATCH_SIZE - 1 || !iter.hasNext())
                                db.endTransaction();
                        }
                    }
                }
            }
        } catch (IOException err) {
            Log.e("test", "", err);
        }

        Toast.makeText(PopulateDatabaseService.this, "Imported tables " + Arrays.toString(tables), Toast.LENGTH_SHORT).show();
    }
}
