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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
final public class OptionsActivity extends PreferenceActivity {

    static final String PREF_FORCE_METRIC = "pref_force_metric";
    static final String PREF_FORCE_LOCATION = "pref_force_location";
    static final String PREF_WEATHER_SOURCE = "pref_weather_source";
    static final String PREF_APP_USE_COMIC_NEUE = "pref_use_comic_neue";
    static final String PREF_APP_DROP_SHADOW = "pref_drop_shadow";
    static final String PREF_APP_TEXT_ON_TOP = "pref_text_on_top";
    static final String PREF_WIDGET_REFRESH = "pref_widget_refresh";
    static final String PREF_WIDGET_TAP_TO_REFRESH = "pref_widget_tap_refresh";
    static final String PREF_WIDGET_USE_COMIC_NEUE = "pref_widget_use_comic_neue";
    static final String PREF_WIDGET_BACKGROUND_FIX = "pref_widget_background_fix";
    static final String PREF_ABOUT_VERSION = "pref_about_version";
    static final String PREF_ABOUT_CONTRIBUTE = "pref_about_contribute";
    static final String PREF_ABOUT_ADD_CREDITS = "pref_about_additional_credits";

    boolean genForceMetric = false;
    String genForceLocation = "";
    WeatherUtil.Source genWeatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
    String widgetRefreshInterval = "1800";
    boolean widgetTapToRefresh = false;
    boolean widgetComicNeue = false;
    boolean widgetBackgroundFix = false;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        genForceMetric = prefs.getBoolean(PREF_FORCE_METRIC, genForceMetric);
        genForceLocation = prefs.getString(PREF_FORCE_LOCATION, genForceLocation);
        String strSource = prefs.getString(PREF_WEATHER_SOURCE, "0");
        genWeatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
        if(strSource.equals("1")) {
            genWeatherSource = WeatherUtil.Source.YAHOO;
        }
        widgetRefreshInterval = prefs.getString(PREF_WIDGET_REFRESH, widgetRefreshInterval);
        widgetTapToRefresh = prefs.getBoolean(PREF_WIDGET_TAP_TO_REFRESH, widgetTapToRefresh);
        widgetComicNeue = prefs.getBoolean(PREF_WIDGET_USE_COMIC_NEUE, widgetComicNeue);
        widgetBackgroundFix = prefs.getBoolean(PREF_WIDGET_BACKGROUND_FIX, widgetBackgroundFix);
        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'app' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_app_header);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_app);
        findPreference(PREF_APP_DROP_SHADOW).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DropShadowDialog(OptionsActivity.this).show();
                return true;
            }
        });

        // Add 'widget' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_widget_header);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_widget);

        // Add 'about' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.about);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_about);

        Preference aboutVersion = findPreference(PREF_ABOUT_VERSION);
        aboutVersion.setTitle(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME.split("-")[0]);
        aboutVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutDialog(OptionsActivity.this).show();
                return true;
            }
        });
        findPreference(PREF_ABOUT_CONTRIBUTE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new ContributeDialog(OptionsActivity.this).show();
                return true;
            }
        });

        findPreference(PREF_ABOUT_ADD_CREDITS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OtherShibesDialog(OptionsActivity.this).show();
                return true;
            }
        });

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(PREF_FORCE_LOCATION));
        bindPreferenceSummaryToValue(findPreference(PREF_WEATHER_SOURCE));
        bindPreferenceSummaryToValue(findPreference(PREF_WIDGET_REFRESH));
    }

    @Override
    protected void onStop() {
        // Refresh widget options if they've changed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean newGenForceMetric = prefs.getBoolean(PREF_FORCE_METRIC, genForceMetric);
        String newGenForceLocation = prefs.getString(PREF_FORCE_LOCATION, genForceLocation);
        String strSource = prefs.getString(PREF_WEATHER_SOURCE, "0");
        WeatherUtil.Source newGenWeatherSource = WeatherUtil.Source.OPEN_WEATHER_MAP;
        if(strSource.equals("1")) {
            newGenWeatherSource = WeatherUtil.Source.YAHOO;
        }
        String newWidgetRefreshInterval = prefs.getString(PREF_WIDGET_REFRESH, widgetRefreshInterval);
        boolean newWidgetTapToRefresh = prefs.getBoolean(PREF_WIDGET_TAP_TO_REFRESH, widgetTapToRefresh);
        boolean newWidgetComicNeue = prefs.getBoolean(PREF_WIDGET_USE_COMIC_NEUE, widgetComicNeue);
        boolean newWidgetBackgroundFix = prefs.getBoolean(PREF_WIDGET_BACKGROUND_FIX, widgetBackgroundFix);
        if(newGenForceMetric != genForceMetric ||
                !newGenForceLocation.equals(genForceLocation) ||
                newGenWeatherSource != genWeatherSource ||
                newWidgetTapToRefresh != widgetTapToRefresh ||
                newWidgetComicNeue != widgetComicNeue ||
                newWidgetBackgroundFix != widgetBackgroundFix) {
            startService(new Intent(this, WidgetService.class)
                    .setAction(WidgetService.ACTION_REFRESH_ALL));
        }
        if(!newWidgetRefreshInterval.equals(widgetRefreshInterval)) {
            WidgetProvider.resetAlarm(this);
        }
        super.onStop();
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue.isEmpty() ? preference.getContext().getString(R.string.unset) : stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), preference.getContext().getString(R.string.unset)));
    }
}
