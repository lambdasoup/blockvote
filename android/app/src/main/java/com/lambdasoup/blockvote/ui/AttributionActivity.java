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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lambdasoup.blockvote.R;

public class AttributionActivity extends AppCompatActivity {

	private final RecyclerView.Adapter<AttributionViewHolder> adapter = new RecyclerView.Adapter<AttributionViewHolder>() {

		@Override
		public AttributionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_attribution, parent, false);
			return new AttributionViewHolder(view);
		}

		@Override
		public void onBindViewHolder(AttributionViewHolder holder, int position) {
			String title, text;

			Attribution attribution = Attribution.values()[position];
			switch (attribution) {
				case BLOCKVOTE:
					title = getString(R.string.license_title_blockvote);
					text = getString(R.string.license_text_blockvote);
					break;

				case SUPPORTLIB:
					title = getString(R.string.license_title_supportlib);
					text = getString(R.string.license_text_supportlib);
					break;

				default:
					throw new IllegalArgumentException("unknown attribution key: " + attribution.name());
			}

			holder.title.setText(title);
			holder.text.setText(text);
		}

		@Override
		public int getItemCount() {
			return Attribution.values().length;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_attribution);

		RecyclerView recyclerView = findViewById(R.id.list);
		recyclerView.setAdapter(adapter);
	}

	private enum Attribution {
		BLOCKVOTE,
		SUPPORTLIB,
	}

	private static class AttributionViewHolder extends RecyclerView.ViewHolder {

		private final TextView title;
		private final TextView text;

		AttributionViewHolder(View itemView) {
			super(itemView);

			title = itemView.findViewById(R.id.title);
			text = itemView.findViewById(R.id.text);
		}
	}
}
