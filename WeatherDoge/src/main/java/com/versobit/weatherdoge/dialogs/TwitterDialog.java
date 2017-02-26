/*
 * Copyright (C) 2014-2015 VersoBit
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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.versobit.weatherdoge.R;

public final class TwitterDialog extends DialogFragment {

    static final String FRAGMENT_TAG = "fragment_dialog_twitter";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String[] accts = getActivity().getResources().getStringArray(R.array.dialog_about_twitter);
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setItems(accts, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s = accts[which].substring(1);
                        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + s)));
                        dismiss();
                    }
                })
                .create();
    }
}
