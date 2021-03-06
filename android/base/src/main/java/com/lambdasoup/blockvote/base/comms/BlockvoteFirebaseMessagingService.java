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

package com.lambdasoup.blockvote.base.comms;

import android.content.ContentValues;
import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.lambdasoup.blockvote.base.data.Id;
import com.lambdasoup.blockvote.base.data.Stats;
import com.lambdasoup.blockvote.base.data.StatsProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class BlockvoteFirebaseMessagingService extends FirebaseMessagingService {

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);

		String from = remoteMessage.getFrom();
		switch (from) {
			case "/topics/v1":
			case "/topics/debug":
				Map<String, String> data = remoteMessage.getData();
				process(data);
				break;
		}
	}

	private void process(Map<String, String> data) {
		String time  = data.get("time");
		String votes = data.get("votes");

		try {
			JSONObject      jsonObject    = new JSONObject(votes);
			JSONObject      s2x           = jsonObject.getJSONObject("s2x");
			JSONObject      ec            = jsonObject.getJSONObject("ec");
			ContentValues   cvS2x         = fromJson(s2x, Id.S2X.name(), time);
			ContentValues   cvEC          = fromJson(ec, Id.EC.name(), time);
			ContentValues[] contentValues = {cvS2x, cvEC};
			getContentResolver().bulkInsert(StatsProvider.CONTENT_URI, contentValues);

		} catch (JSONException e) {
			throw new RuntimeException("could not parse FCM data: " + votes);
		}

		// tell service to update all widgets
		Intent intent = new Intent("com.lambdasoup.blockvote.UPDATE");
		intent.setPackage(getPackageName());
		sendBroadcast(intent);
	}

	private ContentValues fromJson(JSONObject jsonObject, String id, String timestamp) throws JSONException {
		ContentValues cv = new ContentValues();

		cv.put(Stats.ID, id);
		cv.put(Stats.D30, jsonObject.getDouble("d30"));
		cv.put(Stats.D7, jsonObject.getDouble("d7"));
		cv.put(Stats.D1, jsonObject.getDouble("d1"));
		cv.put(Stats.TIME, timestamp);

		return cv;
	}
}
