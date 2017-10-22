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

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.wearable.complications.ComplicationProviderService;
import android.view.View;
import android.widget.Button;

import com.lambdasoup.blockvote.ConfigProvider.Config;
import com.lambdasoup.blockvote.ConfigProvider.Period;
import com.lambdasoup.blockvote.base.data.Id;

public class ConfigActivity extends Activity {

	private final static String TAG = ConfigActivity.class.getSimpleName();

	/* keep in sync with R.array.* */
	private final static Id[]     candidates = {Id.SEGWIT, Id.EC};
	private final static Period[] periods    = {Period.D1, Period.D7, Period.D30};

	private final ContentValues config = new ContentValues();

	/* views */
	private FloatingActionButton fab;
	private Button               candidateView;
	private Button               periodView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);

		fab = findViewById(R.id.confirm);
		periodView = findViewById(R.id.period);
		candidateView = findViewById(R.id.candidate);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			throw new NullPointerException("extras were null");
		}

		// complication id comes from the intent
		int complicationId = extras.getInt(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID);
		config.put(Config.COMPLICATION_ID, complicationId);

		candidateView.setOnClickListener(this::onCandidateClicked);
		periodView.setOnClickListener(this::onPeriodClicked);
		fab.setOnClickListener(this::onConfirmed);
	}

	private void onConfirmed(View view) {
		getContentResolver().insert(ConfigProvider.CONTENT_URI, config);
		setResult(RESULT_OK);
		finish();
	}

	private void onPeriodClicked(View view) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_period)
				.setItems(R.array.periods, this::onPeriodChosen)
				.show();
	}

	private void onCandidateClicked(View view) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_candidate)
				.setItems(R.array.candidates, this::onCandidateChosen)
				.show();
	}

	private void onCandidateChosen(DialogInterface dialogInterface, int which) {
		setId(candidates[which]);
	}

	private void setId(Id id) {
		config.put(Config.CANDIDATE, id.name());

		switch (id) {
			case SEGWIT:
				candidateView.setText(R.string.segwit_long);
				break;
			case EC:
				candidateView.setText(R.string.ec_long);
				break;
		}

		updateFab();
	}

	private void updateFab() {
		if (config.containsKey(Config.CANDIDATE) && config.containsKey(Config.PERIOD)) {
			fab.show();
		}
	}

	private void onPeriodChosen(DialogInterface dialogInterface, int which) {
		setPeriod(periods[which]);
	}

	private void setPeriod(Period period) {
		config.put(Config.PERIOD, period.name());

		switch (period) {
			case D1:
				periodView.setText(R.string.period_d1);
				break;
			case D7:
				periodView.setText(R.string.period_d7);
				break;
			case D30:
				periodView.setText(R.string.period_d30);
				break;
		}

		updateFab();
	}
}
