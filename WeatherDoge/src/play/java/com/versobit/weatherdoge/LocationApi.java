/*
 * Copyright (C) 2014-2015 VersoBit Ltd
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

final class LocationApi implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "GLocationApi";
    private static final int REQUEST_PLAY_ERR_DIAG = 52000000;
    private static final int REQUEST_PLAY_CONN_FAIL_RES = 3643;

    private final Context ctx;
    private final LocationReceiver receiver;
    private final GoogleApiClient client;

    LocationApi(Context ctx, LocationReceiver receiver) {
        this.ctx = ctx;
        this.receiver = receiver;
        client = new GoogleApiClient.Builder(ctx)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    void connect() {
        client.connect();
    }

    void disconnect() {
        LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        client.disconnect();
    }

    boolean isConnected() {
        return client.isConnected();
    }

    boolean isConnecting() {
        return client.isConnected();
    }

    Location getLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(client);
    }

    static boolean isAvailable(Context ctx) {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ctx);
        if(result == ConnectionResult.SUCCESS) {
            return true;
        }
        if(ctx instanceof Activity) {
            Activity act = (Activity)ctx;
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(result, act, REQUEST_PLAY_ERR_DIAG);
            if (errorDialog != null && !act.isFinishing()) {
                errorDialog.show();
            }
        } else {
            GooglePlayServicesUtil.showErrorNotification(result, ctx);
        }
        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        request.setInterval(5000);
        request.setFastestInterval(1000);
        LocationServices.FusedLocationApi.requestLocationUpdates(client, request, this);
        receiver.onConnected();
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(ctx instanceof Activity) {
            Activity act = (Activity)ctx;
            if(!connectionResult.hasResolution()) {
                Toast.makeText(act, "Connection failed.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                connectionResult.startResolutionForResult(act, REQUEST_PLAY_CONN_FAIL_RES);
            } catch (IntentSender.SendIntentException ex) {
                Log.wtf(TAG, ex);
            }
        }
        Log.e(TAG, "Connection failed... " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        receiver.onLocation(location);
    }
}
