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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

final class WhatsNewDialog extends AlertDialog {
    WhatsNewDialog(Context ctx) {
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
        setTitle(getContext().getString(R.string.dialog_whats_new_title));
        View v = getLayoutInflater().inflate(R.layout.dialog_whats_new, null);
        v.findViewById(R.id.dialog_whats_new_email).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent email = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", "support@versobit.com", null));
                email.putExtra(Intent.EXTRA_SUBJECT,
                        getContext().getString(R.string.dialog_whats_new_email_subject));
                getContext().startActivity(Intent.createChooser(email,
                        getContext().getString(R.string.dialog_whats_new_email_chooser)));
            }
        });
        v.findViewById(R.id.dialog_whats_new_versobit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://versobit.com/")));
            }
        });
        setView(v);
        super.onCreate(savedInstanceState);
    }
}
