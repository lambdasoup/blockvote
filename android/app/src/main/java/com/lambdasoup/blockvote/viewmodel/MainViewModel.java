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

package com.lambdasoup.blockvote.viewmodel;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lambdasoup.blockvote.History;
import com.lambdasoup.blockvote.R;
import com.lambdasoup.blockvote.Stats;
import com.lambdasoup.blockvote.Vote;
import com.lambdasoup.blockvote.util.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainViewModel extends ViewModel {

	private final MutableLiveData<TransientData> data = new MutableLiveData<>();
	private OkHttpClient httpClient;
	private String url;

	public void init(Context context) {
		// build http client
		Cache cache = new Cache(context.getCacheDir(), 1024 * 1024);
		httpClient = new OkHttpClient.Builder().cache(cache).build();
		url = context.getString(R.string.backend_history_url);
	}

	public void loadHistory() {
		data.postValue(new TransientData(TransientData.State.PROGRESS, null));

		Request request = new Request.Builder()
				.url(url)
				.build();

		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e) {
				data.postValue(new TransientData(TransientData.State.ERROR, null));
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
				if (!response.isSuccessful()) {
					data.postValue(new TransientData(TransientData.State.ERROR, null));
					return;
				}

				ResponseBody body = response.body();
				if (body == null) {
					throw new IOException("cannot read from null body");
				}

				History history = History.parseFrom(body.byteStream());

				int     count  = history.getStatsCount();
				Date    start  = StringUtils.parseRFC3339UTC(history.getStats(count - 1).getTime());
				Date    end    = StringUtils.parseRFC3339UTC(history.getStats(0).getTime());
				float[] s2x1d  = new float[count];
				float[] s2x7d  = new float[count];
				float[] s2x30d = new float[count];
				float[] ec1d   = new float[count];
				float[] ec7d   = new float[count];
				float[] ec30d  = new float[count];
				for (int i = 0; i < count; i++) {
					Stats stats = history.getStats(i);
					for (Map.Entry<String, Vote> entry : stats.getVotesMap().entrySet()) {
						// reverse order
						int pos = count - i - 1;
						switch (entry.getKey()) {
							case "ec":
								ec1d[pos] = entry.getValue().getD1();
								ec7d[pos] = entry.getValue().getD7();
								ec30d[pos] = entry.getValue().getD30();
								break;

							case "s2x":
								s2x1d[pos] = entry.getValue().getD1();
								s2x7d[pos] = entry.getValue().getD7();
								s2x30d[pos] = entry.getValue().getD30();
								break;
						}
					}
				}

				Data data = new Data(start, end, s2x1d, s2x7d, s2x30d, ec1d, ec7d, ec30d);
				MainViewModel.this.data.postValue(new TransientData(TransientData.State.DATA, data));
			}
		});
	}

	@Override
	protected void onCleared() {
		httpClient.dispatcher().cancelAll();
		super.onCleared();
	}

	public void observeData(LifecycleOwner owner, Observer<TransientData> observer) {
		data.observe(owner, observer);
	}

	public static class TransientData {

		public final State state;
		@Nullable
		public final Data  data;

		private TransientData(State state, @Nullable Data data) {
			this.state = state;
			this.data = data;
		}

		public enum State {
			PROGRESS, DATA, ERROR
		}
	}

	public static class Data {
		public final Date    start;
		public final Date    end;
		public final float[] s2x1d;
		public final float[] s2x7d;
		public final float[] s2x30d;
		public final float[] ec1d;
		public final float[] ec7d;
		public final float[] ec30d;

		public Data(Date start, Date end, float[] s2x1d, float[] s2x7d, float[] s2x30d, float[] ec1d, float[] ec7d, float[] ec30d) {
			this.start = start;
			this.end = end;
			this.s2x1d = s2x1d;
			this.s2x7d = s2x7d;
			this.s2x30d = s2x30d;
			this.ec1d = ec1d;
			this.ec7d = ec7d;
			this.ec30d = ec30d;
		}
	}

}
