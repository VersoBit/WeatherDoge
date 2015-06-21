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
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class WidgetService extends IntentService implements LocationReceiver {

    private static final String TAG = WidgetService.class.getSimpleName();
    static final String ACTION_REFRESH_ALL = "refresh_all";
    static final String ACTION_REFRESH_MULTIPLE = "refresh_multiple";
    static final String ACTION_REFRESH_ONE = "refresh_one";
    static final String EXTRA_WIDGET_ID = "widget_id";

    private CountDownLatch locationLatch = new CountDownLatch(1);
    private AppWidgetManager widgetManager;
    private int[] widgets;
    private PendingIntent pIntent;

    public WidgetService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        locationLatch = new CountDownLatch(1);
        widgetManager = AppWidgetManager.getInstance(this);
        if(ACTION_REFRESH_ALL.equals(intent.getAction())) {
            widgets = widgetManager.getAppWidgetIds(new ComponentName(this, WidgetProvider.class));
        } else if(ACTION_REFRESH_MULTIPLE.equals(intent.getAction())) {
            widgets = intent.getIntArrayExtra(EXTRA_WIDGET_ID);
        } else if(ACTION_REFRESH_ONE.equals(intent.getAction())) {
            widgets = new int[] { intent.getIntExtra(EXTRA_WIDGET_ID, 0) };
        } else {
            Log.wtf(TAG, "Unknown action: " + intent.getAction());
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean forceMetric = prefs.getBoolean(OptionsActivity.PREF_FORCE_METRIC, false);
        String forceLocation = prefs.getString(OptionsActivity.PREF_FORCE_LOCATION, "");
        WeatherUtil.Source weatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
        if("1".equals(prefs.getString(OptionsActivity.PREF_WEATHER_SOURCE, "0"))) {
            weatherSource = WeatherUtil.Source.YAHOO;
        }
        boolean tapToRefresh = prefs.getBoolean(OptionsActivity.PREF_WIDGET_TAP_TO_REFRESH, false);
        boolean showWowText = prefs.getBoolean(OptionsActivity.PREF_WIDGET_SHOW_WOWTEXT, true);
        boolean showDate = prefs.getBoolean(OptionsActivity.PREF_WIDGET_SHOW_DATE, false);
        boolean backgroundFix = prefs.getBoolean(OptionsActivity.PREF_WIDGET_BACKGROUND_FIX, false);

        if(tapToRefresh) {
            pIntent = PendingIntent.getService(this, 0,
                    new Intent(this, WidgetService.class).setAction(ACTION_REFRESH_ALL),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        setStatus(getString(R.string.loading));

        if(forceLocation == null || forceLocation.isEmpty()) {
            if(!LocationApi.isAvailable(this)) {
                showError(BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY) ?
                        R.string.widget_error_no_gms : R.string.widget_error_location_settings);
                return;
            }
        }

        WeatherUtil.WeatherResult result = null;
        WeatherUtil.WeatherData data;
        String locationName = "";
        if(forceLocation == null || forceLocation.isEmpty()) {
            LocationApi locationApi = new LocationApi(this, this);
            locationApi.connect();
            try {
                locationLatch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Log.wtf(TAG, ex);
                showError(R.string.widget_error_unknown);
                return;
            }
            if(!locationApi.isConnected()) {
                showError(R.string.widget_error_gms_connect);
                return;
            }
            Location location = locationApi.getLocation();
            locationApi.disconnect();
            if(location == null) {
                Log.e(TAG, "Unable to retrieve location. (null)");
                showError(BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY) ?
                        R.string.widget_error_location : R.string.widget_error_location_settings);
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
                showError(R.string.widget_error_geocoder);
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
                showError(R.string.widget_error_unknown);
                return;
            }
            switch (result.error) {
                case WeatherUtil.WeatherResult.ERROR_NONE:
                    data = result.data;
                    Cache.putWeatherData(this, data);
                    break;
                case WeatherUtil.WeatherResult.ERROR_API:
                    Log.e(TAG, "ERROR_API: " + (result.msg == null ? "null" : result.msg));
                    showError(R.string.widget_error_api);
                    return;
                case WeatherUtil.WeatherResult.ERROR_THROWABLE:
                    Log.e(TAG, "ERROR_THROWABLE: " + (result.msg == null ? "null" : result.msg), result.throwable);
                    showError(R.string.widget_error_weather_util);
                    return;
                default:
                    Log.wtf(TAG, "Unhandled WeatherResult: " + result.error);
                    showError(R.string.widget_error_unknown);
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
        Date now = new Date();
        StringBuilder time = new StringBuilder();
        if(showDate) {
            time.append(DateFormat.format(" MMM d ", now)).append(getString(R.string.widget_at));
        }
        time.append(" ").append(DateFormat.getTimeFormat(this).format(now)).append(" ");
        Bitmap[] textBitmaps = WidgetProvider.getTextBitmaps(this,
                formattedTemp, condition, locationName, time.toString());

        for(int widget : widgets) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            Bitmap sky = null;
            Bitmap wowLayer = null;
            boolean failed = false;

            views.setOnClickPendingIntent(R.id.widget_root, pIntent);
            views.setImageViewResource(R.id.widget_dogeimg, dogeImg);
            views.setImageViewBitmap(R.id.widget_tempimg, textBitmaps[0]);
            views.setImageViewBitmap(R.id.widget_descimg, textBitmaps[1]);
            views.setImageViewBitmap(R.id.widget_locationimg, textBitmaps[2]);
            views.setImageViewBitmap(R.id.widget_last_updated_img, textBitmaps[3]);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    Bundle options = widgetManager.getAppWidgetOptions(widget);
                    if(!backgroundFix) {
                        sky = WidgetProvider.getSkyBitmap(this, options, skyImg);
                        views.setImageViewBitmap(R.id.widget_sky, sky);
                        views.setInt(R.id.widget_sky, "setVisibility", View.VISIBLE);
                        views.setInt(R.id.widget_sky_compat, "setVisibility", View.GONE);
                    }
                    if(showWowText) {
                        wowLayer = WidgetProvider.getWowLayer(this, options, data.image, (int)data.temperature);
                        views.setImageViewBitmap(R.id.widget_wowlayer, wowLayer);
                    } else {
                        views.setImageViewBitmap(R.id.widget_wowlayer, null);
                    }
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

            if(wowLayer != null && !wowLayer.isRecycled()) {
                wowLayer.recycle();
            }
        }

        for(Bitmap b : textBitmaps) {
            if(b != null && !b.isRecycled()) {
                b.recycle();
            }
        }

    }

    private void showError(final int resId) {
        String error = getString(resId);
        Log.e(TAG, error);
        setStatus(error);
    }

    private void setStatus(String status) {
        Bitmap loading = WidgetProvider.getStatusBitmap(this, status);
        for(int widget : widgets) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            views.setImageViewBitmap(R.id.widget_locationimg, loading);
            views.setImageViewBitmap(R.id.widget_last_updated_img, null);
            views.setOnClickPendingIntent(R.id.widget_root, pIntent);
            widgetManager.partiallyUpdateAppWidget(widget, views);
        }
        loading.recycle();
    }

    @Override
    public void onLocation(Location location) {
        locationLatch.countDown();
    }

    @Override
    public void onConnected() {
        locationLatch.countDown();
    }
}
