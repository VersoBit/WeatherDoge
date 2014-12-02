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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

public final class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(ctx, appWidgetManager, appWidgetId);
        }
    }

    private static PendingIntent getServiceIntent(Context ctx) {
        return PendingIntent.getService(ctx, 0,
                new Intent(ctx, WidgetService.class).setAction(WidgetService.ACTION_REFRESH), 0);
    }

    @Override
    public void onEnabled(Context ctx) {
        AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 300000, getServiceIntent(ctx));
    }

    @Override
    public void onDisabled(Context ctx) {
        AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getServiceIntent(ctx));
    }

    private static void updateAppWidget(Context ctx, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        //views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class), 0));
        views.setOnClickPendingIntent(R.id.widget_root, getServiceIntent(ctx));
        updateFontBitmaps(ctx, views, "-4°", "Mist", "Calgary", "last updated an hour ago");
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static void updateFontBitmaps(Context ctx, RemoteViews views, String temp, String description, String location, String lastUpdated) {
        Resources r = ctx.getResources();
        Typeface font = Typeface.createFromAsset(ctx.getAssets(), "comic.ttf");
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        paint.setTypeface(font);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40, r.getDisplayMetrics()));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(1, 3, 3, Color.BLACK);

        Rect bounds = new Rect();
        paint.getTextBounds(temp, 0, temp.length(), bounds);
        Log.e("LOL", bounds.width() + " " + bounds.height());


        Bitmap b = Bitmap.createBitmap(bounds.width() + 4, bounds.height() + 6, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        //c.drawColor(Color.BLUE);
        c.drawText(temp, bounds.width() / 2f, bounds.height(), paint);

        views.setImageViewBitmap(R.id.widget_tempimg, b);

        // FIXME: Compensate for below-the-line characters
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22, r.getDisplayMetrics()));
        bounds = new Rect();
        paint.getTextBounds(description, 0, description.length(), bounds);

        b = Bitmap.createBitmap(bounds.width() + 4, bounds.height() + 6, Bitmap.Config.ARGB_8888);
        c = new Canvas(b);
        //c.drawColor(Color.BLUE);
        c.drawText(description, bounds.width() / 2f, bounds.height(), paint);
        views.setImageViewBitmap(R.id.widget_descimg, b);

        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, r.getDisplayMetrics()));
        paint.setTypeface(Typeface.createFromAsset(ctx.getAssets(), "RobotoCondensed-Regular.ttf"));
        paint.setShadowLayer(0, 0, 0, Color.BLACK);

        bounds = new Rect();
        paint.getTextBounds(location, 0, location.length(), bounds);

        Rect b2 = new Rect();
        paint.getTextBounds("a", 0, 1, b2);

        b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        c = new Canvas(b);
        //c.drawColor(Color.RED);
        c.drawText(location, c.getWidth() / 2f, (c.getHeight() + b2.height()) / 2f, paint);
        views.setImageViewBitmap(R.id.widget_locationimg, b);

        bounds = new Rect();
        paint.getTextBounds(lastUpdated, 0, lastUpdated.length(), bounds);

        b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        c = new Canvas(b);
        //c.drawColor(Color.RED);
        c.drawText(lastUpdated, c.getWidth() / 2f, (c.getHeight() + b2.height()) / 2f, paint);
        views.setImageViewBitmap(R.id.widget_last_updated_img, b);
    }
}
