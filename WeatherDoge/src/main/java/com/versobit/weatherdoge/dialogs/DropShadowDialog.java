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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.versobit.weatherdoge.BuildConfig;
import com.versobit.weatherdoge.OptionsActivity;
import com.versobit.weatherdoge.R;

import java.text.DecimalFormat;

public final class DropShadowDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_dropshadow";

    private float radius;
    private float x;
    private float y;
    private boolean adjs;

    private TextView preview;
    private TextView txtR;
    private TextView txtX;
    private TextView txtY;

    private SeekBar seekR;
    private SeekBar seekX;
    private SeekBar seekY;

    private CheckBox chkAdjs;

    private DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean useNeue = sp.getBoolean(OptionsActivity.PREF_APP_USE_COMIC_NEUE, false)
                || BuildConfig.FLAVOR.equals(BuildConfig.FLAVOR_FOSS);
        Context ctx = getActivity();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ctx = new ContextThemeWrapper(getActivity(), R.style.AppTheme_Dialog_Light);
        }
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_dropshadow, null);

        preview = (TextView)v.findViewById(R.id.dialog_dropshadow_txtpreview);
        preview.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), useNeue ? "ComicNeue-Regular.ttf" : "comic.ttf"));

        txtR = (TextView)v.findViewById(R.id.dialog_dropshadow_txtradius);
        txtX = (TextView)v.findViewById(R.id.dialog_dropshadow_txtx);
        txtY = (TextView)v.findViewById(R.id.dialog_dropshadow_txty);

        seekR = (SeekBar)v.findViewById(R.id.dialog_dropshadow_seekradius);
        seekX = (SeekBar)v.findViewById(R.id.dialog_dropshadow_seekx);
        seekY = (SeekBar)v.findViewById(R.id.dialog_dropshadow_seeky);

        chkAdjs = (CheckBox)v.findViewById(R.id.dialog_dropshadow_checkadj);

        seekR.setOnSeekBarChangeListener(onSeek);
        seekX.setOnSeekBarChangeListener(onSeek);
        seekY.setOnSeekBarChangeListener(onSeek);

        chkAdjs.setOnCheckedChangeListener(onCheck);

        df.setDecimalSeparatorAlwaysShown(true);
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df.setMaximumIntegerDigits(2);
        df.setMinimumIntegerDigits(1);

        // So wrong but it feels so right (this keeps the numbers tidy on the right edge)
        CharSequence cs = txtR.getText();
        // Measure the size needed for the maximum possible characters
        txtR.setText(df.format(-25f)); // Should produce -25.00 but locale-specific
        txtR.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // Apply the max fixed width to the views
        LinearLayout.LayoutParams widthHeight = new LinearLayout.LayoutParams(txtR.getMeasuredWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);
        txtR.setText(cs); // Restore
        txtR.setLayoutParams(widthHeight);
        txtX.setLayoutParams(widthHeight);
        txtY.setLayoutParams(widthHeight);

        radius = sp.getFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_radius", 1f);
        x = sp.getFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_x", 3f);
        y = sp.getFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_y", 3f);
        adjs = sp.getBoolean(OptionsActivity.PREF_APP_DROP_SHADOW + "_adjs", false);

        seekR.setProgress((int) map(radius, 0, 25, 0, 100));
        seekX.setProgress((int) map(x, -25, 25, 0, 100));
        seekY.setProgress((int) map(y, -25, 25, 0, 100));
        chkAdjs.setChecked(adjs);

        // Fix background image height
        View scrollView = v.findViewById(R.id.dialog_dropshadow_scroll);
        scrollView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        v.findViewById(R.id.dialog_dropshadow_bg).setLayoutParams(
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        scrollView.getMeasuredHeight())
        );

        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.save, (dialog, which) -> sp.edit()
                        .putFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_radius", radius)
                        .putFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_x", x)
                        .putFloat(OptionsActivity.PREF_APP_DROP_SHADOW + "_y", y)
                        .putBoolean(OptionsActivity.PREF_APP_DROP_SHADOW + "_adjs", adjs)
                        .apply())
                .create();
    }

    private SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(seekBar == seekR) {
                radius = map(progress, 0, 100, 0, 25);
                txtR.setText(df.format(radius));
            } else if(seekBar == seekX) {
                x = map(progress, 0, 100, -25, 25);
                txtX.setText(df.format(x));
            } else if(seekBar == seekY) {
                y = map(progress, 0, 100, -25, 25);
                txtY.setText(df.format(y));
            }
            preview.setShadowLayer(radius, x, y, Color.BLACK);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    private CheckBox.OnCheckedChangeListener onCheck = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            adjs = isChecked;
        }
    };

    private float map(float in, float inMin, float inMax, float outMin, float outMax) {
        return (in - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
}
