/*
 * Copyright (C) 2014-2015, 2019 VersoBit
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
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
        // Much more complex than it needs to be but should work until we move away from this
        wv.setWebViewClient(new WebViewClient() {
            @Override
            @TargetApi(Build.VERSION_CODES.N)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.getMethod().equals("GET")) {
                    // GET only
                    return true;
                }
                String scheme = request.getUrl().getScheme();
                if ("https".equals(scheme) || "http".equals(scheme)) {
                    // Let request through
                    return false;
                }
                String url = request.getUrl().toString();
                // With just 3 URLs this naive way is probably the fastest way to do it
                if ("file:///android_asset/othershibes.html".equals(url) ||
                        "file:///android_asset/gpl.html".equals(url)) {
                    wv.loadUrl(url);
                }
                if (BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY) &&
                        "file:///android_asset/comicsans.html".equals(url)) {
                    wv.loadUrl(url);
                }
                // Block WebView from loading request
                return true;
            }
        });
        wv.loadUrl("file:///android_asset/othershibes.html");
        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(wv)
                .setPositiveButton(R.string.wow, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private static final class JsInterface {
        @JavascriptInterface
        public boolean isPlay() {
            return BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_PLAY);
        }
    }
}
