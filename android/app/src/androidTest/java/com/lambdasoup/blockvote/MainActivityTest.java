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

import android.content.ContentValues;
import android.support.annotation.IdRes;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.lambdasoup.blockvote.base.data.Id;
import com.lambdasoup.blockvote.base.data.Stats;
import com.lambdasoup.blockvote.base.data.StatsProvider;
import com.lambdasoup.blockvote.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

	private final Page                           page             = new Page();
	@Rule
	public        ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

	@Test
	public void shows_empty_state() throws Exception {
		// clear db
		activityTestRule.getActivity().getContentResolver().delete(StatsProvider.CONTENT_URI, null, null);

		page.assertEmptyState();
	}

	@Test
	public void shows_data() throws Exception {
		// insert db data
		ContentValues cvSegwit = new ContentValues();
		cvSegwit.put(Stats.ID, Id.SEGWIT.name());
		cvSegwit.put(Stats.D1, .312f);
		cvSegwit.put(Stats.D7, .422f);
		cvSegwit.put(Stats.D30, .522f);
		ContentValues cvUnlimited = new ContentValues();
		cvUnlimited.put(Stats.ID, Id.UNLIMITED.name());
		cvUnlimited.put(Stats.D1, .712f);
		cvUnlimited.put(Stats.D7, .822f);
		cvUnlimited.put(Stats.D30, .922f);
		ContentValues[] cvs = new ContentValues[]{cvSegwit, cvUnlimited};
		activityTestRule.getActivity().getContentResolver().bulkInsert(StatsProvider.CONTENT_URI, cvs);

		page.assertCellValue(R.id.segwit_d1, cvSegwit.getAsFloat(Stats.D1));
		page.assertCellValue(R.id.segwit_d7, cvSegwit.getAsFloat(Stats.D7));
		page.assertCellValue(R.id.segwit_d30, cvSegwit.getAsFloat(Stats.D30));
		page.assertCellValue(R.id.unlimited_d1, cvUnlimited.getAsFloat(Stats.D1));
		page.assertCellValue(R.id.unlimited_d7, cvUnlimited.getAsFloat(Stats.D7));
		page.assertCellValue(R.id.unlimited_d30, cvUnlimited.getAsFloat(Stats.D30));
	}

	private class Page {

		void assertEmptyState() {
			onView(withText(R.string.info_nodata)).check(matches(isDisplayed()));
		}

		void assertCellValue(@IdRes int viewId, float v) {
			String formatted = activityTestRule.getActivity().getString(R.string.formatted, v * 100);
			onView(allOf(withId(viewId), withText(formatted))).check(matches(isDisplayed()));
		}
	}
}
