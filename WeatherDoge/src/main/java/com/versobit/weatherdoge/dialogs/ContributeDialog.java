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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.versobit.weatherdoge.R;

public final class ContributeDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_contribute";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_contribute, null);
        v.findViewById(R.id.dialog_contribute_github_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.addr_github))));
            }
        });
        v.findViewById(R.id.dialog_contribute_dogecoin_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(getString(R.string.addr_dogecoin));
                Toast.makeText(getActivity(), R.string.such_copied, Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.dialog_contribute_bitcoin_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(getString(R.string.addr_bitcoin));
                Toast.makeText(getActivity(), R.string.such_copied, Toast.LENGTH_SHORT).show();
            }
        });
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setPositiveButton(R.string.wow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void copyToClipboard(String text) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cbm = (android.text.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            cbm.setText(text);
            return;
        }
        android.content.ClipData clip = android.content.ClipData.newPlainText(getString(R.string.app_name), text);
        android.content.ClipboardManager cbm = (android.content.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        cbm.setPrimaryClip(clip);
    }
}
