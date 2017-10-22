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

package com.lambdasoup.blockvote.base.data;

import android.database.Cursor;

import static java.lang.Enum.valueOf;

@SuppressWarnings("SameParameterValue")
public class CursorUtils {

	private CursorUtils() {
		// hide constructor
	}

	public static <T extends Enum<T>> T getEnum(Cursor cursor, String col, Class<T> cls) {
		String name = getString(cursor, col);
		return valueOf(cls, name);
	}

	public static float getFloat(Cursor cursor, String col) {
		return cursor.getFloat(cursor.getColumnIndex(col));
	}

	public static String getString(Cursor cursor, String col) {
		return cursor.getString(cursor.getColumnIndex(col));
	}
}
