package com.versobit.weatherdoge;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.webkit.WebView;

class OtherShibesDialog extends AlertDialog {
    OtherShibesDialog(Context ctx) {
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
        WebView wv = new WebView(getContext());
        wv.getSettings().setDefaultTextEncodingName("utf-8");
        wv.loadUrl("file:///android_asset/othershibes.html");
        setView(wv);
        super.onCreate(savedInstanceState);
    }
}
