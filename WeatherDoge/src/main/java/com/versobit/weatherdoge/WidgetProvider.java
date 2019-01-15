/*
 * Copyright (C) 2014-2016, 2019 VersoBit
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

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public final class WidgetProvider extends AppWidgetProvider {

    private final static String WORKER_ALL_TAG = "WidgetWorker_All";
    private final static String UPDATE_ALL_WIDGETS_SEQUENCE = "update_widgets_recurring";

    // Will only be called once (on widget startup)
    @Override
    public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        requeueWorkSchedule(ctx);
        WidgetWorker.enqueueOnceMultiple(appWidgetIds);
    }

    @Override
    public void onEnabled(Context ctx) {
        requeueWorkSchedule(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        WorkManager.getInstance().cancelAllWorkByTag(WORKER_ALL_TAG);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(ctx, appWidgetManager, appWidgetId, newOptions);
        WidgetWorker.enqueueOnceSingle(appWidgetId);
    }

    static void requeueWorkSchedule(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        //noinspection ConstantConditions
        long interval = Long.parseLong(
                prefs.getString(OptionsActivity.PREF_WIDGET_REFRESH,"1800")
        );
        Constraints workerConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data workerData = new Data.Builder()
                .putString(WidgetWorker.ACTION, WidgetWorker.ACTION_REFRESH_ALL)
                .build();
        PeriodicWorkRequest widgetWorkerRequest =
                new PeriodicWorkRequest.Builder(WidgetWorker.class, interval, TimeUnit.SECONDS)
                        .addTag(WORKER_ALL_TAG)
                        .setConstraints(workerConstraints)
                        .setInputData(workerData)
                        .build();
        WorkManager.getInstance().enqueueUniquePeriodicWork(UPDATE_ALL_WIDGETS_SEQUENCE,
                ExistingPeriodicWorkPolicy.REPLACE, widgetWorkerRequest);
    }

    static Bitmap[] getTextBitmaps(Context ctx, String temp, String description, String location, String lastUpdated) {
        Bitmap[] bitmaps = { null, null, null, null };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean useComicNeue = prefs.getBoolean(OptionsActivity.PREF_WIDGET_USE_COMIC_NEUE, false)
                || BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_FOSS);
        Resources res = ctx.getResources();
        Typeface primaryFont = Typeface.createFromAsset(ctx.getAssets(), useComicNeue ?
                "ComicNeue-Regular.ttf" : "comic.ttf");
        Typeface secondaryFont = Typeface.createFromAsset(ctx.getAssets(), "RobotoCondensed-Regular.ttf");
        float shadowRadius = res.getDimension(R.dimen.widget_text_shadow_radius);
        // Odd results with fractional offsets
        float shadowXY = Math.round(res.getDimension(R.dimen.widget_text_shadow_xy));
        // Better to have more padding than not enough
        int shadowPadX = (int)Math.ceil(res.getDimension(R.dimen.widget_text_shadow_padding_width));
        int shadowPadY = (int)Math.ceil(res.getDimension(R.dimen.widget_text_shadow_padding_height));

        // Configure text painter
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        textPaint.setTypeface(primaryFont);
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(res.getDimension(R.dimen.widget_temp_font_size));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(shadowRadius, shadowXY, shadowXY, Color.BLACK);

        //
        Rect textBounds = new Rect();
        textPaint.getTextBounds(temp, 0, temp.length(), textBounds);

        bitmaps[0] = Bitmap.createBitmap(textBounds.width() + shadowPadX, textBounds.height() + shadowPadY, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmaps[0]);
        c.drawText(temp, textBounds.width() / 2f, textBounds.height(), textPaint);

        textPaint.setTextSize(res.getDimension(R.dimen.widget_desc_font_size));
        textBounds = new Rect();
        textPaint.getTextBounds(description + "g", 0, description.length() + 1, textBounds);
        Rect b2 = new Rect();
        textPaint.getTextBounds("a", 0, 1, b2);

        bitmaps[1] = Bitmap.createBitmap(textBounds.width() + shadowPadX, textBounds.height() + shadowPadY, Bitmap.Config.ARGB_8888);
        c = new Canvas(bitmaps[1]);
        c.drawText(description, textBounds.width() / 2f, (textBounds.height() + b2.height()) / 2f, textPaint);

        textPaint.setTextSize(res.getDimension(R.dimen.widget_bottom_bar_font_size));
        textPaint.setTypeface(secondaryFont);
        textPaint.setShadowLayer(0, 0, 0, Color.BLACK);

        textBounds = new Rect();
        textPaint.getTextBounds(location + "g", 0, location.length() + 1, textBounds);

        b2 = new Rect();
        textPaint.getTextBounds("a", 0, 1, b2);

        bitmaps[2] = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Bitmap.Config.ARGB_8888);
        c = new Canvas(bitmaps[2]);
        c.drawText(location, c.getWidth() / 2f, (c.getHeight() + b2.height()) / 2f, textPaint);

        textBounds = new Rect();
        textPaint.getTextBounds(lastUpdated + "g", 0, lastUpdated.length() + 1, textBounds);

        bitmaps[3] = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Bitmap.Config.ARGB_8888);
        c = new Canvas(bitmaps[3]);
        c.drawText(lastUpdated, c.getWidth() / 2f, (c.getHeight() + b2.height()) / 2f + 1, textPaint);

        return bitmaps;
    }

    static Bitmap getStatusBitmap(Context ctx, String status) {
        Typeface roboto = Typeface.createFromAsset(ctx.getAssets(), "RobotoCondensed-Regular.ttf");
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        textPaint.setTypeface(roboto);
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(ctx.getResources().getDimension(R.dimen.widget_bottom_bar_font_size));

        Rect textBounds = new Rect();
        textPaint.getTextBounds(status + "g", 0, status.length() + 1, textBounds);
        Rect baselineBounds = new Rect();
        textPaint.getTextBounds("a", 0, 1, baselineBounds);

        Bitmap loading = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(loading);
        canvas.drawText(status, canvas.getWidth() / 2f, (canvas.getHeight() + baselineBounds.height()) / 2f, textPaint);

        return loading;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static float[] getWidgetSize(DisplayMetrics metrics, Bundle options) {
        return new float[] {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), metrics),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT), metrics)
        };
    }

    // Updates the sky bitmap for a single app widget instance
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static Bitmap getSkyBitmap(Context ctx, Bundle options, int skyId) {
        Resources res = ctx.getResources();
        Bitmap originalSky = BitmapFactory.decodeResource(res, skyId);

        // Obtain (approximate?) size of widget (and by extension the image view)
        float[] widgetSize = getWidgetSize(res.getDisplayMetrics(), options);
        float viewW = widgetSize[0], viewH = widgetSize[1];
        // Obtain size of sky bitmap
        float bmpW = originalSky.getWidth(), bmpH = originalSky.getHeight();

        // For some reason the calculated view height is too small by about a pixel or so. Let's
        // compensate for that and while we're at it increase the height by the radius of the
        // rounded edges to hide the edges behind the bottom bar
        float bottomBarHeight = res.getDimension(R.dimen.widget_bottom_bar_height);
        float radius = res.getDimension(R.dimen.widget_corner_radius);
        float extra = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics());
        viewH += extra + radius - bottomBarHeight;

        // Implement ImageView's CENTER_CROP scale type
        Matrix skyMatrix = new Matrix();
        {
            float scale;
            float dx = 0, dy = 0;
            if (bmpW * viewH > viewW * bmpH) {
                scale = viewH / bmpH;
                dx = (viewW - bmpW * scale) * 0.5f;
            } else {
                scale = viewW / bmpW;
                dy = (viewH - bmpH * scale) * 0.5f;
            }
            skyMatrix.setScale(scale, scale);
            skyMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        }

        // Create a new bitmap/canvas pair at the size we need and draw on it
        Bitmap scaledSky = Bitmap.createBitmap((int) viewW, (int) viewH, Bitmap.Config.ARGB_8888);
        Canvas scaledCanvas = new Canvas(scaledSky);
        scaledCanvas.drawBitmap(originalSky, skyMatrix, new Paint());
        originalSky.recycle();

        // Rounded corner time! We need to round the top two corners.
        BitmapShader shader = new BitmapShader(scaledSky, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setShader(shader);
        Bitmap roundedSky = Bitmap.createBitmap(scaledSky.getWidth(), scaledSky.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas roundedCanvas = new Canvas(roundedSky);
        roundedCanvas.drawRoundRect(new RectF(0, 0, scaledSky.getWidth(), scaledSky.getHeight()),
                radius, radius, cornerPaint);
        scaledSky.recycle();

        return roundedSky;
    }

    static Bitmap getWowLayer(Context ctx, Bundle options, String image, int temp) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean useComicNeue = prefs.getBoolean(OptionsActivity.PREF_WIDGET_USE_COMIC_NEUE, false)
                || BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_FOSS);

        Resources res = ctx.getResources();

        // Find the size in pixels of the widget
        float[] widgetSize = getWidgetSize(res.getDisplayMetrics(), options);
        // Subtract out the bottom bar so now we have the rough size of the wowlayer
        widgetSize[1] -= res.getDimension(R.dimen.widget_bottom_bar_height);

        // Text shadow radius for wow text
        float shadowRadius = res.getDimension(R.dimen.widget_wowlayer_shadow_radius);
        // Odd results with fractional offsets
        float shadowXY = Math.max(1, Math.round(res.getDimension(R.dimen.widget_wowlayer_shadow_xy)));

        // Create a bitmap + canvas the size of the wowlayer to draw wows on
        Bitmap bitmap = Bitmap.createBitmap((int) widgetSize[0], (int) widgetSize[1], Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Setup the initial text painter
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        textPaint.setTypeface(Typeface.createFromAsset(ctx.getAssets(), useComicNeue ?
                "ComicNeue-Regular.ttf" : "comic.ttf"));
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(res.getDimension(useComicNeue ? R.dimen.widget_wowlayer_font_size_neue
                : R.dimen.widget_wowlayer_font_size));
        textPaint.setShadowLayer(shadowRadius, shadowXY, shadowXY, Color.BLACK);

        // Initialize the random number generator
        Random r = new Random();

        // Find the colors, and text to use for our dogeisms
        int[] colors = res.getIntArray(R.array.wow_colors);
        String[] wows = res.getStringArray(R.array.wows);
        String[] dogefixes = res.getStringArray(R.array.dogefix);
        String[] weatherAdjs = ArrayUtils.addAll(
                res.getStringArray(WeatherDoge.getTempAdjectives(temp)),
                res.getStringArray(WeatherDoge.getBgAdjectives(image))
        );

        // Calculate how many dogeisms to display for a given density independent widget area
        double area = (widgetSize[0] / res.getDisplayMetrics().density) *
                (widgetSize[1] / res.getDisplayMetrics().density);
        int total = (int)Math.max(1, Math.round(0.000066156726853611d * area + 1.1833074350128d));

        // Store our drawn rectangles so we can avoid text overlaps
        Rect[] drawnRects = new Rect[total + 1];
        // Prevent text from drawing underneath the temperature display at the bottom left
        drawnRects[total] = new Rect(0,
                (int)(widgetSize[1] - res.getDimension(R.dimen.widget_wowlayer_rect_top)),
                (int)res.getDimension(R.dimen.widget_wowlayer_rect_right), (int)widgetSize[1]);

        // Store dogeisms used to avoid duplicates
        String[] drawnText = new String[total];

        // Loop over all the dogeisms we're going to create
        for(int i = 0; i < total; i++) {
            Rect textBounds = new Rect();
            // The rectangle which represents the text's actual position on the canvas
            Rect realRect = new Rect();
            textPaint.setColor(colors[r.nextInt(colors.length)]);

            // The text to draw
            drawnText[i] = getUniqueDogeism(drawnText, r, wows, dogefixes, weatherAdjs);

            // Find the bounding rectangle of the text
            textPaint.getTextBounds(drawnText[i], 0, drawnText[i].length(), textBounds);
            // Prevent the text from going offscreen
            int xMax = (int)widgetSize[0] - textBounds.width();
            int yMax = (int)widgetSize[1] - textBounds.height();
            do {
                // Create the real rectangle using a random origin
                realRect.left = r.nextInt(xMax);
                realRect.right = realRect.left + textBounds.width();
                realRect.bottom = r.nextInt(yMax);
                realRect.top = realRect.bottom - textBounds.height();
                // Continue to loop until we find a valid in-bounds rectangle.
                // Since the drawText origin is the bottom left we need to make certain that
                // our text will not clip off the top edge of the widget.
                // We also need to check if this rectangle intersects any previously drawn
                // text rectangles. We don't want text-on-text.
            } while(realRect.bottom < textBounds.height() || intersects(realRect, drawnRects));

            // realRect is valid. Draw it out.
            canvas.drawText(drawnText[i], realRect.left, realRect.bottom, textPaint);

            // Store it for intersection checks
            drawnRects[i] = realRect;
        }

        return bitmap;
    }

    private static String getUniqueDogeism(String[] existing, Random r, String[] wows,
                                           String[] dogefixes, String[] weatherAdjs) {
        String ism = null;
        while(ism == null) {
            ism = WeatherDoge.getDogeism(r, wows, dogefixes, weatherAdjs);
            for(String s : existing) {
                if(s == null) {
                    continue;
                }
                if(ism.equals(s)) {
                    ism = null;
                    break;
                }
            }
        }
        return ism;
    }

    // Searches a Rect array for an intersection with a given Rect
    private static boolean intersects(Rect needle, Rect[] haystack) {
        for(Rect r : haystack) {
            if(r != null && Rect.intersects(r, needle)) {
                return true;
            }
        }
        return false;
    }
}
