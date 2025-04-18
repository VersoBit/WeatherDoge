/*
 * Copyright (C) 2014-2015, 2019, 2023, 2025 VersoBit
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
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.versobit.weatherdoge.BuildConfig;
import com.versobit.weatherdoge.R;

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
}
