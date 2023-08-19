/*
 * Copyright (C) 2014-2015, 2019, 2023 VersoBit
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

package com.versobit.weatherdoge.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.versobit.weatherdoge.BuildConfig;
import com.versobit.weatherdoge.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class AboutDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_about";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null);
        v.findViewById(R.id.dialog_about_vb).setOnClickListener(v1 ->
                startActivity(
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.addr_versobit)))
                ));
        ((TextView) v.findViewById(R.id.dialog_about_version)).setText(BuildConfig.VERSION_NAME);
        ((TextView)v.findViewById(R.id.dialog_about_text2)).setMovementMethod(LinkMovementMethod.getInstance());
        v.findViewById(R.id.dialog_about_contact).setOnClickListener(v12 -> composeEmail());
        v.findViewById(R.id.dialog_about_privacy).setOnClickListener(v1 ->
                startActivity(
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.dialog_about_privacy_url)))
                ));
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setPositiveButton(R.string.wow, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void composeEmail() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        DateFormat iso8601Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Formatter.setTimeZone(utc);
        String time = iso8601Formatter.format(Calendar.getInstance(utc, Locale.US).getTime());
        Intent intent = new Intent(Intent.ACTION_SENDTO)
                .setData(Uri.parse("mailto:"))
                .putExtra(Intent.EXTRA_EMAIL,
                        new String[] { getString(R.string.dialog_about_contact_email) })
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.dialog_about_contact_subject))
                .putExtra(Intent.EXTRA_TEXT,
                        getString(
                                R.string.dialog_about_contact_body,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                                BuildConfig.FLAVOR,
                                BuildConfig.APPLICATION_ID,
                                BuildConfig.BUILD_TYPE,
                                BuildConfig.DEBUG,
                                Build.VERSION.RELEASE,
                                Build.VERSION.SDK_INT,
                                Build.DEVICE,
                                Build.MODEL,
                                Build.PRODUCT,
                                Build.MANUFACTURER,
                                Build.BRAND,
                                time
                        ));
        if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
            Toast.makeText(getActivity(), R.string.dialog_about_contact_noemail, Toast.LENGTH_LONG)
                    .show();
            return;
        }
        startActivity(intent);
    }
}
