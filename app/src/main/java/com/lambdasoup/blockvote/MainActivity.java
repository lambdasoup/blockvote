package com.lambdasoup.blockvote;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	private StatsCardView statsView;
	private final Runnable tickTask = () -> runOnUiThread(this::onTick);
	private ScheduledFuture<?> tickFuture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FirebaseMessaging.getInstance().subscribeToTopic("v1");

		statsView = (StatsCardView) findViewById(R.id.stats);

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		tickFuture = executor.scheduleAtFixedRate(tickTask, 0, 1, TimeUnit.SECONDS);
	}

	@UiThread
	private void onTick() {
		statsView.tick();
	}

	@Override
	protected void onPause() {
		tickFuture.cancel(false);

		super.onPause();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = new CursorLoader(this);
		loader.setUri(StatsProvider.CONTENT_URI);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		statsView.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		statsView.swapCursor(null);
	}
}
