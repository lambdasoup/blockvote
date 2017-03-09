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

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.lambdasoup.blockvote.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class HistoryCardView extends CardView implements View.OnClickListener {

	/* match to layout ids */
	private final static int CHILD_PROGRESS = 0;
	private final static int CHILD_DATA     = 1;
	private final static int CHILD_ERROR    = 2;
	private final HistoryView     historyView;
	private final ViewAnimator    animatorView;
	private final TextView        intervalView;
	private       OnRetryListener listener;

	public HistoryCardView(Context context) {
		this(context, null);
	}

	public HistoryCardView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HistoryCardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		LayoutInflater.from(context).inflate(R.layout.card_history, this);

		historyView = (HistoryView) findViewById(R.id.history);
		animatorView = (ViewAnimator) findViewById(R.id.animator);
		intervalView = (TextView) findViewById(R.id.interval);
		findViewById(R.id.btn_retry).setOnClickListener(this);
	}

	public void setProgress() {
		intervalView.setVisibility(INVISIBLE);
		show(CHILD_PROGRESS);
	}

	public void setError() {
		intervalView.setVisibility(INVISIBLE);
		show(CHILD_ERROR);
	}

	public void setData(HistoryView.Data data) {
		intervalView.setVisibility(VISIBLE);
		String formattedStart    = SimpleDateFormat.getDateInstance().format(data.start);
		String formattedEnd      = SimpleDateFormat.getDateInstance().format(data.end);
		String formattedInterval = String.format(Locale.ENGLISH, "%s - %s", formattedStart, formattedEnd);
		intervalView.setText(formattedInterval);

		historyView.setData(data);
		show(CHILD_DATA);
	}

	private void show(int child) {
		animatorView.setDisplayedChild(child);
	}

	void setOnRetryListener(OnRetryListener listener) {
		this.listener = listener;
	}

	@Override
	public void onClick(View v) {
		if (listener == null) {
			return;
		}

		listener.onRetry();
	}

	interface OnRetryListener {
		void onRetry();
	}
}
