/*
 * Copyright (C) 2014-2015 VersoBit Ltd
 *
 * This file is part of Weather Doge.
 *
 * Weather Doge is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Weather Doge is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Weather Doge.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.weatherdoge;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class WidgetService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WidgetService.class.getSimpleName();
    static final String ACTION_REFRESH_ALL = "refresh_all";
    static final String ACTION_REFRESH_MULTIPLE = "refresh_multiple";
    static final String ACTION_REFRESH_ONE = "refresh_one";
    static final String EXTRA_WIDGET_ID = "widget_id";

    private CountDownLatch locationLatch = new CountDownLatch(1);
    private Handler uiHandler;

    public WidgetService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uiHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        locationLatch = new CountDownLatch(1);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        int[] widgets;
        if(ACTION_REFRESH_ALL.equals(intent.getAction())) {
            widgets = widgetManager.getAppWidgetIds(new ComponentName(this, WidgetProvider.class));
        } else if(ACTION_REFRESH_MULTIPLE.equals(intent.getAction())) {
            widgets = intent.getIntArrayExtra(EXTRA_WIDGET_ID);
        } else if(ACTION_REFRESH_ONE.equals(intent.getAction())) {
            widgets = new int[] { intent.getIntExtra(EXTRA_WIDGET_ID, 0) };
        } else {
            Log.wtf(TAG, "Unknown action: " + intent.getAction());
            showToast(R.string.widget_error_action);
            return;
        }

        Bitmap loading = WidgetProvider.getLoadingBitmap(this);
        for(int widget : widgets) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            views.setImageViewBitmap(R.id.widget_locationimg, loading);
            widgetManager.partiallyUpdateAppWidget(widget, views);
        }
        loading.recycle();

        int gmsCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(gmsCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.showErrorNotification(gmsCode, this);
            showToast(R.string.widget_error_no_gms);
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean forceMetric = prefs.getBoolean(OptionsActivity.PREF_FORCE_METRIC, false);
        String forceLocation = prefs.getString(OptionsActivity.PREF_FORCE_LOCATION, "");
        WeatherUtil.Source weatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
        if(prefs.getString(OptionsActivity.PREF_WEATHER_SOURCE, "0").equals("1")) {
            weatherSource = WeatherUtil.Source.YAHOO;
        }
        boolean tapToRefresh = prefs.getBoolean(OptionsActivity.PREF_WIDGET_TAP_TO_REFRESH, false);
        boolean backgroundFix = prefs.getBoolean(OptionsActivity.PREF_WIDGET_BACKGROUND_FIX, false);

        WeatherUtil.WeatherResult result = null;
        WeatherUtil.WeatherData data;
        String locationName = "";
        if(forceLocation.isEmpty()) {
            GoogleApiClient locationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            locationClient.connect();
            try {
                locationLatch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Log.wtf(TAG, ex);
                showToast(R.string.widget_error_unknown);
                return;
            }
            if(!locationClient.isConnected()) {
                showToast(R.string.widget_error_gms_connect);
                return;
            }
            Location location = LocationServices.FusedLocationApi.getLastLocation(locationClient);
            locationClient.disconnect();
            if(location == null) {
                Log.e(TAG, "Unable to retrieve location. (null)");
                showToast(R.string.widget_error_location);
                return;
            }
            data = Cache.getWeatherData(this, location.getLatitude(),location.getLongitude());

            if(data == null || data.source != weatherSource) {
                result = WeatherUtil.getWeather(location.getLatitude(), location.getLongitude(),
                        weatherSource);
            }

            Geocoder geocoder = new Geocoder(this);
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses != null && addresses.size() > 0) {
                    locationName = addresses.get(0).getLocality();
                }
            } catch (IOException ex) {
                Log.wtf(TAG, ex);
                showToast(R.string.widget_error_geocoder);
                return;
            }
        } else {
            locationName = forceLocation;
            data = Cache.getWeatherData(this, forceLocation);
            if(data == null || data.source != weatherSource) {
                result = WeatherUtil.getWeather(forceLocation, weatherSource);
            }
        }

        if(data == null || data.source != weatherSource) {
            if(result == null) {
                Log.wtf(TAG, "data: " + (data == null ? "null" : data) + ", data.source: " +
                        ((data == null || data.source == null) ? "null" : data.source) +
                        ", weatherSource: " + weatherSource);
                showToast(R.string.widget_error_unknown);
                return;
            }
            switch (result.error) {
                case WeatherUtil.WeatherResult.ERROR_NONE:
                    data = result.data;
                    Cache.putWeatherData(this, data);
                    break;
                case WeatherUtil.WeatherResult.ERROR_API:
                    Log.e(TAG, "ERROR_API: " + (result.msg == null ? "null" : result.msg));
                    showToast(R.string.widget_error_api);
                    return;
                case WeatherUtil.WeatherResult.ERROR_THROWABLE:
                    Log.e(TAG, "ERROR_THROWABLE: " + (result.msg == null ? "null" : result.msg), result.throwable);
                    showToast(R.string.widget_error_weather_util);
                    return;
                default:
                    Log.wtf(TAG, "Unhandled WeatherResult: " + result.error);
                    showToast(R.string.widget_error_unknown);
                    return;
            }
        }

        double temp = data.temperature;
        if(UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric) {
            temp = temp * 1.8d + 32d; // F
        }
        temp = Math.round(temp);
        DecimalFormat tempFormat = new DecimalFormat();
        tempFormat.setMaximumFractionDigits(0);
        tempFormat.setDecimalSeparatorAlwaysShown(false);
        tempFormat.setGroupingUsed(false);
        String formattedTemp = tempFormat.format(temp) + "°";

        int dogeImg = WeatherDoge.dogeSelect(data.image);
        int skyImg = WeatherDoge.skySelect(data.image);

        if(locationName == null || locationName.isEmpty()) {
            locationName = data.place;
        }

        // Generate the common text bitmaps
        formattedTemp = formattedTemp.isEmpty() ? " " : formattedTemp;
        String condition = data.condition.isEmpty() ? " " : data.condition;
        locationName = locationName.isEmpty() ? " " : locationName;
        Bitmap[] textBitmaps = WidgetProvider.getTextBitmaps(this,
                formattedTemp, condition, locationName,
                " " + DateFormat.getTimeFormat(this).format(data.time) + " ");

        PendingIntent pIntent;
        if(tapToRefresh) {
            pIntent = PendingIntent.getService(this, 0,
                    new Intent(this, WidgetService.class).setAction(ACTION_REFRESH_ALL),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        for(int widget : widgets) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            Bitmap sky = null;
            boolean failed = false;

            views.setOnClickPendingIntent(R.id.widget_root, pIntent);
            views.setImageViewResource(R.id.widget_dogeimg, dogeImg);
            views.setImageViewBitmap(R.id.widget_tempimg, textBitmaps[0]);
            views.setImageViewBitmap(R.id.widget_descimg, textBitmaps[1]);
            views.setImageViewBitmap(R.id.widget_locationimg, textBitmaps[2]);
            views.setImageViewBitmap(R.id.widget_last_updated_img, textBitmaps[3]);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && !backgroundFix) {
                try {
                    Bundle options = widgetManager.getAppWidgetOptions(widget);
                    sky = WidgetProvider.getSkyBitmap(this, options, skyImg);
                    views.setImageViewBitmap(R.id.widget_sky, sky);
                    views.setInt(R.id.widget_sky, "setVisibility", View.VISIBLE);
                    views.setInt(R.id.widget_sky_compat, "setVisibility", View.GONE);
                } catch (Exception ex) {
                    Log.wtf(TAG, ex);
                    failed = true;
                }
            }
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || failed || backgroundFix) {
                views.setInt(R.id.widget_sky, "setVisibility", View.GONE);
                views.setInt(R.id.widget_sky_compat, "setVisibility", View.VISIBLE);
                views.setImageViewResource(R.id.widget_sky_compat, skyImg);
            }

            widgetManager.updateAppWidget(widget, views);

            if(sky != null && !sky.isRecycled()) {
                sky.recycle();
            }
        }

        for(Bitmap b : textBitmaps) {
            if(b != null && !b.isRecycled()) {
                b.recycle();
            }
        }

    }

    // Thanks rony, http://stackoverflow.com/a/5420929/238374
    private void showToast(final int resId) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WidgetService.this, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationLatch.countDown();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //
    }
}
