/*
 * Copyright (C) 2014 VersoBit
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

import java.util.Locale;

// Based on http://stackoverflow.com/a/7860788/238374
final class UnitLocale {
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
