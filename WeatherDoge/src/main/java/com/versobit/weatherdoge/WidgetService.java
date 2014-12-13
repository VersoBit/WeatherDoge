/*
 * Copyright (C) 2014 VersoBit Ltd
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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.gms.common.ConnectionResult;
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
    static final String ACTION_REFRESH = "action_refresh";

    private final CountDownLatch locationLatch = new CountDownLatch(1);

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(!ACTION_REFRESH.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean forceMetric = prefs.getBoolean(OptionsActivity.PREF_FORCE_METRIC, false);
        String forceLocation = prefs.getString(OptionsActivity.PREF_FORCE_LOCATION, "");

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
                locationLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Log.wtf(TAG, ex);
                return;
            }
            if(!locationClient.isConnected()) {
                return;
            }
            Location location = LocationServices.FusedLocationApi.getLastLocation(locationClient);
            locationClient.disconnect();
            data = Cache.getWeatherData(this, location.getLatitude(),location.getLongitude());

            if(data == null) {
                result = WeatherUtil.getWeather(location.getLatitude(), location.getLongitude(),
                        WeatherUtil.Source.YAHOO);
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
            }
        } else {
            locationName = forceLocation;
            data = Cache.getWeatherData(this, forceLocation);
            if(data == null) {
                result = WeatherUtil.getWeather(forceLocation, WeatherUtil.Source.YAHOO);
            }
        }

        if(data == null) {
            if(result == null) {
                return;
            }
            switch (result.error) {
                case WeatherUtil.WeatherResult.ERROR_NONE:
                    data = result.data;
                    Cache.putWeatherData(this, data);
                    break;
                case WeatherUtil.WeatherResult.ERROR_API:
                    return;
                case WeatherUtil.WeatherResult.ERROR_THROWABLE:
                    return;
                default:
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

        // Generate the common text bitmaps
        Bitmap[] textBitmaps = WidgetProvider.getTextBitmaps(this, formattedTemp, data.condition, locationName, "just now");

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);

        for(int widget : widgetManager.getAppWidgetIds(new ComponentName(this, WidgetProvider.class))) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            Bitmap sky = null;
            boolean failed = false;

            views.setImageViewResource(R.id.widget_dogeimg, dogeImg);
            views.setImageViewBitmap(R.id.widget_tempimg, textBitmaps[0]);
            views.setImageViewBitmap(R.id.widget_descimg, textBitmaps[1]);
            views.setImageViewBitmap(R.id.widget_locationimg, textBitmaps[2]);
            views.setImageViewBitmap(R.id.widget_last_updated_img, textBitmaps[3]);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || failed) {
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
