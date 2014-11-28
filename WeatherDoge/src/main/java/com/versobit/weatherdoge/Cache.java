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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class Cache {

    private static final String TAG = Cache.class.getSimpleName();
    private static final String WEATHER_DATA_FILE = "weather_data";
    private static final int CACHE_MAX_AGE = 10 * 60 * 1000; // 10 minutes
    // http://gis.stackexchange.com/a/8674
    private static final int CACHE_COORD_FUZZ = 2; // 1.1km, more digits -> less fuzz

    private Cache() {
        //
    }

    static WeatherUtil.WeatherData getWeatherData(Context ctx, double latitude, double longitude) {
        return getWeatherData(ctx, latitude, longitude, null);
    }

    static WeatherUtil.WeatherData getWeatherData(Context ctx, String location) {
        return getWeatherData(ctx, Double.MIN_VALUE, Double.MIN_VALUE, location);
    }

    // This is not async because generally you're going to want to tie the result of this to your
    // UI and that's best left up to the caller.
    // Returns null if retrieval failed or the data has expired.
    private static WeatherUtil.WeatherData getWeatherData(Context ctx, double latitude, double longitude, String location) {
        FileInputStream fileIn = null;
        ObjectInputStream objectIn = null;
        try {
            File file = new File(ctx.getCacheDir(), WEATHER_DATA_FILE);
            if(!file.exists()) {
                return null;
            }

            fileIn = new FileInputStream(file);
            objectIn = new ObjectInputStream(fileIn);
            WeatherUtil.WeatherData data = (WeatherUtil.WeatherData)objectIn.readObject();

            // Expired?
            if((data.time.getTime() + CACHE_MAX_AGE) < System.currentTimeMillis()) {
                return null;
            }

            // Location still accurate?
            if(latitude == Double.MIN_VALUE && longitude == Double.MIN_VALUE) {
                if(location == null) {
                    throw new IllegalArgumentException();
                }
                if(!data.place.equals(location)) {
                    return null;
                }
            } else {
                // Round both sets of coordinates
                double[] cacheCoords = {
                        BigDecimal.valueOf(data.latitude).setScale(CACHE_COORD_FUZZ, RoundingMode.DOWN).doubleValue(),
                        BigDecimal.valueOf(data.longitude).setScale(CACHE_COORD_FUZZ, RoundingMode.DOWN).doubleValue() };
                double[] currentCoords = {
                        BigDecimal.valueOf(latitude).setScale(CACHE_COORD_FUZZ, RoundingMode.DOWN).doubleValue(),
                        BigDecimal.valueOf(longitude).setScale(CACHE_COORD_FUZZ, RoundingMode.DOWN).doubleValue() };
                if(cacheCoords[0] != currentCoords[0] || cacheCoords[1] != currentCoords[1]) {
                    // Difference must be larger than the radius possible with CACHE_COORD_FUZZ digits
                    return null;
                }
            }
            // Must be good to go!
            return data;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to retrieve or load WeatherData.", ex);
        } finally {
            close(objectIn);
            close(fileIn);
        }
        return null;
    }

    // Fire and forget cache storage
    static void putWeatherData(Context ctx, WeatherUtil.WeatherData data) {
        if(ctx == null || data == null) {
            throw new IllegalArgumentException();
        }
        File file = new File(ctx.getCacheDir(), WEATHER_DATA_FILE);
        new PutWeatherDataTask().execute(file, data);
    }

    private static class PutWeatherDataTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            FileOutputStream fileOut = null;
            ObjectOutputStream objectOut = null;
            try {
                fileOut = new FileOutputStream((File)params[0], false);
                objectOut = new ObjectOutputStream(fileOut);
                objectOut.writeObject(params[1]);
            } catch (IOException ex) {
                Log.e(TAG, "Failed to write WeatherData to cache. " + params[1].toString(), ex);
            } finally {
                close(objectOut);
                close(fileOut);
            }
            return null;
        }
    }

    private static void close(Closeable c) {
        if(c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException ex) {
            //
        }
    }
}
