package com.example.dift1155.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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

    public static final int DATABASE_INITIAL_SCHEMA = 1,
            DATABASE_MIGRATION_1_1 = 2;

    /**
     * Applique une migration sur la base de données fournie.
     *
     * @param db
     * @param version     version appliquée une fois la migration réussie
     * @param migrationId identifiant de la ressource qui contient les requête SQL
     */
    private void applyMigration(SQLiteDatabase db, int version, int migrationId) {
        if (db.needUpgrade(version)) {
            InputStream migration = getResources().openRawResource(migrationId);
            db.beginTransaction();
            try {
                String schema = IOUtils.toString(migration)
                        .replaceAll("--.*\r?\n", " ") // commentaires
                        .replaceAll("\r?\n", " ");  // nouvelles lignes
                for (String statement : schema.split(";")) {
                    String st = StringUtils.normalizeSpace(statement);
                    Log.i("test", st);
                    if (!st.isEmpty())
                        db.execSQL(st);
                }
                db.setVersion(version);
                db.setTransactionSuccessful();
                Log.i("test", "La migration " + version + " a été appliquée.");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                IOUtils.closeQuietly(migration);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteDatabase("stm_gtfs");
        final SQLiteDatabase db = openOrCreateDatabase("stm_gtfs", MODE_PRIVATE, null);

        // création du schéma 1.0 et migrations
        applyMigration(db, DATABASE_INITIAL_SCHEMA, R.raw.schema);
        applyMigration(db, DATABASE_MIGRATION_1_1, R.raw.migration_1_1);

        // TODO: détecter les mises à jour
        Intent populateDatabaseIntent = new Intent(MainActivity.this, PopulateDatabaseService.class);

        String[] tables = {"calendar_dates", "feed_info", "stops", "routes", "shapes", "trips", "stop_times"};
        populateDatabaseIntent.putExtra("tables", tables);

        startService(populateDatabaseIntent);

        if (true)
            return;

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.clickable(true);
                for (String latLon : "45.527092,-73.586233;45.527033,-73.586111;45.526997,-73.586081;45.526961,-73.58608;45.52685,-73.586179;45.526724,-73.58632;45.526697,-73.586345;45.526413,-73.586624;45.526638,-73.587168;45.526717,-73.587333;45.52674,-73.587387;45.52677,-73.58746;45.526764,-73.587542;45.526423,-73.587857;45.526285,-73.587983;45.526246,-73.588018;45.525606,-73.588575;45.525576,-73.588602;45.525156,-73.588984;45.525125,-73.589012;45.524904,-73.589214;45.524873,-73.589242;45.524499,-73.589597;45.524467,-73.589624;45.524076,-73.589956;45.523958,-73.590057;45.523927,-73.590085;45.522915,-73.590991;45.522569,-73.591304;45.52244,-73.591421;45.523064,-73.592768;45.523047,-73.59285;45.522533,-73.593292;45.522501,-73.59332;45.521985,-73.593796;45.521897,-73.593875;45.521859,-73.593909;45.52102,-73.594634;45.520989,-73.594661;45.520345,-73.595221;45.520313,-73.595248;45.519821,-73.595675;45.519655,-73.595819;45.519616,-73.595851;45.519343,-73.596095;45.518713,-73.596657;45.518681,-73.596687;45.518085,-73.597214;45.518053,-73.597241;45.517463,-73.597764;45.517432,-73.597791;45.517026,-73.598134;45.51695,-73.598198;45.516918,-73.598225;45.516383,-73.598683;45.516352,-73.598712;45.516053,-73.599005;45.515917,-73.600646;45.515891,-73.601086;45.515888,-73.601139;45.515858,-73.601649;45.515836,-73.602022;45.515832,-73.602074;45.515653,-73.604178;45.515617,-73.604576;45.51558,-73.604872;45.51551,-73.60524;45.515499,-73.605289;45.515443,-73.60549;45.515375,-73.605815;45.515363,-73.605871;45.515295,-73.606316;45.515288,-73.606368;45.515245,-73.606664;45.515182,-73.606973;45.515092,-73.607435;45.514973,-73.607909;45.514842,-73.608384;45.514811,-73.608498;45.514691,-73.608957;45.514675,-73.609016;45.514594,-73.609292;45.514223,-73.610656;45.514207,-73.610715;45.514079,-73.611159;45.514079,-73.611159;45.514077,-73.611165;45.513975,-73.611378;45.513948,-73.61143;45.513554,-73.612152;45.513345,-73.612523;45.51332,-73.612564;45.51308,-73.612984;45.512994,-73.613136;45.512461,-73.614132;45.512438,-73.614176;45.511976,-73.615066;45.511706,-73.615551;45.511663,-73.615526;45.511476,-73.615106;45.510945,-73.613919;45.510775,-73.613531;45.510756,-73.613488;45.510568,-73.613069;45.510549,-73.613027;45.510379,-73.612641;45.510227,-73.612293;45.510203,-73.612231;45.510112,-73.612338;45.510061,-73.61241;45.510024,-73.612488;45.509981,-73.612563;45.509927,-73.612728;45.509889,-73.612808;45.50979,-73.612975;45.509789,-73.612976;45.509785,-73.61298;45.509721,-73.613044;45.508693,-73.613983;45.508127,-73.6145;45.507392,-73.615154;45.507296,-73.615239;45.507266,-73.615266;45.505371,-73.616992;45.505305,-73.617052;45.505275,-73.61708;45.50427,-73.617982;45.504236,-73.618012;45.504056,-73.618166;45.50348,-73.618688;45.503378,-73.618782;45.503289,-73.618865;45.503257,-73.618893;45.501343,-73.620632;45.501279,-73.620691;45.501249,-73.620719;45.499511,-73.622321;45.4993,-73.622515;45.499241,-73.622497;45.49878,-73.621658;45.498758,-73.621617;45.498367,-73.620915;45.498344,-73.620875;45.498276,-73.620757;45.497954,-73.620183;45.497931,-73.620145;45.49785,-73.620004;45.497793,-73.619905;45.497772,-73.619863;45.497127,-73.618559;45.497106,-73.618517;45.496937,-73.618194;45.496822,-73.617948;45.496705,-73.617701;45.496612,-73.617746;45.496281,-73.618047;45.49625,-73.618074;45.495713,-73.618557;45.495683,-73.618586;45.495309,-73.618924;45.495163,-73.619055;45.495124,-73.619089;45.494986,-73.619207;45.494947,-73.619238;45.494892,-73.619276;45.494651,-73.619378;45.494614,-73.619411;45.493716,-73.620231;45.493081,-73.620813;45.493049,-73.620843;45.491284,-73.622413;45.490936,-73.622745;45.490896,-73.622783;45.4904,-73.623231;45.490131,-73.623451;45.490097,-73.62348;45.489921,-73.623646;45.489834,-73.623728;45.489257,-73.624238;45.488754,-73.624704;45.488653,-73.624797;45.488618,-73.624828;45.487317,-73.625994;45.487212,-73.626089;45.487011,-73.62629;45.486981,-73.62632;45.485358,-73.627749;45.485272,-73.627824;45.485242,-73.627851;45.484877,-73.628167;45.48485,-73.628203;45.484777,-73.628328;45.484773,-73.628334;45.484771,-73.628336;45.484473,-73.628585;45.484391,-73.628654;45.48436,-73.628679;45.483846,-73.629059;45.483807,-73.629091;45.483529,-73.629326;45.483491,-73.629359;45.482805,-73.629956;45.482734,-73.630017;45.482703,-73.630046;45.482022,-73.630681;45.481991,-73.630709;45.481221,-73.631407;45.481123,-73.631495;45.481088,-73.631528;45.480511,-73.632055;45.480437,-73.632123;45.480406,-73.632152;45.480088,-73.632456;45.480009,-73.632523;45.479795,-73.63273;45.479764,-73.632758;45.479128,-73.63333;45.479097,-73.633358;45.478518,-73.633883;45.478425,-73.633968;45.478395,-73.633996;45.478267,-73.634108;45.478152,-73.634245;45.478028,-73.634471;45.477967,-73.634633;45.477921,-73.634798;45.477831,-73.635282;45.477741,-73.635906;45.477688,-73.63662;45.477649,-73.636667;45.477594,-73.636659;45.477558,-73.636654;45.476808,-73.636555;45.476763,-73.636561;45.476665,-73.636582;45.47658,-73.636629;45.476373,-73.63682;45.476086,-73.637087;45.475705,-73.63743;45.475674,-73.637458;45.475175,-73.63789;45.475102,-73.637954;45.475071,-73.637982;45.474489,-73.638527;45.474458,-73.638555;45.473893,-73.63905;45.473805,-73.639128;45.473773,-73.639154;45.473146,-73.639713;45.473115,-73.63974;45.473002,-73.63982;45.472875,-73.639884;45.472873,-73.639884;45.472871,-73.639885;45.472726,-73.63991;45.472571,-73.639923;45.472213,-73.639922;45.472033,-73.639947;45.471855,-73.639985;45.471847,-73.639987;45.471703,-73.640028;45.471666,-73.640043;45.471094,-73.640479;45.471062,-73.640503;45.470517,-73.64095;45.470485,-73.640975;45.469913,-73.641434;45.469882,-73.64146;45.469439,-73.641823;45.469341,-73.641903;45.469301,-73.641934;45.468761,-73.642352;45.468728,-73.642378;45.468183,-73.642798;45.468153,-73.642822;45.467618,-73.643237;45.467526,-73.643308;45.467495,-73.643332;45.466824,-73.643856;45.466792,-73.643882;45.466086,-73.64443;45.466053,-73.644455;45.465465,-73.644921;45.465392,-73.644978;45.46536,-73.645003;45.464717,-73.645488;45.464684,-73.645513;45.464014,-73.64601;45.463982,-73.646036;45.463577,-73.646365;45.463451,-73.646566;45.463435,-73.646592;45.46339,-73.646654;45.463361,-73.64669;45.463306,-73.646749;45.462834,-73.647158;45.462801,-73.647184;45.462332,-73.647535;45.462257,-73.647591;45.462225,-73.647617;45.461717,-73.648025;45.461684,-73.64805;45.461274,-73.648375;45.461185,-73.648445;45.461153,-73.648471;45.460653,-73.648866;45.460622,-73.64889;45.460464,-73.649015;45.46007,-73.649323;45.460014,-73.6493;45.459918,-73.649044;45.459898,-73.648993;45.458612,-73.645654;45.458572,-73.64555;45.458553,-73.645499;45.4575,-73.642758;45.45745,-73.642629;45.457433,-73.642584;45.456608,-73.64046;45.456528,-73.640254;45.455904,-73.640762;45.455873,-73.640788;45.455546,-73.641056;45.455378,-73.641191;45.455292,-73.64126;45.45526,-73.641285;45.455033,-73.641463;45.454701,-73.641718;45.454634,-73.641781;45.454519,-73.641801;45.454483,-73.641805;45.454479,-73.641805;45.454478,-73.641805;45.454397,-73.641779;45.454392,-73.641775;45.454386,-73.641769;45.454243,-73.641487;45.454241,-73.641483;45.45424,-73.641479;45.454237,-73.641465".split(";")) {
                    polylineOptions.add(new LatLng(Double.parseDouble(latLon.split(",")[0]), Double.parseDouble(latLon.split(",")[1])));
                }
                googleMap.addPolyline(polylineOptions);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.527092d, -73.586233d), 10.0f));
                googleMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
                    @Override
                    public void onPolylineClick(Polyline polyline) {
                        polyline.setColor(Color.RED);
                    }
                });
            }
        });


        //
        try {
            String kBestTrips = IOUtils.toString(getResources().openRawResource(R.raw.k_best_trips))
                    .replaceAll("[\r\n]|--$", "")
                    .replaceAll(":\\w+", "?");

            Log.i("test", kBestTrips);
            // [\w-] [A-Za-z]
            String departureTime = "now";
            String aLat = "45.5010115", aLon = "-73.6179101";
            String maxWalkingDistance = "1000";
            String bLat = "45.4961719", bLon = "-73.6247091";
            Cursor cursor = db.rawQuery(StringUtils.normalizeSpace(kBestTrips), new String[]{
                    departureTime,
                    aLat,
                    aLon,
                    aLat,
                    aLon,
                    maxWalkingDistance,
                    bLat,
                    bLon,
                    maxWalkingDistance,
                    aLat,
                    aLon,
                    bLat,
                    bLon,
                    maxWalkingDistance,
                    bLat,
                    bLon,
                    "10"
            });
        } catch (IOException e) {
            e.printStackTrace();
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
