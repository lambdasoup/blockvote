/*
 * Copyright 2017 mh@lambdasoup.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lambdasoup.blockvote.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.widget.RemoteViews;

import com.lambdasoup.blockvote.R;
import com.lambdasoup.blockvote.base.data.Id;
import com.lambdasoup.blockvote.base.data.Stats;
import com.lambdasoup.blockvote.base.data.StatsProvider;
import com.lambdasoup.blockvote.ui.MainActivity;

import static com.lambdasoup.blockvote.base.data.CursorUtils.getEnum;
import static com.lambdasoup.blockvote.base.data.CursorUtils.getFloat;
import static java.lang.String.format;

public class AppWidgetUpdateService extends JobIntentService {

	@Override
	protected void onHandleWork(@NonNull Intent intent) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		int[]            ids              = appWidgetManager.getAppWidgetIds(new ComponentName(this, AppWidget.class));
		RemoteViews      views            = new RemoteViews(getPackageName(), R.layout.app_widget);

		try (Cursor cursor = getContentResolver().query(StatsProvider.CONTENT_URI, null, null, null, null)) {
			if (cursor == null || cursor.getCount() <= 0) {
				return;
			}


			cursor.moveToPrevious();
			while (cursor.moveToNext()) {
				switch (getEnum(cursor, Stats.ID, Id.class)) {
					case S2X:
						setCell(views, R.id.appwidget_value_sw, getFloat(cursor, Stats.D1));
						break;

					case EC:
						setCell(views, R.id.appwidget_value_ec, getFloat(cursor, Stats.D1));
						break;
				}
			}
		}

		Intent        mainIntent    = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);
		views.setOnClickPendingIntent(R.id.appwidget_root, pendingIntent);
		appWidgetManager.updateAppWidget(ids, views);
	}

	@SuppressLint("DefaultLocale")
	private void setCell(RemoteViews views, @IdRes int idRes, float value) {
		views.setTextViewText(idRes, format("%.1f%%", value * 100));
	}
}
