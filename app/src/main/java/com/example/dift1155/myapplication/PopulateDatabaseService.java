package com.example.dift1155.myapplication;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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

public class PopulateDatabaseService extends Service {

    public static final int BATCH_SIZE = 50;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends android.os.Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String[] tables =  msg.getData().getStringArray("tables");

            ZipInputStream zis =  new ZipInputStream(getResources().openRawResource(R.raw.gtfs_stm));

            ZipEntry currentEntry;

            SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);

            db.setForeignKeyConstraintsEnabled(true);

            try {
                while ((currentEntry = zis.getNextEntry()) != null) {

                    for (int i = 0; i < tables.length; i++) {

                        String tableName = tables[i];

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
                Log.e("", "", err);
            }

            Toast.makeText(PopulateDatabaseService.this, "Imported tables " + Arrays.toString(tables), Toast.LENGTH_SHORT).show();
        }
    }

    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Message m = mServiceHandler.obtainMessage();
        m.setData(intent.getExtras());
        mServiceHandler.sendMessage(m);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
