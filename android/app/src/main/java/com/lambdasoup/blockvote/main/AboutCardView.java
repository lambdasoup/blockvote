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
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.lambdasoup.blockvote.R;

import static android.net.Uri.parse;

public class AboutCardView extends CardView {

	public AboutCardView(Context context) {
		this(context, null);
	}

	public AboutCardView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AboutCardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		LayoutInflater.from(context).inflate(R.layout.view_about, this);

		findViewById(R.id.about_button).setOnClickListener(this::onButtonClicked);
	}

	@SuppressWarnings("UnusedParameters")
	private void onButtonClicked(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW, parse("https://lambdasoup.github.io/blockvote/"));
		getContext().startActivity(intent);
	}
}
