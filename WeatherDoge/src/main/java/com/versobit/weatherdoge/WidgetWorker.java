/*
 * Copyright (C) 2019 VersoBit
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

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.versobit.weatherdoge.location.ApiStatus;
import com.versobit.weatherdoge.location.DogeLocationApi;
import com.versobit.weatherdoge.location.FlavoredApiSelector;
import com.versobit.weatherdoge.location.LocationReceiver;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static android.content.Context.NOTIFICATION_SERVICE;

public final class WidgetWorker extends Worker implements LocationReceiver {

    private static final String TAG = WidgetWorker.class.getSimpleName();

    static final String TASK_ALL_TAG = TAG + "_All";
    static final String TASK_MULTIPLE_TAG = TAG + "_Multiple";
    static final String TASK_ONE_TAG = TAG + "_One";

    static final String ACTION = "action";
    static final String ACTION_REFRESH_ALL = "refresh_all";
    static final String ACTION_REFRESH_MULTIPLE = "refresh_multiple";
    static final String ACTION_REFRESH_ONE = "refresh_one";
    static final String EXTRA_WIDGET_ID = "widget_id";

    private static final String WIDGETS_NOTIFICATION_GROUP_ID = "Widgets";
    private static final String WIDGET_PERMISSION_REQ_NOTIFICATION_CHANNEL_ID = "WidgetLocationPermissionRequired";
    static final int PERMISSION_NOTIFICATION_ID = 410;

    private final AtomicReference<Location> locationRef = new AtomicReference<>();
    private final CyclicBarrier locationBarrier = new CyclicBarrier(2);

    private AppWidgetManager widgetManager;
    private int[] widgets;
    private PendingIntent pIntent;

    public WidgetWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        locationBarrier.reset();
        widgetManager = AppWidgetManager.getInstance(getApplicationContext());
        if(ACTION_REFRESH_ALL.equals(getInputData().getString(ACTION))) {
            widgets = widgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), WidgetProvider.class));
        } else if(ACTION_REFRESH_MULTIPLE.equals(getInputData().getString(ACTION))) {
            widgets = getInputData().getIntArray(EXTRA_WIDGET_ID);
        } else if(ACTION_REFRESH_ONE.equals(getInputData().getString(ACTION))) {
            widgets = new int[] { getInputData().getInt(EXTRA_WIDGET_ID, 0) };
        } else {
            Log.wtf(TAG, "Unknown action: " + getInputData().getString(ACTION));
            return Result.failure();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
            pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), WidgetRefreshReceiver.class).setAction(ACTION_REFRESH_ALL),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        setStatus(getApplicationContext().getString(R.string.loading), true);

        DogeLocationApi locationApi = FlavoredApiSelector.get();
        locationApi.configure(getApplicationContext(), this);
        if(forceLocation == null || forceLocation.isEmpty()) {
            if(!locationApi.isAvailable()) {
                showError(BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY) ?
                        R.string.widget_error_no_gms : R.string.widget_error_location_settings);
                return Result.failure();
            }
        }

        WeatherUtil.WeatherResult result = null;
        WeatherUtil.WeatherData data;
        String locationName = "";
        if(forceLocation == null || forceLocation.isEmpty()) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), WeatherDoge.LOCATION_PERMISSION)
                    != PackageManager.PERMISSION_GRANTED) {
                showError(R.string.widget_error_permission);
                showPermissionNotification();
                return Result.failure();
            }
            locationApi.connect();
            try {
                locationBarrier.await(15, TimeUnit.SECONDS);
            } catch (BrokenBarrierException | TimeoutException | InterruptedException ex) {
                Log.wtf(TAG, ex);
                showError(R.string.widget_error_unknown);
                locationApi.disconnect();
                return Result.retry();
            }
            if (locationApi.getStatus() != ApiStatus.CONNECTED) {
                showError(R.string.widget_error_gms_connect);
                locationApi.disconnect();
                return Result.retry();
            }
            Location location = locationRef.get();
            locationApi.disconnect();
            if(location == null) {
                Log.e(TAG, "Unable to retrieve location. (null)");
                showError(BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY) ?
                        R.string.widget_error_location : R.string.widget_error_location_settings);
                return Result.retry();
            }
            data = Cache.getWeatherData(getApplicationContext(), location.getLatitude(),location.getLongitude());

            if(data == null || data.source != weatherSource) {
                result = WeatherUtil.getWeather(location.getLatitude(), location.getLongitude(),
                        weatherSource);
            }

            Geocoder geocoder = new Geocoder(getApplicationContext());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses != null && addresses.size() > 0) {
                    locationName = addresses.get(0).getLocality();
                }
            } catch (IOException ex) {
                Log.wtf(TAG, ex);
                showError(R.string.widget_error_geocoder);
                return Result.retry();
            }
        } else {
            locationName = forceLocation;
            data = Cache.getWeatherData(getApplicationContext(), forceLocation);
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
                return Result.retry();
            }
            switch (result.error) {
                case WeatherUtil.WeatherResult.ERROR_NONE:
                    data = result.data;
                    Cache.putWeatherData(getApplicationContext(), data);
                    break;
                case WeatherUtil.WeatherResult.ERROR_API:
                    Log.e(TAG, "ERROR_API: " + (result.msg == null ? "null" : result.msg));
                    showError(R.string.widget_error_api);
                    return Result.retry();
                case WeatherUtil.WeatherResult.ERROR_THROWABLE:
                    Log.e(TAG, "ERROR_THROWABLE: " + (result.msg == null ? "null" : result.msg), result.throwable);
                    showError(R.string.widget_error_weather_util);
                    return Result.retry();
                default:
                    Log.wtf(TAG, "Unhandled WeatherResult: " + result.error);
                    showError(R.string.widget_error_unknown);
                    return Result.retry();
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
            time.append(DateFormat.format(" MMM d ", now)).append(getApplicationContext().getString(R.string.widget_at));
        }
        time.append(" ").append(DateFormat.getTimeFormat(getApplicationContext()).format(now)).append(" ");
        Bitmap[] textBitmaps = WidgetProvider.getTextBitmaps(getApplicationContext(),
                formattedTemp, condition, locationName, time.toString());

        for(int widget : widgets) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            Bitmap sky = null;
            Bitmap wowLayer = null;
            boolean failed = false;

            views.setViewVisibility(R.id.widget_loading, View.GONE);
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
                        sky = WidgetProvider.getSkyBitmap(getApplicationContext(), options, skyImg);
                        views.setImageViewBitmap(R.id.widget_sky, sky);
                        views.setInt(R.id.widget_sky, "setVisibility", View.VISIBLE);
                        views.setInt(R.id.widget_sky_compat, "setVisibility", View.GONE);
                    }
                    if(showWowText) {
                        wowLayer = WidgetProvider.getWowLayer(getApplicationContext(), options, data.image, (int)data.temperature);
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

        return Result.success();
    }

    public static void enqueueOnceAll() {
        enqueueOnceNow(ACTION_REFRESH_ALL, null);
    }

    public static void enqueueOnceMultiple(int[] appWidgetIds) {
        enqueueOnceNow(ACTION_REFRESH_MULTIPLE, appWidgetIds);
    }

    public static void enqueueOnceSingle(int appWidgetId) {
        enqueueOnceNow(ACTION_REFRESH_ONE, new int[] { appWidgetId });
    }

    private static void enqueueOnceNow(String action, int[] appWidgetIds) {
        Data.Builder dataBuilder = new Data.Builder()
                .putString(ACTION, action);
        OneTimeWorkRequest.Builder workRequestBuilder =
                new OneTimeWorkRequest.Builder(WidgetWorker.class);
        switch (action) {
            case ACTION_REFRESH_ALL:
                workRequestBuilder.addTag(TASK_ALL_TAG);
                break;
            case ACTION_REFRESH_MULTIPLE:
                dataBuilder.putIntArray(EXTRA_WIDGET_ID, appWidgetIds);
                workRequestBuilder.addTag(TASK_MULTIPLE_TAG);
                break;
            case ACTION_REFRESH_ONE:
                dataBuilder.putInt(EXTRA_WIDGET_ID, appWidgetIds[0]);
                workRequestBuilder.addTag(TASK_ONE_TAG);
        }
        workRequestBuilder.setInputData(dataBuilder.build());
        WorkManager.getInstance().enqueue(workRequestBuilder.build());
    }

    private void showError(final int resId) {
        String error = getApplicationContext().getString(resId);
        Log.e(TAG, error);
        setStatus(error);
    }

    private void setStatus(String status) {
        setStatus(status, false);
    }

    private void setStatus(String status, boolean isLoading) {
        Bitmap loading = WidgetProvider.getStatusBitmap(getApplicationContext(), status);
        for(int widget : widgets) {
            RemoteViews views = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget);
            views.setImageViewBitmap(R.id.widget_locationimg, loading);
            views.setImageViewBitmap(R.id.widget_last_updated_img, null);
            views.setOnClickPendingIntent(R.id.widget_root, pIntent);
            views.setViewVisibility(R.id.widget_loading, isLoading ? View.VISIBLE : View.GONE);
            widgetManager.partiallyUpdateAppWidget(widget, views);
        }
        loading.recycle();
    }

    private void createNotificationGroupAndChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence groupName = getApplicationContext().getString(R.string.notification_group_widgets_name);
            NotificationChannelGroup group = new NotificationChannelGroup(WIDGETS_NOTIFICATION_GROUP_ID, groupName);

            CharSequence channelName = getApplicationContext().getString(R.string.notification_channel_widgets_location_name);
            String channelDesc = getApplicationContext().getString(R.string.notification_channel_widgets_location_desc);
            NotificationChannel channel = new NotificationChannel(WIDGET_PERMISSION_REQ_NOTIFICATION_CHANNEL_ID,
                    channelName, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(channelDesc);
            channel.setGroup(WIDGETS_NOTIFICATION_GROUP_ID);

            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(group);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showPermissionNotification() {
        createNotificationGroupAndChannel();
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), WIDGET_PERMISSION_REQ_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_doge_circle_notif) // TODO: Needs a real icon
                .setContentTitle(getApplicationContext().getString(R.string.widget_notification_permission_title))
                .setContentText(getApplicationContext().getString(R.string.widget_notification_permission_body))
                .setContentIntent(intent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(getApplicationContext().getString(R.string.widget_notification_permission_title))
                        .bigText(getApplicationContext().getString(R.string.widget_notification_permission_body)));
        ((NotificationManager)getApplicationContext().getSystemService(NOTIFICATION_SERVICE))
                .notify(PERMISSION_NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onLocation(Location location) {
        locationRef.set(location);
        try {
            locationBarrier.await();
        } catch (BrokenBarrierException | InterruptedException ex) {
            //
        }
    }

    @Override
    public void onConnected() {}
}
