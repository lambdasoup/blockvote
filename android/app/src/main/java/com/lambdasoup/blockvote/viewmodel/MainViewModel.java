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

public class MainViewModel extends ViewModel {

	private OkHttpClient httpClient;
	private String url;

	private final MutableLiveData<TransientData> data = new MutableLiveData<>();

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
			public void onFailure(Call call, IOException e) {
				data.postValue(new TransientData(TransientData.State.ERROR, null));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					data.postValue(new TransientData(TransientData.State.ERROR, null));
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

				Data data = new Data(start, end, sw1d, sw7d, sw30d, bu1d, bu7d, bu30d);
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

		private TransientData(State state, @Nullable Data data) {
			this.state = state;
			this.data = data;
		}

		public enum State {
			PROGRESS, DATA, ERROR
		}

		public final State state;

		@Nullable
		public final Data data;
	}

	public static class Data {
		public final         Date    start;
		public final         Date    end;
		public final float[] sw1d;
		public final float[] sw7d;
		public final float[] sw30d;
		public final float[] bu1d;
		public final float[] bu7d;
		public final float[] bu30d;

		public Data(Date start, Date end, float[] sw1d, float[] sw7d, float[] sw30d, float[] bu1d, float[] bu7d, float[] bu30d) {
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
