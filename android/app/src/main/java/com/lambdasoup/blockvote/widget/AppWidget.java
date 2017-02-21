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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.lambdasoup.blockvote.R;
import com.lambdasoup.blockvote.main.MainActivity;

public class AppWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			RemoteViews   views         = new RemoteViews(context.getPackageName(), R.layout.app_widget_empty);
			Intent        intent        = new Intent(context, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.appwidget_root, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}

		// tell service to update all widgets
		Intent intent = new Intent(context, AppWidgetUpdateService.class);
		context.startService(intent);
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}
}

