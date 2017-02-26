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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.versobit.weatherdoge.BuildConfig;
import com.versobit.weatherdoge.R;

public final class OtherShibesDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_othershibes";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        WebView wv = new WebView(getActivity());
        wv.getSettings().setDefaultTextEncodingName("utf-8");
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new JsInterface(), "App");
        wv.loadUrl("file:///android_asset/othershibes.html");
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(wv)
                .setPositiveButton(R.string.wow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    private static final class JsInterface {
        @JavascriptInterface
        public boolean isPlay() {
            return BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY);
        }
    }
}
