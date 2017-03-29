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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.lambdasoup.blockvote.R;

import java.util.Date;
import java.util.Locale;

/**
 * local variable warnings suppressed for draw-loop optimization reasons
 */
@SuppressWarnings("FieldCanBeLocal")
public class HistoryView extends View {

	// axes and marks
	private final TextPaint xLabelTextPaint;
	private final TextPaint yLabelTextPaint;
	private final Paint     axisPaint;
	private final Paint     marksPaint;
	private final Path      axisPath;
	private final Path      marksPath;

	/* data paths */
	private final Path sw1dPath  = new Path();
	private final Path sw7dPath  = new Path();
	private final Path sw30dPath = new Path();
	private final Path bu1dPath  = new Path();
	private final Path bu7dPath  = new Path();
	private final Path bu30dPath = new Path();

	/* data paints */
	private final Paint sw1dPaint;
	private final Paint sw7dPaint;
	private final Paint sw30dPaint;
	private final Paint bu1dPaint;
	private final Paint bu7dPaint;
	private final Paint bu30dPaint;

	private final Matrix chartMatrix = new Matrix();
	private final int labelHeight;
	private final int padding;
	private final RectF src = new RectF();
	private final RectF dst = new RectF();
	private final int  strokeWidth;
	private       Data data;
	private float labelWidth = 0f;
	private int chartHeight;

	public HistoryView(Context context) {
		this(context, null);
	}

	public HistoryView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HistoryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		padding = getResources().getDimensionPixelSize(R.dimen.history_padding);
		labelHeight = getResources().getDimensionPixelSize(R.dimen.history_label);
		strokeWidth = getResources().getDimensionPixelSize(R.dimen.history_strokewidth);

		sw1dPaint = makePaint(Option.SW, Interval.D1, strokeWidth);
		sw7dPaint = makePaint(Option.SW, Interval.D7, strokeWidth);
		sw30dPaint = makePaint(Option.SW, Interval.D30, strokeWidth);
		bu1dPaint = makePaint(Option.BU, Interval.D1, strokeWidth);
		bu7dPaint = makePaint(Option.BU, Interval.D7, strokeWidth);
		bu30dPaint = makePaint(Option.BU, Interval.D30, strokeWidth);

		// axes
		axisPaint = new Paint();
		axisPaint.setStrokeWidth(3.0f);
		axisPaint.setColor(Color.DKGRAY);
		axisPaint.setStyle(Paint.Style.STROKE);
		axisPath = new Path();

		// marks
		marksPaint = new Paint();
		marksPaint.setStrokeWidth(3.0f);
		marksPaint.setColor(Color.LTGRAY);
		marksPaint.setStyle(Paint.Style.STROKE);
		marksPath = new Path();

		// labels
		yLabelTextPaint = new TextPaint();
		yLabelTextPaint.setColor(Color.GRAY);
		yLabelTextPaint.setTextSize(labelHeight);
		yLabelTextPaint.setAntiAlias(true);
		yLabelTextPaint.setTextAlign(Paint.Align.RIGHT);
		xLabelTextPaint = new TextPaint();
		xLabelTextPaint.setColor(Color.GRAY);
		xLabelTextPaint.setTextSize(labelHeight);
		xLabelTextPaint.setAntiAlias(true);
		xLabelTextPaint.setTextAlign(Paint.Align.CENTER);

		labelWidth = Layout.getDesiredWidth("100%", yLabelTextPaint);

		if (isInEditMode()) {
			setData(new Data(
					new Date(), new Date(), new float[]{0.1f, 0.2f, 0.15f, 0.18f},
							new float[]{0.3f, 0.5f, 0.1f, 0.5f},
							new float[]{0.3f, 0.5f, 0.1f, 0.5f},
							new float[]{0.3f, 0.5f, 0.1f, 0.5f},
							new float[]{0.3f, 0.5f, 0.1f, 0.5f},
							new float[]{0.3f, 0.5f, 0.1f, 0.5f}
					)
			);
		}
	}

	private static Paint makePaint(Option option, Interval interval, int strokeWidth) {
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		// 1d and 7d invisible until a nice way to display/configure is found
		int   alpha;
		switch (interval) {
			case D1:
				alpha = 0;
				break;
			case D7:
				alpha = 0;
				break;
			case D30:
				alpha = 200;
				break;
			default:
				throw new IllegalArgumentException();
		}
		paint.setStrokeWidth(strokeWidth);

		int color;
		switch (option) {
			case SW:
				color = Color.argb(alpha, 255, 0, 0);
				break;
			case BU:
				color = Color.argb(alpha, 0, 0, 255);
				break;
			default:
				throw new IllegalArgumentException();
		}
		paint.setColor(color);

		return paint;
	}

	private static void calcPath(Path path, float[] ys, Matrix m) {
		path.reset();
		int xs = ys.length;
		for (int x = 0; x < xs; x++) {
			if (x == 0) {
				path.moveTo(x / (float) (xs - 1), 1 - ys[x]);
				continue;
			}
			path.lineTo(x / (float) (xs - 1), 1 - ys[x]);
		}
		path.transform(m);
	}

	public void setData(Data data) {
		this.data = data;
		calcPaths();
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width  = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		chartHeight = height - labelHeight;

		// draw chart
		src.set(0, 0, 1, 1);
		dst.set(labelWidth + padding, labelHeight / 2, width, chartHeight - padding - labelHeight / 2);
		chartMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);

		axisPath.reset();
		axisPath.moveTo(0, 0);
		axisPath.lineTo(0, 1);
		axisPath.lineTo(1, 1);
		axisPath.transform(chartMatrix);

		marksPath.reset();
		marksPath.moveTo(0, 0);
		marksPath.lineTo(1, 0);
		marksPath.moveTo(0, .25f);
		marksPath.lineTo(1, .25f);
		marksPath.moveTo(0, .5f);
		marksPath.lineTo(1, .5f);
		marksPath.moveTo(0, .75f);
		marksPath.lineTo(1, .75f);
		marksPath.transform(chartMatrix);

		calcPaths();

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private void calcPaths() {
		if (data == null) {
			return;
		}

		calcPath(sw1dPath, data.sw1d, chartMatrix);
		calcPath(sw7dPath, data.sw7d, chartMatrix);
		calcPath(sw30dPath, data.sw30d, chartMatrix);
		calcPath(bu1dPath, data.bu1d, chartMatrix);
		calcPath(bu7dPath, data.bu7d, chartMatrix);
		calcPath(bu30dPath, data.bu30d, chartMatrix);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawPath(marksPath, marksPaint);
		canvas.drawPath(axisPath, axisPaint);

		if (data != null) {
			canvas.drawPath(sw1dPath, sw1dPaint);
			canvas.drawPath(sw7dPath, sw7dPaint);
			canvas.drawPath(sw30dPath, sw30dPaint);
			canvas.drawPath(bu1dPath, bu1dPaint);
			canvas.drawPath(bu7dPath, bu7dPaint);
			canvas.drawPath(bu30dPath, bu30dPaint);
		}

		// y-labels
		for (int i = 4; i >= 0; i--) {
			canvas.drawText(String.format(Locale.getDefault(), "%d%%", i * 25), labelWidth, labelHeight + (1.5f * labelHeight + chartHeight) - (i + 1) * (1.5f * labelHeight + chartHeight) / 5, yLabelTextPaint);
		}
	}

	private enum Option {
		SW, BU
	}

	private enum Interval {
		D1, D7, D30
	}

	static class Data {
		final         Date    start;
		final         Date    end;
		private final float[] sw1d;
		private final float[] sw7d;
		private final float[] sw30d;
		private final float[] bu1d;
		private final float[] bu7d;
		private final float[] bu30d;

		Data(Date start, Date end, float[] sw1d, float[] sw7d, float[] sw30d, float[] bu1d, float[] bu7d, float[] bu30d) {
			this.start = start;
			this.end = end;
			this.sw1d = sw1d;
			this.sw7d = sw7d;
			this.sw30d = sw30d;
			this.bu1d = bu1d;
			this.bu7d = bu7d;
			this.bu30d = bu30d;
		}
	}
}
