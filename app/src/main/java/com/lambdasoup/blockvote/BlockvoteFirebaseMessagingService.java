package com.lambdasoup.blockvote;

import android.content.ContentValues;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class BlockvoteFirebaseMessagingService extends FirebaseMessagingService {

	private static final String TAG = BlockvoteFirebaseMessagingService.class.getName();

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);

		String from = remoteMessage.getFrom();
		switch (from) {
			case "/topics/v1":
				Map<String, String> data = remoteMessage.getData();
				process(data);
				break;
		}
	}

	private void process(Map<String, String> data) {
		String time  = data.get("time");
		String votes = data.get("votes");
		Log.d(TAG, "message received");

		try {
			JSONObject      jsonObject    = new JSONObject(votes);
			JSONObject      segwit        = jsonObject.getJSONObject("segwit");
			JSONObject      unlimited     = jsonObject.getJSONObject("unlimited");
			ContentValues   cvSegwit      = fromJson(segwit, Id.SEGWIT.name());
			ContentValues   cvUnlimited   = fromJson(unlimited, Id.UNLIMITED.name());
			ContentValues[] contentValues = {cvSegwit, cvUnlimited};
			getContentResolver().bulkInsert(StatsProvider.CONTENT_URI, contentValues);

		} catch (JSONException e) {
			throw new RuntimeException("could not parse FCM data: " + votes);
		}
	}

	private ContentValues fromJson(JSONObject jsonObject, String id) throws JSONException {
		ContentValues cv = new ContentValues();

		cv.put(Stats.ID, id);
		cv.put(Stats.D30, jsonObject.getDouble("d30"));
		cv.put(Stats.D7, jsonObject.getDouble("d7"));
		cv.put(Stats.D1, jsonObject.getDouble("d1"));

		return cv;
	}
}
