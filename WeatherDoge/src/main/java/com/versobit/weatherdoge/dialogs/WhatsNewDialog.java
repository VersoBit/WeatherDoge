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

package com.versobit.weatherdoge.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import com.versobit.weatherdoge.R;

public final class WhatsNewDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_whatsnew";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_whats_new, null);
        v.findViewById(R.id.dialog_whats_new_email).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent email = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", "support@versobit.com", null));
                email.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.dialog_whats_new_email_subject));
                startActivity(Intent.createChooser(email,
                        getString(R.string.dialog_whats_new_email_chooser)));
            }
        });
        v.findViewById(R.id.dialog_whats_new_versobit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.addr_versobit))));
            }
        });
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setTitle(R.string.dialog_whats_new_title)
                .setView(v)
                .setPositiveButton(R.string.wow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }
}
