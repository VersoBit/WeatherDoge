package com.versobit.weatherdoge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private static final int REQUEST_PLAY_ERR_DIAG = 52000000;
    private static final int REQUEST_PLAY_CONN_FAIL_RES = 3643;
    private static final String TAG = "MainActivity";

    private boolean forceMetric = false;
    private String forceLocation = "";

    private RelativeLayout suchLayout;
    private ImageView suchDoge;
    private TextView suchStatus;
    private TextView suchNegative;
    private TextView suchTemp;
    private TextView suchDegree;
    private TextView suchLocation;
    private ImageView suchShare;
    private ImageView suchOptions;
    private LocationClient wowClient;
    private Location whereIsDoge;
    private Typeface wowComicSans;

    private AlertDialog errorDialog;

    private double currentTemp;
    private String currentLocation;
    private String[] dogefixes;
    private String[] tempAdjectives;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        loadOptions();

        dogefixes = getResources().getStringArray(R.array.dogefix);

        wowComicSans = Typeface.createFromAsset(getAssets(), "comic.ttf");
        suchLayout = (RelativeLayout)findViewById(R.id.main_suchlayout);
        suchDoge = (ImageView)findViewById(R.id.main_suchdoge);
        suchDoge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whereIsDoge = wowClient.getLastLocation();
                new GetWeather(MainActivity.this).execute(whereIsDoge);
            }
        });
        suchStatus = (TextView)findViewById(R.id.main_suchstatus);
        suchStatus.setTypeface(wowComicSans);
        suchNegative = (TextView)findViewById(R.id.main_suchnegative);
        suchNegative.setTypeface(wowComicSans);
        suchTemp = (TextView)findViewById(R.id.main_suchtemp);
        suchTemp.setTypeface(wowComicSans);
        suchDegree = (TextView)findViewById(R.id.main_suchdegree);
        suchDegree.setTypeface(wowComicSans);
        suchLocation = (TextView)findViewById(R.id.main_suchlocation);
        suchLocation.setTypeface(wowComicSans);
        suchShare = (ImageView)findViewById(R.id.main_suchshare);
        suchShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                String unit = (char)0x00b0 + ((UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric) ? "F" : "C");
                String temp = String.valueOf(Math.round(currentTemp)) + ' ' + unit;
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title, temp, currentLocation));
                Random r = new Random();
                String dogeism = String.format(dogefixes[r.nextInt(dogefixes.length)], tempAdjectives[r.nextInt(tempAdjectives.length)]);
                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, dogeism, temp, currentLocation));
                startActivity(Intent.createChooser(i, getString(R.string.action_share)));
            }
        });
        suchShare.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, R.string.action_share, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        suchOptions = (ImageView)findViewById(R.id.main_suchoptions);
        suchOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OptionsActivity.class));
            }
        });
        suchOptions.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, R.string.action_options, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        if(!forceLocation.isEmpty()) {
            new GetWeather(this).execute();
        } else if(playServicesAvailable()) {
            wowClient = new LocationClient(this, this, this);
        }
    }

    private void loadOptions() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        forceMetric = sp.getBoolean(OptionsActivity.PREF_FORCE_METRIC, false);
        forceLocation = sp.getString(OptionsActivity.PREF_FORCE_LOCATION, "");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(wowClient != null) {
            wowClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if(wowClient != null && wowClient.isConnected()) {
            wowClient.removeLocationUpdates(this);
            wowClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOptions();
        if(forceLocation.isEmpty()) {
            if(wowClient == null) {
                wowClient = new LocationClient(this, this, this);
            }
            if(!wowClient.isConnected() && !wowClient.isConnecting()) {
                wowClient.connect();
            }
        } else {
            if(wowClient != null && wowClient.isConnected()) {
                wowClient.removeLocationUpdates(this);
                wowClient.disconnect();
            }
            new GetWeather(this).execute();
        }
    }

    private boolean playServicesAvailable() {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(result == ConnectionResult.SUCCESS) {
            return true;
        }
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(result, this, REQUEST_PLAY_ERR_DIAG);
        if(errorDialog != null) {
            errorDialog.show();
        }
        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Play Services Connected");
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        request.setInterval(5000);
        request.setFastestInterval(1000);
        wowClient.requestLocationUpdates(request, this);
        whereIsDoge = wowClient.getLastLocation();
        if(whereIsDoge == null) {
            Log.e(TAG, "dunno where this shibe is");
            return;
        }
        new GetWeather(this).execute(whereIsDoge);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Play Services Disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(!connectionResult.hasResolution()) {
            Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            connectionResult.startResolutionForResult(this, REQUEST_PLAY_CONN_FAIL_RES);
        } catch (IntentSender.SendIntentException ex) {
            Log.wtf(TAG, ex);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        whereIsDoge = location;
        new GetWeather(this).execute(whereIsDoge);
    }

    private class GetWeather extends AsyncTask<Location, Void, JSONArray> {
        private static final String TAG = "GetWeather";
        private Context ctx;
        GetWeather(Context ctx) {
            super();
            this.ctx = ctx;
        }
        @Override
        protected JSONArray doInBackground(Location... params) {
            if(!Geocoder.isPresent() && forceLocation.isEmpty()) {
                Log.wtf(TAG, new UnsupportedOperationException("No Geocoder is present on this device."));
                return null;
            }
            JSONArray data = new JSONArray();
            Address address = null;
            if(forceLocation.isEmpty()) {
                Geocoder geocoder = new Geocoder(ctx);
                Location loc = params[0];
                try {
                    List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if(addresses == null || addresses.size() == 0) {
                        return null;
                    }
                    address = addresses.get(0);
                } catch (Exception ex) {
                    if(ex.getMessage().equalsIgnoreCase("Service not Available")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(errorDialog != null && errorDialog.isShowing()) {
                                    return;
                                }
                                AlertDialog.Builder adb = new AlertDialog.Builder(new ContextThemeWrapper(ctx, R.style.AppTheme_Options));
                                adb.setTitle(R.string.geocoder_error_title).setMessage(R.string.geocoder_error_msg);
                                adb.setNeutralButton(R.string.wow, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                errorDialog = adb.show();
                            }
                        });
                    }
                    Log.wtf(TAG, ex);
                    return null;
                }
                data.put(address.getLocality());
            }

            try {
                HttpClient httpClient = new DefaultHttpClient();
                String queryString;
                if(forceLocation.isEmpty() && address != null) {
                    queryString = "?lat=" + URLEncoder.encode(String.valueOf(address.getLatitude()), "ISO-8859-1") +
                            "&lon=" + URLEncoder.encode(String.valueOf(address.getLongitude()), "ISO-8859-1");
                } else {
                    queryString = "?q=" + URLEncoder.encode(forceLocation, "ISO-8859-1");
                }
                HttpGet httpGet = new HttpGet("http://api.openweathermap.org/data/2.5/weather" + queryString);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                JSONObject weatherJson = new JSONObject(IOUtils.toString(httpEntity.getContent()));
                if(weatherJson.getInt("cod") != 200) {
                    Toast.makeText(ctx, weatherJson.getString("message"), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "OWM Error " + weatherJson.getInt("cod") + ", " + weatherJson.getString("message"));
                    return null;
                }
                if(!forceLocation.isEmpty()) {
                    data.put(weatherJson.getString("name"));
                }
                data.put(weatherJson);
            } catch (Exception ex) {
                Log.wtf(TAG, ex);
                return null;
            }
            return data;
        }

        @Override
        protected void onPostExecute(JSONArray data) {
            if(data == null) {
                return;
            }

            try {
                currentLocation = data.getString(0);
                suchLocation.setText(currentLocation);
                JSONObject subWeather = data.getJSONObject(1).getJSONArray("weather").getJSONObject(0);
                JSONObject subMain = data.getJSONObject(1).getJSONObject("main");
                suchStatus.setText(getString(R.string.wow) + " " + subWeather.getString("description").trim().toLowerCase());
                setTemp(subMain.getDouble("temp"));
                suchDoge.setImageResource(WeatherDoge.dogeSelect(subWeather.getString("icon")));
                suchLayout.setBackgroundResource(WeatherDoge.skySelect(subWeather.getString("icon")));
            } catch (JSONException ex) {
                Log.wtf(TAG, ex);
            }
        }

        private void setTemp(double temp) {
            temp = temp - 273.15d; // C
            tempAdjectives = WeatherDoge.getTempAdjectives(getResources(), (int)Math.round(temp));
            if(UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric) {
                temp = temp * 1.8d + 32d; // F
            }
            currentTemp = temp;
            temp = Math.round(temp);
            DecimalFormat df = new DecimalFormat();
            df.setNegativePrefix("");
            df.setNegativeSuffix("");
            df.setMaximumFractionDigits(0);
            df.setDecimalSeparatorAlwaysShown(false);
            df.setGroupingUsed(false);
            suchTemp.setText(df.format(temp));
            suchNegative.setVisibility(temp < 0d ? View.VISIBLE : View.GONE);
            suchDegree.setVisibility(View.VISIBLE);
        }
    }
}
