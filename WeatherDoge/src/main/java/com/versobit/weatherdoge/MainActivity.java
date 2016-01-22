/*
 * Copyright (C) 2014-2016 VersoBit Ltd
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.apache.commons.lang3.ArrayUtils;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

final public class MainActivity extends Activity implements LocationReceiver,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final long WOW_INTERVAL = 2300;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_LOCATION_PERMISSION = 410;

    private boolean forceMetric = false;
    private String forceLocation = "";
    private WeatherUtil.Source weatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
    private boolean useNeue = false;
    private float shadowR = 1f;
    private float shadowX = 3f;
    private float shadowY = 3f;
    private boolean shadowAdjs = false;
    private boolean textOnTop = false;
    private int lastVersion = 0;

    private ImageView suchBg;
    private RelativeLayout suchOverlay;
    private RelativeLayout suchTopOverlay;
    private LinearLayout suchInfoGroup;
    private ImageView suchDoge;
    private TextView suchStatus;
    private RelativeLayout suchTempGroup;
    private TextView suchNegative;
    private TextView suchTemp;
    private TextView suchDegree;
    private TextView suchLocation;
    private LocationApi wowApi;
    private Location whereIsDoge;
    private Typeface wowComicSans;

    private AlertDialog errorDialog;
    private AlertDialog rationaleDialog;
    private Snackbar locationSnackbar;

    private double currentTemp;
    private boolean currentlyMetric;
    private String currentLocation;
    private String[] dogefixes;
    private String[] wows;
    private String[] weatherAdjectives;
    private int[] colors;
    private Timer overlayTimer = new Timer("OverlayTimer", true);
    private OverlayTimerTask overlayTask;
    private WowText newWowText;
    private Queue<WowText> overlays = new ArrayDeque<>(4);
    private int currentBackgroundId = Integer.MIN_VALUE;
    private int currentDogeId = R.drawable.doge_01d;
    private boolean currentlyAnim = false;

    private static final class WowText {
        private RelativeLayout.LayoutParams params;
        private TextView view;

        private WowText(RelativeLayout.LayoutParams params, TextView view) {
            this.params = params;
            this.view = view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadOptions();

        dogefixes = getResources().getStringArray(R.array.dogefix);
        wows = getResources().getStringArray(R.array.wows);
        colors = getResources().getIntArray(R.array.wow_colors);

        suchBg = (ImageView)findViewById(R.id.main_suchbg);
        suchOverlay = (RelativeLayout)findViewById(R.id.main_suchoverlay);
        suchTopOverlay = (RelativeLayout)findViewById(R.id.main_suchtopoverlay);
        suchInfoGroup = (LinearLayout)findViewById(R.id.main_suchinfogroup);
        suchDoge = (ImageView)findViewById(R.id.main_suchdoge);
        suchDoge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!forceLocation.isEmpty()) {
                    new GetWeather().execute();
                } else if(wowApi != null && wowApi.isConnected()) {
                    requestLocation();
                }
            }
        });
        suchStatus = (TextView)findViewById(R.id.main_suchstatus);
        suchTempGroup = (RelativeLayout)findViewById(R.id.main_suchtempgroup);
        suchNegative = (TextView)findViewById(R.id.main_suchnegative);
        suchTemp = (TextView)findViewById(R.id.main_suchtemp);
        suchDegree = (TextView)findViewById(R.id.main_suchdegree);
        suchLocation = (TextView)findViewById(R.id.main_suchlocation);
        final FloatingActionsMenu suchMenu = (FloatingActionsMenu)findViewById(R.id.main_fam);
        findViewById(R.id.man_fab_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                if(weatherAdjectives == null) {
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text).split("\n\n")[1]);
                } else {
                    String unit = (char)0x00b0 + "C";
                    double tempTemp = currentTemp; // temporary temperature...
                    if(UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric) {
                        tempTemp = tempTemp * 1.8d + 32d; // F
                        unit = (char)0x00b0 + "F";
                    }
                    String temp = String.valueOf(Math.round(tempTemp)) + ' ' + unit;
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title, temp, currentLocation));
                    i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, WeatherDoge.getDogeism(wows, dogefixes, weatherAdjectives), temp, currentLocation));
                }
                suchMenu.collapse();
                startActivity(Intent.createChooser(i, getString(R.string.action_share)));
            }
        });
        findViewById(R.id.man_fab_options).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suchMenu.collapse();
                startActivity(new Intent(MainActivity.this, OptionsActivity.class));
            }
        });

        updateFont();
        updateShadow();

        if(!forceLocation.isEmpty()) {
            new GetWeather().execute();
        } else if(LocationApi.isAvailable(this)) {
            wowApi = new LocationApi(this, this);
        }
        new SetBackgroundTask(R.drawable.sky_01d).execute();

        if(BuildConfig.VERSION_CODE > lastVersion) {
            lastVersion = BuildConfig.VERSION_CODE;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.edit().putInt(OptionsActivity.PREF_INTERNAL_LAST_VERSION, lastVersion).apply();
        }
    }

    private void loadOptions() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        forceMetric = sp.getBoolean(OptionsActivity.PREF_FORCE_METRIC, false);
        forceLocation = sp.getString(OptionsActivity.PREF_FORCE_LOCATION, "");
        String strSource = sp.getString(OptionsActivity.PREF_WEATHER_SOURCE, "0");
        if("0".equals(strSource)) {
            weatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
        } else {
            weatherSource = WeatherUtil.Source.YAHOO;
        }
        useNeue = sp.getBoolean(OptionsActivity.PREF_APP_USE_COMIC_NEUE, false);
        shadowR = sp.getFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_radius", 1f);
        shadowX = sp.getFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_x", 3f);
        shadowY = sp.getFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_y", 3f);
        shadowAdjs = sp.getBoolean(OptionsActivity.PREF_APP_DROP_SHADOW + "_adjs", false);
        textOnTop = sp.getBoolean(OptionsActivity.PREF_APP_TEXT_ON_TOP, false);
        lastVersion = sp.getInt(OptionsActivity.PREF_INTERNAL_LAST_VERSION, lastVersion);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(wowApi != null) {
            requestLocation();
        }
        overlayTask = new OverlayTimerTask();
        overlayTimer.schedule(overlayTask, 0, WOW_INTERVAL);
    }

    @Override
    protected void onStop() {
        if(wowApi != null && wowApi.isConnected()) {
            wowApi.disconnect();
        }
        overlayTask.cancel();
        overlayTask = null;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean oldNeue = useNeue;
        float[] oldShadowFloats = { shadowR, shadowX, shadowY };
        boolean oldShadowBool = shadowAdjs;
        loadOptions();
        if(useNeue != oldNeue) {
            updateFont();
        }
        if(oldShadowFloats[0] != shadowR || oldShadowFloats[1] != shadowX || oldShadowFloats[2] != shadowY
                || oldShadowBool != shadowAdjs) {
            updateShadow();
        }
        if(forceLocation.isEmpty()) {
            if(wowApi == null) {
                wowApi = new LocationApi(this, this);
            }
            if(!wowApi.isConnected() && !wowApi.isConnecting()) {
                // FIXME: Double request after selecting cancel on the first permission dialog
                requestLocation();
            }
        } else {
            if(wowApi != null && wowApi.isConnected()) {
                wowApi.disconnect();
            }
            new GetWeather().execute();
        }
    }

    private void updateFont() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)suchTemp.getLayoutParams();
        if(useNeue || BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_FOSS)) {
            wowComicSans = Typeface.createFromAsset(getAssets(), "ComicNeue-Regular.ttf");

            // Horizontal and vertical centering for proper display
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

            // This only works when going from Comic Sans -> Comic Neue, not the other way around
            // Android doesn't redraw Comic Sans correctly, or something...
            for(WowText wowText : overlays) {
                wowText.view.setTypeface(wowComicSans);
            }
        }
        else {
            wowComicSans = Typeface.createFromAsset(getAssets(), "comic.ttf");
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0); // Disable vertical center
        }
        suchDegree.setTypeface(wowComicSans);
        suchStatus.setTypeface(wowComicSans);
        suchLocation.setTypeface(wowComicSans);
        suchNegative.setTypeface(wowComicSans);
        suchTemp.setTypeface(wowComicSans);
        suchTemp.setLayoutParams(layoutParams);
    }

    private void updateShadow() {
        suchStatus.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
        suchNegative.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
        suchTemp.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
        suchDegree.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
        suchLocation.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
        for(WowText wowText : overlays) {
            if(shadowAdjs) {
                wowText.view.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
                continue;
            }
            wowText.view.setShadowLayer(0, 0, 0, 0);
        }
    }

    @Override
    public void onConnected() {
        requestLocation();
    }

    @Override
    public void onLocation(Location location) {
        whereIsDoge = location;
        if(whereIsDoge == null) {
            Log.e(TAG, "dunno where this shibe is");
            return;
        }
        new GetWeather().execute(whereIsDoge);
    }

    private void requestLocation() {
        requestLocation(false);
    }

    // Asynchronously request the current location
    private void requestLocation(boolean skipRationale) {
        // Check if we need to request the permission on >= Marshmallow
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // We need to request it
            if (!skipRationale && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show the rationale dialog if we need to
                showLocationRationaleDialog();
            } else {
                // Just request the permission
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                        REQUEST_LOCATION_PERMISSION);
            }
        } else {
            // We already have it or this is < Marshmallow
            doCheckedLocationRequest();
        }
    }

    // Show the location rationale dialog which explains to the user why we need their location
    private void showLocationRationaleDialog() {
        if ((rationaleDialog != null && rationaleDialog.isShowing())
                || (locationSnackbar != null && locationSnackbar.isShown())) {
            return;
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.location_rationale_text)
                .setNegativeButton(R.string.location_rationale_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Go directly to the constant location setting in the options
                        startActivity(new Intent(MainActivity.this, OptionsActivity.class)
                                .putExtra(OptionsActivity.EXTRA_SHORTCUT,
                                        OptionsActivity.EXTRA_SHORTCUT_FORCE_LOCATION
                                ));
                    }
                })
                .setPositiveButton(R.string.location_rationale_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Request the permission again
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                                REQUEST_LOCATION_PERMISSION);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        showLocationSnackbar();
                    }
                });
        // Prevent crash if MainActivity is finishing while attempting to display a new dialog
        if(!isFinishing()) {
            rationaleDialog = adb.show();
        }
    }

    private void showLocationSnackbar() {
        if(locationSnackbar != null && locationSnackbar.isShown()) {
            return;
        }

        locationSnackbar = Snackbar.make(findViewById(R.id.main_suchlayout),
                R.string.location_snackbar_text, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.location_snackbar_button, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestLocation(true);
                    }
                })
                .setActionTextColor(ContextCompat.getColor(this, R.color.primary_dark));
        locationSnackbar.show();
    }

    // Called after we're certain we have the location permission
    private void doCheckedLocationRequest() {
        // Make sure we're going to connect
        if(!wowApi.isConnected() && !wowApi.isConnecting()) {
            wowApi.connect();
        }
        onLocation(wowApi.getLocation());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            // Not a request we're interested in
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Remove the widget notification, if any
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                    .cancel(WidgetService.PERMISSION_NOTIFICATION_ID);
            // Refresh the widget
            startService(new Intent(this, WidgetService.class).setAction(WidgetService.ACTION_REFRESH_ALL));
            // Dismiss the snackbar, if any
            if (locationSnackbar != null && locationSnackbar.isShown()) {
                locationSnackbar.dismiss();
            }
            // Good to go, execute the actual location action
            doCheckedLocationRequest();
        } else {
            showLocationSnackbar();
        }
    }

    private final class OverlayTimerTask extends TimerTask {
        @Override
        public void run() {
            if(weatherAdjectives == null) {
                return;
            }

            WowText wowText;

            // Set up the RNG
            Random r = new Random();

            // Continue to loop until we come out the other side with a valid wowText
            while(true) {
                // Create the new view
                wowText = new WowText(null, new TextView(MainActivity.this));

                // 15sp is a magic padding number I've tested with
                int padding = (int)Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, getResources().getDisplayMetrics()));

                // Set up the view with the basics
                wowText.view.setTypeface(wowComicSans);
                if(shadowAdjs) {
                    wowText.view.setShadowLayer(shadowR, shadowX, shadowY, Color.BLACK);
                }

                // How big is the overlay layer?
                int[] layoutDim = { suchOverlay.getWidth(), suchOverlay.getHeight() };

                wowText.view.setText(getUniqueDogeism(r));
                wowText.view.setTextColor(colors[r.nextInt(colors.length)]);
                wowText.view.setTextSize(TypedValue.COMPLEX_UNIT_SP, r.nextInt(15) + 25);

                // How big is this textview going to be?
                wowText.view.measure(layoutDim[0], layoutDim[1]);
                int[] textDim = { wowText.view.getMeasuredWidth(), wowText.view.getMeasuredHeight() };

                // Set a fixed width and height
                wowText.params = new RelativeLayout.LayoutParams(textDim[0], textDim[1]);

                // Find the maximum left and top margins
                int[] absPos = { layoutDim[0] - textDim[0], layoutDim[1] - textDim[1] };

                // Can we fit this text on the screen?
                if(absPos[0] < 0 || absPos[1] < 0) {
                    continue; // Can't fit with that dogeism, text size, and layout dimensions
                }

                wowText.params.leftMargin = absPos[0] == 0 ? 0 : r.nextInt(absPos[0]);
                wowText.params.topMargin = absPos[1] == 0 ? 0 : r.nextInt(absPos[1]);

                wowText.params.width += padding * 2; // left + right
                wowText.params.height += padding * 2; // top + bottom
                // Padding is subtracted from top/left margins so the measured values are still accurate
                // We don't care if the shadow clips on the edge of the screen
                // abs prevents possibly dangerous negative margins
                wowText.params.leftMargin = Math.abs(wowText.params.leftMargin - padding);
                wowText.params.topMargin = Math.abs(wowText.params.topMargin - padding);
                wowText.view.setGravity(Gravity.CENTER); // Text is centered within the now padded view

                if(checkWowTextConflict(wowText)) {
                    continue;
                }
                break;
            }

            newWowText = wowText;
            runOnUiThread(overlayUiRunnable);
        }

        private String getUniqueDogeism(Random r) {
            String ism = null;
            while(ism == null) {
                ism = WeatherDoge.getDogeism(r, wows, dogefixes, weatherAdjectives);
                WowText head = overlays.peek();
                for(WowText wow : overlays) {
                    if(head == wow && overlays.size() == 4) {
                        continue; // Continues on the inner loop
                    }
                    if(ism.contentEquals(wow.view.getText())) {
                        ism = null;
                        break;
                    }
                }
            }
            return ism;
        }

        private boolean checkWowTextConflict(WowText needle) {
            Rect needleRect = layoutParamsToRect(needle.params);
            // head is the one to be removed
            WowText head = overlays.peek();
            for(WowText wow : overlays) {
                if(head == wow && overlays.size() == 4) {
                    // We do not want to compare against head because
                    // it'll be removed
                    continue;
                }
                if(Rect.intersects(needleRect, layoutParamsToRect(wow.params))) {
                    return true;
                }
            }
            return false;
        }

        private Rect layoutParamsToRect(RelativeLayout.LayoutParams params) {
            return new Rect(params.leftMargin, params.topMargin,
                    params.leftMargin + params.width, params.topMargin + params.height);
        }

        private Runnable overlayUiRunnable = new Runnable() {
            @Override
            public void run() {
                if(overlays.size() == 4) {
                    // If the view doesn't exist in the particular overlay it will not throw an exception
                    View v = overlays.remove().view;
                    suchOverlay.removeView(v);
                    suchTopOverlay.removeView(v);
                }

                if(textOnTop) {
                    suchTopOverlay.addView(newWowText.view, newWowText.params);
                } else {
                    suchOverlay.addView(newWowText.view, newWowText.params);
                }
                overlays.add(newWowText);
            }
        };
    }

    private final class SetDogeTask extends AsyncTask<Void, Void, Animation> {

        private final int resId;

        private SetDogeTask(int resId) {
            this.resId = resId;
        }

        @Override
        protected void onPreExecute() {
            if (currentDogeId == resId) {
                cancel(true);
                return;
            }
            currentDogeId = resId;
        }

        @Override
        protected Animation doInBackground(Void... params) {
            Animation zoomOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.dogezoom_out);
            final Animation zoomIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.dogezoom_in);
            zoomOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    suchDoge.setImageResource(resId);
                    suchDoge.startAnimation(zoomIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) { }
            });

            return zoomOut;
        }

        @Override
        protected void onPostExecute(Animation animation) {
            suchDoge.startAnimation(animation);
        }
    }

    private final class SetBackgroundTask extends AsyncTask<Void, Void, Bitmap> {

        private final int resId;

        private SetBackgroundTask(int resId) {
            this.resId = resId;
        }

        @Override
        protected void onPreExecute() {
            if(currentBackgroundId == resId) {
                cancel(true);
                return;
            }
            currentBackgroundId = resId;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if(isCancelled()) {
                return null;
            }

            // Manually resize/crop the sky background because god forbid if Android can do this well on its own
            // Load in the full bitmap
            Bitmap theSky = BitmapFactory.decodeResource(getResources(), resId);
            // Get display info
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // Create empty bitmap the size of the screen
            Bitmap scaledSky = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.ARGB_8888);

            // Use a canvas to draw on the bitmap
            Canvas canvas = new Canvas(scaledSky);
            float skyHeight = (float)theSky.getScaledHeight(canvas); // Height of the sky scaled on the canvas
            skyHeight = skyHeight == 0f ? metrics.heightPixels : skyHeight; // If not scaled, use device height
            int compensationPixels = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? 4 : 0; // Weird. Compensate for translucent status bar
            float newScale = (metrics.heightPixels + compensationPixels) / skyHeight; // The scale we need to achieve the device's height
            // Magic number to get some important image elements onscreen
            float moveAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180, metrics);

            Matrix matrix = new Matrix();
            matrix.setScale(newScale, newScale, moveAmount, 0);
            canvas.setMatrix(matrix);
            canvas.drawBitmap(theSky, 0, 0, new Paint()); // Draw the bitmap

            theSky.recycle();

            return scaledSky;
        }

        @Override
        protected void onPostExecute(Bitmap scaledSky) {
            Drawable current = suchBg.getDrawable();
            if(current != null) {
                if(current instanceof TransitionDrawable) {
                    current = ((TransitionDrawable) current).getDrawable(1);
                }
                Drawable[] drawables = new Drawable[] { current, new BitmapDrawable(getResources(), scaledSky)};
                TransitionDrawable transition = new TransitionDrawable(drawables);
                transition.setCrossFadeEnabled(true);
                suchBg.setImageDrawable(transition);
                transition.startTransition(getResources().getInteger(R.integer.anim_refresh_time) * 2);
            } else {
                suchBg.setImageDrawable(new BitmapDrawable(getResources(), scaledSky));
            }
        }
    }

    private final class GetWeather extends AsyncTask<Location, Void, Object[]> {
        @Override
        protected void onPreExecute() {
            // Dismiss the snackbar and dialog, if any
            if (rationaleDialog != null && rationaleDialog.isShowing()) {
                rationaleDialog.dismiss();
            }
            if (locationSnackbar != null && locationSnackbar.isShown()) {
                locationSnackbar.dismiss();
            }
        }

        private final String TAG = GetWeather.class.getSimpleName();
        @Override
        protected Object[] doInBackground(Location... params) {
            if(!Geocoder.isPresent() && forceLocation.isEmpty()) {
                return new Object[] { new UnsupportedOperationException(getString(R.string.geocoder_error_code)) };
            }

            WeatherUtil.WeatherData data;
            if(forceLocation.isEmpty()) {
                if(params[0] != null) {
                    data = Cache.getWeatherData(MainActivity.this, params[0].getLatitude(), params[0].getLongitude());
                } else {
                    return new Object[] { new RuntimeException(getString(R.string.error_ensure_location_settings)) };
                }
            } else {
                data = Cache.getWeatherData(MainActivity.this, forceLocation);
            }

            if(data == null || data.source != weatherSource) {
                WeatherUtil.WeatherResult result;
                if(forceLocation.isEmpty()) {
                    result = WeatherUtil.getWeather(params[0].getLatitude(), params[0].getLongitude(), weatherSource);
                } else {
                    result = WeatherUtil.getWeather(forceLocation, weatherSource);
                }
                if(result.error != WeatherUtil.WeatherResult.ERROR_NONE) {
                    return new Object[] { result };
                }
                data = result.data;
                Cache.putWeatherData(MainActivity.this, data);
            }

            Log.d(TAG, data.toString());

            Address addr = null;
            if(forceLocation.isEmpty()) {
                addr = getAddress(data.latitude, data.longitude);
            }

            // Start the first half of the animation update still in the background thread
            if (currentlyAnim) {
                return new Object[] { null };
            }

            currentlyAnim = true;
            if (forceLocation.isEmpty() && addr != null) {
                String locality = addr.getLocality();
                currentLocation = locality == null ? data.place : locality;
            } else {
                currentLocation = data.place;
            }
            String[] tempAdjs = getResources().getStringArray(WeatherDoge.getTempAdjectives((int)data.temperature));
            String[] bgAdjs = getResources().getStringArray(WeatherDoge.getBgAdjectives(data.image));
            weatherAdjectives = ArrayUtils.addAll(tempAdjs, bgAdjs);

            return new Object[] { data, addr };
        }

        private Address getAddress(double latitude, double longitude) {
            Geocoder geocoder = new Geocoder(MainActivity.this);
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
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
                            AlertDialog.Builder adb = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AppTheme_Options));
                            adb.setTitle(R.string.geocoder_error_title).setMessage(R.string.geocoder_error_msg);
                            adb.setNeutralButton(R.string.wow, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            // Prevent crash if MainActivity is finishing while attempting to display a new dialog
                            if(!isFinishing()) {
                                errorDialog = adb.show();
                            }
                        }
                    });
                }
                Log.wtf(TAG, ex);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object[] result) {
            if (result[0] instanceof Throwable) {
                Log.wtf(TAG, (Throwable) result[0]);
                Toast.makeText(MainActivity.this, ((Throwable)result[0]).getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            else if (result[0] instanceof WeatherUtil.WeatherResult) {
                WeatherUtil.WeatherResult wResult = (WeatherUtil.WeatherResult)result[0];
                switch (wResult.error) {
                    case WeatherUtil.WeatherResult.ERROR_API:
                        Log.e(TAG, wResult.msg);
                        Toast.makeText(MainActivity.this, wResult.msg, Toast.LENGTH_LONG).show();
                        break;
                    case WeatherUtil.WeatherResult.ERROR_THROWABLE:
                        String errorMsg = wResult.msg != null ? wResult.msg : wResult.throwable.getMessage();
                        Log.e(TAG, errorMsg, wResult.throwable);
                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        break;
                }
                return;
            }
            else if (!(result[0] instanceof WeatherUtil.WeatherData)) {
                return;
            }

            // This is a continuation of the animation work started in the background thread
            // The stuff here must be done on the current UI thread
            WeatherUtil.WeatherData data = (WeatherUtil.WeatherData)result[0];

            // Do the doge and background animations
            new SetDogeTask(WeatherDoge.dogeSelect(data.image)).execute();
            new SetBackgroundTask(WeatherDoge.skySelect(data.image)).execute();

            // Do we need to animate?
            String description = getString(R.string.wow) + " " + data.condition.trim().toLowerCase();
            if (suchStatus.getText().equals(description) && (currentTemp == data.temperature) &&
                    (currentlyMetric != (UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric)) &&
                    suchLocation.getText().equals(currentLocation)) {
                currentlyAnim = false;
                return;
            }

            // Do the second half of the animation work on a background thread
            new WeatherDataTask().execute(data, description);
        }
    }

    private final class WeatherDataTask extends AsyncTask<Object, Void, Animation> {
        @Override
        protected Animation doInBackground(Object... params) {
            final WeatherUtil.WeatherData data = (WeatherUtil.WeatherData)params[0];
            final String description = (String)params[1];

            final FormattedTemp formattedTemp = new FormattedTemp(data.temperature);

            final int animTime = (int)(getResources().getInteger(R.integer.anim_refresh_time) / 2.5);

            final Animation[] fadeOuts = { AnimationUtils.loadAnimation(MainActivity.this, R.anim.textfade_out),
                    AnimationUtils.loadAnimation(MainActivity.this, R.anim.textfade_out),
                    AnimationUtils.loadAnimation(MainActivity.this, R.anim.textfade_out) };
            final Animation[] fadeIns = { AnimationUtils.loadAnimation(MainActivity.this, R.anim.textfade_in),
                    AnimationUtils.loadAnimation(MainActivity.this, R.anim.textfade_in),
                    AnimationUtils.loadAnimation(MainActivity.this, R.anim.textfade_in) };

            final Handler uiHandler = new Handler(Looper.getMainLooper());
            final Runnable tempGroupRunnable = new Runnable() {
                @Override
                public void run() {
                    suchTempGroup.startAnimation(fadeOuts[1]);
                }
            };

            final Runnable locationRunnable = new Runnable() {
                @Override
                public void run() {
                    suchLocation.startAnimation(fadeOuts[2]);
                }
            };

            fadeOuts[0].setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    uiHandler.postDelayed(tempGroupRunnable, animTime);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    suchStatus.setText(description);
                    suchStatus.startAnimation(fadeIns[0]);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            fadeOuts[1].setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    uiHandler.postDelayed(locationRunnable, animTime);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    currentTemp = data.temperature;
                    currentlyMetric = UnitLocale.getDefault() != UnitLocale.IMPERIAL || forceMetric;
                    suchTemp.setText(formattedTemp.str);
                    suchNegative.setVisibility(formattedTemp.temp < 0d ? View.VISIBLE : View.GONE);
                    suchDegree.setVisibility(View.VISIBLE);
                    suchTempGroup.startAnimation(fadeIns[1]);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            fadeOuts[2].setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    suchLocation.setText(currentLocation);
                    suchLocation.startAnimation(fadeIns[2]);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            fadeIns[2].setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    currentlyAnim = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            return fadeOuts[0];
        }

        private final class FormattedTemp {
            private final double temp;
            private final String str;

            FormattedTemp(double inTemp) {
                if (UnitLocale.getDefault() == UnitLocale.IMPERIAL && !forceMetric) {
                    inTemp = inTemp * 1.8d + 32d; // F
                }
                inTemp = Math.round(inTemp);
                DecimalFormat df = new DecimalFormat();
                df.setNegativePrefix("");
                df.setNegativeSuffix("");
                df.setMaximumFractionDigits(0);
                df.setDecimalSeparatorAlwaysShown(false);
                df.setGroupingUsed(false);
                temp = inTemp;
                str = df.format(inTemp);
            }
        }

        @Override
        protected void onPostExecute(Animation kickoff) {
            suchStatus.startAnimation(kickoff);
        }
    }
}
