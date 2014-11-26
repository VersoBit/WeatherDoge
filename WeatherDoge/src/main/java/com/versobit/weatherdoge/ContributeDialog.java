/*
 * Copyright (C) 2014 VersoBit Ltd
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

final class ContributeDialog extends AlertDialog {
    ContributeDialog(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setButton(BUTTON_NEUTRAL, getContext().getString(R.string.wow), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        View v = getLayoutInflater().inflate(R.layout.dialog_contribute, null);
        v.findViewById(R.id.dialog_contribute_github_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getContext().getString(R.string.addr_github))));
            }
        });
        v.findViewById(R.id.dialog_contribute_dogecoin_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(getContext().getString(R.string.addr_dogecoin));
                Toast.makeText(getContext(), R.string.such_copied, Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.dialog_contribute_bitcoin_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(getContext().getString(R.string.addr_bitcoin));
                Toast.makeText(getContext(), R.string.such_copied, Toast.LENGTH_SHORT).show();
            }
        });
        setView(v);
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    void copyToClipboard(String text) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cbm = (android.text.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cbm.setText(text);
            return;
        }
        android.content.ClipData clip = android.content.ClipData.newPlainText(getContext().getString(R.string.app_name), text);
        android.content.ClipboardManager cbm = (android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cbm.setPrimaryClip(clip);
    }
}
