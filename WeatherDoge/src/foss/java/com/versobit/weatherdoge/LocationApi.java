/*
 * Copyright (C) 2015 VersoBit Ltd
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

final class LocationApi implements LocationListener {

    private static final String PROVIDER = LocationManager.NETWORK_PROVIDER;

    private final LocationReceiver receiver;
    private final LocationManager locationManager;

    LocationApi(Context ctx, LocationReceiver receiver) {
        this.receiver = receiver;
        locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
    }

    void connect() {
        locationManager.requestLocationUpdates(PROVIDER, 0, 0, this);
        receiver.onConnected();
    }

    void disconnect() {
        locationManager.removeUpdates(this);
    }

    boolean isConnected() {
        return true;
    }

    boolean isConnecting() {
        return false;
    }

    Location getLocation() {
        return locationManager.getLastKnownLocation(PROVIDER);
    }

    static boolean isAvailable(Context ctx) {
        return ((LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(PROVIDER);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onLocationChanged(Location location) {
        receiver.onLocation(location);
    }
}
