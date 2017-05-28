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

package com.lambdasoup.blockvote.ui;

import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lambdasoup.blockvote.BuildConfig;
import com.lambdasoup.blockvote.R;
import com.lambdasoup.blockvote.base.data.StatsProvider;
import com.lambdasoup.blockvote.viewmodel.MainViewModel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.net.Uri.parse;

public class MainActivity extends LifecycleActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	/* views */
	private HistoryCardView historyView;
	private StatsCardView      statsView;

	/* viewmodel */
	private MainViewModel viewModel;

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

		// bind views
		statsView = findViewById(R.id.stats);
		historyView = findViewById(R.id.card_history);
		historyView.setOnRetryListener(this::onRetryHistory);

		// get viewmodel
		viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
		// TODO get rid of this line?
		viewModel.init(this);
		viewModel.observeData(this, this::onHistoryUpdated);
		viewModel.loadHistory();

		// bind ticker
		Ticker.with(this).bind(this::onTick);

		// load stats
		getSupportLoaderManager().initLoader(0, null, this);
	}

	private void onHistoryUpdated(MainViewModel.TransientData data) {
		switch (data.state) {
			case PROGRESS:
				historyView.setProgress();
				break;
			case DATA:
				historyView.setData(data.data);
				break;
			case ERROR:
				historyView.setProgress();
				break;
		}
	}

	private void onRetryHistory() {
		viewModel.loadHistory();
	}

	private void onTick() {
		statsView.tick();
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
