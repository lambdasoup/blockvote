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

package com.lambdasoup.blockvote.main;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lambdasoup.blockvote.BuildConfig;
import com.lambdasoup.blockvote.History;
import com.lambdasoup.blockvote.R;
import com.lambdasoup.blockvote.Stats;
import com.lambdasoup.blockvote.Vote;
import com.lambdasoup.blockvote.base.data.StatsProvider;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.net.Uri.parse;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private ScheduledFuture<?> tickFuture;
	private StatsCardView      statsView;
	private final Runnable tickTask = () -> runOnUiThread(this::onTick);
	private HistoryCardView historyView;

	private OkHttpClient httpClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// init google services
		GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
		FirebaseMessaging.getInstance().subscribeToTopic("v1");
		if (BuildConfig.DEBUG) {
			FirebaseMessaging.getInstance().subscribeToTopic("debug");
		}

		// build http client
		Cache cache = new Cache(getCacheDir(), 1024 * 1024);
		httpClient = new OkHttpClient.Builder().cache(cache).build();

		// bind views
		statsView = (StatsCardView) findViewById(R.id.stats);
		historyView = (HistoryCardView) findViewById(R.id.card_history);
		historyView.setOnRetryListener(this::onRetryHistory);

		// load stats
		getSupportLoaderManager().initLoader(0, null, this);
	}

	private void onRetryHistory() {
		loadHistory();
	}

	@Override
	protected void onStart() {
		super.onStart();

		// load history from network
		loadHistory();
	}

	private void loadHistory() {
		historyView.setProgress();

		String url = getString(R.string.backend_history_url);
		Request request = new Request.Builder()
				.url(url)
				.build();

		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				runOnUiThread(() -> onHistoryLoadError());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					runOnUiThread(() -> onHistoryLoadError());
					return;
				}

				History history = History.parseFrom(response.body().byteStream());

				int     count = history.getStatsCount();
				Date    start = StringUtils.parseRFC3339UTC(history.getStats(count - 1).getTime());
				Date    end   = StringUtils.parseRFC3339UTC(history.getStats(0).getTime());
				float[] sw1d  = new float[count];
				float[] sw7d  = new float[count];
				float[] sw30d = new float[count];
				float[] bu1d  = new float[count];
				float[] bu7d  = new float[count];
				float[] bu30d = new float[count];
				for (int i = 0; i < count; i++) {
					Stats stats = history.getStats(i);
					for (Map.Entry<String, Vote> entry : stats.getVotesMap().entrySet()) {
						// reverse order
						int pos = count - i - 1;
						switch (entry.getKey()) {
							case "unlimited":
								bu1d[pos] = entry.getValue().getD1();
								bu7d[pos] = entry.getValue().getD7();
								bu30d[pos] = entry.getValue().getD30();
								break;

							case "segwit":
								sw1d[pos] = entry.getValue().getD1();
								sw7d[pos] = entry.getValue().getD7();
								sw30d[pos] = entry.getValue().getD30();
								break;
						}
					}
				}

				runOnUiThread(() -> onHistoryLoaded(new HistoryView.Data(start, end, sw1d, sw7d, sw30d, bu1d, bu7d, bu30d)));
			}
		});
	}

	private void onHistoryLoaded(HistoryView.Data data) {
		if (isDestroyed()) {
			return;
		}

		historyView.setData(data);
	}

	private void onHistoryLoadError() {
		if (isDestroyed()) {
			return;
		}

		historyView.setError();
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
	protected void onDestroy() {
		httpClient.dispatcher().cancelAll();
		super.onDestroy();
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


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_attributions:
				startActivity(new Intent(this, AttributionActivity.class));
				return true;
			case R.id.menu_privacy:
				startActivity(new Intent(Intent.ACTION_VIEW, parse("https://lambdasoup.com/privacypolicy-blockvote/")));
				return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
