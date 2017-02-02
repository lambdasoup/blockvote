package com.lambdasoup.blockvote;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
	}
}
