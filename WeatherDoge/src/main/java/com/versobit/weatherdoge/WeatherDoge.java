package com.versobit.weatherdoge;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;


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
                resId = day ? R.drawable.sky_10d : R.drawable.sky_10n;
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
                resId = day ? R.drawable.doge_02d : R.drawable.doge_02n;
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
    static String[] getTempAdjectives(Resources res, int temp) {
        if(temp <= -30) {
            return res.getStringArray(R.array.weather_polarvortex);
        } else if(temp > -30 && temp <= -15) {
            return res.getStringArray(R.array.weather_yuck);
        } else if(temp > -15 && temp <= -7) {
            return res.getStringArray(R.array.weather_notokay);
        } else if(temp > -7 && temp <= 0) {
            return res.getStringArray(R.array.weather_chilly);
        } else if(temp > 0 && temp <= 10) {
            return res.getStringArray(R.array.weather_concern);
        } else if(temp > 10 && temp <= 20) {
            return res.getStringArray(R.array.weather_whatever);
        } else if(temp > 20 && temp <= 30) {
            return res.getStringArray(R.array.weather_warmth);
        } else if(temp > 30) {
            return res.getStringArray(R.array.weather_globalwarming);
        } else {
            return res.getStringArray(R.array.weather_wow);
        }
    }
}
