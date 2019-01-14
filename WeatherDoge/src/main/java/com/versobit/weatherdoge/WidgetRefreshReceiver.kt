/*
 * Copyright (C) 2019 VersoBit
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

package com.versobit.weatherdoge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val workerDataBuilder = Data.Builder()
                .putString(WidgetWorker.ACTION, intent.action)
        when (intent.action) {
            WidgetWorker.ACTION_REFRESH_MULTIPLE -> workerDataBuilder.putIntArray(
                    WidgetWorker.EXTRA_WIDGET_ID,
                    intent.getIntArrayExtra(WidgetWorker.EXTRA_WIDGET_ID))
            WidgetWorker.ACTION_REFRESH_ONE -> workerDataBuilder.putInt(
                    WidgetWorker.EXTRA_WIDGET_ID,
                    intent.getIntExtra(WidgetWorker.EXTRA_WIDGET_ID, 0))
        }
        val widgetWorkerRequest = OneTimeWorkRequestBuilder<WidgetWorker>()
                .addTag(when (intent.action) {
                    WidgetWorker.ACTION_REFRESH_MULTIPLE -> WidgetWorker.TASK_MULTIPLE_TAG
                    WidgetWorker.ACTION_REFRESH_ONE -> WidgetWorker.TASK_ONE_TAG
                    else -> WidgetWorker.TASK_ALL_TAG
                })
                .setInputData(workerDataBuilder.build())
                .build()
        WorkManager.getInstance().enqueue(widgetWorkerRequest)
    }
}
