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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class Ticker implements LifecycleObserver {

	/* ticker */
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private ScheduledFuture<?> tickFuture;
	private final TickListener listener;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable tickTask = () -> handler.post(new Runnable() {
		@Override
		public void run() {
			listener.onTick();
		}
	});

	private Ticker(LifecycleOwner owner, TickListener listener) {
		owner.getLifecycle().addObserver(this);
		this.listener = listener;
	}

	static TickerSender with(LifecycleOwner owner) {
		return new TickerSender(owner);
	}

	static class TickerSender {

		private final LifecycleOwner owner;

		private TickerSender(LifecycleOwner owner) {
			this.owner = owner;
		}

		void bind(TickListener listener) {
			new Ticker(owner, listener);
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	void start() {
		tickFuture = executor.scheduleAtFixedRate(tickTask, 0, 1, TimeUnit.SECONDS);
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	void stop() {
		tickFuture.cancel(false);
	}

	interface TickListener {
		void onTick();
	}
}
