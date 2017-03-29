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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class StringUtils {

	private StringUtils() {
		// hide constructor for utility classes
	}

	/**
	 * time comes in RFC3339 2006-01-02T15:04:05[...]
	 * this is cheapo-parsing, since we ignore the timezone. fortunately, we use our server and we know
	 * it'll be in UTC
	 */
	static Date parseRFC3339UTC(String s) {
		try {
			String           truncated = s.substring(0, 19);
			SimpleDateFormat sdf       = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			return sdf.parse(truncated);
		} catch (ParseException e) {
			throw new RuntimeException("cannot parse " + s);
		}
	}

}
