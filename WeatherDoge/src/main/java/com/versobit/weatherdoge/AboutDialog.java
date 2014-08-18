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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

class AboutDialog extends AlertDialog {
    AboutDialog(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setButton(DialogInterface.BUTTON_NEUTRAL, getContext().getString(R.string.wow), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        View v = getLayoutInflater().inflate(R.layout.dialog_about, null);
        v.findViewById(R.id.dialog_about_vb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://versobit.com/")));
            }
        });
        ((TextView) v.findViewById(R.id.dialog_about_version)).setText(BuildConfig.VERSION_NAME);
        ((TextView)v.findViewById(R.id.dialog_about_text2)).setMovementMethod(LinkMovementMethod.getInstance());
        v.findViewById(R.id.dialog_about_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder adb = new Builder(getContext());
                final String[] accts = getContext().getResources().getStringArray(R.array.dialog_about_twitter);
                adb.setItems(accts, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s = accts[which].substring(1);
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + s)));
                        dismiss();
                    }
                });
                adb.show();
            }
        });
        setView(v);
        super.onCreate(savedInstanceState);
    }
}
