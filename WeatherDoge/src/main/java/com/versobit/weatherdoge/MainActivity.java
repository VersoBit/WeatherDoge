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
import android.util.TypedValue;
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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private static final int REQUEST_PLAY_ERR_DIAG = 52000000;
    private static final int REQUEST_PLAY_CONN_FAIL_RES = 3643;
    private static final long WOW_INTERVAL = 2300;
    private static final String TAG = "MainActivity";
    private static final String CACHE_DATA = "cache_data";
    private static final String CACHE_EXPIRES = "cache_expiry";
    private static final long CACHE_MAXAGE = 10 * 60 * 1000; // 10 minutes
    private static final int CACHE_COORD_FUZZ = 2; // 1.1km, more digits -> less fuzz

    private boolean forceMetric = false;
    private String forceLocation = "";

    private RelativeLayout suchLayout;
    private RelativeLayout suchOverlay;
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
    private String[] wows;
    private String[] weatherAdjectives;
    private int[] colors;
    private Timer overlayTimer;
    private Queue<TextView> overlays = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        loadOptions();

        dogefixes = getResources().getStringArray(R.array.dogefix);
        wows = getResources().getStringArray(R.array.wows);
        colors = getResources().getIntArray(R.array.wow_colors);

        wowComicSans = Typeface.createFromAsset(getAssets(), "comic.ttf");
        suchLayout = (RelativeLayout)findViewById(R.id.main_suchlayout);
        suchOverlay = (RelativeLayout)findViewById(R.id.main_suchoverlay);
        suchDoge = (ImageView)findViewById(R.id.main_suchdoge);
        suchDoge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!forceLocation.isEmpty()) {
                    new GetWeather(MainActivity.this).execute();
                } else if(wowClient != null && wowClient.isConnected()) {
                    whereIsDoge = wowClient.getLastLocation();
                    new GetWeather(MainActivity.this).execute(whereIsDoge);
                }
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
                if(weatherAdjectives == null) {
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text).split("\n\n")[1]);
                } else {
                    String unit = (char)0x00b0 + ((UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric) ? "F" : "C");
                    String temp = String.valueOf(Math.round(currentTemp)) + ' ' + unit;
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title, temp, currentLocation));
                    i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, WeatherDoge.getDogeism(wows, dogefixes, weatherAdjectives), temp, currentLocation));
                }
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

    private void initOverlayTimer() {
        TimerTask handleOverlayText = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weatherAdjectives == null) {
                            return;
                        }
                        TextView tv = new TextView(MainActivity.this);
                        Random r = new Random();
                        tv.setText(WeatherDoge.getDogeism(wows, dogefixes, weatherAdjectives));
                        tv.setTypeface(wowComicSans);
                        tv.setTextColor(colors[r.nextInt(colors.length)]);
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, r.nextInt(15) + 25);
                        int[] layoutDim = { suchOverlay.getWidth(), suchOverlay.getHeight() };
                        tv.measure(layoutDim[0], layoutDim[1]);
                        int[] textDim = { tv.getMeasuredWidth(), tv.getMeasuredHeight() };
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(textDim[0], textDim[1]);
                        int[] absPos = { layoutDim[0] - textDim[0], layoutDim[1] - textDim[1] };
                        if(absPos[0] < 0 || absPos[1] < 0) {
                            return; // Can't fit with that dogeism, text size, and layout dimensions
                        }
                        params.leftMargin = absPos[0] == 0 ? 0 : r.nextInt(absPos[0]);
                        params.topMargin = absPos[1] == 0 ? 0 : r.nextInt(absPos[1]);
                        if(overlays.size() == 4) {
                            suchOverlay.removeView(overlays.remove());
                        }
                        overlays.add(tv);
                        suchOverlay.addView(tv, params);
                    }
                });
            }
        };
        overlayTimer = new Timer();
        overlayTimer.schedule(handleOverlayText, 0, WOW_INTERVAL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(wowClient != null) {
            wowClient.connect();
        }
        initOverlayTimer();
    }

    @Override
    protected void onStop() {
        if(wowClient != null && wowClient.isConnected()) {
            wowClient.removeLocationUpdates(this);
            wowClient.disconnect();
        }
        overlayTimer.cancel();
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
            Location loc = forceLocation.isEmpty() ? params[0] : null;
            JSONArray data = getCache(loc);
            if(data == null) {
                data = new JSONArray();
                Address addr = getAddress(loc);
                JSONObject weather = getWeatherFromApi(addr);
                if(weather == null) {
                    return null; // Error should be previously reported
                }
                try {
                    if(forceLocation.isEmpty()) {
                        data.put(BigDecimal.valueOf(loc.getLatitude()).setScale(CACHE_COORD_FUZZ, BigDecimal.ROUND_DOWN).doubleValue());
                        data.put(BigDecimal.valueOf(loc.getLongitude()).setScale(CACHE_COORD_FUZZ, BigDecimal.ROUND_DOWN).doubleValue());
                        data.put("").put(addr.getLocality());
                    } else {
                        // Impossible lat lon values
                        data.put(Double.MIN_VALUE).put(Double.MIN_VALUE).put(forceLocation).put(weather.getString("name"));
                    }
                } catch (JSONException ex) {
                    Log.wtf(TAG, ex);
                    return null;
                }
                data.put(weather);
                setCache(data);
            }
            return data;
        }

        private JSONArray getCache(Location loc) {
            // cache: 0 = lat, 1 = lon, 2 = forceLocation, 3 = locality, 4 = weather json
            try {
                SharedPreferences actPref = getPreferences(MODE_PRIVATE);
                long expiresWhen = actPref.getLong(CACHE_EXPIRES, Long.MIN_VALUE);
                if (expiresWhen > System.currentTimeMillis()) {
                    try {
                        JSONArray cache = new JSONArray(actPref.getString(CACHE_DATA, "[]"));
                        if (cache.length() == 0) {
                            return null;
                        }
                        // Check if location is still relevant
                        if (forceLocation.isEmpty()) {
                            double cacheLat = BigDecimal.valueOf(loc.getLatitude()).setScale(CACHE_COORD_FUZZ, BigDecimal.ROUND_DOWN).doubleValue();
                            double cacheLon = BigDecimal.valueOf(loc.getLongitude()).setScale(CACHE_COORD_FUZZ, BigDecimal.ROUND_DOWN).doubleValue();
                            if (cache.getDouble(0) != cacheLat || cache.getDouble(1) != cacheLon) {
                                return null;
                            }
                        } else if (!cache.getString(2).equalsIgnoreCase(forceLocation)) {
                            return null;
                        }
                        Log.d(TAG, "Cache expires in " + (expiresWhen - System.currentTimeMillis()) / 1000 + " seconds");
                        return cache;
                    } catch (JSONException ex) {
                        Log.wtf(TAG, ex);
                    }
                }
            } catch (NullPointerException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                // Catches issues if the cache was improperly saved
            }
            return null;
        }

        private Address getAddress(Location loc) {
            if(loc == null) {
                return null;
            }
            Geocoder geocoder = new Geocoder(ctx);
            try {
                List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if(addresses == null || addresses.size() == 0) {
                    return null;
                }
                return addresses.get(0);
            } catch (Exception ex) {
                if(ex.getMessage() != null && ex.getMessage().equalsIgnoreCase("Service not Available")) {
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
        }

        private JSONObject getWeatherFromApi(Address address) {
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
                final JSONObject weatherJson = new JSONObject(IOUtils.toString(httpEntity.getContent()));
                if(weatherJson.getInt("cod") != 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(ctx, weatherJson.getString("message"), Toast.LENGTH_LONG).show();
                            } catch (JSONException ex) {
                                //
                            }
                        }
                    });
                    Log.e(TAG, "OWM Error " + weatherJson.getInt("cod") + ", " + weatherJson.getString("message"));
                    return null;
                }
                return weatherJson;
            } catch (Exception ex) {
                Log.wtf(TAG, ex);
            }
            return null;
        }

        private void setCache(JSONArray data) {
            Log.d(TAG, "Cache updated");
            SharedPreferences.Editor actPref = getPreferences(MODE_PRIVATE).edit();
            actPref.putString(CACHE_DATA, data.toString());
            actPref.putLong(CACHE_EXPIRES, System.currentTimeMillis() + CACHE_MAXAGE);
            actPref.apply();
        }

        @Override
        protected void onPostExecute(JSONArray data) {
            if(data == null) {
                return;
            }

            try {
                currentLocation = data.getString(3);
                suchLocation.setText(currentLocation);
                JSONObject subWeather = data.getJSONObject(4).getJSONArray("weather").getJSONObject(0);
                JSONObject subMain = data.getJSONObject(4).getJSONObject("main");
                suchStatus.setText(getString(R.string.wow) + " " + subWeather.getString("description").trim().toLowerCase());
                double temp = subMain.getDouble("temp");
                setTemp(temp);
                suchDoge.setImageResource(WeatherDoge.dogeSelect(subWeather.getString("icon")));
                suchLayout.setBackgroundResource(WeatherDoge.skySelect(subWeather.getString("icon")));

                String[] tempAdjs = getResources().getStringArray(WeatherDoge.getTempAdjectives((int)Math.round(temp - 273.15d)));
                String[] bgAdjs = getResources().getStringArray(WeatherDoge.getBgAdjectives(subWeather.getString("icon")));
                weatherAdjectives = WeatherDoge.condoge(tempAdjs, bgAdjs);
            } catch (JSONException ex) {
                Log.wtf(TAG, ex);
            }
        }

        private void setTemp(double temp) {
            temp = temp - 273.15d; // C
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
