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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
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
import android.util.TypedValue;

public final class WidgetProvider extends AppWidgetProvider {

    // Will only be called once (on widget startup)
    @Override
    public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        resetAlarm(ctx);
        ctx.startService(new Intent(ctx, WidgetService.class)
                .setAction(WidgetService.ACTION_REFRESH_MULTIPLE)
                .putExtra(WidgetService.EXTRA_WIDGET_ID, appWidgetIds));
    }

    @Override
    public void onEnabled(Context ctx) {
        resetAlarm(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(PendingIntent.getService(ctx, 0, new Intent(ctx, WidgetService.class)
                .setAction(WidgetService.ACTION_REFRESH_ALL), 0));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(ctx, appWidgetManager, appWidgetId, newOptions);
        ctx.startService(new Intent(ctx, WidgetService.class)
                .setAction(WidgetService.ACTION_REFRESH_ONE)
                .putExtra(WidgetService.EXTRA_WIDGET_ID, appWidgetId));
    }

    static void resetAlarm(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        long interval = Integer.parseInt(prefs.getString(OptionsActivity.PREF_WIDGET_REFRESH, "1800"))
                * 1000l;
        AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pIntent = PendingIntent.getService(ctx, 0,
                new Intent(ctx, WidgetService.class).setAction(WidgetService.ACTION_REFRESH_ALL), 0);
        am.cancel(pIntent);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, interval, pIntent);
    }

    static Bitmap[] getTextBitmaps(Context ctx, String temp, String description, String location, String lastUpdated) {
        Bitmap[] bitmaps = { null, null, null, null };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean useComicNeue = prefs.getBoolean(OptionsActivity.PREF_WIDGET_USE_COMIC_NEUE, false);
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
        textPaint.getTextBounds(description, 0, description.length(), textBounds);
        Rect b2 = new Rect();
        textPaint.getTextBounds("a", 0, 1, b2);

        bitmaps[1] = Bitmap.createBitmap(textBounds.width() + shadowPadX, textBounds.height() + shadowPadY, Bitmap.Config.ARGB_8888);
        c = new Canvas(bitmaps[1]);
        c.drawText(description, textBounds.width() / 2f, (textBounds.height() + b2.height()) / 2f, textPaint);

        textPaint.setTextSize(res.getDimension(R.dimen.widget_bottom_bar_font_size));
        textPaint.setTypeface(Typeface.createFromAsset(ctx.getAssets(), "RobotoCondensed-Regular.ttf"));
        textPaint.setShadowLayer(0, 0, 0, Color.BLACK);

        textBounds = new Rect();
        textPaint.getTextBounds(location, 0, location.length(), textBounds);

        b2 = new Rect();
        textPaint.getTextBounds("a", 0, 1, b2);

        bitmaps[2] = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Bitmap.Config.ARGB_8888);
        c = new Canvas(bitmaps[2]);
        c.drawText(location, c.getWidth() / 2f, (c.getHeight() + b2.height()) / 2f, textPaint);

        textBounds = new Rect();
        textPaint.getTextBounds(lastUpdated, 0, lastUpdated.length(), textBounds);

        bitmaps[3] = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Bitmap.Config.ARGB_8888);
        c = new Canvas(bitmaps[3]);
        c.drawText(lastUpdated, c.getWidth() / 2f, (c.getHeight() + b2.height()) / 2f, textPaint);

        return bitmaps;
    }

    static Bitmap getLoadingBitmap(Context ctx) {
        String loadingText = ctx.getString(R.string.loading);
        Typeface roboto = Typeface.createFromAsset(ctx.getAssets(), "RobotoCondensed-Regular.ttf");
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        textPaint.setTypeface(roboto);
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(ctx.getResources().getDimension(R.dimen.widget_bottom_bar_font_size));

        Rect textBounds = new Rect();
        textPaint.getTextBounds(loadingText, 0, loadingText.length(), textBounds);
        Rect baselineBounds = new Rect();
        textPaint.getTextBounds("a", 0, 1, baselineBounds);

        Bitmap loading = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(loading);
        canvas.drawText(loadingText, canvas.getWidth() / 2f, (canvas.getHeight() + baselineBounds.height()) / 2f, textPaint);

        return loading;
    }

    // Updates the sky bitmap for a single app widget instance
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static Bitmap getSkyBitmap(Context ctx, Bundle options, int skyId) {
        Resources res = ctx.getResources();
        Bitmap originalSky = BitmapFactory.decodeResource(res, skyId);

        // Obtain (approximate?) size of widget (and by extension the image view)
        float viewW = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                res.getDisplayMetrics());
        float viewH = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                res.getDisplayMetrics());
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
}
