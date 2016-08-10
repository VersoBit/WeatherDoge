/*
 * Copyright (C) 2015-2016 VersoBit Ltd
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class LocationApi implements LocationListener {

    private static final String TAG = "FossLocationApi";
    // Accurate to about 110 meters
    private static final int COORD_FUZZ = 3;
    private static final int LOC_ACCURACY = 110;

    private final LocationReceiver receiver;
    private final LocationManager locationManager;

    LocationApi(Context ctx, LocationReceiver receiver) {
        this.receiver = receiver;
        locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
    }

    void connect() {
        try {
            locationManager.requestLocationUpdates(getBestProvider(), 0, 0, this);
        } catch (SecurityException ex) {
            Log.wtf(TAG, ex);
            return;
        }
        receiver.onConnected();
    }

    void disconnect() {
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException ex) {
            Log.wtf(TAG, ex);
        }
    }

    boolean isConnected() {
        return true;
    }

    boolean isConnecting() {
        return false;
    }

    Location getLocation() {
        try {
            return fuzzLocation(locationManager.getLastKnownLocation(getBestProvider()));
        } catch (SecurityException ex) {
            Log.wtf(TAG, ex);
        }
        return null;
    }

    static boolean isAvailable(Context ctx) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private String getBestProvider() {
        // The network provider should be fastest and has enough accuracy for weather
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        // Fall back to network and hope it works
        return LocationManager.NETWORK_PROVIDER;
    }

    private static Location fuzzLocation(Location location) {
        // Because we are receiving precise locations (unlike the Google Play flavor) we need to do
        // the fuzzing on our own before sending the locations off to the APIs. Truncating the
        // coordinates to three decimals places will give similar accuracy. It's probably much
        // simpler than whatever Play Services is doing but hopefully it has a similar effect.
        location.setLatitude(BigDecimal.valueOf(location.getLatitude()).setScale(COORD_FUZZ, RoundingMode.DOWN).doubleValue());
        location.setLongitude(BigDecimal.valueOf(location.getLongitude()).setScale(COORD_FUZZ, BigDecimal.ROUND_DOWN).doubleValue());
        location.setAccuracy(LOC_ACCURACY);
        return location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onLocationChanged(Location location) {
        receiver.onLocation(fuzzLocation(location));
    }
}
