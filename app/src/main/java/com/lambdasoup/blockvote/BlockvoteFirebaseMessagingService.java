package com.lambdasoup.blockvote;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BlockvoteFirebaseMessagingService extends FirebaseMessagingService {

	private static final String TAG = BlockvoteFirebaseMessagingService.class.getName();

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);

		Log.d(TAG, "message received");
	}
}
