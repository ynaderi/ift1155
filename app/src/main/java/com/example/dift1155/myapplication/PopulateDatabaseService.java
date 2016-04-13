package com.example.dift1155.myapplication;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Sink;

public class PopulateDatabaseService extends IntentService {

    /**
     * Taille d'une batch pour insérer dans une transaction.
     */
    public static int BATCH_SIZE = 100000;

    public PopulateDatabaseService() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        startForeground(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                .setContentTitle("Chargement des données")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setProgress(100, 0, true)
                .build());

        SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String[] tables = intent.getExtras().getStringArray("tables");

        Log.d("test", "Population des tables: " + StringUtils.join(tables, ", "));

        Cursor feedInfoCursor = db.query("feed_info", new String[]{"feed_start_date", "feed_end_date"}, null, new String[]{}, null, null, null);

        OkHttpClient client = new OkHttpClient();

        if (feedInfoCursor.moveToFirst()) {
            String feedStartDate  = feedInfoCursor.getString(feedInfoCursor.getColumnIndex("feed_start_date"));
            String feedEndDate = feedInfoCursor.getString(feedInfoCursor.getColumnIndex("feed_end_date"));

            try {
                String lastModified = client.newCall(new Request.Builder()
                        .head()
                        .url("http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip")
                        .build()).execute().header("Last-Modified");

                Log.i("test", feedStartDate + " <= " + lastModified + " <= " + feedEndDate);

                // TODO: si feedStartDate <= lastModified <= feedEndDate then return!

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        nm.notify(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                .setContentTitle("Téléchargement des données...")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setProgress(100, 0, true)
                .build());

        Request req = new Request.Builder()
                .get()
                .url("http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip")
                .build();

        File outputFile;
        try {
            outputFile = File.createTempFile("gtfs_stm.zip", null, getCacheDir());
            FileOutputStream fio = new FileOutputStream(outputFile);
            IOUtils.copyLarge(client.newCall(req).execute().body().byteStream(), fio);
            IOUtils.closeQuietly(fio);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(outputFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // prépare la base pour l'insertion *massive*
        db.setForeignKeyConstraintsEnabled(false);
        db.rawQuery("pragma journal_mode=off", new String[]{});
        db.rawQuery("pragma synchronous=off", new String[]{});

        // vider les tables en question!
        for (String table : tables) {
            db.delete(table, null, new String[]{});
        }

        try {
            ZipEntry currentEntry;
            while ((currentEntry = zis.getNextEntry()) != null) {

                Log.d("test", currentEntry.getName());

                for (int i = 0; i < tables.length; i++) {

                    String tableName = tables[i];

                    if (tableName.equals(currentEntry.getName().substring(0, currentEntry.getName().lastIndexOf(".")))) {
                        long begin = System.currentTimeMillis();

                        Log.i("test", "Chargement de la table " + tableName + "...");

                        nm.notify(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                                .setContentTitle("Chargement de la table " + tableName + "...")
                                .setSmallIcon(android.R.drawable.ic_popup_sync)
                                .setCategory(Notification.CATEGORY_PROGRESS)
                                .setProgress(100, 0, true)
                                .build());

                        CSVParser parser = new CSVParser(new InputStreamReader(zis), CSVFormat.RFC4180.withHeader());
                        ContentValues values = new ContentValues();
                        DatabaseUtils.InsertHelper insertHelper = new DatabaseUtils.InsertHelper(db, tableName);
                        long cumulativeSizeApproximation = 0;
                        long batchBegin = System.currentTimeMillis();

                        for (CSVRecord row : parser) {
                            if ((row.getRecordNumber() - 1) % BATCH_SIZE == 0) {
                                batchBegin = System.currentTimeMillis();
                            }

                            for (Map.Entry<String, String> e : row.toMap().entrySet()) {
                                if (e.getValue().isEmpty()) { // gère les valeurs vides en CSV
                                    values.putNull(e.getKey());
                                } else {
                                    values.put(e.getKey(), e.getValue());
                                }
                            }

                            insertHelper.insert(values);

                            cumulativeSizeApproximation += row.toString().length();

                            if ((row.getRecordNumber() - 1) % BATCH_SIZE == BATCH_SIZE - 1) {
                                insertHelper.prepareForInsert();
                                insertHelper.execute();
                                Log.i("test", "Inséré la batch #"
                                        + (row.getRecordNumber() / BATCH_SIZE) + "/~" + ((currentEntry.getSize() / (cumulativeSizeApproximation / row.getRecordNumber())) / BATCH_SIZE)
                                        + " dans la table " + tableName
                                        + " en " + (System.currentTimeMillis() - batchBegin) + "ms"
                                        + " (" + BATCH_SIZE / (((System.currentTimeMillis() - batchBegin) / 1000)) + " insert/sec)");
                                nm.notify(R.id.PROGRESS_NOTIFICATION_ID, new Notification.Builder(PopulateDatabaseService.this)
                                        .setContentTitle("Chargement de la table " + tableName + "...")
                                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                                        .setCategory(Notification.CATEGORY_PROGRESS)
                                        .setProgress(100, (int) (cumulativeSizeApproximation / currentEntry.getSize()) * 100, false)
                                        .build());
                            }
                        }

                        // insert incomplete batch (if appliable)
                        insertHelper.prepareForInsert();
                        insertHelper.execute();

                        Log.i("test", "Terminé l'insertion de la table " + tableName + " en " + (System.currentTimeMillis() - begin) + "ms");
                    }
                }
            }

            Toast.makeText(PopulateDatabaseService.this, "Importé les tables: " + Arrays.toString(tables), Toast.LENGTH_SHORT).show();

        } catch (IOException err) {
            err.printStackTrace();
        } finally {
            nm.cancel(R.id.PROGRESS_NOTIFICATION_ID);
            outputFile.delete();
        }
    }
}
