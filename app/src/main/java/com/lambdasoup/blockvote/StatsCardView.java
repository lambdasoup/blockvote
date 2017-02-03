package com.lambdasoup.blockvote;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

public class StatsCardView extends CardView {

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

	private static Id getId(Cursor cursor) {
		String name = cursor.getString(cursor.getColumnIndex(Stats.ID));
		return Id.valueOf(name);
	}

	private static float getFloat(Cursor cursor, String col) {
		return cursor.getFloat(cursor.getColumnIndex(col));
	}

	private void setEmpty(boolean empty) {
		findViewById(R.id.empty).setVisibility(empty ? VISIBLE : GONE);
		findViewById(R.id.table).setVisibility(empty ? GONE : VISIBLE);
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
			switch (getId(cursor)) {
				case SEGWIT:
					setCell(R.id.segwit_d1, getFloat(cursor, Stats.D1));
					setCell(R.id.segwit_d7, getFloat(cursor, Stats.D7));
					setCell(R.id.segwit_d30, getFloat(cursor, Stats.D30));
					break;

				case UNLIMITED:
					setCell(R.id.unlimited_d1, getFloat(cursor, Stats.D1));
					setCell(R.id.unlimited_d7, getFloat(cursor, Stats.D7));
					setCell(R.id.unlimited_d30, getFloat(cursor, Stats.D30));
					break;
			}
		}
	}
}
