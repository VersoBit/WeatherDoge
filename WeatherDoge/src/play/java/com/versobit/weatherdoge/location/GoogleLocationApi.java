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

package com.versobit.weatherdoge.location;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;

public final class GoogleLocationApi implements DogeLocationApi {

    private static final String TAG = GoogleLocationApi.class.getSimpleName();
    private static final int REQUEST_PLAY_ERR_DIAG = 52000000;
    private static final int REQUEST_PLAY_CONN_FAIL_RES = 3643;
    private static final long UPDATE_INTERVAL = 5000;
    private static final long FASTEST_UPDATE_INTERVAL = 1000;
    private static final long DELAY_BETWEEN_FAIL_DIAG = 5000;

    private Context ctx;
    private LocationReceiver receiver;
    private FusedLocationProviderClient client;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    private final OnSuccessListener<LocationSettingsResponse> locationSettingsSuccessCallback = new OnSuccessListener<LocationSettingsResponse>() {
        @Override
        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            try {
                if (receiver != null) {
                    client.requestLocationUpdates(locationRequest, locationCallback, null);
                    status = ApiStatus.CONNECTED;
                    receiver.onConnected();
                } else {
                    Log.e(TAG, "Connected to location services but receiver is unavailable");
                    disconnect();
                }
            } catch (SecurityException ex) {
                Log.wtf(TAG, ex);
                disconnect();
            }
        }
    };

    private final OnFailureListener locationSettingsFailureCallback = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
            status = ApiStatus.DISCONNECTED;
            if (ctx instanceof Activity && ex instanceof ResolvableApiException) {
                if (SystemClock.elapsedRealtime() < lastFailDiag + DELAY_BETWEEN_FAIL_DIAG) {
                    return;
                }
                try {
                    ((ResolvableApiException) ex).startResolutionForResult((Activity) ctx, REQUEST_PLAY_CONN_FAIL_RES);
                    lastFailDiag = SystemClock.elapsedRealtime();
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, sendEx.getMessage(), sendEx);
                }
            } else {
                Toast.makeText(ctx, "Connection to location API failed.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                Log.e(TAG, "LocationCallback was provided a null LocationResult");
                return;
            }
            if (receiver != null) {
                receiver.onLocation(locationResult.getLastLocation());
            } else {
                Log.e(TAG, "No receiver is available to the LocationCallback");
                disconnect();
            }
        }
    };

    private ApiStatus status = ApiStatus.DISCONNECTED;
    private long lastFailDiag = -DELAY_BETWEEN_FAIL_DIAG - 1;

    GoogleLocationApi() {}

    @NotNull
    @Override
    public DogeLocationApi configure(@NotNull Context ctx, @NotNull LocationReceiver receiver) {
        this.ctx = ctx;
        this.receiver = receiver;
        locationRequest = new LocationRequest()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_LOW_POWER);
        locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build();
        return this;
    }

    @Override
    public void connect() {
        if (status != ApiStatus.DISCONNECTED) {
            return;
        }
        status = ApiStatus.CONNECTING;
        client = LocationServices.getFusedLocationProviderClient(ctx);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(ctx)
                .checkLocationSettings(locationSettingsRequest);
        task.addOnSuccessListener(locationSettingsSuccessCallback);
        task.addOnFailureListener(locationSettingsFailureCallback);
    }

    @Override
    public void disconnect() {
        if (client != null) {
            client.removeLocationUpdates(locationCallback);
            client = null;
        }
        status = ApiStatus.DISCONNECTED;
    }

    @NotNull
    @Override
    public ApiStatus getStatus() {
        return status;
    }

    @Override
    public boolean isAvailable() {
        GoogleApiAvailability apiAvail = GoogleApiAvailability.getInstance();
        int result = apiAvail.isGooglePlayServicesAvailable(ctx);
        if(result == ConnectionResult.SUCCESS) {
            return true;
        }
        if(ctx instanceof Activity) {
            Activity act = (Activity)ctx;
            Dialog errorDialog = apiAvail.getErrorDialog(act, result, REQUEST_PLAY_ERR_DIAG);
            if (errorDialog != null && !act.isFinishing()) {
                errorDialog.show();
            }
        } else {
            apiAvail.showErrorNotification(ctx, result);
        }
        return false;
    }

}
