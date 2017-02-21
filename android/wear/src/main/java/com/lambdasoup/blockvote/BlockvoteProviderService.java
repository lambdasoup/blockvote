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

package com.lambdasoup.blockvote;

import android.database.Cursor;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;

import com.google.firebase.messaging.FirebaseMessaging;
import com.lambdasoup.blockvote.ConfigProvider.Config;

import java.util.Locale;

public class BlockvoteProviderService extends ComplicationProviderService {

	private static final String TAG = BlockvoteProviderService.class.getSimpleName();

	@Override
	public void onComplicationActivated(int complicationId, int type, ComplicationManager manager) {
		// register FCM topic
		FirebaseMessaging.getInstance().subscribeToTopic("v1");
		if (BuildConfig.DEBUG) {
			FirebaseMessaging.getInstance().subscribeToTopic("debug");
		}
	}

	@Override
	public void onComplicationUpdate(int id, int type, ComplicationManager complicationManager) {
		Cursor configCursor = getContentResolver().query(ConfigProvider.CONTENT_URI, null, Config.COMPLICATION_ID + " = " + id, null, null);
		if (configCursor == null || !configCursor.moveToFirst()) {
			ComplicationData data = new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
					.setMinValue(0f)
					.setMaxValue(1f)
					.setValue(0f)
					.setShortText(ComplicationText.plainText("--"))
					.setShortTitle(ComplicationText.plainText("--"))
					.build();
			complicationManager.updateComplicationData(id, data);
			return;
		}
		configCursor.close();

		switch (type) {
			case ComplicationData.TYPE_RANGED_VALUE:
				float value = .235f;
				ComplicationData data = new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
						.setMinValue(0f)
						.setMaxValue(1f)
						.setValue(value)
						.setShortText(ComplicationText.plainText(String.format(Locale.getDefault(), "%.1f%%", value * 100)))
						.setShortTitle(ComplicationText.plainText("SW"))
						.build();

				complicationManager.updateComplicationData(id, data);
				break;
		}
	}
}
