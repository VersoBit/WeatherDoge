/*
 * Copyright (C) 2014-2015, 2017, 2019 VersoBit
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
import android.view.View;

import com.versobit.weatherdoge.R;
import com.versobit.weatherdoge.WeatherDoge;

public final class ContributeDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_contribute";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_contribute, null);
        v.findViewById(R.id.dialog_contribute_github_layout).setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.addr_github)));
            WeatherDoge.applyChromeCustomTab(getActivity(), i);
            startActivity(i);
        });
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setPositiveButton(R.string.wow, (dialog, which) -> dialog.dismiss())
                .create();
    }
}
