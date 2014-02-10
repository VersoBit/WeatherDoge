package com.versobit.weatherdoge;

import java.util.Locale;

// Based on http://stackoverflow.com/a/7860788/238374
class UnitLocale {
    static final int IMPERIAL = 0;
    static final int METRIC = 1;

    static int getDefault() {
        return getFrom(Locale.getDefault());
    }
    static int getFrom(Locale locale) {
        String countryCode = locale.getCountry();
        if ("US".equals(countryCode)) return IMPERIAL; // USA
        if ("LR".equals(countryCode)) return IMPERIAL; // Liberia
        if ("MM".equals(countryCode)) return IMPERIAL; // Burma
        return METRIC;
    }
}
