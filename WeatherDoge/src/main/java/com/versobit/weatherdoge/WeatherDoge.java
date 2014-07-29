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

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Array;
import java.util.Random;


public class WeatherDoge extends Application {
    static PackageInfo getPackageInfo(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ex) {
            //
        }
        return null;
    }

    static int skySelect(String icon) {
        int img = Integer.parseInt(icon.substring(0, 2));
        boolean day = icon.charAt(2) == 'd';
        int resId = R.drawable.sky_01d;
        switch (img) {
            case 1:
                resId = day ? R.drawable.sky_01d : R.drawable.sky_01n;
                break;
            case 2:
                resId = day ? R.drawable.sky_02d : R.drawable.sky_02n;
                break;
            case 3:
                resId = day ? R.drawable.sky_03d : R.drawable.sky_03n;
                break;
            case 4:
                resId = day ? R.drawable.sky_04d : R.drawable.sky_04n;
                break;
            case 9:
                resId = day ? R.drawable.sky_09d : R.drawable.sky_09n;
                break;
            case 10:
                resId = day ? R.drawable.sky_10d : R.drawable.sky_09n;
                break;
            case 11:
                resId = day ? R.drawable.sky_11d : R.drawable.sky_11n;
                break;
            case 13:
                resId = day ? R.drawable.sky_13d : R.drawable.sky_13n;
                break;
            case 50:
                resId = day ? R.drawable.sky_50d : R.drawable.sky_50n;
                break;
        }
        return resId;
    }

    static int dogeSelect(String icon) {
        int img = Integer.parseInt(icon.substring(0, 2));
        boolean day = icon.charAt(2) == 'd';
        int resId = R.drawable.doge_01d;
        switch (img) {
            case 1:
                resId = day ? R.drawable.doge_01d : R.drawable.doge_01n;
                break;
            case 2:
                resId = day ? R.drawable.doge_04 : R.drawable.doge_02n;
                break;
            case 3:
                resId = day ? R.drawable.doge_03d : R.drawable.doge_03n;
                break;
            case 4:
                resId = R.drawable.doge_04;
                break;
            case 9:
                resId = R.drawable.doge_09;
                break;
            case 10:
                resId = R.drawable.doge_10;
                break;
            case 11:
                resId = R.drawable.doge_11;
                break;
            case 13:
                resId = R.drawable.doge_13;
                break;
            case 50:
                resId = R.drawable.doge_50;
                break;
        }
        return resId;
    }

    // Temp must be in celsius
    static int getTempAdjectives(int temp) {
        int i;
        if(temp <= -30) {
            i = R.array.weather_polarvortex;
        } else if(temp > -30 && temp <= -15) {
            i = R.array.weather_yuck;
        } else if(temp > -15 && temp <= -7) {
            i = R.array.weather_notokay;
        } else if(temp > -7 && temp <= 0) {
            i = R.array.weather_chilly;
        } else if(temp > 0 && temp <= 10) {
            i = R.array.weather_concern;
        } else if(temp > 10 && temp <= 20) {
            i = R.array.weather_whatever;
        } else if(temp > 20 && temp <= 30) {
           i = R.array.weather_warmth;
        } else if(temp > 30) {
            i = R.array.weather_globalwarming;
        } else {
            i = R.array.weather_wow;
        }
        return i;
    }

    static int getBgAdjectives(String bg) {
        int img = Integer.parseInt(bg.substring(0, 2));
        boolean day = bg.charAt(2) == 'd';
        int resId = R.array.bg_01d;
        switch (img) {
            case 1:
                resId = day ? R.array.bg_01d : R.array.bg_01n;
                break;
            case 2:
                resId = day ? R.array.bg_02d : R.array.bg_02d;
                break;
            case 3:
                resId = day ? R.array.bg_03d : R.array.bg_03n;
                break;
            case 4:
                resId = R.array.bg_04;
                break;
            case 9:
                resId = day ? R.array.bg_09d : R.array.bg_09n;
                break;
            case 10:
                resId = day ? R.array.bg_10d : R.array.bg_10n;
                break;
            case 11:
                resId = day ? R.array.bg_11d : R.array.bg_11n;
                break;
            case 13:
                resId = day ? R.array.bg_13d : R.array.bg_13n;
                break;
            case 50:
                resId = R.array.bg_50;
                break;
        }
        return resId;
    }

    // concat = very no
    static <T> T[] condoge(T[]... arrays) {
        int totalLength = 0;
        for(T[] s : arrays) {
            totalLength += s.length;
        }
        T[] full = (T[]) Array.newInstance(arrays[0].getClass().getComponentType(), totalLength);
        int pos = 0;
        for(T[] s : arrays) {
            System.arraycopy(s, 0, full, pos, s.length);
            pos += s.length;
        }
        return full;
    }

    static String getDogeism(String[] wows, String[] dogefixes, String[] weatherAdjectives) {
        Random r = new Random();
        // We need to treat so wow and wow as individual dogeisms, not overrepresented top-level dogefixes
        int wowOrNot = r.nextInt(weatherAdjectives.length + wows.length);
        if(wowOrNot >= weatherAdjectives.length) {
            // Wow or so wow
            return wows[wowOrNot - weatherAdjectives.length];
        }
        // Otherwise use a random dogefix with a random weather adjective
        return String.format(dogefixes[r.nextInt(dogefixes.length)], weatherAdjectives[r.nextInt(weatherAdjectives.length)]);
    }
}
