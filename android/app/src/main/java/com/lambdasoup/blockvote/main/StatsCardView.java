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
import android.database.Cursor;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.lambdasoup.blockvote.R;
import com.lambdasoup.blockvote.base.data.CursorUtils;
import com.lambdasoup.blockvote.base.data.Stats;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StatsCardView extends CardView {

	private final TextView timeView;

	@Nullable
	private Cursor cursor;

	public StatsCardView(Context context) {
		this(context, null);
	}

	public StatsCardView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StatsCardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		LayoutInflater.from(context).inflate(R.layout.view_stats, this);
		timeView = (TextView) findViewById(R.id.time);

		if (isInEditMode()) {
			setEmpty(false);
			setCell(R.id.segwit_d1, 21.4f);
			setCell(R.id.segwit_d7, 20.2f);
			setCell(R.id.segwit_d30, 24.8f);
			setCell(R.id.unlimited_d1, 31.1f);
			setCell(R.id.unlimited_d7, 33.3f);
			setCell(R.id.unlimited_d30, 37.9f);
		}
	}

	private void setEmpty(boolean empty) {
		findViewById(R.id.empty).setVisibility(empty ? VISIBLE : GONE);
		findViewById(R.id.table).setVisibility(empty ? GONE : VISIBLE);
		timeView.setVisibility(empty ? GONE : VISIBLE);
	}

	private void setCell(@IdRes int viewId, float v) {
		String   formatted = getResources().getString(R.string.formatted, v * 100);
		TextView view      = (TextView) findViewById(viewId);
		view.setText(formatted);
	}

	void swapCursor(@Nullable Cursor cursor) {
		this.cursor = cursor;
		updateData();
	}

	private void updateData() {
		if (cursor == null || !cursor.moveToFirst()) {
			setEmpty(true);
			return;
		}
		setEmpty(false);

		cursor.moveToPrevious();
		while (cursor.moveToNext()) {
			setLastUpdated(CursorUtils.getString(cursor, Stats.TIME));

			switch (CursorUtils.getId(cursor)) {
				case SEGWIT:
					setCell(R.id.segwit_d1, CursorUtils.getFloat(cursor, Stats.D1));
					setCell(R.id.segwit_d7, CursorUtils.getFloat(cursor, Stats.D7));
					setCell(R.id.segwit_d30, CursorUtils.getFloat(cursor, Stats.D30));
					break;

				case UNLIMITED:
					setCell(R.id.unlimited_d1, CursorUtils.getFloat(cursor, Stats.D1));
					setCell(R.id.unlimited_d7, CursorUtils.getFloat(cursor, Stats.D7));
					setCell(R.id.unlimited_d30, CursorUtils.getFloat(cursor, Stats.D30));
					break;
			}
		}
	}

	private void setLastUpdated(String string) {
		// time comes in RFC3339 2006-01-02T15:04:05[...]
		// this is cheapo-parsing, since we ignore the timezone. fortunately, this is our server and we know
		// it'll be in UTC
		try {
			String           truncated = string.substring(0, 19);
			SimpleDateFormat sdf       = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date         parsed             = sdf.parse(truncated);
			CharSequence relativeTimeString = DateUtils.getRelativeTimeSpanString(parsed.getTime());
			timeView.setText(relativeTimeString);
		} catch (ParseException e) {
			throw new RuntimeException("cannot parse " + string);
		}
	}

	public void tick() {
		updateData();
	}
}
